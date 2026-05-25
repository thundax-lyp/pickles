# Governance Engine Requirements

## 1. Purpose

本文档定义 Pickles Governance Engine 的 MVP 需求。

目标是明确 `pickles-runtime/` 作为治理执行内核必须完成的能力。

## 2. Scope

当前范围：

- `.pickles/config.json` 读取和校验
- Incremental Workspace Index
- ArchUnit 命令执行
- ESLint 命令执行
- Problem 聚合
- Repair-Oriented Summary

不在范围内：

- 不实现 IntelliJ UI
- 不实现 Codex Hook
- 不定义 HTTP endpoint
- 不实现 MCP server
- 不保存全量语义持久化数据

## 3. Bounded Context

Governance Engine 服务于 IntelliJ Plugin。

IntelliJ Plugin 负责 UI、IDE 集成和本地 HTTP 入口。Governance Engine 负责读取配置、更新 workspace index、执行规则命令和聚合问题。

## 4. Module Mapping

- `pickles-runtime/`: Governance Engine 实现。
- `docs/20-interfaces/PICKLES-CONFIG-CONTRACT.md`: 配置契约。
- `docs/20-interfaces/PROBLEM-MODEL-CONTRACT.md`: 问题模型契约。
- `docs/30-designs/GOVERNANCE-ENGINE-DESIGN.md`: engine 设计。

## 5. Core Objects

- `PicklesConfig`
- `ChangedFile`
- `IncrementalWorkspaceIndex`
- `RuleCommand`
- `Problem`
- `RepairOrientedSummary`

## 6. Global Constraints

- Engine 不拥有 UI。
- Engine 实现语言固定为 Node.js / TypeScript。
- Engine 不修改业务代码、测试代码或工程实现代码。
- Engine 读取目标工程 `.pickles/config.json`。
- Engine 直接调用 `.pickles/config.json` 中配置的用户工程命令。
- MVP 只支持 ArchUnit 与 ESLint。
- MVP 只使用规则工具返回的 `ERROR` / `WARN`。

## 7. Functional Requirements

### 7.1 Config Loading

Engine 必须从目标工程 `.pickles/config.json` 读取配置。

Engine 必须校验 `.pickles/config.json` 是否是合法 JSON。

配置读取失败时，Engine 必须返回可展示错误。

### 7.2 Workspace Index

Engine 必须维护 Incremental Workspace Index。

Engine 必须基于 Hook 上报的文件名、before 内容和 after 内容更新 workspace index。

MVP index 只服务 workspace 级问题聚合。

### 7.3 Rule Execution

Engine 必须根据 `.pickles/config.json` 执行启用的规则命令：

- `rules.archunit.command`
- `rules.eslint.command`

命令为空且对应工具启用时，Engine 必须返回配置缺失问题。

### 7.4 Problem Aggregation

Engine 必须把 ArchUnit 与 ESLint 输出转换为 Problem。

Problem 必须符合 `PROBLEM-MODEL-CONTRACT.md`。

### 7.5 Repair-Oriented Summary

Engine 必须向 Codex 提供 Repair-Oriented Summary。

MVP 不定义完整 Repair-Oriented Summary 结构。

## 8. Key Flows

### 8.1 Detection Flow

1. Engine 接收变动集。
2. Engine 更新 Incremental Workspace Index。
3. Engine 读取 `.pickles/config.json`。
4. Engine 执行启用的规则命令。
5. Engine 聚合 Problem。
6. Engine 返回 Problem Board 数据。

## 9. Non-Functional Requirements

- 命令执行必须带有超时。
- 命令失败必须转换为可展示 Problem 或可展示错误。
- Engine 不写 `.pickles/config.json`。
- Engine 不保存全量语义持久化数据。

## 10. Open Items

- 命令执行超时时间。
- ArchUnit 输出解析规则。
- ESLint 输出解析规则。
- Incremental Workspace Index 的内部结构。
- Repair-Oriented Summary 结构。
