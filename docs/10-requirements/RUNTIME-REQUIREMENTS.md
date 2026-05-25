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

## 3. Bounded Context

Runtime 服务于 IntelliJ Plugin。

IntelliJ Plugin 负责 UI、IDE 集成、本地 HTTP 入口和文件跳转。Runtime 负责读取配置、更新 workspace index、解析语法索引、加载规则定义、执行规则、聚合 Problem，并生成面向 Agent 的治理反馈。

IntelliJ Plugin 固定作为 Runtime lifecycle owner。MVP 中 Plugin 启动并管理独立 Node.js Runtime 子进程。

Runtime 与 Plugin 固定通过 stdio JSON request / response 通信。

Runtime MVP 不暴露 HTTP server。

Codex Hook 固定只与 IntelliJ Plugin 本地 HTTP server 通信，不直接调用 Runtime。

Runtime 固定作为 Pickles Rule Engine。Runtime 不只是外部命令执行器。

Pickles native rules 固定使用类似 ESLint config 的 JS / TS 可编程规则定义。规则入口固定由 Pickles runtime config 指向。

Pickles runtime config 固定作为规则声明和配置真相源。Agent-side skill 生成规则时，默认将规则声明写入 Pickles runtime config；当需要独立 rule module 或辅助实现时，生成物默认放入目标工程 `.pickles/rules/`，并由 Pickles runtime config 显式 import。

Runtime MVP 的主路径固定为 Pickles native rule execution。Runtime MVP 不执行 ArchUnit / ESLint 命令。

ArchUnit 和 ESLint 可以作为后续迁移参考或兼容输入，不作为 Runtime MVP 执行能力。

Runtime 内部实现语言固定为 Node.js / TypeScript。

Runtime 内部 parser 体系固定为 `tree-sitter-xxx`。Java 语法解析固定使用 `tree-sitter-java`。

`tree-sitter-java` 只提供 Java 语法树能力。它不是 Java compiler，不提供类型解析、依赖解析、classpath 语义或跨文件符号解析。

IntelliJ Plugin、Codex Hook、rules 和 MCP 不拥有 parser，不直接依赖 tree-sitter API。

`pickles-agent-governance-skill` 是 Agent 侧治理入口，负责提示 Agent 读取规则、运行检查、理解 Problem 并复检。它与 IntelliJ Plugin 对等，二者都不拥有规则真相和 Rule Engine。

`pickles-rule-authoring-skill` 负责规则创作、迁移、解释和维护。它不执行规则。

Runtime 不加载 skill。Runtime 只加载 Pickles runtime config、native rule module 和 plugin rule package。

Runtime 不自动扫描 `.pickles/` 发现规则或检测脚本。`.pickles/` 只承载被 Pickles runtime config 显式引用的项目本地生成物和运行时状态。

## 4. Module Mapping

- `pickles-runtime/`: Rule Engine 实现、配置读取、workspace index、parser integration、native rule execution 和 Problem 聚合。
- `pickles-intellij-plugin/`: Runtime 编排、IDE 展示、本地 HTTP 入口和文件跳转。
- `pickles-hooks/`: 文件变动捕获、before / after 内容提交。
- `pickles-rules/`: Pickles native rule authoring contract、规则说明书、规则模板和可复用规则包。
- `pickles-skills/pickles-rule-authoring-skill/`: Agent 侧规则创作 skill，生成和维护 Pickles rules。
- `pickles-skills/pickles-agent-governance-skill/`: Agent 侧治理 skill，提示 Agent 运行检查、解释 Problem 并引导复检。
- `pickles-mcp/`: 读取 Runtime 稳定输出，不直接读取 parser。
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
- `ProblemInput`
- `SyntaxParser`
- `SyntaxIndex`
- `JavaSyntaxFile`
- `JavaTypeDeclaration`
- `JavaMethodDeclaration`
- `ParserDiagnostic`
- `Problem`
- `RepairOrientedSummary`

## 6. Global Constraints

- Runtime 不拥有 UI。
- Runtime 不拥有 HTTP server。
- Runtime 实现语言固定为 Node.js / TypeScript。
- Runtime 运行环境固定为 Node.js 22。
- Runtime 的 config loader、native rule 和 Runtime 调用的 `.pickles/*` 脚本固定以 Node.js 22 作为运行与验证基线。
- Runtime 不依赖 Codex、Claude Code 或其他 Agent CLI 当前进程内的 Node.js 版本。
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
- Runtime 不自动扫描 `.pickles/` 发现 native rule 或检测脚本。
- Runtime stdout 固定只用于 stdio JSON response。
- Runtime stderr 固定用于日志。
- Runtime 不修改业务代码、测试代码或工程实现代码。
- Runtime 不执行用户业务代码。
- Runtime 不执行 Java source code。
- Runtime 不写 Pickles runtime config。
- Runtime 不保存全量语义持久化数据。
- Runtime MVP 必须按文件重建 syntax index，不依赖 tree-sitter incremental edit API。
- Runtime 生成的 range 固定使用 1-based line 和 1-based column。
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

