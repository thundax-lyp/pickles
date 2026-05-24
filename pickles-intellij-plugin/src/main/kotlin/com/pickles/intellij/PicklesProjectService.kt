package com.pickles.intellij

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
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
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val listeners = CopyOnWriteArrayList<() -> Unit>()
    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "Pickles Project Service").apply { isDaemon = true }
    }

    @Volatile
    private var httpServer: HttpServer? = null

    @Volatile
    private var currentProblems: List<PicklesProblem> = emptyList()

    @Volatile
    var lastStatus: String = "Pickles is idle."
        private set

    val projectRoot: Path?
        get() = project.basePath?.let(Path::of)

    fun addListener(listener: () -> Unit): Disposable {
        listeners.add(listener)
        return Disposable { listeners.remove(listener) }
    }

    fun loadConfig(): Result<PicklesConfig> = runCatching {
        val configFile = configPath()
        if (!Files.exists(configFile)) {
            return Result.success(PicklesConfig())
        }
        Files.newBufferedReader(configFile, StandardCharsets.UTF_8).use { reader ->
            gson.fromJson(reader, PicklesConfig::class.java) ?: PicklesConfig()
        }
    }

    fun saveConfig(config: PicklesConfig): Result<Unit> = runCatching {
        val configFile = configPath()
        Files.createDirectories(configFile.parent)
        Files.writeString(configFile, gson.toJson(config) + "\n", StandardCharsets.UTF_8)
        updateStatus("Configuration saved.")
    }

    fun bindStatus(config: PicklesConfig = loadConfig().getOrDefault(PicklesConfig())): BindStatus {
        val root = requireProjectRoot()
        return BindStatus(
            configEnabled = config.bind.enabled,
            agentsFileExists = Files.exists(root.resolve(config.bind.agentsFile)),
            hooksFileExists = Files.exists(root.resolve(".codex").resolve("hooks.json")),
        )
    }

    fun bind(): Result<Unit> = runCatching {
        val config = loadConfig().getOrThrow()
        val root = requireProjectRoot()
        val agentsPath = root.resolve(config.bind.agentsFile)
        val hooksPath = root.resolve(".codex").resolve("hooks.json")

        Files.createDirectories(agentsPath.parent ?: root)
        if (!Files.exists(agentsPath)) {
            Files.writeString(agentsPath, "# Project Agents\n", StandardCharsets.UTF_8)
        }

        Files.createDirectories(hooksPath.parent)
        if (!Files.exists(hooksPath)) {
            Files.writeString(hooksPath, "{\n}\n", StandardCharsets.UTF_8)
        }

        saveConfig(config.copy(bind = config.bind.copy(enabled = true))).getOrThrow()
        updateStatus("Project bound to Pickles.")
    }

    fun unbind(): Result<Unit> = runCatching {
        val config = loadConfig().getOrThrow()
        saveConfig(config.copy(bind = config.bind.copy(enabled = false))).getOrThrow()
        updateStatus("Project unbound from Pickles.")
    }

    fun problems(): List<PicklesProblem> = currentProblems

    fun deleteProblem(problem: PicklesProblem) {
        currentProblems = currentProblems.filterNot { it == problem }
        updateStatus("Problem removed from current board.")
    }

    fun openProblem(problem: PicklesProblem) {
        val root = requireProjectRoot()
        val filePath = root.resolve(problem.file).normalize()
        val virtualFile: VirtualFile = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(filePath)
            ?: return showError("Cannot open ${problem.file}: file does not exist.")

        ApplicationManager.getApplication().invokeLater {
            val line = (problem.position.line - 1).coerceAtLeast(0)
            val column = (problem.position.column - 1).coerceAtLeast(0)
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
                writeServerFile(server.address.port)
                updateStatus("Local HTTP server started on port ${server.address.port}.")
            }.onFailure {
                thisLogger().warn("Failed to start Pickles HTTP server", it)
                showError("Failed to start Pickles local HTTP server: ${it.message}")
            }
        }
    }

    override fun dispose() {
        runCatching { httpServer?.stop(0) }
        httpServer = null
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

    private fun readRequestBody(exchange: HttpExchange): String =
        exchange.requestBody.use { String(it.readBytes(), StandardCharsets.UTF_8) }

    private fun contractHandler(): PicklesHttpContractHandler =
        PicklesHttpContractHandler(
            gson = gson,
            projectRoot = requireProjectRoot(),
        )

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

    private fun configPath(): Path = requireProjectRoot().resolve(".pickles").resolve("config.json")

    private fun requireProjectRoot(): Path =
        projectRoot ?: throw IOException("Project root is unavailable.")

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

    fun formatConfig(config: PicklesConfig): String = gson.toJson(config)

    fun parseConfig(text: String): Result<PicklesConfig> = runCatching {
        JsonParser.parseString(text)
        gson.fromJson(text, PicklesConfig::class.java) ?: PicklesConfig()
    }
}

fun Project.picklesService(): PicklesProjectService = service()
