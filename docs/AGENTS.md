# Docs Agent

只给 AI / harness 读。目标：少读，快读，读对。

## Core Rules

- 只加载完成当前任务必需的文档。
- 先读治理文档，再读模块文档。
- `docs/` 不是默认全量输入目录。
- 工程级规则优先于模块文档。
- `50-prompts/` 与 `60-human/` 永不默认加载。

## Mandatory Entry

实现、修改、评审代码前固定先读：

1. [`00-governance/ARCHITECTURE.md`](./00-governance/ARCHITECTURE.md)
2. [`00-governance/TODO-RULES.md`](./00-governance/TODO-RULES.md)

## Task Router

- 纯实现、修 bug、重构模块逻辑：
  读 `ARCHITECTURE.md`，再读对应 `10-requirements/*-REQUIREMENTS.md`。
- IntelliJ Plugin 实现、修复、重构：
  读 `10-requirements/INTELLIJ-PLUGIN-REQUIREMENTS.md`
  再按需读 `30-designs/INTELLIJ-PLUGIN-DESIGN.md`。
- Governance Engine / Runtime 实现、修复、重构：
  读 `10-requirements/RUNTIME-REQUIREMENTS.md`
  再按需读 `30-designs/RUNTIME-DESIGN.md`。
- Codex Hook 实现、修复、重构：
  读 `10-requirements/CODEX-HOOKS-REQUIREMENTS.md`
  再读 `20-interfaces/CODEX-HOOKS-OFFICIAL-REFERENCE.md`
  再按需读 `20-interfaces/HOOK-PLUGIN-HTTP-CONTRACT.md`。
- Pickles runtime config 配置读写或 schema 变化：
  读 `20-interfaces/PICKLES-CONFIG-CONTRACT.md`。
- Problem model 或 Problem Board 数据变化：
  读 `20-interfaces/PROBLEM-MODEL-CONTRACT.md`。
- 新增目录、改目录、判断文件归属：
  再读 `00-governance/NAMING-AND-PLACEMENT-RULES.md`。
- 改文档：
  再读 `00-governance/DOCUMENT-RULES.md`。
- TODO 协作、任务拆解、人机审阅、任务列表重写：
  读 `00-governance/TODO-RULES.md`。
- 任务收口、测试检查、文档同步、小步提交：
  读 `00-governance/TODO-RULES.md`。
- 一次性复杂任务执行手册、跨模块清理、迁移、删除、重构 RUNBOOK：
  读 `00-governance/DOCUMENT-RULES.md`
  再读 `00-governance/TODO-RULES.md`
  产物固定使用 `docs/30-designs/RUNBOOK-*.md` 和 `TODO.md` 待审阅任务，最后一项通常清理 RUNBOOK 和现场。
- 专项方案、路线图、跨模块设计：
  按需读 `30-designs/`。

## Module Router

- `pickles-hooks/` -> hook implementation and configuration work.
- `pickles-intellij-plugin/` -> IntelliJ plugin work.
- `pickles-mcp/` -> MCP integration work.
- `pickles-rules/` -> rules, policies, and shared specifications.
- `pickles-runtime/` -> runtime implementation.
- `pickles-skills/` -> Agent-side skills. `pickles-rule-authoring-skill` creates and maintains rules; `pickles-agent-governance-skill` prompts Agents to run checks and interpret Problems.

When a module gains its own `AGENTS.md`, read that file after this router and before editing that module.

## Load Limits

- 单模块任务：不要默认加载其他模块文档。
- 跨模块任务：只加载涉及的模块。
- commit 整理、纯格式调整、无实现判断的机械修改：
  不额外加载模块文档。
- 只有当当前文档明确引用下一个文档时，才继续向下追。

## TODO Router

- 根目录 `TODO.md` 是未关闭任务队列，不是完成历史。
- TODO 类型、格式、删除、测试检查和提交收口固定读 [`00-governance/TODO-RULES.md`](./00-governance/TODO-RULES.md)。
- 本文件只负责路由，不重复 TODO 细则。

## Directory Map

- `00-governance/`: 全局规则
- `10-requirements/`: 模块需求
- `20-interfaces/`: 接口、协议和契约设计
- `30-designs/`: 专项设计与 RUNBOOK
- `40-readiness/`: 上线、发布和运维准备
- `50-prompts/`: 人工触发的生成提示词
- `60-human/`: 人类阅读材料与项目叙事
