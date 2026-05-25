# TODO List

## 说明

- `TODO.md` 是未关闭任务面板，不是完成历史。
- 宏观任务必须先讨论边界，再拆解为可执行 TODO。
- 已完成任务必须删除，不在 `TODO.md` 中打勾长期保留。
- 完成历史保留在 commit 或 PR 中。

## 当前任务项

## 待审阅任务项

- [ ] `docs/10-requirements/RUNTIME-REQUIREMENTS.md`：核对 Runtime config 加载优先级是否仍为未决项
    - 任务类型：执行任务
    - 依据文档：`docs/10-requirements/RUNTIME-REQUIREMENTS.md`
    - 范围对象：`docs/10-requirements/RUNTIME-REQUIREMENTS.md`、`docs/20-interfaces/PICKLES-CONFIG-CONTRACT.md`
    - 处理动作：对齐 Runtime Requirements 与 Pickles Config Contract 中 config 加载优先级的状态
    - 验收点：已关闭则删除 Open Item，未关闭则补齐待决问题
    - 重要度：8/10

- [ ] `pickles-rules`：编写 AI-generated rule authoring guide
    - 任务类型：执行任务
    - 依据文档：`docs/10-requirements/RUNTIME-REQUIREMENTS.md`
    - 范围对象：`pickles-rules/` AI rule authoring guide
    - 处理动作：把已确认的规则作者契约整理成 AI 可执行说明
    - 验收点：guide 能覆盖 config 内联规则、`.pickles/*` 脚本引用和 Problem 输出
    - 重要度：9/10

- [ ] `RUNTIME_SAMPLE_PROJECT`：自动化 Runtime sample project testcase
    - 任务类型：执行任务
    - 依据文档：`docs/40-readiness/E2E-TEST-CASES.md`
    - 范围对象：`RUNTIME_SAMPLE_PROJECT`
    - 处理动作：实现 Runtime 基于 sample project config 执行规则并生成 Problem 的自动化验证
    - 验收点：testcase 可通过仓库验证入口稳定运行
    - 重要度：8/10

- [ ] `PLUGIN_RUNTIME_FLOW`：自动化 Plugin-Runtime flow testcase
    - 任务类型：执行任务
    - 依据文档：`docs/40-readiness/E2E-TEST-CASES.md`
    - 范围对象：`PLUGIN_RUNTIME_FLOW`
    - 处理动作：实现 Plugin 触发 Runtime 检测并展示 Problem 的自动化验证
    - 验收点：testcase 可覆盖 Plugin 不执行规则命令的边界
    - 重要度：7/10

- [ ] `E2E_FULL_FLOW`：自动化 full flow testcase
    - 任务类型：执行任务
    - 依据文档：`docs/40-readiness/E2E-TEST-CASES.md`
    - 范围对象：`E2E_FULL_FLOW`
    - 处理动作：实现 Hook、Plugin、Runtime 和 Problem Board 的端到端自动化验证
    - 验收点：testcase 可作为阶段 PR 完整验证入口
    - 重要度：7/10

## 待讨论项

- [ ] 定义 Pickles native rule authoring contract
    - 任务类型：待讨论项
    - 关联任务：`pickles-rules`
    - 决策要求：确定 `defineRule` 作者侧字段、约束、输入输出和禁止事项
    - 重要度：10/10

- [ ] 设计 external adapter execution
    - 任务类型：待讨论项
    - 关联任务：`External Adapter`
    - 决策要求：确定 ArchUnit、ESLint 和 `.pickles/*` 脚本作为后续兼容能力的执行方式、工作目录、超时和失败表达
    - 重要度：6/10

- [ ] 设计 Rule templates
    - 任务类型：待讨论项
    - 关联任务：`pickles-rules`
    - 决策要求：确定 MVP 需要提供的 native rule 和 Java syntax query 模板
    - 重要度：8/10

- [ ] 定义 Skill-to-rules workflow
    - 任务类型：待讨论项
    - 关联任务：`pickles-skills/pickles-rule-authoring-skill`
    - 决策要求：确认 skill 从用户规则意图到 config、`.pickles/*` 生成物和复检入口的流程
    - 重要度：10/10

- [ ] 确定 Incremental Workspace Index 内部结构
    - 任务类型：待讨论项
    - 关联任务：`pickles-runtime`
    - 决策要求：确定文件索引、Java type 索引、annotation/import 查询索引和更新策略
    - 重要度：9/10

- [ ] 确定问题去重规则
    - 任务类型：待讨论项
    - 关联任务：`Problem Model`
    - 决策要求：确认 Problem 去重 key、同源/跨源合并策略和展示保留规则
    - 重要度：8/10

- [ ] 定义 Repair-Oriented Summary 稳定 JSON contract
    - 任务类型：待讨论项
    - 关联任务：`Repair-Oriented Summary`
    - 决策要求：确定 summary 的字段、序列化形态和 MCP 接入前稳定边界
    - 重要度：8/10

- [ ] 确定 Runtime 与 Plugin 的进程边界
    - 任务类型：待讨论项
    - 关联任务：`Runtime-Plugin Boundary`
    - 决策要求：在同进程调用 TypeScript bundle、独立 Node 子进程、本地 HTTP server 中确定 MVP 方案
    - 重要度：10/10

- [ ] 确定首次 workspace 全量索引触发时机
    - 任务类型：待讨论项
    - 关联任务：`Runtime-Plugin Boundary`
    - 决策要求：确认打开项目、首次检测、Hook 首次通知或手动刷新中的触发时机
    - 重要度：9/10

- [ ] 设计 Tool Window 具体布局
    - 任务类型：待讨论项
    - 关联任务：`IntelliJ Plugin`
    - 决策要求：确认 Tool Window 的区域、列表字段、操作入口和错误状态展示
    - 重要度：7/10

- [ ] 确定 AGENTS.md 注入块格式、marker 和幂等更新细节
    - 任务类型：待讨论项
    - 关联任务：`AGENTS.md Injection`
    - 决策要求：确认 Pickles 注入块 marker、重复更新、删除和冲突处理规则
    - 重要度：8/10
