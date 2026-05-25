# Pickles Skills Agent

只给 AI / harness 读。目标：把 Agent-side skills 与 Runtime、Plugin 和 rules 的边界保持清楚。

## Scope

- `pickles-rule-authoring-skill/`: 规则创作、迁移、解释和维护 skill。
- `pickles-agent-governance-skill/`: 提示 Agent 读取规则、运行检查、理解 Problem 并复检的治理 skill。

## Working Rules

- Skill 不拥有 Rule Engine。
- Skill 不直接实现 parser。
- Skill 不直接依赖 tree-sitter API。
- Skill 不作为规则真相源。
- `pickles-rule-authoring-skill` 只生成和维护 Pickles rules、config 或 rule package。
- `pickles-agent-governance-skill` 只提示 Agent 使用 Runtime、CLI、MCP 或 Plugin 暴露的稳定检查入口。
- Runtime 只加载 Pickles runtime config、native rule module 和 plugin rule package，不加载 skill。

## Documentation

- Skill 边界变化同步 `../docs/00-governance/ARCHITECTURE.md`。
- Skill 需求变化同步对应 `../docs/10-requirements/` 文档。
- Runtime 与 skill 的协作边界同步 `../docs/10-requirements/RUNTIME-REQUIREMENTS.md`。
