package com.pickles.intellij

data class RuntimeChangedFile(
    val fileName: String,
    val before: String?,
    val after: String?,
)

interface PicklesRuntimeClient {
    fun inspect(files: List<RuntimeChangedFile>): List<PicklesProblem>
}

class EmptyPicklesRuntimeClient : PicklesRuntimeClient {
    override fun inspect(files: List<RuntimeChangedFile>): List<PicklesProblem> = emptyList()
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
