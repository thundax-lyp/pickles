# Pickles MVP

## Slogan

> Keep sandwiches together.

---

# 愿景

Pickles 是一个面向 Coding Agent 的语义治理 Sidecar。

它持续观察工程变化，增量收集结构性问题，并在任务完成前向 Agent 提供治理反馈。

Pickles 不修改代码。  
Agent 始终是唯一写入者。

---

# 目标

- 防止 Agent 开发过程中的架构漂移
- 提供增量语义反馈
- 将治理能力独立于 IDE 与 Agent 厂商
- 复用现有协议，而不是重新发明协议

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
| Incremental Semantic Graph | 基于工程变化增量更新的语义关系图 |
| Agent Hooks | Agent Runtime 内的生命周期触发点 |
| MCP Server | Agent 拉取治理反馈的标准接口 |

---

# 架构

```text
Agent Runtime
    ↓
Agent Hooks
    ↓
MCP Pull
    ↓
Pickles Governance Server
    ↑
Incremental Semantic Graph
    ↑
IDEA Plugin（VFS / PSI Observer）
```

---

# 模块职责

## IDEA Plugin

负责：

- 监听 Workspace 变化
- 跟踪 PSI/VFS 更新
- 触发增量分析
- 展示 Problem Board
- 管理本地 Governance Server
- 执行 Agent Bind

Plugin 永远不修改代码。

---

## Governance Server

负责：

- 维护 Incremental Semantic Graph
- 执行规则与脚本
- 聚合问题
- 暴露 MCP Tools
- 提供 Repair-Oriented Summary

---

## Agent Hooks

安装于 Agent Runtime 环境中。

负责：

- Task 生命周期检查点
- 触发 MCP 拉取
- 在任务完成前请求治理反馈

---

## MCP Interface

示例 Tools：

```text
get_problem_board
get_architecture_violations
get_problems_since
get_repair_suggestions
```

Agent 主动 Pull 问题，而不是被 Push。

---

# 工作流

```text
Agent 修改工程
    ↓
IDEA 感知变化
    ↓
语义图增量更新
    ↓
规则执行
    ↓
Problem Board 更新
    ↓
Agent Task 完成
    ↓
Hook 调用 MCP
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

# 第一阶段技术栈

| 层级 | 技术 |
|---|---|
| IDE | IntelliJ IDEA |
| Agent | Codex |
| Protocol | MCP |
| Semantic Engine | IntelliJ PSI/VFS |
| Rules | ArchUnit + Custom Scripts |

---

# MVP 范围

## 包含

- IntelliJ Plugin
- 本地 Governance Server
- 增量依赖跟踪
- Problem Board UI
- MCP Server
- AGENTS.md Bind
- 基于 ArchUnit 的规则系统

---

## 不包含

- Multi-Agent Orchestration
- Cloud Runtime
- Auto Repair
- Distributed Execution
- 自定义 ACP 实现
- 全量语义持久化

---

# 设计原则

- Single Writer Model
- Runtime First
- Incremental over Batch
- Pull over Push
- Governance over Generation
- Vendor Neutral

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
