# Runtime Design

## 1. Purpose

本文档定义 `pickles-runtime/` 的完整设计。

目标是让 Runtime 作为治理执行内核完成配置读取、Java 语法级索引、规则命令执行、Problem 聚合和 repair-oriented summary，并保持 IntelliJ Plugin、Codex Hook、MCP 与规则声明之间的边界清晰。

需求来源固定为 [`../10-requirements/RUNTIME-REQUIREMENTS.md`](../10-requirements/RUNTIME-REQUIREMENTS.md)。

## 2. Scope

当前范围：

- Runtime project setup。
- Pickles runtime config 读取。
- Incremental Workspace Index。
- Java 文件语法解析。
- External adapter config。
- Problem 聚合。
- Runtime 内部对象边界。
- Runtime 对 Plugin / Hook / MCP 的公开契约边界。
- 初始实现顺序和验证入口。

不在范围内：

- 不重新定义需求验收口径。
- 不实现 Java 类型解析。
- 不实现 classpath 符号解析。
- 不实现跨仓库全量语义持久化。
- 不替代 ArchUnit / ESLint 命令执行。
- 不把 tree-sitter API 暴露给 Plugin、Hook、MCP 或 rules。
- 不在 IntelliJ Plugin 进程内直接运行 tree-sitter。

## 3. Bounded Context

`pickles-runtime/` 固定作为治理执行内核。

Node.js / TypeScript Runtime 负责读取目标工程文件内容、解析 Java 语法结构、更新 workspace index、执行外部规则命令并聚合 Problem。

IntelliJ Plugin 负责 IDE 集成、Problem Board 展示和本地 HTTP 入口。Plugin 只调用 Runtime 的稳定入口，不依赖 tree-sitter API。

Codex Hook 负责捕获 before / after 文件变动并通知 Plugin 或 Runtime。Hook 不解析 Java。

`tree-sitter-java` 固定作为 Runtime 的 Java syntax parser。它只提供语法树，不提供 Java 编译语义。

## 4. Module Mapping

- `pickles-runtime/`: Node.js / TypeScript Runtime、Java syntax parser adapter、Incremental Workspace Index、规则命令执行、Problem 聚合。
- `pickles-intellij-plugin/`: Runtime 启动、HTTP 入口编排、Problem Board 展示、文件跳转。
- `pickles-hooks/`: Codex Hook、文件变动捕获、before / after 内容提交。
- `pickles-rules/`: Native rule authoring contract、规则说明书、规则模板和可复用规则包。
- `pickles-mcp/`: 后续读取 Runtime 产出的稳定 summary 或 query 入口。
- `docs/20-interfaces/HOOK-PLUGIN-HTTP-CONTRACT.md`: Hook 到 Plugin 的 HTTP 契约。
- `docs/20-interfaces/PROBLEM-MODEL-CONTRACT.md`: Runtime 输出 Problem 的契约。
- `docs/20-interfaces/PICKLES-CONFIG-CONTRACT.md`: Runtime 读取 Pickles runtime config 的契约。

## 5. Core Objects

### 5.1 `RuntimeHost`

Runtime 进程入口。

固定职责：

- 加载 workspace。
- 读取 Pickles runtime config。
- 接收变动集。
- 调用 `WorkspaceIndexService`。
- 调用规则执行器。
- 返回 Problem 与 repair-oriented summary。

### 5.2 `ChangedFile`

Hook 或 Plugin 提交给 Runtime 的文件变动。

固定字段：

- `path`
- `language`
- `changeType`
- `beforeContent`
- `afterContent`

`language` 对 Java 文件固定为 `java`。

### 5.3 `JavaSyntaxParser`

Runtime 内部 parser adapter。

固定职责：

- 封装 `tree-sitter` 与 `tree-sitter-java`。
- 接收 Java source text。
- 返回 `JavaSyntaxFile`。
- 隐藏 tree-sitter node、tree、query 等第三方 API。

### 5.4 `JavaSyntaxFile`

Java 文件的语法级索引结果。

固定字段：

- `path`
- `packageName`
- `imports`
- `topLevelTypes`
- `diagnostics`

### 5.5 `JavaTypeDeclaration`

Java 类型声明。

固定字段：

- `kind`
- `name`
- `qualifiedName`
- `annotations`
- `modifiers`
- `extendsTypes`
- `implementsTypes`
- `methods`
- `fields`
- `range`

`kind` 固定为 `class`、`interface`、`enum`、`annotation` 或 `record`。

### 5.6 `JavaMethodDeclaration`

Java 方法或构造器声明。

固定字段：

- `name`
- `kind`
- `annotations`
- `modifiers`
- `returnType`
- `parameters`
- `throwsTypes`
- `range`

`kind` 固定为 `method` 或 `constructor`。

