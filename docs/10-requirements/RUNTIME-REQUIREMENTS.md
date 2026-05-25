# Runtime Requirements

## 1. Purpose

本文档定义 Pickles Runtime 的 MVP 需求。

目标是明确 `pickles-runtime/` 作为治理执行内核必须提供的能力、内部技术约束、模块边界和验收口径。

## 2. Scope

当前范围：

- Runtime package baseline
- Pickles runtime config 读取和校验
- Programmable rule config
- Native rule execution
- Incremental Workspace Index
- TypeScript + `tree-sitter-xxx` parser integration
- Java syntax index
- Parser diagnostic
- ArchUnit adapter execution
- ESLint adapter execution
- Problem 聚合
- Repair-Oriented Summary

不在范围内：

- 不实现 IntelliJ UI
- 不实现 Codex Hook
- 不实现 Agent skill
- 不定义 HTTP endpoint
- 不实现 MCP server
- 不实现 Java 类型解析
- 不实现 classpath 符号解析
- 不实现跨文件 Java symbol resolution
- 不保存全量语义持久化数据
- 不从 ArchUnit Java tests 反向读取规则
- 不把 ArchUnit 作为增量反馈的唯一规则来源
- 不定义完整 Repair-Oriented Summary 稳定 JSON contract

## 3. Bounded Context

Runtime 服务于 IntelliJ Plugin。

IntelliJ Plugin 负责 UI、IDE 集成、本地 HTTP 入口和文件跳转。Runtime 负责读取配置、更新 workspace index、解析语法索引、加载规则定义、执行规则、聚合 Problem，并生成面向 Agent 的治理反馈。

Runtime 固定作为 Pickles Rule Engine。Runtime 不只是外部命令执行器。

Pickles native rules 固定使用类似 ESLint config 的 JS / TS 可编程规则定义。规则入口固定由 Pickles runtime config 指向。

ArchUnit 和 ESLint 固定作为 external rule adapter。它们服务于兼容已有项目和完整验证，不作为 Pickles 增量反馈的唯一规则来源。

Runtime 内部实现语言固定为 Node.js / TypeScript。

Runtime 内部 parser 体系固定为 `tree-sitter-xxx`。Java 语法解析固定使用 `tree-sitter-java`。

`tree-sitter-java` 只提供 Java 语法树能力。它不是 Java compiler，不提供类型解析、依赖解析、classpath 语义或跨文件符号解析。

IntelliJ Plugin、Codex Hook、rules 和 MCP 不拥有 parser，不直接依赖 tree-sitter API。

`pickles-agent-governance-skill` 是 Agent 侧治理入口，负责提示 Agent 读取规则、运行检查、理解 Problem 并复检。它与 IntelliJ Plugin 对等，二者都不拥有规则真相和 Rule Engine。

`pickles-rule-authoring-skill` 负责规则创作、迁移、解释和维护。它不执行规则。

Runtime 不加载 skill。Runtime 只加载 Pickles runtime config、native rule module 和 plugin rule package。

## 4. Module Mapping

- `pickles-runtime/`: Rule Engine 实现、配置读取、workspace index、parser integration、native rule execution、external rule adapter execution 和 Problem 聚合。
- `pickles-intellij-plugin/`: Runtime 编排、IDE 展示、本地 HTTP 入口和文件跳转。
- `pickles-hooks/`: 文件变动捕获、before / after 内容提交。
- `pickles-rules/`: Pickles native rule 说明书、规则模板、可复用规则包和 adapter 模板。
- `pickles-skills/pickles-rule-authoring-skill/`: Agent 侧规则创作 skill，生成和维护 Pickles rules。
- `pickles-skills/pickles-agent-governance-skill/`: Agent 侧治理 skill，提示 Agent 运行检查、解释 Problem 并引导复检。
- `pickles-mcp/`: 后续读取 Runtime 稳定输出，不直接读取 parser。
- `docs/20-interfaces/PICKLES-CONFIG-CONTRACT.md`: 配置契约。
- `docs/20-interfaces/PROBLEM-MODEL-CONTRACT.md`: 问题模型契约。
- `docs/30-designs/RUNTIME-DESIGN.md`: Runtime 实现设计。

