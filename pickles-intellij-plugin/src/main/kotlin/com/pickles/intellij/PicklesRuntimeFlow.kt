package com.pickles.intellij

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit

data class RuntimeChangedFile(
    val fileName: String,
    val before: String?,
    val after: String?,
) {
    val changeType: String
        get() = when {
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
