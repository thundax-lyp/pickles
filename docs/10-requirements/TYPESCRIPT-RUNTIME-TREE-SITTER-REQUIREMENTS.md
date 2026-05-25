# TypeScript Runtime Tree-Sitter Requirements

## 1. Purpose

本文档定义 `pickles-runtime/` 使用 Node.js / TypeScript 接入 `tree-sitter-java` 的需求。

目标是确认 Runtime 语言、Java 语法索引能力、模块边界和验收标准，再由设计文档定义实现结构。

## 2. Scope

当前范围：

- Runtime 实现语言。
- Java 文件语法解析。
- Java syntax index。
- Incremental Workspace Index 更新。
- Parser diagnostic 到 Problem 的转换。
- Runtime 与 Plugin / Hook / rules / MCP 的职责边界。

不在范围内：

- 不定义具体目录结构。
- 不定义具体类和函数实现。
- 不定义 tree-sitter query 细节。
- 不实现 Java 类型解析。
- 不实现 classpath 符号解析。
- 不实现跨仓库全量语义持久化。
- 不替代 ArchUnit / ESLint。
- 不定义 MCP summary contract。

## 3. Bounded Context

`pickles-runtime/` 是治理执行内核。

Runtime 必须使用 Node.js / TypeScript 实现。Runtime 必须负责 Java 文件语法解析、workspace index 更新、规则命令执行和 Problem 聚合。

`tree-sitter-java` 只提供 Java 语法树能力。它不是 Java compiler，不提供类型解析、依赖解析或 classpath 语义。

IntelliJ Plugin、Codex Hook、rules 和 MCP 不拥有 Java parser。

## 4. Module Mapping

- `pickles-runtime/`: Node.js / TypeScript Runtime、Java syntax parsing、workspace index、规则命令执行、Problem 聚合。
- `pickles-intellij-plugin/`: Runtime 编排、IDE 展示、本地 HTTP 入口、文件跳转。
- `pickles-hooks/`: 文件变动捕获、before / after 内容提交。
- `pickles-rules/`: 规则配置、命令模板和治理声明。
- `pickles-mcp/`: 后续读取 Runtime 稳定输出，不直接读取 parser。
- `docs/30-designs/TYPESCRIPT-RUNTIME-TREE-SITTER-DESIGN.md`: 本需求的实现设计。

## 5. Core Objects

- `ChangedFile`
- `JavaSyntaxFile`
- `JavaTypeDeclaration`
- `JavaMethodDeclaration`
- `WorkspaceIndex`
- `ParserDiagnostic`
- `Problem`
- `RepairOrientedSummary`

## 6. Global Constraints

- Runtime 实现语言固定为 Node.js / TypeScript。
- Runtime 运行环境固定为 Node.js 22。
- Runtime 必须使用 TypeScript strict mode。
- Runtime 必须使用 ESM。
- Runtime 必须通过 lockfile 固定 npm 依赖版本。
- Runtime 必须封装 tree-sitter API。
- Runtime 不得向 Plugin、Hook、rules 或 MCP 暴露 tree-sitter 原生对象。
- Runtime 不得修改用户业务代码。
- Runtime 不得执行 Java source code。
- Runtime 不得保存全量语义持久化数据。
- Runtime MVP 必须按文件重建 Java syntax index，不依赖 tree-sitter incremental edit API。
- Runtime 生成的 range 固定使用 1-based line 和 1-based column。

## 7. Functional Requirements

### 7.1 Runtime Package

`pickles-runtime/` 必须成为独立 Node.js / TypeScript package。

Runtime 必须提供以下脚本：

- `npm run build`
- `npm run typecheck`
- `npm run test`
- `npm run lint`

### 7.2 Java File Detection

Runtime 必须只对 `.java` 文件执行 Java syntax parsing。

Runtime 必须忽略非 Java 文件的 Java syntax parsing。

Runtime 必须支持 Java 文件新增、修改和删除。

### 7.3 Java Syntax Parsing

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

### 7.4 Workspace Index

Runtime 必须维护进程内 workspace index。

Workspace index 必须支持：

- 按 file path 更新 Java syntax file。
- 按 file path 删除 Java syntax file。
- 按 file path 查询 Java syntax file。
- 按 qualified type name 查询 Java type declaration。
- 按 annotation name 查询 Java type declaration。
- 按 import target 查询 Java file。

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

### 7.6 Problem Aggregation

Runtime 必须把 parser diagnostic 聚合为 Problem。

Runtime 必须继续聚合 ArchUnit / ESLint 输出。

Parser diagnostic 不得改变 ArchUnit / ESLint 的 Problem severity 规则。

### 7.7 Repair-Oriented Summary

Runtime 必须能基于 Java syntax index 生成面向 Agent 的修复摘要。

摘要必须包含：

- 变动 Java 文件。
- 受影响 type。
- 受影响 method。
- parser diagnostic。
- 当前规则命令返回的 Problem。

摘要不得包含 tree-sitter 原生节点结构。

### 7.8 Failure Handling

单个 Java 文件解析失败不得中断整批检测。

Runtime 必须把 parser failure 转换为可展示 diagnostic 或可展示 error。

Runtime 必须继续处理同一批次中的其他文件。

### 7.9 Size Limits

Runtime 必须限制单文件解析输入大小。

初始上限固定为 2 MiB。

Runtime 必须限制单批次变动文件数。

初始上限固定为 200。

## 8. Key Flows

### 8.1 Java Modified Flow

1. Hook 捕获 Java 文件 before / after。
2. Plugin 或 Runtime 接收变动集。
3. Runtime 识别 `.java` 文件。
4. Runtime 解析 after 内容。
5. Runtime 更新 workspace index。
6. Runtime 执行启用的规则命令。
7. Runtime 聚合 parser diagnostic 和规则 Problem。
8. Plugin 展示 Problem Board。

### 8.2 Java Deleted Flow

1. Hook 捕获 Java 文件删除。
2. Runtime 接收删除变动。
3. Runtime 删除 workspace index 中的对应文件项。
4. Runtime 执行启用的规则命令。
5. Runtime 聚合并返回 Problem。

### 8.3 Parser Failure Flow

1. Runtime 调用 Java parser。
2. Parser 返回 syntax error 或抛出异常。
3. Runtime 生成 parser diagnostic。
4. Runtime 继续处理批次中的其他文件。
5. Runtime 返回可展示 Problem。

## 9. Non-Functional Requirements

- Runtime parser 测试必须使用真实 Java sample。
- Runtime index 测试必须覆盖新增、修改和删除。
- Runtime Problem 聚合测试必须覆盖 parser diagnostic。
- Runtime public DTO 必须可 JSON 序列化。
- Runtime parser adapter 必须是 tree-sitter API 的唯一直接依赖点。
- Runtime 接入后必须纳入仓库验证脚本。

## 10. Open Items

- Runtime 与 Plugin 的进程边界。
- Runtime 首次 workspace 全量索引触发时机。
- Repair-oriented summary 的稳定 JSON contract。
