# Repository Guidelines

## Read Order

- Read `docs/AGENTS.md` first.
- For implementation work, read `docs/00-governance/ARCHITECTURE.md`.
- Do not treat root `README.md` as implementation authority.

## Working Rules

- Load the minimum docs needed for the task.
- Prefer the simplest workable solution.
- Do not add abstraction, config, directories, or helper layers without a concrete need.
- Keep changes scoped to the requested project area.
- Preserve existing user changes. Do not revert, overwrite, or clean up unrelated work.
- Follow existing patterns once a module establishes its own build system, style, or test layout.

## Project Layout

- Primary product: IntelliJ IDEA plugin for Coding Agent semantic governance.
- `pickles-intellij-plugin/`: IntelliJ plugin implementation, editor integration, Problem Board UI, and IDE-side orchestration.
- `pickles-runtime/`: local governance runtime used by the plugin.
- `pickles-mcp/`: MCP integration exposed to Coding Agents.
- `pickles-hooks/`: Agent runtime hook integration.
- `pickles-rules/`: rules, policies, and shared governance specifications.
- `docs/`: AI routing docs, governance docs, designs, prompts, and human-facing notes.

## Code Rules

- Follow the conventions of the module being changed.
- Prefer stable names and explicit ownership over ambiguous shared helpers.
- Keep module boundaries clear. Do not introduce cross-module coupling without documenting the reason.
- Add or update documentation when behavior, setup, or developer workflow changes.

## Testing

- Run the narrowest relevant validation available for the files changed.
- Behavior changes require test updates when a test framework exists for the affected module.
- If no validation exists, mention that clearly in the final response.

## Commits

- Every file modification must be committed before ending the task.
- Commit format: `Type(scope): 中文说明`
- Split unrelated changes into separate commits.
- Commit message must state the concrete capability changed.
- Include code, tests, documentation sync, and corresponding `TODO.md` cleanup in the same commit when they belong to one closed task.

Examples:

- `Docs(governance): 初始化文档治理入口`
- `Feat(runtime): 增加规则加载入口`
- `Fix(mcp): 修复工具参数校验`