配置加载优先级固定为 `pickles.config.ts`、`pickles.config.mjs`、`pickles.config.js`。

Runtime 固定使用自身依赖的 `tsx` 加载 `pickles.config.ts`。

目标工程不需要安装 `tsx`。

`pickles.config.ts`、native rule 和由 Runtime 调用的 `.pickles/*` TypeScript 文件固定按 Node.js 22 + ESM 语义加载。

Runtime 不读取目标工程 `tsconfig` 的 path alias 作为 MVP config 加载能力。

Runtime 必须校验配置加载结果。

配置不存在、加载失败或字段非法时，Runtime 必须返回可展示错误。

Runtime 不得自动修复、写入或重排 Pickles runtime config。

Rule plugin package 固定通过 Pickles runtime config 中的标准 ESM import 引用。

Runtime 只读取 Pickles runtime config 最终导出的 `rules` 数组。

Runtime 不自动发现 rule package。

Runtime 不读取 rule package metadata 作为规则入口。

Runtime MVP 不支持字符串式 rule package 引用。

Runtime MVP 不支持 `plugins` 字段自动展开规则。

### 7.3 Programmable Rule Config

Runtime 必须支持类似 ESLint config 的 JS / TS 可编程规则定义。

可编程规则配置必须能表达：

- rule id
- rule title
- rule message
- rule fix hint
- rule type
- severity
- language scope
- file include / exclude
- rule options
- rule implementation

Rule implementation 必须通过 Runtime 提供的 `RuleContext` 获取输入。

Rule metadata 必须至少包含 `id`、`title`、`message`、`type`、`severity`、`language` 和 `files`。

Rule metadata 的 `message` 必须作为 Agent-facing 默认规则反馈。

Rule metadata 的 `fixHint` 必须作为可选 Agent-facing 默认修复建议。

RuleContext 固定分为通用层和 language-specific helper。

通用层必须提供文件查询、受控 syntax query 和 problem factory。

Language-specific helper 按语言扩展。

Runtime MVP 只实现 Java helper。

Native rule 的 `language` 字段必须为 string。Runtime MVP 只支持 `java`，遇到未支持 language 必须返回配置错误。

`RuleContext` 必须至少提供：

- workspace root
- changed files
- file query
- syntax query
- Java helper
- problem factory

Native rule 固定为 lint-style 纯检查函数。

Native rule 只能通过 `RuleContext` 和 JSON-serializable `options` 获取输入。

Rule implementation 必须返回 `ProblemInput[]` 或 `Promise<ProblemInput[]>`。

Native rule helper 必须遵守同样的无副作用约束。

Native rule 不得修改文件、创建目录、启动外部命令、访问网络、依赖 Agent / IDE / Hook / MCP 内部状态、直接使用 parser 原生对象、读取隐藏全局状态或保存跨 invocation 状态。

Rule author 调用 `ctx.problem(input)` 时，`input.message` 固定必填。

`ProblemInput.title` 固定可选，未提供时 Runtime 必须使用 rule metadata 的 `title`。

`ProblemInput.fixHint` 固定可选，未提供时 Runtime 必须使用 rule metadata 的 `fixHint` 或 `null`。

`ProblemInput.file` 固定为 string 或 `null`。

`ProblemInput.position` 固定为 `Position` 或 `null`。

`ProblemInput` 不得覆盖 `severity`。

`ProblemInput` 不得覆盖 `source`。

Runtime 必须为 native rule problem 自动补齐 `source.tool` 和 `source.rule`。

Native rule Problem 的 `source.tool` 固定为 `pickles-native`。

Runtime 必须把 rule result 归一化为 Problem。

Runtime 不得要求 rule implementation 直接读取 tree-sitter 原生对象。

Runtime 不得要求 rule implementation 直接读取 Plugin 或 Hook 内部状态。

Runtime 必须提供受控 syntax query 能力。

Syntax query 必须返回 Pickles-owned DTO。

Syntax query result 必须包含 capture 信息。

Runtime 不得向 rule implementation 暴露 raw tree-sitter node。

### 7.4 AI-Generated Rule Support

Pickles native rule API 必须适合 AI 根据说明书生成。

