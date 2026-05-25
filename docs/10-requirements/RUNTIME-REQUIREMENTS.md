# Runtime Requirements

## 1. Purpose

本文档定义 Pickles Runtime 的 MVP 需求。

目标是明确 `pickles-runtime/` 作为治理执行内核必须完成的能力、内部技术约束和对其他模块的稳定边界。

## 2. Scope

当前范围：

- `.pickles/config.json` 读取和校验
- Incremental Workspace Index
- Java syntax index
- Runtime parser diagnostic
- ArchUnit 命令执行
- ESLint 命令执行
- Problem 聚合
- Repair-Oriented Summary

不在范围内：

- 不实现 IntelliJ UI
- 不实现 Codex Hook
- 不定义 HTTP endpoint
- 不实现 MCP server
- 不实现 Java 类型解析
- 不实现 classpath 符号解析
- 不保存全量语义持久化数据
- 不替代 ArchUnit / ESLint

## 3. Bounded Context

Runtime 服务于 IntelliJ Plugin。

IntelliJ Plugin 负责 UI、IDE 集成和本地 HTTP 入口。Runtime 负责读取配置、更新 workspace index、解析 Java syntax index、执行规则命令和聚合问题。

Runtime 内部固定使用 TypeScript + tree-sitter parser family。Java 语法解析固定使用 `tree-sitter-java`。

`tree-sitter-java` 只提供 Java 语法树能力。它不是 Java compiler，不提供类型解析、依赖解析或 classpath 语义。

## 4. Module Mapping

- `pickles-runtime/`: Runtime 实现。
- `docs/20-interfaces/PICKLES-CONFIG-CONTRACT.md`: 配置契约。
- `docs/20-interfaces/PROBLEM-MODEL-CONTRACT.md`: 问题模型契约。
- `docs/30-designs/RUNTIME-DESIGN.md`: Runtime 实现设计。

## 5. Core Objects

- `PicklesConfig`
- `ChangedFile`
- `IncrementalWorkspaceIndex`
- `JavaSyntaxFile`
- `JavaTypeDeclaration`
- `JavaMethodDeclaration`
- `ParserDiagnostic`
- `RuleCommand`
- `Problem`
- `RepairOrientedSummary`

## 6. Global Constraints

- Runtime 不拥有 UI。
- Runtime 实现语言固定为 Node.js / TypeScript。
- Runtime 运行环境固定为 Node.js 22。
- Runtime 必须使用 TypeScript strict mode。
- Runtime 必须使用 ESM。
- Runtime 内部 parser 固定使用 tree-sitter parser family。
- Runtime Java parser 固定使用 `tree-sitter-java`。
- Runtime 必须封装 tree-sitter API。
- Runtime 不得向 Plugin、Hook、rules 或 MCP 暴露 tree-sitter 原生对象。
- Runtime 不修改业务代码、测试代码或工程实现代码。
- Runtime 不执行 Java source code。
- Runtime 读取目标工程 `.pickles/config.json`。
- Runtime 直接调用 `.pickles/config.json` 中配置的用户工程命令。
- Runtime MVP 必须按文件重建 Java syntax index，不依赖 tree-sitter incremental edit API。
- Runtime 生成的 range 固定使用 1-based line 和 1-based column。
- MVP 只支持 ArchUnit 与 ESLint。
- MVP 只使用规则工具返回的 `ERROR` / `WARN`。

## 7. Functional Requirements

### 7.1 Runtime Package

`pickles-runtime/` 必须成为独立 Node.js / TypeScript package。

Runtime 必须提供以下脚本：

- `npm run build`
- `npm run typecheck`
- `npm run test`
- `npm run lint`

### 7.2 Config Loading

Runtime 必须从目标工程 `.pickles/config.json` 读取配置。

Runtime 必须校验 `.pickles/config.json` 是否是合法 JSON。

配置读取失败时，Runtime 必须返回可展示错误。

### 7.3 Workspace Index

Runtime 必须维护 Incremental Workspace Index。

Runtime 必须基于 Hook 上报的文件名、before 内容和 after 内容更新 workspace index。

MVP index 只服务 workspace 级问题聚合。

Workspace index 必须支持：