## 5. Core Objects

- `PicklesRuntimeConfig`
- `ChangedFile`
- `IncrementalWorkspaceIndex`
- `PicklesRuleConfig`
- `PicklesNativeRule`
- `RuleContext`
- `RuleResult`
- `RuleAdapter`
- `SyntaxParser`
- `SyntaxIndex`
- `JavaSyntaxFile`
- `JavaTypeDeclaration`
- `JavaMethodDeclaration`
- `ParserDiagnostic`
- `ExternalRuleCommand`
- `Problem`
- `RepairOrientedSummary`

## 6. Global Constraints

- Runtime 不拥有 UI。
- Runtime 实现语言固定为 Node.js / TypeScript。
- Runtime 运行环境固定为 Node.js 22。
- Runtime 必须使用 TypeScript strict mode。
- Runtime 必须使用 ESM。
- Runtime 必须通过 lockfile 固定 npm 依赖版本。
- Runtime 内部 parser 依赖固定使用 `tree-sitter-xxx` 包族。
- Runtime Java parser 固定使用 `tree-sitter-java`。
- Runtime 必须封装 tree-sitter API。
- Runtime 不得向 Plugin、Hook、rules 或 MCP 暴露 tree-sitter 原生对象。
- Runtime 必须支持 JS / TS 可编程规则入口。
- Runtime rule API 必须能被 AI 根据说明书生成。
- Runtime rule API 必须避免依赖隐藏全局状态。
- Runtime rule 执行必须通过 Runtime 提供的 `RuleContext` 访问 index、config 和文件变动。
- Runtime 不加载 `pickles-rule-authoring-skill`。
- Runtime 不加载 `pickles-agent-governance-skill`。
- Runtime 不修改业务代码、测试代码或工程实现代码。
- Runtime 不执行用户业务代码。
- Runtime 不执行 Java source code。
- Runtime 不写 Pickles runtime config。
- Runtime 不保存全量语义持久化数据。
- Runtime MVP 必须按文件重建 syntax index，不依赖 tree-sitter incremental edit API。
- Runtime 生成的 range 固定使用 1-based line 和 1-based column。
- MVP external rule adapter 固定只支持 ArchUnit 与 ESLint。
- MVP severity 固定只使用规则工具或 parser diagnostic 返回的 `ERROR` / `WARN`。

## 7. Functional Requirements

### 7.1 Runtime Package

`pickles-runtime/` 必须成为独立 Node.js / TypeScript package。

Runtime package 必须提供以下脚本：

- `npm run build`
- `npm run typecheck`
- `npm run test`
- `npm run lint`

`lint` 在未引入独立 lint 工具前固定等同于 `typecheck`。

### 7.2 Runtime Config Loading

Runtime 必须读取 Pickles runtime config。

Runtime 必须支持 JS / TS / MJS 可编程配置入口。

可编程配置入口固定命名为 `pickles.config.js`、`pickles.config.mjs` 或 `pickles.config.ts`。

Runtime 必须校验配置加载结果。

配置不存在、加载失败或字段非法时，Runtime 必须返回可展示错误。

Runtime 不得自动修复、写入或重排 Pickles runtime config。

### 7.3 Programmable Rule Config

Runtime 必须支持类似 ESLint config 的 JS / TS 可编程规则定义。

可编程规则配置必须能表达：

- rule id
- rule title
- rule type
- severity
- language scope
- file include / exclude
- rule options
- rule implementation

Rule implementation 必须通过 Runtime 提供的 `RuleContext` 获取输入。

`RuleContext` 必须至少提供：

- workspace root
- changed files
- syntax index query
- config query
- problem factory

Rule implementation 必须返回 `RuleResult` 或 Problem-compatible result。

Runtime 必须把 rule result 归一化为 Problem。

Runtime 不得要求 rule implementation 直接读取 tree-sitter 原生对象。

Runtime 不得要求 rule implementation 直接读取 Plugin 或 Hook 内部状态。

### 7.4 AI-Generated Rule Support

Pickles native rule API 必须适合 AI 根据说明书生成。

