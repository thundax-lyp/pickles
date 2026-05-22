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
- 使用 `.pickles.json` 承载项目级治理配置

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
| Incremental Workspace Index | 基于 Agent Hook 上报的增量文件变动，更新 workspace 级工程索引与治理问题 |
| Agent Hooks | 部署在 Codex Runtime 内的生命周期触发点 |
| Plugin Notify Protocol | Hook 向 IntelliJ Plugin 发送通知的本地协议，MVP 默认使用 Plugin 启动的本地 HTTP 服务 |
| Project Configuration | 目标工程根目录 `.pickles.json`，作为被治理项目的配置真相源 |

---

# 架构

```text
Codex Runtime
    ↓
Agent Hooks
    ↓  Local HTTP
Pickles IntelliJ IDEA Plugin
    ↓
Pickles Governance Server
    ↓
Incremental Workspace Index
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

- 维护 Incremental Workspace Index
- 基于 Hook 变动集增量更新 workspace 级工程索引
- 执行 ArchUnit 与 ESLint 规则检测
- 聚合问题
- 提供 Repair-Oriented Summary

---

## Agent Hooks

安装于 Codex Runtime 环境中。

负责：

- Task 生命周期检查点
- 固定挂载 `SessionStart`、`PreToolUse`、`PostToolUse` 和 `Stop`
- 在 `SessionStart` 读取目标工程配置并检查本地 Plugin 可用性
- 在 `PreToolUse` 记录 before 内容或变动线索
- 收集任务期间的增量文件变动
- 在 `PostToolUse` 向 IntelliJ Plugin 发送变动通知，通知内容包含文件名、before 内容和 after 内容
- 在 `Stop` 请求治理反馈

---

## Hook Notify Protocol

Hook 到 Plugin 的通知协议在 MVP 阶段默认使用 Plugin 启动的本地 HTTP 服务。

协议选择不得改变职责边界：Hook 负责通知与拉取反馈，Plugin / Governance Server 负责聚合、分析和展示。

## Hook Event Payload

Hook 向 Plugin 上报的最小变动单元包含：

- 文件名
- before 内容
- after 内容

Hook 知道 Codex task 生命周期。MVP 按 workspace 聚合问题，不按单个 task 单独保存 Problem Board。

## Rule Source

MVP 检测用户项目，也就是 Agent 工作目录下的项目。

规则属于 Pickles，但检测命令来自用户项目。

IntelliJ Plugin 通过 IDEA 获取用户项目使用的 ArchUnit 与 ESLint 命令，并同步到目标工程 `.pickles.json`。

Governance Server 直接调用 `.pickles.json` 中配置的用户工程命令执行检测。

Plugin 配置界面可以提供规则配置，也可以引入 script 指令。

## Problem Severity

Problem severity 固定使用规则工具返回的级别：

- ERROR
- WARN

Pickles 不在 MVP 中新增独立 severity 体系。

---

# Project Configuration

Pickles 使用目标工程根目录 `.pickles.json` 作为项目级配置文件。

目标工程指 IntelliJ IDEA 当前打开、且 Codex Agent 正在工作的用户项目。对 Pickles 仓库自身的 e2e 场景，示例目标工程固定为 `e2e/sample-project/`。

`.pickles.json` 是被治理项目的配置真相源。IntelliJ Plugin、Codex Hook 和 Governance Server 都读取该文件。

Plugin 配置界面只负责展示和修改 `.pickles.json`，不拥有独立配置真相。

MVP 最小配置：

```json
{
  "version": 1,
  "agent": "codex",
  "bind": {
    "agentsFile": "AGENTS.md",
    "enabled": false
  },
  "hook": {
    "protocol": "http"
  },
  "rules": {
    "archunit": {
      "enabled": true,
      "command": ""
    },
    "eslint": {
      "enabled": true,
      "command": ""
    },
    "scripts": []
  },
  "problemBoard": {
    "aggregation": "workspace"
  }
}
```

运行时端口、进程号和 server URL 不写入 `.pickles.json`。

Pickles 插件仓库根目录不放置 `.pickles.json`；只有被治理的目标工程根目录放置该文件。

---

# 工作流

```text
Agent 修改工程
    ↓
Codex Hook 捕获文件变动
    ↓
Hook 提交变动集
    ↓
Governance Server 增量更新 workspace index
    ↓
规则执行
    ↓
Problem Board 更新
    ↓
Agent Task 完成
    ↓
Hook 通过本地 HTTP 请求治理反馈
    ↓
Agent 修复或汇报问题
```

---

# AGENTS.md 集成

Pickles 会向工程 AGENTS.md 注入治理约束。

Pickles 先检测当前工程是否已经绑定治理约束：

- 同时检查目标工程 `<repo>/.codex/hooks.json`。
- 不读取、不修改、不依赖用户全局 `~/.codex`。
- 未绑定时，Plugin 显示绑定按钮。
- 已绑定时，Plugin 显示解除绑定按钮。

示例：

```md
Before finalizing a task:

1. Call get_problem_board
2. Fix ERROR issues
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
| Hook Notify Protocol | Local HTTP |
| Workspace Input | Codex Hook 增量文件变动 |
| Rule Detection | ArchUnit + ESLint |

---

# 规则检测范围

MVP 只支持两类规则检测工具：

- ArchUnit
- ESLint

MVP 不接入其他规则检测工具或规则语言。

MVP 规则检测对象固定为用户项目，即 Agent 工作目录下的项目。

ArchUnit 与 ESLint 的执行命令固定从目标工程 `.pickles.json` 读取。

## Problem Board UI

Problem Board 是嵌入 IntelliJ IDEA 的工具窗口。

MVP 只显示以下字段：

- title
- type
- message

每个问题项提供删除按钮。点击问题项时，跳转到对应文件位置。

---

# MVP 范围

## 包含

- IntelliJ Plugin
- 本地 Governance Server
- 增量依赖跟踪
- Problem Board UI
- AGENTS.md Bind
- `.pickles.json` 项目配置
- Codex Hook
- Hook 到 Plugin 的本地 HTTP 通知协议
- 基于 ArchUnit 与 ESLint 的规则检测系统

---

## 不包含

- Multi-Agent Orchestration
- Cloud Runtime
- Auto Repair
- Distributed Execution
- 自定义 ACP 实现
- 全量语义持久化
- MCP 或 WebSocket 作为 Hook 通知协议
- 除 ArchUnit 与 ESLint 之外的规则检测工具
- Hook HTTP API 细节
- AGENTS.md 注入块格式、marker 和幂等更新细节

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
