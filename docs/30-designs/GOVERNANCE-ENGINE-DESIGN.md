# Governance Engine Design

## 1. Purpose

本文档定义 `pickles-runtime/` 的 MVP 设计。

目标是明确 Governance Server 不是 UI，而是治理执行内核。

## 2. Scope

当前范围：

- Incremental Workspace Index
- `.pickles/config.json` 读取
- ArchUnit / ESLint 命令执行
- Problem 聚合

不在范围内：

- 不实现 Problem Board UI
- 不实现 Codex Hook
- 不实现 MCP server
- 不实现全量语义持久化

## 3. Bounded Context

Governance Server 服务于 IntelliJ Plugin。Plugin 负责 UI、IDE 集成和本地 HTTP 入口。Governance Server 负责治理执行。

## 4. Module Mapping

- `pickles-runtime/`: Governance Server 和 engine。
- `pickles-intellij-plugin/`: 调用 runtime 并展示结果。
- `pickles-hooks/`: 提供变动集。
- `pickles-rules/`: 提供 Pickles 规则配置和 script 模板。

## 5. Core Objects

- `PicklesConfig`
- `HookEvent`
- `ChangedFile`
- `IncrementalWorkspaceIndex`
- `RuleCommand`
- `Problem`

## 6. Global Constraints

- Runtime 不拥有 UI。
- Runtime 实现语言固定为 Node.js / TypeScript。
- Runtime 不修改业务代码。
- Runtime 直接调用 `.pickles/config.json` 中配置的用户工程命令。
- Runtime 只支持 ArchUnit 与 ESLint 两类规则检测工具。
- Runtime 不在 MVP 中新增独立 severity 体系。

## 7. Functional Requirements

### 7.1 Config Loading

Runtime 从目标工程 `.pickles/config.json` 读取配置。

配置读取失败时，Runtime 返回可展示的错误。

### 7.2 Workspace Index Update

Runtime 基于 Hook 变动集更新 Incremental Workspace Index。

MVP index 只需要支持 workspace 级问题聚合，不保存全量语义持久化数据。

### 7.3 Rule Execution

Runtime 根据 `.pickles/config.json` 执行用户工程命令：

- `rules.archunit.command`
- `rules.eslint.command`

命令为空且对应工具启用时，Runtime 返回配置缺失问题。

### 7.4 Problem Aggregation

Runtime 将 ArchUnit 与 ESLint 输出转换为 Problem。

Problem severity 固定使用工具返回的 `ERROR` / `WARN`。

## 8. Key Flows

### 8.1 Detection Flow

1. Runtime 接收变动集。
2. Runtime 更新 Incremental Workspace Index。
3. Runtime 读取 `.pickles/config.json`。
4. Runtime 执行启用的规则命令。
5. Runtime 聚合 Problem。
6. Runtime 返回 Problem Board 数据。

## 9. Non-Functional Requirements

- 命令执行必须带有超时。
- 命令失败必须转换为可展示 Problem 或可展示错误。
- Runtime 不写 `.pickles/config.json`，配置写入由 Plugin 负责。

## 10. Open Items

- 命令执行超时时间。
- ArchUnit 输出解析规则。
- ESLint 输出解析规则。
- Incremental Workspace Index 的内部结构。
