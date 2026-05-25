# Naming And Placement Rules

## 1. Purpose

本文档定义 Pickles 的命名、目录和文件归属规则。

目标是让新增文件能稳定落到正确模块，避免后续 AI 或开发者因为边界不清而扩大改动范围。

## 2. Scope

当前范围：

- 顶层模块归属
- 文档命名和目录归属
- 新增文件前的判断顺序
- 跨模块共享能力的放置原则

不在范围内：

- 不替代各模块内部语言、框架或构建工具约定
- 不定义未来尚未出现的模块内部包结构

## 3. Top-Level Placement

- hook 生命周期、hook 脚本、hook 适配固定放在 `pickles-hooks/`。
- IntelliJ 插件、IDE UI、编辑器集成固定放在 `pickles-intellij-plugin/`。
- MCP server、MCP tool、MCP resource 和 MCP 协议适配固定放在 `pickles-mcp/`。
- 规则、策略、规范和共享规则声明固定放在 `pickles-rules/`。
- 规则运行、加载、解析、执行和运行时支撑固定放在 `pickles-runtime/`。
- Agent-side skill 固定放在 `pickles-skills/`。
- AI 读取路由、治理规则、需求、设计、提示词和人类材料固定放在 `docs/`。

## 4. Documentation Placement

- 稳定治理规则放在 `docs/00-governance/`。
- 模块需求放在 `docs/10-requirements/`。
- 接口、协议和契约设计放在 `docs/20-interfaces/`。
- 专项设计、跨模块方案和一次性 RUNBOOK 放在 `docs/30-designs/`。
- 上线、发布和运维准备放在 `docs/40-readiness/`。
- 人工触发的生成提示词放在 `docs/50-prompts/`。
- 人类阅读材料和项目叙事放在 `docs/60-human/`。

## 5. Naming Rules

- 文档文件名使用大写英文和 `-`。
- 文档文件名不得使用中文和空格。
- 模块目录名保持现有 `pickles-*` 前缀。
- 新增目录名必须表达稳定职责，不使用临时任务名。
- 新增文件名必须优先表达对象职责，不使用实现过程或个人偏好命名。

## 6. Shared Capability Rule

- 只有至少两个模块真实依赖同一能力时，才考虑抽到共享位置。
- 共享能力必须有清晰所有者。
- 不能因为“以后可能复用”提前创建共享层。
- 共享契约变化必须同步对应文档。

## 7. Pre-Checks

新增文件或目录前固定判断：

1. 当前能力是否属于已有顶层模块。
2. 当前能力是否只是一次性任务产物。
3. 当前能力是否需要长期维护。
4. 当前能力是否会被多个模块依赖。
5. 当前能力是否需要先写入治理文档或设计文档。

## 8. Open Items

无