### 5.7 `RuleCommandRunner`

规则命令执行入口。

固定职责：

- 读取 Pickles runtime config 中启用的规则命令。
- 执行 ArchUnit / ESLint 命令。
- 捕获 stdout / stderr / exit code。
- 将命令失败转换为可展示错误或 Problem 输入。

### 5.8 `WorkspaceIndexService`

workspace index 更新入口。

固定职责：

- 判断变动文件语言。
- 对 Java 文件调用 `JavaSyntaxParser`。
- 对删除文件移除索引项。
- 保留每个文件最新 `JavaSyntaxFile`。
- 为规则执行和 summary 生成提供查询能力。

### 5.9 `RuntimeProblemAggregator`

Problem 聚合入口。

固定职责：

- 接收 ArchUnit / ESLint / Runtime parser diagnostics。
- 转换为 `PROBLEM-MODEL-CONTRACT.md` 定义的 Problem。
- 保留 `source.tool`。

Tree-sitter parser diagnostics 的 `source.tool` 固定为 `tree-sitter-java`。

## 6. Global Constraints

- Runtime 实现语言固定为 Node.js / TypeScript。
- Runtime 固定使用 ESM。
- Runtime 固定运行在 Node.js 22。
- Runtime 固定以 `package-lock.json` 锁定依赖版本。
- Runtime 固定使用 TypeScript strict mode。
- `tree-sitter` 和 `tree-sitter-java` 只允许出现在 `pickles-runtime/` 依赖中。
- Runtime 不向外暴露 tree-sitter 原生对象。
- Runtime 不修改用户业务代码。
- Runtime 不保存全量语义持久化数据。
- Runtime index 固定为进程内状态，MVP 不落盘。
- Runtime parser failure 必须转换为可展示 diagnostic 或 Runtime error。
- Java syntax index 不替代 ArchUnit。
- Java syntax index 不替代 Java compiler。
- Java syntax index 不执行用户代码。
- Java syntax index 只读取 Hook 提供的 after 内容或 workspace 文件内容。

## 7. Functional Requirements

### 7.1 Runtime Project Setup

`pickles-runtime/` 必须成为独立 Node.js / TypeScript package。

固定文件：

- `pickles-runtime/package.json`
- `pickles-runtime/package-lock.json`
- `pickles-runtime/tsconfig.json`
- `pickles-runtime/src/`
- `pickles-runtime/test/`

固定脚本：

- `npm run build`
- `npm run typecheck`
- `npm run test`
- `npm run lint`

`lint` 在初始阶段固定等同于 `typecheck`，直到引入独立 lint 工具。

### 7.2 Tree-Sitter Dependency

Runtime 必须通过 npm 依赖接入 tree-sitter。

固定依赖：

- `tree-sitter`
- `tree-sitter-java`

依赖版本由 `pickles-runtime/package-lock.json` 固定。设计文档不承担版本锁定职责。

### 7.3 Java File Detection

Runtime 必须只对 `.java` 文件执行 Java syntax parse。

非 Java 文件进入 workspace index 时不得调用 `JavaSyntaxParser`。

删除 `.java` 文件时，Runtime 必须从 index 中删除对应 `JavaSyntaxFile`。

### 7.4 Java Syntax Parse

`JavaSyntaxParser` 必须解析以下结构：

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

解析结果必须使用 Runtime 自有 DTO。

### 7.5 Parse Diagnostics

当 tree-sitter 结果包含 syntax error 时，Runtime 必须生成 parser diagnostic。

Parser diagnostic 必须包含：

- `message`
- `severity`
- `file`
- `position`
- `source`

`severity` 固定为 `WARN`。

### 7.6 Incremental Index Update

Runtime 必须基于 `ChangedFile` 更新 index。

更新规则：

- `added`：解析 after 内容并写入 index。
- `modified`：解析 after 内容并替换 index。
- `deleted`：删除 index 项。
- `unchanged`：不更新 index。

Runtime 不依赖 tree-sitter incremental edit API 完成 MVP。MVP 固定按文件重建语法索引。

### 7.7 Query API

Runtime 内部必须提供最小 query 能力。

固定 query：

- 按文件路径读取 `JavaSyntaxFile`。
- 按 qualified type name 查找 `JavaTypeDeclaration`。
- 按 annotation name 查找 type。
- 按 import target 查找 file。

Query API 只返回 Runtime DTO。

### 7.8 Rule Command Execution

Runtime 继续执行 Pickles runtime config 中配置的 ArchUnit / ESLint 命令。

Tree-sitter Java syntax index 固定在命令执行前更新。

Rule command output 仍按既有 Problem 聚合规则处理。

命令为空且对应工具启用时，Runtime 必须返回配置缺失问题。

### 7.9 Repair-Oriented Summary

