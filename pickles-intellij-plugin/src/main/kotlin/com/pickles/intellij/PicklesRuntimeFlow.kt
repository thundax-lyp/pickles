package com.pickles.intellij

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.io.path.isRegularFile
import kotlin.io.path.relativeTo

data class RuntimeChangedFile(
    val fileName: String,
    val before: String?,
    val after: String?,
    private val explicitChangeType: String? = null,
) {
    val changeType: String
        get() = explicitChangeType ?: when {
            before == null -> "added"
            after == null -> "deleted"
            else -> "modified"
        }
}

interface PicklesRuntimeClient {
    fun inspect(files: List<RuntimeChangedFile>): List<PicklesProblem>
}

class EmptyPicklesRuntimeClient : PicklesRuntimeClient {
    override fun inspect(files: List<RuntimeChangedFile>): List<PicklesProblem> = emptyList()
}

class PicklesWorkspaceIndexGate {
    private val running = AtomicBoolean(false)

    fun tryStart(): Boolean = running.compareAndSet(false, true)

    fun finish() {
        running.set(false)
    }

    fun isRunning(): Boolean = running.get()
}

enum class RuntimeQueueSource {
    REINDEX,
    NOTIFY,
    MIXED,
}

data class RuntimeQueueRequest(
    val source: RuntimeQueueSource,
    val files: List<RuntimeChangedFile>,
) {
    val affectedFiles: Set<String> = files.map { it.fileName }.toSet()
}

data class RuntimeQueueRun(
    val version: Long,
    val request: RuntimeQueueRequest,
    val invalidated: Boolean,
)

data class RuntimeQueueCompletion(
    val shouldApplyResult: Boolean,
    val nextRun: RuntimeQueueRun?,
)

data class RuntimeQueueSnapshot(
    val running: Boolean,
    val pending: Boolean,
    val currentInvalidated: Boolean,
)

class PicklesRuntimeQueue {
    private var currentRun: MutableRuntimeQueueRun? = null
    private var pendingRequest: RuntimeQueueRequest? = null
    private var nextVersion: Long = 1

    @Synchronized
    fun enqueue(request: RuntimeQueueRequest): RuntimeQueueRun? {
        val current = currentRun
        if (current == null) {
            return start(request)
        }

        if (overlaps(current.request, request)) {
            current.invalidated = true
        }
        pendingRequest = merge(pendingRequest, request)
        return null
    }

    @Synchronized
    fun complete(version: Long): RuntimeQueueCompletion {
        val current = currentRun ?: return RuntimeQueueCompletion(
            shouldApplyResult = false,
            nextRun = null,
        )
        if (current.version != version) {
            return RuntimeQueueCompletion(
                shouldApplyResult = false,
                nextRun = null,
            )
        }

        val shouldApply = !current.invalidated
        currentRun = null
        val nextRun = pendingRequest?.let { pending ->
            pendingRequest = null
            start(pending)
        }

        return RuntimeQueueCompletion(
            shouldApplyResult = shouldApply,
            nextRun = nextRun,
        )
    }

    @Synchronized
    fun snapshot(): RuntimeQueueSnapshot = RuntimeQueueSnapshot(
        running = currentRun != null,
        pending = pendingRequest != null,
        currentInvalidated = currentRun?.invalidated == true,
    )

    private fun start(request: RuntimeQueueRequest): RuntimeQueueRun {
        val run = MutableRuntimeQueueRun(
            version = nextVersion++,
            request = request,
        )
        currentRun = run
        return run.toRun()
    }

    private fun overlaps(first: RuntimeQueueRequest, second: RuntimeQueueRequest): Boolean = first.affectedFiles.intersect(second.affectedFiles).isNotEmpty()

    private fun merge(
        existing: RuntimeQueueRequest?,
        incoming: RuntimeQueueRequest,
    ): RuntimeQueueRequest {
        if (existing == null) {
            return incoming
        }

        val filesByPath = linkedMapOf<String, RuntimeChangedFile>()
        existing.files.forEach { file -> filesByPath[file.fileName] = file }
        incoming.files.forEach { file -> filesByPath[file.fileName] = file }
        val source = if (existing.source == incoming.source) existing.source else RuntimeQueueSource.MIXED

        return RuntimeQueueRequest(
            source = source,
            files = filesByPath.values.toList(),
        )
    }

    private data class MutableRuntimeQueueRun(
        val version: Long,
        val request: RuntimeQueueRequest,
        var invalidated: Boolean = false,
    ) {
        fun toRun(): RuntimeQueueRun = RuntimeQueueRun(
            version = version,
            request = request,
            invalidated = invalidated,
        )
    }
}

