# Pickles Architecture

本文件只保留架构决策和实现红线。详细目录树、模块装配示例和具体运行命令应放在对应模块文档中。

## Project Baseline

- project: multi-area repository
- root docs: `docs/`
- implementation areas:
  - `pickles-hooks/`
  - `pickles-intellij-plugin/`
  - `pickles-mcp/`
  - `pickles-rules/`
  - `pickles-runtime/`

## Architecture Shape

- 规则定义、运行时执行、工具集成和 IDE 集成保持边界清晰。
- 模块之间优先通过稳定契约交互，不通过临时内部实现互相耦合。
- 新增共享能力前，先确认它被至少两个模块真实需要。
- 不为了未来可能的扩展提前新增抽象层。

## Module Boundaries

- `pickles-rules/`: 规则、策略、规范和可复用声明。
- `pickles-runtime/`: 规则加载、解析、执行和运行时能力。
- `pickles-hooks/`: 与 hook 生命周期相关的入口、脚本或适配。
- `pickles-mcp/`: MCP server、tool、resource 和外部协议适配。
- `pickles-intellij-plugin/`: IntelliJ 平台集成、编辑器体验和 IDE 侧交互。
- `docs/`: AI 读取路由、治理规则、设计文档和人工材料。

## Cross-Module Rule

- 跨模块调用必须依赖稳定契约、公开入口或明确文档化的接口。
- 不得从一个模块直接读取另一个模块的临时内部状态。
- 模块边界变化必须同步 `docs/AGENTS.md` 或相关治理文档。

## Documentation Rule

- 稳定工程规则进入 `docs/00-governance/`。
- 模块需求进入 `docs/10-requirements/`。
- 接口、协议和契约设计进入 `docs/20-interfaces/`。
- 专项设计和一次性 RUNBOOK 进入 `docs/30-designs/`。
- 人类阅读材料进入 `docs/60-human/`，不作为 AI 默认执行入口。

## Quality Tooling

- 新增或调整静态规则时，先判断这是格式整理还是规约约束。
- 格式整理工具只负责格式、import 和版式。
- 规约门禁工具只负责检查、报错和阻断，不负责改代码。
- 不要把同一类职责同时配置到多个工具，避免重复约束和误导后续 AI。

## Implementation Default

- 先保证模块边界正确，再写代码。
- 先复用现有模块模式，再新增抽象。
- 先写清楚稳定契约，再让多个模块依赖它。
- 先做最小可验证闭环，再扩大范围。
