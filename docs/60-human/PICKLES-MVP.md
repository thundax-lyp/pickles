# Pickles MVP

## Slogan

> Keep sandwiches together.

---

# 愿景

Pickles 是一个面向 Coding Agent 的语义治理 Sidecar。

MVP 固定运行在 IntelliJ IDEA + Codex 环境中。

它通过部署在 Codex Runtime 中的 Agent Hook 捕获增量文件变动，收集结构性问题，并在任务完成前向 Codex 提供治理反馈。

Pickles 不修改业务代码、测试代码或工程实现代码。  
Pickles 可以管理治理文件，例如注入或更新 `AGENTS.md` 中的治理约束。  
Agent 始终是业务代码、测试代码和产品实现的唯一写入者。

---

# 目标

- 在 IntelliJ IDEA + Codex 环境中防止 Agent 开发过程中的架构漂移
- 基于 Codex Hook 捕获的增量文件变动提供治理反馈
- 在任务完成前向 Codex 暴露 Problem Board 与修复建议
- 通过 Pickles 管理 `AGENTS.md` 中的治理约束

---

# 非目标

- 不是 AI Coding Assistant
- 不是 ACP 替代品
- 不是代码生成器
- 不是自动重构引擎
- 不是 CI 平台

---

# 核心概念

| 概念 | 描述 |
|---|---|
| Problem Board | 聚合后的语义与架构问题看板 |
| Governance Server | 规则执行与问题聚合运行时 |
| Incremental Semantic Graph | 基于 Agent Hook 上报的增量文件变动，更新工程语义关系与治理问题 |
| Agent Hooks | 部署在 Codex Runtime 内的生命周期触发点 |
| Plugin Notify Protocol | Hook 向 IntelliJ Plugin 发送通知的本地协议，MVP 可使用 MCP、HTTP 或 WebSocket |

---

# 架构

```text
Codex Runtime
    ↓
Agent Hooks
    ↓  MCP / HTTP / WebSocket
Pickles IntelliJ IDEA Plugin
    ↓
Pickles Governance Server
    ↓
Incremental Semantic Graph
    ↓
Problem Board
```

---

# 模块职责

## IDEA Plugin

负责：

- 接收 Codex Hook 的增量文件变动通知
- 展示 Problem Board
- 管理本地 Governance Server
- 执行 Agent Bind
- 提供 Hook 通知入口

Plugin 不修改业务代码、测试代码或工程实现代码。

---

## Governance Server

负责：

- 维护 Incremental Semantic Graph
- 基于 Hook 变动集增量更新语义关系
- 执行 ArchUnit 与 lint 规则检测
- 聚合问题
- 提供 Repair-Oriented Summary

---

## Agent Hooks

安装于 Codex Runtime 环境中。

负责：

- Task 生命周期检查点
- 收集任务期间的增量文件变动
- 在检查点向 IntelliJ Plugin 发送变动通知
- 在任务完成前请求治理反馈

---

## Hook Notify Protocol

Hook 到 Plugin 的通知协议在 MVP 阶段固定为本地协议候选集合：

- MCP
- Plugin 启动的本地 HTTP 服务
- Plugin 启动的本地 WebSocket 服务

协议选择不得改变职责边界：Hook 负责通知与拉取反馈，Plugin / Governance Server 负责聚合、分析和展示。

---

# 工作流

```text
Agent 修改工程
    ↓
Codex Hook 捕获文件变动
    ↓
Hook 提交变动集
    ↓
Governance Server 增量更新语义关系
    ↓
规则执行
    ↓
Problem Board 更新
    ↓
Agent Task 完成
    ↓
Hook 通过本地协议请求治理反馈
    ↓
Agent 修复或汇报问题
```

---

# AGENTS.md 集成

Pickles 会向工程 AGENTS.md 注入治理约束。

示例：

```md
Before finalizing a task:

1. Call get_problem_board
2. Fix BLOCKER issues
3. Review WARN issues
4. Do not finalize tasks with unresolved architecture violations
```

---

# MVP 技术栈

| 层级 | 技术 |
|---|---|
| IDE | IntelliJ IDEA |
| Agent | Codex |
| Hook Runtime | Codex Runtime |
| Hook Notify Protocol | MCP / HTTP / WebSocket |
| Semantic Input | Codex Hook 增量文件变动 |
| Rule Detection | ArchUnit + lint |

---

# 规则检测范围

MVP 只支持两类规则检测工具：

- ArchUnit
- lint

MVP 不接入其他规则检测工具或规则语言。

---

# MVP 范围

## 包含

- IntelliJ Plugin
- 本地 Governance Server
- 增量依赖跟踪
- Problem Board UI
- AGENTS.md Bind
- Codex Hook
- Hook 到 Plugin 的本地通知协议
- 基于 ArchUnit 与 lint 的规则检测系统

---

## 不包含

- Multi-Agent Orchestration
- Cloud Runtime
- Auto Repair
- Distributed Execution
- 自定义 ACP 实现
- 全量语义持久化
- 除 ArchUnit 与 lint 之外的规则检测工具

---

# 设计原则

- Single Writer Model
- Runtime First
- Incremental over Batch
- Change Notify, Feedback Pull
- Governance over Generation

---

# 仓库结构

```text
pickles/
 ├── pickles-intellij-plugin
 ├── pickles-runtime
 ├── pickles-mcp
 ├── pickles-hooks
 ├── pickles-rules
 └── docs
```