`pickles-rules/` 必须承载 Pickles native rule authoring contract、规则说明书和可复用规则模板。

MVP rule templates 固定放在 `pickles-rules/templates/`。

MVP rule templates 固定只提供 Java native rule 模板：

- file-level native rule template。
- Java syntax query rule template。
- workspace-level native rule template。

MVP 不提供 external adapter、ArchUnit、ESLint、TypeScript、Python、auto-fix 或 package publishing 模板。

`pickles-rule-authoring-skill` 必须使用规则说明书生成和维护 Pickles rules。

`pickles-rule-authoring-skill` workflow 固定为：

1. 将用户规则意图整理为 rule spec。
2. 在 file-level native rule、Java syntax query rule 和 workspace-level native rule 模板中选择一个。
3. 生成或更新 Pickles runtime config、`.pickles/rules/*` 或 rule package。
4. 对生成物做形态自检。
5. 将复检交给 Runtime、CLI、MCP 或 Plugin 的稳定检查入口。
6. 后续维护必须按 rule id 定位并收窄修改。

`pickles-rule-authoring-skill` 不得执行 rule。

`pickles-rule-authoring-skill` 不得自行判断项目是否违规。

`pickles-rule-authoring-skill` 的形态自检只检查生成物是否遵守 Pickles native rule contract，不替代 Runtime 检测。

`pickles-rule-authoring-skill` 生成 native rule 声明时，默认写入 `pickles.config.js`、`pickles.config.mjs` 或 `pickles.config.ts`。

`pickles-rule-authoring-skill` 生成项目本地 native rule module 或辅助实现时，默认写入目标工程 `.pickles/rules/`。

`.pickles/rules/` 内部目录结构在 MVP 不继续细分。

Pickles runtime config 必须显式 import `.pickles/rules/*` 中需要启用的 rule。

`.pickles/` 不得成为第二个规则真相源。

`pickles-agent-governance-skill` 必须使用 Runtime、CLI、MCP 或 Plugin 暴露的稳定检查入口提示 Agent 执行规则检查。

`pickles-agent-governance-skill` 不得自行实现 parser 或 Rule Engine。

规则说明书必须能让 AI 生成以下内容：

- `pickles.config.js`
- `pickles.config.mjs`
- `pickles.config.ts`
- standalone rule module
- reusable skill/plugin rule package

Reusable rule package 必须导出 `defineRule({...})` 返回值，或由这些 rule 组成的数组。

Rule API 必须优先使用稳定字段、显式命名和 JSON-serializable options。

Rule API 不得依赖复杂继承、隐式上下文或运行时 monkey patch。

Pickles native rule metadata 必须包含 `message`。

Pickles native rule metadata 可以包含 `fixHint`。

Pickles runtime config 必须使用 `export default defineConfig({...})`。

Pickles native rule 必须使用 `defineRule({...})`。

`defineConfig` 和 `defineRule` 固定由 `@pickles/runtime/config` 提供。

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

MVP index 服务 native rule execution、workspace 级问题聚合和 repair-oriented summary。

Workspace Index MVP 固定使用进程内文件级索引。

Workspace Index 必须至少维护：

- `filesByPath`
- Java `filesByPath`
- Java `typesByQualifiedName`
- Java `typeNamesByAnnotation`
- Java `filePathsByImport`
- Java `contributionsByPath`

Workspace index 必须支持：

- 按 file path 更新 syntax file。
- 按 file path 删除 syntax file。
- 按 file path 查询 syntax file。
- 按 qualified type name 查询 Java type declaration。
- 按 annotation name 查询 Java type declaration。
- 按 import target 查询 Java file。

Java `contributionsByPath` 必须记录每个文件贡献的 qualified type、annotation 和 import 索引 key。

文件修改或删除时，Runtime 必须先清理该文件旧贡献，再写入新索引。

Workspace Index 不得保存 raw tree-sitter node 或 tree。

Runtime 可以在单次检测过程中使用 parser 内部对象完成 DTO 构建和 syntax query，但不得将 raw parser object 写入 Workspace Index 或暴露给 rule。

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
- `fixHint`

Java parser diagnostic 的 `source.tool` 固定为 `tree-sitter-java`。

Parser diagnostic 的 `severity` 固定为 `WARN`。

Parser diagnostic 的 `fixHint` 固定为 `null`。

单个文件解析失败不得中断整批检测。

Runtime 必须继续处理同一批次中的其他文件。

### 7.10 Native Rule Execution

Runtime 必须加载 Pickles native rules。

Runtime 必须基于当前变动集和 workspace index 执行 native rules。

Runtime 必须优先执行 native rules 以提供增量反馈。

