package com.pickles.intellij

import java.nio.file.Files
import java.nio.file.Path

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

    fun replaceProblems(problems: List<PicklesProblem>) {
        currentProblems = problems
    }

    fun deleteProblem(problem: PicklesProblem) {
        currentProblems = currentProblems.filterNot { it == problem }
    }
}
