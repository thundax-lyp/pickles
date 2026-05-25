# Pickles Runtime Config Contract

## 1. Purpose

本文档定义目标工程 Pickles runtime config 的 MVP 配置契约。

目标是让 IntelliJ Plugin、Runtime 和 Agent-side skills 使用同一个项目级配置真相源。

## 2. Scope

当前范围：

- runtime config 文件位置
- MVP 固定入口
- 可编程配置字段
- 规则配置边界
- 运行时状态边界

不在范围内：

- 不定义 Plugin UI 的具体交互控件
- 不定义 `.pickles/server.json` 的完整 schema
- 不定义 AGENTS.md 注入块格式
- 不定义 native rule API 的完整 TypeScript 类型

## 3. Bounded Context

Pickles runtime config 固定放在被治理目标工程根目录。

目标工程指 IntelliJ IDEA 当前打开、且 Codex Agent 正在工作的用户项目。Pickles 仓库的 e2e 示例目标工程固定为 `e2e/sample-project/`。

Pickles runtime config 是可提交配置文件。目标工程 `.pickles/` 目录固定承载本地运行时状态文件，例如 `server.json`、hook state 和 cache。

Pickles runtime config 是配置真相源。IntelliJ Plugin、Runtime 和 Agent-side skills 都围绕该配置协作。

## 4. Module Mapping

- `pickles-intellij-plugin/`: 展示和更新 Pickles runtime config。
- `pickles-runtime/`: 加载 Pickles runtime config、native rules 和 external adapters。
- `pickles-hooks/`: 读取本地运行时状态。
- `pickles-rules/`: 提供 native rule authoring contract、规则说明书、规则模板和 plugin rule package。
- `pickles-skills/`: 基于规则说明书生成配置或提示 Agent 运行检查。
- `e2e/sample-project/`: 保存示例 `pickles.config.ts`。

## 5. Core Objects

### 5.1 PicklesRuntimeConfig

固定字段：

- `agent`
- `hook`
- `rules`
- `problemBoard`

### 5.2 HookConfig

固定字段：

- `protocol`

### 5.3 RuleConfig

固定字段：

- `id`
- `title`
- `type`
- `severity`

### 5.4 NativeRuleConfig

固定字段：

- `id`
- `title`
- `type`
- `severity`
- `language`
- `files`
- `options`
- `rule`

### 5.5 ExternalAdapterRuleConfig

固定字段：

- `id`
- `title`
- `type`
- `severity`
- `adapter`
- `command`

### 5.6 ProblemBoardConfig

固定字段：

- `aggregation`

## 6. Global Constraints

- Pickles runtime config 文件名固定支持 `pickles.config.ts`、`pickles.config.mjs` 和 `pickles.config.js`。
- 加载优先级固定为 `pickles.config.ts`、`pickles.config.mjs`、`pickles.config.js`。
- `agent` 固定为 `codex`。
- `hook.protocol` 在 MVP 固定为 `http`。
- `problemBoard.aggregation` 在 MVP 固定为 `workspace`。
- Runtime config 必须可提交到用户工程仓库。
- Runtime config 不得包含本机端口、进程号、server URL、临时绝对路径或 token。
- 本地 HTTP 端口固定写入目标工程 `<repo>/.pickles/server.json`。
- `rules` 固定使用数组。
- Native rules 固定由 Runtime 执行。
- ArchUnit 与 ESLint 固定作为 external adapter rule。

## 7. Functional Requirements

### 7.1 Minimal Config

MVP 最小配置固定为：

```ts
export default {
    agent: "codex",
    hook: {
        protocol: "http",
    },
    rules: [
        {
            id: "sample-eslint",
            title: "Sample TypeScript validation",
            type: "external-adapter",
            severity: "ERROR",
            adapter: "eslint",
            command: "npm run lint",
        },
    ],
    problemBoard: {
        aggregation: "workspace",
    },
};
```

### 7.2 Native Rule Config

Native rule config 必须能表达 JS / TS 可编程规则。

Native rule 必须通过 Runtime 提供的 `RuleContext` 读取输入。

Native rule 的 options 必须可 JSON 序列化。

Native rule 的输出必须能归一化为 Problem。

### 7.3 External Adapter Config

External adapter rule 必须能表达外部命令。

MVP 固定 adapter：

- `archunit`
- `eslint`

External adapter execution 和输出归一化由后续 adapter design 定义。

### 7.4 Config Read Flow

1. IntelliJ Plugin 定位目标工程根目录。
2. IntelliJ Plugin 展示 Pickles runtime config。
3. Runtime 加载 Pickles runtime config。
4. Codex Hook 读取本地运行时状态。
5. Agent-side skills 基于 Pickles runtime config 提示规则创作或规则检查。

### 7.5 Config Update Flow

1. Plugin 配置界面展示当前 Pickles runtime config。
2. 用户修改配置。
3. Plugin 写回 Pickles runtime config。
4. Runtime 后续检测使用更新后的配置。

## 8. Key Flows

### 8.1 Native Rule Flow

1. Runtime 加载 Pickles runtime config。
2. Runtime 加载 native rule。
3. Runtime 构造 `RuleContext`。
4. Runtime 执行 native rule。
5. Runtime 将 rule result 归一化为 Problem。

## 9. Non-Functional Requirements

- 配置读取失败时，Plugin 必须在 Problem Board 或配置界面展示可理解错误。
- Runtime config 必须适合 AI 生成和维护。
- Runtime config 必须适合 rule package 复用。
- Runtime config 必须支持 TypeScript 类型提示。

## 10. Open Items

- Native rule API 的完整 TypeScript 类型。
- Rule plugin package 引用格式。
- Config TypeScript 加载器选择。
- External adapter execution design。
