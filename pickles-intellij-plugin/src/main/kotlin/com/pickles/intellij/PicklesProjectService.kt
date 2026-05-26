package com.pickles.intellij

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.io.IOException
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import javax.swing.SwingUtilities

@Service(Service.Level.PROJECT)
class PicklesProjectService(private val project: Project) : Disposable {
    private val gson: Gson = GsonBuilder().serializeNulls().setPrettyPrinting().create()
    private val listeners = CopyOnWriteArrayList<() -> Unit>()
    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "Pickles Project Service").apply { isDaemon = true }
    }
    private val problemBoard = PicklesProblemBoardState()
    private val indexGate = PicklesWorkspaceIndexGate()

    @Volatile
    private var httpServer: HttpServer? = null

    @Volatile
    private var runtimeClient: PicklesRuntimeClient? = null

    @Volatile
    private var httpServerStatus: PicklesHttpServerStatus = PicklesHttpServerStatus.STOPPED

    @Volatile
    private var runtimeStatus: PicklesRuntimeStatus = PicklesRuntimeStatus.UNKNOWN

    @Volatile
    private var indexStatus: PicklesIndexStatus = PicklesIndexStatus.IDLE

    @Volatile
    var lastStatus: String = "Pickles is idle."
        private set

    val projectRoot: Path?
        get() = project.basePath?.let(Path::of)

    fun addListener(listener: () -> Unit): Disposable {
        listeners.add(listener)
        return Disposable { listeners.remove(listener) }
    }

    fun loadConfigText(): Result<String> = runCatching {
        val configFile = configPath()
        if (!Files.exists(configFile)) {
            return Result.success(defaultConfigText())
        }
        Files.readString(configFile, StandardCharsets.UTF_8)
    }

    fun saveConfigText(configText: String): Result<Unit> = runCatching {
        val configFile = configPath()
        Files.writeString(configFile, configText.trimEnd() + "\n", StandardCharsets.UTF_8)
        updateStatus("Configuration saved.")
    }

    fun bindStatus(): BindStatus {
        val root = requireProjectRoot()
        val agentsPath = root.resolve(AGENTS_FILE)
        return BindStatus(
            agentsBlockBound = Files.exists(agentsPath) && PicklesAgentsBinding.isBound(Files.readString(agentsPath, StandardCharsets.UTF_8)),
            hooksFileExists = Files.exists(root.resolve(".codex").resolve("hooks.json")),
        )
    }

    fun bind(): Result<Unit> = runCatching {
        val root = requireProjectRoot()
        val agentsPath = root.resolve(AGENTS_FILE)
        val hooksPath = root.resolve(".codex").resolve("hooks.json")

        Files.createDirectories(agentsPath.parent ?: root)
        val agentsContent = if (Files.exists(agentsPath)) Files.readString(agentsPath, StandardCharsets.UTF_8) else null
        Files.writeString(agentsPath, PicklesAgentsBinding.bind(agentsContent), StandardCharsets.UTF_8)

        Files.createDirectories(hooksPath.parent)
        if (!Files.exists(hooksPath)) {
            Files.writeString(hooksPath, "{\n}\n", StandardCharsets.UTF_8)
        }

        updateStatus("Project bound to Pickles.")
    }

    fun unbind(): Result<Unit> = runCatching {
        val root = requireProjectRoot()
        val agentsPath = root.resolve(AGENTS_FILE)
        if (Files.exists(agentsPath)) {
            val agentsContent = Files.readString(agentsPath, StandardCharsets.UTF_8)
            val updatedContent = PicklesAgentsBinding.unbind(agentsContent)
            if (updatedContent != null) {
                Files.writeString(agentsPath, updatedContent, StandardCharsets.UTF_8)
            }
        }
        updateStatus("Project unbound from Pickles.")
    }

    fun problems(): List<PicklesProblem> = problemBoard.problems()

    fun statusSnapshot(): PicklesServiceStatusSnapshot = PicklesServiceStatusSnapshot(
        httpServerStatus = httpServerStatus,
        runtimeStatus = runtimeStatus,
        indexStatus = indexStatus,
        problemSummary = problemBoard.summary(),
        message = lastStatus,
    )

    fun deleteProblem(problem: PicklesProblem) {
        problemBoard.deleteProblem(problem)
        updateStatus("Problem removed from current board.")
    }

    fun reindexWorkspace() {
        if (!indexGate.tryStart()) {
            updateStatus("Workspace indexing is already running.")
            return
        }

        executor.execute {
            try {
                runWorkspaceInspection()
            } finally {
                indexGate.finish()
            }
        }
    }

    fun openProblem(problem: PicklesProblem) {
        val file = problem.file ?: return
        val position = problem.position ?: return
        val root = requireProjectRoot()
        val filePath = root.resolve(file).normalize()
        val virtualFile: VirtualFile = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(filePath)
            ?: return showError("Cannot open $file: file does not exist.")

        ApplicationManager.getApplication().invokeLater {
            val line = (position.line - 1).coerceAtLeast(0)
            val column = (position.column - 1).coerceAtLeast(0)
            OpenFileDescriptor(project, virtualFile, line, column).navigate(true)
        }
    }

    fun startHttpServer() {
        if (httpServer != null || project.isDisposed) {
            return
        }
        executor.execute {
            runCatching {
                val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
                server.createContext("/health") { exchange -> handleHealth(exchange) }
                server.createContext("/notify") { exchange -> handleNotify(exchange) }
                server.createContext("/feedback") { exchange -> handleFeedback(exchange) }
                server.executor = executor
                server.start()
                httpServer = server
                httpServerStatus = PicklesHttpServerStatus.RUNNING
                writeServerFile(server.address.port)
                updateStatus("Local HTTP server started on port ${server.address.port}.")
                reindexWorkspace()
            }.onFailure {
                thisLogger().warn("Failed to start Pickles HTTP server", it)
                showError("Failed to start Pickles local HTTP server: ${it.message}")
            }
        }
    }

    override fun dispose() {
        runCatching { httpServer?.stop(0) }
        httpServer = null
        httpServerStatus = PicklesHttpServerStatus.STOPPED
        executor.shutdownNow()
    }

    private fun handleNotify(exchange: HttpExchange) {
        if (exchange.requestMethod != "POST") {
            respond(exchange, contractHandler().methodNotAllowed())
            return
        }
        val result = contractHandler().notify(readRequestBody(exchange))
        respond(exchange, result)
        if (result.status == 202) {
            updateStatus("Hook notification received.")
        }
    }

    private fun handleFeedback(exchange: HttpExchange) {
        if (exchange.requestMethod != "POST") {
            respond(exchange, contractHandler().methodNotAllowed())
            return
        }
        respond(exchange, contractHandler().feedback(readRequestBody(exchange)))
    }

    private fun handleHealth(exchange: HttpExchange) {
        if (exchange.requestMethod != "GET") {
            respond(exchange, contractHandler().methodNotAllowed())
            return
        }
        respond(exchange, contractHandler().health())
    }

    private fun readRequestBody(exchange: HttpExchange): String = exchange.requestBody.use { String(it.readBytes(), StandardCharsets.UTF_8) }

    private fun contractHandler(): PicklesHttpContractHandler {
        val client = runtimeClient()
        return PicklesHttpContractHandler(
            gson = gson,
            projectRoot = requireProjectRoot(),
            runtimeClient = client,
            problemBoard = if (client == null) null else problemBoard,
        )
    }

    private fun runtimeClient(): PicklesRuntimeClient? {
        runtimeClient?.let { return it }

        val root = projectRoot ?: return null
        val runtimeRoot = PicklesRuntimeLocator.find(root)
            ?: run {
                runtimeStatus = PicklesRuntimeStatus.UNAVAILABLE
                return null
            }
        val client = NodePicklesRuntimeClient(
            workspaceRoot = root,
            runtimeRoot = runtimeRoot,
            gson = gson,
        )
        runtimeClient = client
        runtimeStatus = PicklesRuntimeStatus.AVAILABLE
        return client
    }

    private fun runWorkspaceInspection() {
        indexStatus = PicklesIndexStatus.RUNNING
        updateStatus("Workspace indexing is running.")

        val client = runtimeClient()
        if (client == null) {
            indexStatus = PicklesIndexStatus.FAILED
            runtimeStatus = PicklesRuntimeStatus.UNAVAILABLE
            updateStatus("Pickles Runtime is unavailable.")
            return
        }

        runCatching {
            PicklesWorkspaceInspection.inspect(
                workspaceRoot = requireProjectRoot(),
                runtimeClient = client,
                problemBoard = problemBoard,
            )
        }.onSuccess { problems ->
            runtimeStatus = PicklesRuntimeStatus.AVAILABLE
            indexStatus = PicklesIndexStatus.SUCCEEDED
            updateStatus("Workspace indexing completed with ${problems.size} problem(s).")
        }.onFailure { failure ->
            runtimeStatus = PicklesRuntimeStatus.UNAVAILABLE
            indexStatus = PicklesIndexStatus.FAILED
            updateStatus(failure.message ?: "Pickles Runtime failed.")
        }
    }

    private fun respond(exchange: HttpExchange, result: PicklesHttpResult) {
        respond(exchange, result.status, gson.toJson(result.body))
    }

    private fun respond(exchange: HttpExchange, status: Int, body: String) {
        val bytes = body.toByteArray(StandardCharsets.UTF_8)
        exchange.responseHeaders.add("Content-Type", "application/json; charset=utf-8")
        exchange.sendResponseHeaders(status, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
    }

    private fun writeServerFile(port: Int) {
        val serverFile = requireProjectRoot().resolve(".pickles").resolve("server.json")
        Files.createDirectories(serverFile.parent)
        Files.writeString(serverFile, gson.toJson(mapOf("port" to port)) + "\n", StandardCharsets.UTF_8)
    }

    private fun configPath(): Path = requireProjectRoot().resolve("pickles.config.ts")

    private fun requireProjectRoot(): Path = projectRoot ?: throw IOException("Project root is unavailable.")

    private fun updateStatus(message: String) {
        lastStatus = message
        SwingUtilities.invokeLater {
            listeners.forEach { it.invoke() }
        }
    }

    private fun showError(message: String) {
        updateStatus(message)
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Pickles")
            ?.createNotification(message, NotificationType.ERROR)
            ?.notify(project)
    }

    fun defaultConfigText(): String = """
        import { defineConfig } from "@pickles/runtime/config";

        export default defineConfig({
            agent: "codex",
            hook: {
                protocol: "http",
            },
            rules: [],
            problemBoard: {
                aggregation: "workspace",
            },
        });
    """.trimIndent()

    private companion object {
        const val AGENTS_FILE = "AGENTS.md"
    }
}

fun Project.picklesService(): PicklesProjectService = service()
