package com.pickles.intellij

data class PicklesProblem(
    val title: String,
    val type: String,
    val message: String,
    val severity: String = "WARN",
    val source: ProblemSource = ProblemSource(),
    val file: String? = null,
    val position: ProblemPosition? = null,
    val fixHint: String? = null,
)

data class ProblemSource(
    val tool: String = "pickles",
    val rule: String? = null,
)

data class ProblemPosition(
    val line: Int = 1,
    val column: Int = 1,
)

data class BindStatus(
    val agentsBlockBound: Boolean,
    val hooksFileExists: Boolean,
) {
    val bound: Boolean
        get() = agentsBlockBound && hooksFileExists
}