Native rule execution 必须支持按变动文件收窄检查范围。

Native rule execution 必须支持 workspace-level Problem。

Native rule execution 不得依赖 ArchUnit 全量执行。

### 7.11 Problem Aggregation

Runtime 必须把 Pickles native rule result 转换为 Problem。

Runtime 必须把 parser diagnostic 聚合为 Problem。

Problem 必须符合 `PROBLEM-MODEL-CONTRACT.md`。

Problem `source.tool` 必须保留来源工具：

- `pickles-native`
- `tree-sitter-java`

### 7.12 Repair-Oriented Summary

Runtime 必须向 Codex 提供 Repair-Oriented Summary。

Runtime 必须能基于 workspace index 和 Problem 生成面向 Agent 的修复摘要。

Repair-Oriented Summary 必须是 JSON-serializable object。

Repair-Oriented Summary 的 `schemaVersion` 固定为 `pickles.repairSummary.v1`。

Repair-Oriented Summary 固定字段：

- `schemaVersion`
- `changedFiles`
- `changedJavaFiles`
- `affectedTypes`
- `affectedMethods`
- `problems`
- `problemStats`
- `repairHints`

`changedFiles` 必须包含本次检测涉及的文件路径、变更类型和语言。

`changedJavaFiles` 必须包含 Java 文件路径、package name、top-level type names 和 parser diagnostic 数量。

`affectedTypes` 必须包含 type name、qualified name、kind、file 和 range。

`affectedMethods` 必须包含 owner qualified name、method name、kind、file 和 range。

`problems` 必须使用 `PROBLEM-MODEL-CONTRACT.md` 定义的去重后 Problem。

`problemStats` 必须包含 `errorCount` 和 `warnCount`。

`repairHints` 必须从 Problem 的 `source.rule`、`title`、`message`、`fixHint`、`file` 和 `position` 派生。

摘要不得包含 tree-sitter 原生节点结构。

摘要不得包含 raw parser object。

摘要不得包含未结构化 source。

摘要中的文件路径固定使用目标工程相对路径。

Hook `/feedback` 的 `FeedbackSummary.text` 可以由 Repair-Oriented Summary 派生，但不替代该稳定 JSON contract。

## 8. Key Flows

### 8.1 Detection Flow

1. Runtime 接收变动集。
2. Runtime 读取 Pickles runtime config。
3. Runtime 基于 changed files 即时更新 Incremental Workspace Index。
4. Runtime 对 changed Java files 生成 Java syntax index。
5. Runtime 加载 Pickles native rules。
6. Runtime 执行 Pickles native rules。
7. Runtime 聚合 native rule result 和 parser diagnostic。
8. Runtime 生成 Repair-Oriented Summary。
9. Runtime 返回 Problem Board 数据和治理反馈。

### 8.2 Initial Workspace Index Flow

1. IntelliJ Plugin 打开目标工程。
2. Plugin 启动本地 HTTP server。
3. Plugin 启动 Runtime 子进程。
4. Plugin 在后台触发 Runtime 首次 workspace 全量索引。
5. Runtime 按当前 Pickles runtime config 和文件限制建立 workspace baseline index。
6. 首次全量索引不得阻塞 IDE UI。
7. Hook `SessionStart` 不触发全量索引，只执行 Plugin health check。
8. `/notify` 路径必须始终支持基于 changed files 的即时检测；当首次全量索引尚未完成时，Runtime 必须先索引 changed files 并返回当前可计算 Problem。
9. Tool Window 手动 Refresh / Reindex 可以重新触发 workspace 全量索引。

### 8.3 Java File Added Or Modified Flow

1. Runtime 接收 Java 文件新增或修改变动。
2. Runtime 校验 after content 大小。
3. Runtime 调用 Java parser。
4. Runtime 更新 Java syntax index。
5. Runtime 保存 parser diagnostic。

### 8.4 Java File Deleted Flow

1. Runtime 接收 Java 文件删除变动。
2. Runtime 删除 workspace index 中的对应文件项。
3. Runtime 保留其他文件 index。
4. Runtime 继续执行启用的 native rule。

### 8.5 Parser Failure Flow

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
- Runtime parser 测试必须使用真实 Java sample。
- Runtime index 测试必须覆盖新增、修改和删除。
- Runtime native rule 测试必须覆盖可编程规则加载、执行和 Problem 归一化。
- Runtime Problem 聚合测试必须覆盖 parser diagnostic。
- Runtime public DTO 必须可 JSON 序列化。
- Runtime parser adapter 必须是 tree-sitter API 的唯一直接依赖点。
- Runtime 接入后必须纳入仓库验证脚本。

## 10. Open Items

无
