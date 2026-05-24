package com.pickles.intellij

data class PicklesConfig(
    val version: Int = 1,
    val agent: String = "codex",
    val bind: BindConfig = BindConfig(),
    val hook: HookConfig = HookConfig(),
    val rules: RulesConfig = RulesConfig(),
    val problemBoard: ProblemBoardConfig = ProblemBoardConfig(),
)

data class BindConfig(
    val agentsFile: String = "AGENTS.md",
    val enabled: Boolean = false,
)

data class HookConfig(
    val protocol: String = "http",
)

data class RulesConfig(
    val archunit: RuleCommandConfig = RuleCommandConfig(),
    val eslint: RuleCommandConfig = RuleCommandConfig(),
    val scripts: List<String> = emptyList(),
)

data class RuleCommandConfig(
    val enabled: Boolean = true,
    val command: String = "",
)

data class ProblemBoardConfig(
    val aggregation: String = "workspace",
)

data class PicklesProblem(
    val title: String,
    val type: String,
    val message: String,
    val severity: String = "WARN",
    val source: ProblemSource = ProblemSource(),
    val file: String = "",
    val position: ProblemPosition = ProblemPosition(),
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
    val configEnabled: Boolean,
    val agentsFileExists: Boolean,
    val hooksFileExists: Boolean,
) {
    val bound: Boolean
        get() = configEnabled && agentsFileExists && hooksFileExists
}