`pickles-rules/` 必须承载规则说明书和可复用规则模板。

`pickles-rule-authoring-skill` 必须使用规则说明书生成和维护 Pickles rules。

`pickles-agent-governance-skill` 必须使用 Runtime、CLI、MCP 或 Plugin 暴露的稳定检查入口提示 Agent 执行规则检查。

`pickles-agent-governance-skill` 不得自行实现 parser 或 Rule Engine。

规则说明书必须能让 AI 生成以下内容：

- `pickles.config.js`
- `pickles.config.mjs`
- `pickles.config.ts`
- standalone rule module
- reusable skill/plugin rule package

Rule API 必须优先使用稳定字段、显式命名和 JSON-serializable options。

Rule API 不得依赖复杂继承、隐式上下文或运行时 monkey patch。

### 7.5 Changed File Input

Runtime 必须接收文件变动集。

文件变动必须能表达：

- file path
- change type
- before content
- after content

`change type` 固定支持：

- `added`
- `modified`
- `deleted`
- `unchanged`

Runtime 必须忽略 `unchanged` 文件。

### 7.6 Incremental Workspace Index

Runtime 必须维护进程内 Incremental Workspace Index。

Runtime 必须基于文件变动集更新 workspace index。

MVP index 只服务 workspace 级问题聚合和 repair-oriented summary。

Workspace index 必须支持：

- 按 file path 更新 syntax file。
- 按 file path 删除 syntax file。
- 按 file path 查询 syntax file。
- 按 qualified type name 查询 Java type declaration。
- 按 annotation name 查询 Java type declaration。
- 按 import target 查询 Java file。

Runtime 进程退出后，MVP 不要求保留 index。

### 7.7 Parser Integration

Runtime 必须提供内部 parser integration。

Parser integration 固定使用 TypeScript 封装 `tree-sitter-xxx`。

Parser integration 必须按语言分派 parser。

MVP 固定支持 Java parser。

Java parser 固定使用 `tree-sitter-java`。

非 Java 文件不得进入 Java parser。

Parser integration 输出必须转换为 Runtime 自有对象。

Parser integration 不得向 Runtime 外部暴露 tree-sitter 原生对象。

### 7.8 Java Syntax Index

Runtime 必须对 `.java` 文件生成 Java syntax index。

Runtime 必须支持 Java 文件新增、修改和删除。

Runtime 必须解析 Java 文件中的以下结构：

- package declaration
- import declaration
- class declaration
- interface declaration
- enum declaration
- annotation declaration
- record declaration
- method declaration
- constructor declaration
- field declaration
- annotations
- modifiers
- extends / implements
- source range

Java syntax index 必须包含文件路径、package、imports、top-level types 和 parser diagnostics。

Java syntax index 不承担类型推断、继承解析、依赖解析或跨文件符号解析。

### 7.9 Parser Diagnostics

Runtime 必须把 parser error 转换为 parser diagnostic。

Parser diagnostic 必须包含：

- `message`
- `severity`
- `file`
- `position`
- `source`

Java parser diagnostic 的 `source.tool` 固定为 `tree-sitter-java`。

Parser diagnostic 的 `severity` 固定为 `WARN`。

单个文件解析失败不得中断整批检测。

Runtime 必须继续处理同一批次中的其他文件。

### 7.10 Native Rule Execution

Runtime 必须加载 Pickles native rules。

Runtime 必须基于当前变动集和 workspace index 执行 native rules。

Runtime 必须优先执行 native rules 以提供增量反馈。

Native rule execution 必须支持按变动文件收窄检查范围。

Native rule execution 必须支持 workspace-level Problem。

Native rule execution 不得依赖 ArchUnit 全量执行。

### 7.11 External Rule Adapter Execution

Runtime 必须根据 Pickles runtime config 执行启用的 adapter command。

MVP 固定支持：

- `adapter: "archunit"`
- `adapter: "eslint"`

命令为空且对应工具启用时，Runtime 必须返回配置缺失问题。

Runtime 必须在规则命令执行前完成当前变动集的 workspace index 更新。

Runtime 必须捕获命令的 stdout、stderr、exit code 和 timeout。