object PicklesWorkspaceInspection {
    fun collectJavaFiles(workspaceRoot: Path): List<RuntimeChangedFile> {
        val normalizedRoot = workspaceRoot.toAbsolutePath().normalize()
        if (!Files.isDirectory(normalizedRoot)) {
            return emptyList()
        }
        val ignoreMatcher = PicklesWorkspaceIgnoreMatcher.load(normalizedRoot)

        return Files.walk(normalizedRoot).use { paths ->
            paths
                .filter { it.isRegularFile() }
                .filter { it.fileName.toString().endsWith(".java") }
                .filter { !ignoreMatcher.ignores(it.relativeTo(normalizedRoot).toString()) }
                .sorted()
                .map { file ->
                    RuntimeChangedFile(
                        fileName = file.relativeTo(normalizedRoot).toString(),
                        before = null,
                        after = Files.readString(file, StandardCharsets.UTF_8),
                        explicitChangeType = "modified",
                    )
                }
                .toList()
        }
    }

    fun inspect(
        workspaceRoot: Path,
        runtimeClient: PicklesRuntimeClient,
        problemBoard: PicklesProblemBoardState,
    ): List<PicklesProblem> {
        val problems = runtimeClient.inspect(collectJavaFiles(workspaceRoot))
        problemBoard.replaceProblems(problems)
        return problems
    }
}

class PicklesWorkspaceIgnoreMatcher private constructor(
    private val directoryPatterns: Set<String>,
    private val fileNamePatterns: Set<String>,
) {
    fun ignores(relativePath: String): Boolean {
        val normalized = relativePath.replace('\\', '/').trimStart('/')
        val segments = normalized.split('/').filter { it.isNotEmpty() }
        if (segments.any { segment -> directoryPatterns.contains("$segment/") }) {
            return true
        }
        if (directoryPatterns.any { pattern -> normalized.startsWith(pattern) }) {
            return true
        }
        return fileNamePatterns.any { pattern -> matchesFilePattern(normalized, pattern) }
    }

    private fun matchesFilePattern(path: String, pattern: String): Boolean = when {
        pattern.startsWith("*.") -> path.substringAfterLast('/').endsWith(pattern.removePrefix("*"))
        pattern.contains("*") -> Regex(pattern.replace(".", "\\.").replace("*", ".*")).matches(path.substringAfterLast('/'))
        else -> path == pattern || path.endsWith("/$pattern")
    }

    companion object {
        private val BUILT_IN_DIRECTORIES = setOf(".git/", ".idea/", ".gradle/", "build/", "out/", "node_modules/")

        fun load(workspaceRoot: Path): PicklesWorkspaceIgnoreMatcher {
            val gitignorePatterns = readGitignorePatterns(workspaceRoot)
            return PicklesWorkspaceIgnoreMatcher(
                directoryPatterns = BUILT_IN_DIRECTORIES + gitignorePatterns.filter { it.endsWith("/") },
                fileNamePatterns = gitignorePatterns.filterNot { it.endsWith("/") }.toSet(),
            )
        }

        private fun readGitignorePatterns(workspaceRoot: Path): Set<String> {
            val gitignore = workspaceRoot.resolve(".gitignore")
            if (!Files.isRegularFile(gitignore)) {
                return emptySet()
            }

            return Files.readAllLines(gitignore, StandardCharsets.UTF_8)
                .asSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .filterNot { it.startsWith("#") }
                .filterNot { it.startsWith("!") }
                .map { it.trimStart('/') }
                .toSet()
        }
    }
}

object PicklesProblemOrdering {
    fun sorted(problems: List<PicklesProblem>): List<PicklesProblem> = problems
        .withIndex()
        .sortedWith(
            compareBy<IndexedValue<PicklesProblem>> { severityRank(it.value.severity) }
                .thenBy { locationRank(it.value) }
                .thenBy { it.index },
        )
        .map { it.value }

    private fun severityRank(severity: String): Int = when (severity) {
        "ERROR" -> 0
        "WARN" -> 1
        else -> 2
    }

    private fun locationRank(problem: PicklesProblem): Int = if (problem.file != null || problem.position != null) 0 else 1
}

class NodePicklesRuntimeClient(
    private val workspaceRoot: Path,
    private val runtimeRoot: Path,
    private val nodeExecutable: String = System.getProperty(NODE_PATH_PROPERTY, "node"),
    private val timeoutSeconds: Long = DEFAULT_TIMEOUT_SECONDS,
    private val gson: Gson = GsonBuilder().serializeNulls().create(),
) : PicklesRuntimeClient {
    override fun inspect(files: List<RuntimeChangedFile>): List<PicklesProblem> {
        val process = ProcessBuilder(nodeExecutable, "--import", "tsx", "src/stdio.ts")
            .directory(runtimeRoot.toFile())
            .start()
        val request = RuntimeCheckRequest(
            workspaceRoot = workspaceRoot.toAbsolutePath().normalize().toString(),
            changedFiles = files.map { it.toRuntimeFile() },
        )

        process.outputStream.bufferedWriter(StandardCharsets.UTF_8).use { writer ->
            writer.write(gson.toJson(request))
        }

        val completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS)
        if (!completed) {
            process.destroyForcibly()
            throw IOException("Pickles Runtime timed out.")
        }

        val stdout = process.inputStream.readAllBytes().toString(StandardCharsets.UTF_8)
        val stderr = process.errorStream.readAllBytes().toString(StandardCharsets.UTF_8)
        return NodePicklesRuntimeResponseParser.parse(
            stdout = stdout,
            stderr = stderr,
            exitCode = process.exitValue(),
            gson = gson,
        )
    }

    private fun RuntimeChangedFile.toRuntimeFile(): RuntimeCheckFile = RuntimeCheckFile(
        path = fileName,
        changeType = changeType,
        before = before,
        after = after,
    )

    private data class RuntimeCheckRequest(
        val workspaceRoot: String,
        val changedFiles: List<RuntimeCheckFile>,
    )

    private data class RuntimeCheckFile(
        val path: String,
        val changeType: String,
        val before: String?,
        val after: String?,
    )

    private companion object {
        const val NODE_PATH_PROPERTY = "pickles.node.path"
        const val DEFAULT_TIMEOUT_SECONDS = 30L
    }
}

