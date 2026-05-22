# DOCUMENT REQUIREMENTS

## 1. Purpose

本文档定义项目内需求文档和 AI 输入文档的统一写作要求。
目标是让文档适合 AI 读取、适合工程实现、适合持续维护。

## 2. File Naming

- 文档文件名使用大写英文。
- 文件名可使用 `-` 连接单词。
- 文件名不得使用中文。
- 文件名不得使用空格。
- 治理文档固定放在 `docs/00-governance/`。
- `HOW-TO` 文档固定放在 `docs/00-governance/how-to/`。
- `HOW-TO` 文档命名固定为 `HOW-TO-XXX.md`。
- `RUNBOOK` 文档固定放在 `docs/30-designs/`。
- `RUNBOOK` 文档命名固定为 `RUNBOOK-XXX.md`。

## 3. Governance Entry Map

治理文档固定入口：

- 架构、模块边界、分层职责：[`ARCHITECTURE.md`](./ARCHITECTURE.md)
- 命名、目录、文件归属：[`NAMING-AND-PLACEMENT-RULES.md`](./NAMING-AND-PLACEMENT-RULES.md)
- TODO 格式、协作、删除、测试检查和提交收口规则：[`TODO-RULES.md`](./TODO-RULES.md)
- 文档写作、路由和提交口径：本文档

新增稳定治理规则时，必须放入对应入口文档。临时执行步骤只放入 `TODO.md` 或 `docs/30-designs/RUNBOOK-*.md`，不得混入长期架构红线。

## 4. Language Rules

- 文档说明内容使用中文。
- 代码定义相关名称使用英文。
- 模块名、类名、接口名、服务名、字段名、枚举值、工具名和协议名必须保留英文原文。
- 不为了“纯中文”而翻译代码概念。
- 不为了“纯英文”而把业务说明改成英文。

## 5. Content Principles

- 文档必须清晰、明确、可执行。
- 文档必须适合 AI 读取。
- 文档必须适合直接指导实现。
- 文档不得包含冗余说明。
- 文档不得保留模糊口径。
- 同一规则不得在多处重复且表述不一致。
- 治理文档只沉淀稳定规则，不记录完成清单。
- `TODO.md` 的任务格式、协作、删除、测试检查和提交收口规则固定由 `TODO-RULES.md` 承载。
- `HOW-TO` 只承载高频操作步骤，不承载 `TODO.md` 治理规则。
- `RUNBOOK` 只承载一次性复杂任务的执行编排，不沉淀为长期通用规则。
- 临时讨论结论进入 `TODO.md` 或 `RUNBOOK`；稳定后再收敛到治理文档，完成后从临时材料中清理。
- `docs/50-prompts/` 只保存人工明确触发的生成提示词，不承载工程规则、业务需求或完成清单。
- `docs/60-human/` 只保存人类阅读材料、项目叙事和非实现约束材料，不承载 AI 默认执行规则。

## 6. RUNBOOK And HOW-TO Boundary

`RUNBOOK` 是一次性临时执行手册，用于某一个复杂任务的执行编排。`RUNBOOK` 固定服务于具体任务，不沉淀为长期通用流程。

`RUNBOOK` 通常与一组 `TODO.md` 任务一起出现。伴随 `RUNBOOK` 出现的 `TODO.md` 任务必须固定放在 `待审阅任务项`，经人工审阅后再执行。

`RUNBOOK` 最后必须被清理。清理 `RUNBOOK` 通常作为伴随 `TODO.md` 任务的最后一项，和残留引用扫描、测试验证、文档收口、工作区状态检查一起完成。

`HOW-TO` 是长期复用的操作手册，用于沉淀稳定、通用、可反复执行的方法。

AI 不得自行新增 `HOW-TO-*` 文档。只有用户明确准许新增 `HOW-TO-*` 时，AI 才能创建或扩展 `HOW-TO` 文档。

## 7. Requirement Style

- 使用确定性表达。
- 不使用“建议”“可考虑”“视情况”“后续再看”“如有需要”等不确定措辞。
- 已确认的规则必须直接写成约束。
- 需要固定的内容必须明确写成“固定”。
- `Open Items` 为空时明确写 `无`。

## 8. Structure Requirements

文档优先采用以下结构：

1. `Purpose`
2. `Scope`
3. `Bounded Context`
4. `Module Mapping`
5. `Core Objects`
6. `Global Constraints`
7. `Functional Requirements`
8. `Key Flows`
9. `Non-Functional Requirements`
10. `Open Items`

`RUNBOOK` 文档至少说明：

- `Purpose`
- `Scope`
- `Execution Order`
- `Verification`
- `Open Items`

总体要求：

- 先写全局边界，再写对象能力。
- 先写统一规则，再写功能需求。
- 先写稳定约束，再写补充说明。
- 同类规则集中在一个位置，不分散重复。

## 9. Boundary Writing Rules

- 明确模块归属。
- 明确职责边界。
- 明确谁拥有主规则、主数据或主契约。
- 明确谁负责流程，谁负责适配。
- 跨模块调用必须说明依赖层级。
- 不允许出现一个能力同时归属两个模块但不说明边界。
- 每份文档必须能够单独成立。
- 跨模块依赖的关键入口、关键枚举、关键状态映射和关键调用前置条件，必须在本文件中重述。

## 10. Open Items

无