Runtime 生成 repair-oriented summary 时可以读取 Java syntax index。

Summary 中不得包含 tree-sitter 原生节点结构。

Summary 必须面向 Agent 修复任务表达：

- 变动 Java 文件。
- 受影响 type。
- 受影响 method。
- parser diagnostic。
- 当前规则命令返回的 Problem。

### 7.10 Plugin Integration

Plugin 固定通过稳定 Runtime 入口触发检测。

Plugin 不直接调用 `JavaSyntaxParser`。

Plugin 不读取 Runtime 内部 index。

Plugin 只接收 Problem Board 数据、health 状态和可展示 summary。

## 8. Key Flows

### 8.1 Java File Modified Flow

1. Hook 捕获 Java 文件 before / after。
2. Hook 通过 HTTP 通知 Plugin。
3. Plugin 将变动集交给 Runtime。
4. Runtime 识别 `.java` 文件。
5. Runtime 调用 `JavaSyntaxParser` 解析 after 内容。
6. Runtime 更新 `WorkspaceIndexService`。
7. Runtime 执行启用的 ArchUnit / ESLint 命令。
8. Runtime 聚合 parser diagnostic 与规则命令 Problem。
9. Plugin 刷新 Problem Board。

### 8.2 Java File Deleted Flow

1. Hook 捕获 Java 文件删除。
2. Runtime 接收 `changeType = deleted`。
3. Runtime 删除对应 `JavaSyntaxFile`。
4. Runtime 执行启用的规则命令。
5. Runtime 聚合并返回 Problem。

### 8.3 Runtime Startup Flow

1. Plugin 启动 Runtime 进程或 Runtime host。
2. Runtime 读取 workspace root。
3. Runtime 初始化空 index。
4. Runtime 等待 Hook / Plugin 提交变动集。
5. 首次检测时 Runtime 按需要读取 workspace 文件建立初始 index。

### 8.4 Parser Failure Flow

1. Runtime 调用 `JavaSyntaxParser`。
2. Parser 抛出异常或返回异常结果。
3. Runtime 捕获失败。
4. Runtime 生成 Runtime diagnostic。
5. Runtime 继续处理同一批次中其他文件。
6. Runtime 返回可展示 Problem。

## 9. Non-Functional Requirements

- 单个 Java 文件 parse failure 不得中断整批检测。
- Runtime 必须限制单文件解析输入大小，初始上限固定为 2 MiB。
- Runtime 必须限制单批次变动文件数，初始上限固定为 200。
- Runtime 必须为外部命令执行设置超时。
- Runtime 必须将 parser、index、command、aggregation 分层测试。
- Runtime parser test 必须使用真实 Java sample。
- Runtime 对 tree-sitter 的依赖必须集中在 `JavaSyntaxParser`。
- Runtime public DTO 必须可 JSON 序列化。
- Runtime 内部 range 固定使用 1-based line 和 1-based column，对齐 Problem navigation。

## 10. Implementation Order

固定实现顺序：

1. 创建 `pickles-runtime/` TypeScript package。
2. 建立 build / typecheck / test / lint 脚本。
3. 接入 `tree-sitter` 与 `tree-sitter-java`。
4. 实现 `JavaSyntaxParser` 最小 parse smoke test。
5. 实现 `JavaSyntaxFile` / `JavaTypeDeclaration` / `JavaMethodDeclaration` DTO。
6. 实现 package / import / top-level type 提取。
7. 实现 method / constructor / field 提取。
8. 实现 parser diagnostic 转 Problem。
9. 实现 `WorkspaceIndexService` 文件级更新。
10. 实现最小 query API。
11. 实现 Pickles runtime config 读取。
12. 实现 ArchUnit / ESLint 命令执行入口。
13. 将 Runtime 检测入口接入 Plugin 编排。
14. 将 Runtime 验证接入 `scripts/verify-all.sh`。

## 11. Verification

固定验证入口：

- 在 `pickles-runtime/` 运行 `npm run typecheck`。
- 在 `pickles-runtime/` 运行 `npm run test`。
- 在 `pickles-runtime/` 运行 `npm run lint`。
- 在仓库根目录运行 `npm run format:check`。
- Runtime 接入总体验证后，运行 `scripts/verify-all.sh`。

初始 parser 测试必须覆盖：

- 普通 class。
- interface。
- enum。
- annotation。
- record。
- nested type。
- annotated method。
- constructor。
- syntax error。

## 12. Open Items

- Runtime 与 Plugin 的进程边界：同进程调用 TypeScript bundle、独立 Node 子进程、本地 HTTP server 三者必须在实现前确定。
- Runtime 首次 workspace 全量索引触发时机必须在 Plugin 编排设计中确定。
- Repair-oriented summary 的稳定 JSON contract 必须在 MCP 接入前单独定义。
