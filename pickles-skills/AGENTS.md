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

## Rule Authoring Workflow

`pickles-rule-authoring-skill` 固定按以下流程工作：

1. 将用户规则意图整理为 rule spec。
2. 选择 file-level native rule、Java syntax query rule 或 workspace-level native rule 模板。
3. 生成或更新 Pickles runtime config、`.pickles/rules/*` 或 rule package。
4. 对生成物做形态自检。
5. 将复检交给 Runtime、CLI、MCP 或 Plugin 的稳定检查入口。
6. 后续维护按 rule id 定位并收窄修改。

`pickles-rule-authoring-skill` 不执行 rule。

`pickles-rule-authoring-skill` 不直接判断项目是否违规。

形态自检只检查生成物是否遵守 Pickles native rule contract，不替代 Runtime 检测。

## Documentation

- Skill 边界变化同步 `../docs/00-governance/ARCHITECTURE.md`。
- Skill 需求变化同步对应 `../docs/10-requirements/` 文档。
- Runtime 与 skill 的协作边界同步 `../docs/10-requirements/RUNTIME-REQUIREMENTS.md`。
