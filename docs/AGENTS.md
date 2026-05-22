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
  读 `ARCHITECTURE.md`，再读对应模块文档。
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

When a module gains its own `AGENTS.md`, read that file after this router and before editing that module.

## Load Limits

- 单模块任务：不要默认加载其他模块文档。
- 跨模块任务：只加载涉及的模块。
- commit 整理、纯格式调整、无实现判断的机械修改：
  不额外加载模块文档。
- 只有当当前文档明确引用下一个文档时，才继续向下追。

## TODO Lifecycle

- 根目录 `TODO.md` 是任务执行队列，不是完成历史。
- 宏观任务进入 `TODO.md` 后，按 [`00-governance/TODO-RULES.md`](./00-governance/TODO-RULES.md) 完成人机讨论、任务拆解、人工审阅和执行关闭。
- 已完成任务不得在 `TODO.md` 中打勾长期保留，必须直接删除。
- 删除已完成 TODO 项必须和完成该任务的代码、文档或测试修改放在同一个 commit。
- 任务只完成一部分时，不得删除整项；必须拆分或收窄为剩余未完成内容。
- 待讨论项完成决策后，必须删除待讨论项；若仍需执行，新增明确执行项。
- 完成历史以 commit / PR 保留，不在 `TODO.md` 中重复记录。

## Directory Map

- `00-governance/`: 全局规则
- `10-requirements/`: 模块需求
- `20-interfaces/`: 接口、协议和契约设计
- `30-designs/`: 专项设计与 RUNBOOK
- `40-readiness/`: 上线、发布和运维准备
- `50-prompts/`: 人工触发的生成提示词
- `60-human/`: 人类阅读材料与项目叙事