- 按 file path 更新 Java syntax file。
- 按 file path 删除 Java syntax file。
- 按 file path 查询 Java syntax file。
- 按 qualified type name 查询 Java type declaration。
- 按 annotation name 查询 Java type declaration。
- 按 import target 查询 Java file。

### 7.4 Java Syntax Parsing

Runtime 必须只对 `.java` 文件执行 Java syntax parsing。

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

解析结果必须转换为 Runtime 自有 DTO。

### 7.5 Parser Diagnostics

Runtime 必须把 Java parse error 转换为 parser diagnostic。

Parser diagnostic 必须包含：

- `message`
- `severity`
- `file`
- `position`
- `source`

Parser diagnostic 的 `severity` 固定为 `WARN`。

Parser diagnostic 的 `source.tool` 固定为 `tree-sitter-java`。

单个 Java 文件解析失败不得中断整批检测。

### 7.6 Rule Execution

Runtime 必须根据 `.pickles/config.json` 执行启用的规则命令：

- `rules.archunit.command`
- `rules.eslint.command`

命令为空且对应工具启用时，Runtime 必须返回配置缺失问题。

Runtime 必须在规则命令执行前更新 Java syntax index。

### 7.7 Problem Aggregation

Runtime 必须把 ArchUnit 与 ESLint 输出转换为 Problem。

Runtime 必须把 parser diagnostic 聚合为 Problem。

Problem 必须符合 `PROBLEM-MODEL-CONTRACT.md`。

Parser diagnostic 不得改变 ArchUnit / ESLint 的 Problem severity 规则。

### 7.8 Repair-Oriented Summary

Runtime 必须向 Codex 提供 Repair-Oriented Summary。

Runtime 必须能基于 Java syntax index 生成面向 Agent 的修复摘要。

摘要必须包含：

- 变动 Java 文件。
- 受影响 type。
- 受影响 method。
- parser diagnostic。
- 当前规则命令返回的 Problem。

摘要不得包含 tree-sitter 原生节点结构。

MVP 不定义完整 Repair-Oriented Summary 稳定 JSON contract。

## 8. Key Flows

### 8.1 Detection Flow

1. Runtime 接收变动集。
2. Runtime 更新 Incremental Workspace Index。
3. Runtime 解析 Java 文件并更新 Java syntax index。
4. Runtime 读取 `.pickles/config.json`。
5. Runtime 执行启用的规则命令。
6. Runtime 聚合 parser diagnostic 和规则 Problem。
7. Runtime 返回 Problem Board 数据。

### 8.2 Java Deleted Flow

1. Runtime 接收 Java 文件删除变动。
2. Runtime 删除 workspace index 中的对应文件项。
3. Runtime 执行启用的规则命令。
4. Runtime 聚合并返回 Problem。

### 8.3 Parser Failure Flow

1. Runtime 调用 Java parser。
2. Parser 返回 syntax error 或抛出异常。
3. Runtime 生成 parser diagnostic。
4. Runtime 继续处理批次中的其他文件。
5. Runtime 返回可展示 Problem。

## 9. Non-Functional Requirements

- 命令执行必须带有超时。
- 命令失败必须转换为可展示 Problem 或可展示错误。
- Runtime 不写 `.pickles/config.json`。
- Runtime 不保存全量语义持久化数据。
- Runtime 必须限制单文件解析输入大小。
- 初始上限固定为 2 MiB。
- Runtime 必须限制单批次变动文件数。
- 初始上限固定为 200。
- Runtime parser 测试必须使用真实 Java sample。
- Runtime index 测试必须覆盖新增、修改和删除。
- Runtime Problem 聚合测试必须覆盖 parser diagnostic。
- Runtime public DTO 必须可 JSON 序列化。
- Runtime parser adapter 必须是 tree-sitter API 的唯一直接依赖点。
- Runtime 接入后必须纳入仓库验证脚本。

## 10. Open Items

- 命令执行超时时间。
- ArchUnit 输出解析规则。
- ESLint 输出解析规则。
- Incremental Workspace Index 的内部结构。
- Repair-Oriented Summary 结构。
- Runtime 与 Plugin 的进程边界。
- Runtime 首次 workspace 全量索引触发时机。