object NodePicklesRuntimeResponseParser {
    fun parse(
        stdout: String,
        stderr: String,
        exitCode: Int,
        gson: Gson = GsonBuilder().serializeNulls().create(),
    ): List<PicklesProblem> {
        if (stdout.isBlank()) {
            throw IOException(stderr.trim().ifEmpty { "Pickles Runtime returned an empty response." })
        }

        val response = try {
            gson.fromJson(stdout, RuntimeCheckResponse::class.java)
        } catch (_: JsonSyntaxException) {
            throw IOException("Pickles Runtime returned invalid JSON.")
        }

        val errorMessage = response.error?.message
        if (exitCode != 0 || errorMessage != null) {
            throw IOException(errorMessage ?: stderr.trim().ifEmpty { "Pickles Runtime failed." })
        }

        return response.problems ?: throw IOException("Pickles Runtime returned an invalid response.")
    }

    private data class RuntimeCheckResponse(
        val problems: List<PicklesProblem>?,
        val error: RuntimeCheckError?,
    )

    private data class RuntimeCheckError(
        val message: String?,
    )
}

object PicklesRuntimeLocator {
    fun find(projectRoot: Path): Path? {
        val configuredRuntime = System.getProperty(RUNTIME_DIR_PROPERTY)
            ?.takeIf { it.isNotBlank() }
            ?.let(Path::of)
        val candidates = listOfNotNull(
            configuredRuntime,
            projectRoot.resolve("pickles-runtime"),
            projectRoot.resolve("../pickles-runtime"),
            projectRoot.resolve("../../pickles-runtime"),
        )

        return candidates
            .map { it.toAbsolutePath().normalize() }
            .firstOrNull { isRuntimeRoot(it) }
    }

    private fun isRuntimeRoot(path: Path): Boolean = Files.isRegularFile(path.resolve("package.json")) &&
        Files.isRegularFile(path.resolve("src").resolve("stdio.ts"))

    private const val RUNTIME_DIR_PROPERTY = "pickles.runtime.dir"
}

class PicklesProblemBoardState {
    @Volatile
    private var currentProblems: List<PicklesProblem> = emptyList()

    fun problems(): List<PicklesProblem> = currentProblems

    fun summary(): PicklesProblemSummary = PicklesProblemSummary.from(currentProblems)

    fun replaceProblems(problems: List<PicklesProblem>) {
        currentProblems = problems
    }

    fun deleteProblem(problem: PicklesProblem) {
        currentProblems = currentProblems.filterNot { it == problem }
    }
}

enum class PicklesHttpServerStatus {
    STOPPED,
    RUNNING,
}

enum class PicklesRuntimeStatus {
    UNKNOWN,
    AVAILABLE,
    UNAVAILABLE,
}

enum class PicklesIndexStatus {
    IDLE,
    RUNNING,
    SUCCEEDED,
    FAILED,
}

data class PicklesProblemSummary(
    val totalCount: Int,
    val errorCount: Int,
    val warnCount: Int,
    val text: String,
) {
    companion object {
        fun from(problems: List<PicklesProblem>): PicklesProblemSummary {
            val errorCount = problems.count { it.severity == "ERROR" }
            val warnCount = problems.count { it.severity == "WARN" }
            val totalCount = problems.size
            val text = when {
                totalCount == 0 -> "No Pickles governance problems."
                errorCount > 0 -> "Pickles found $errorCount blocking problem(s) and $warnCount warning(s)."
                else -> "Pickles found $warnCount warning(s)."
            }

            return PicklesProblemSummary(
                totalCount = totalCount,
                errorCount = errorCount,
                warnCount = warnCount,
                text = text,
            )
        }
    }
}

data class PicklesServiceStatusSnapshot(
    val httpServerStatus: PicklesHttpServerStatus,
    val runtimeStatus: PicklesRuntimeStatus,
    val indexStatus: PicklesIndexStatus,
    val problemSummary: PicklesProblemSummary,
    val message: String,
)