命令执行失败必须转换为可展示 Problem 或可展示错误。

ArchUnit adapter 不得作为增量反馈的唯一规则来源。

Runtime 不从 ArchUnit Java tests 反向读取规则。

### 7.12 Problem Aggregation

Runtime 必须把 Pickles native rule result 转换为 Problem。

Runtime 必须把 ArchUnit 输出转换为 Problem。

Runtime 必须把 ESLint 输出转换为 Problem。

Runtime 必须把 parser diagnostic 聚合为 Problem。

Problem 必须符合 `PROBLEM-MODEL-CONTRACT.md`。

Parser diagnostic 不得改变 ArchUnit / ESLint 的 Problem severity 规则。

Problem `source.tool` 必须保留来源工具：

- `pickles`
- `archunit`
- `eslint`
- `tree-sitter-java`

### 7.13 Repair-Oriented Summary

Runtime 必须向 Codex 提供 Repair-Oriented Summary。

Runtime 必须能基于 workspace index 和 Problem 生成面向 Agent 的修复摘要。

摘要必须包含：

- 变动文件。
- 变动 Java 文件。
- 受影响 Java type。
- 受影响 Java method。
- parser diagnostic。
- 当前规则命令返回的 Problem。

摘要不得包含 tree-sitter 原生节点结构。

MVP 不定义完整 Repair-Oriented Summary 稳定 JSON contract。

## 8. Key Flows

### 8.1 Detection Flow

1. Runtime 接收变动集。
2. Runtime 读取 Pickles runtime config。
3. Runtime 更新 Incremental Workspace Index。
4. Runtime 对 Java 文件生成 Java syntax index。
5. Runtime 加载 Pickles native rules。
6. Runtime 执行 Pickles native rules。
7. Runtime 执行启用的 external rule adapters。
8. Runtime 聚合 native rule result、parser diagnostic 和 adapter Problem。
9. Runtime 生成 Repair-Oriented Summary。
10. Runtime 返回 Problem Board 数据和治理反馈。

### 8.2 Java File Added Or Modified Flow

1. Runtime 接收 Java 文件新增或修改变动。
2. Runtime 校验 after content 大小。
3. Runtime 调用 Java parser。
4. Runtime 更新 Java syntax index。
5. Runtime 保存 parser diagnostic。

### 8.3 Java File Deleted Flow

1. Runtime 接收 Java 文件删除变动。
2. Runtime 删除 workspace index 中的对应文件项。
3. Runtime 保留其他文件 index。
4. Runtime 继续执行启用的规则命令。

### 8.4 Parser Failure Flow

1. Runtime 调用 parser。
2. Parser 返回 syntax error 或抛出异常。
3. Runtime 生成 parser diagnostic。
4. Runtime 继续处理批次中的其他文件。
5. Runtime 返回可展示 Problem。

## 9. Non-Functional Requirements

- Runtime 必须限制单文件解析输入大小。
- 初始单文件解析输入上限固定为 2 MiB。
- Runtime 必须限制单批次变动文件数。
- 初始单批次变动文件数上限固定为 200。
- Runtime 必须为外部命令执行设置超时。
- 初始命令执行超时时间固定为 30 秒。
- Runtime parser 测试必须使用真实 Java sample。
- Runtime index 测试必须覆盖新增、修改和删除。
- Runtime rule command 测试必须覆盖成功、失败和超时。
- Runtime native rule 测试必须覆盖可编程规则加载、执行和 Problem 归一化。
- Runtime Problem 聚合测试必须覆盖 parser diagnostic。
- Runtime public DTO 必须可 JSON 序列化。
- Runtime parser adapter 必须是 tree-sitter API 的唯一直接依赖点。
- Runtime 接入后必须纳入仓库验证脚本。

## 10. Open Items

- ArchUnit 输出解析规则。
- ESLint 输出解析规则。
- Incremental Workspace Index 的内部结构。
- Pickles native rule API 的具体 TypeScript 类型。
- Pickles runtime config 加载优先级。
- Repair-Oriented Summary 结构。
- Runtime 与 Plugin 的进程边界。
- Runtime 首次 workspace 全量索引触发时机。
