# Codex Hooks Requirements

## 1. Purpose

本文档定义 Pickles Codex Hook 的 MVP 需求。

目标是明确 Hook 在 Codex Runtime 中必须捕获什么、发送什么、以及不负责什么。

## 2. Scope

当前范围：

- Codex task 生命周期检查点
- 文件变动捕获
- 本地 HTTP 通知
- 任务完成前治理反馈请求

不在范围内：

- 不执行规则命令
- 不展示 Problem Board
- 不修改业务代码、测试代码或工程实现代码
- 不定义 HTTP endpoint 细节
- 不实现 MCP 或 WebSocket 通知协议

## 3. Bounded Context

Codex Hook 部署在 Codex Runtime 环境中。

Hook 知道 Codex task 生命周期。MVP 按 workspace 聚合问题，不按单个 task 单独保存 Problem Board。

Hook 负责通知与拉取反馈。IntelliJ Plugin / Governance Engine 负责聚合、分析和展示。

## 4. Module Mapping

- `pickles-hooks/`: Codex Hook 实现。
- `pickles-intellij-plugin/`: 本地 HTTP 接收入口。
- `pickles-runtime/`: 处理变动集并返回治理反馈。
- `docs/20-interfaces/HOOK-PLUGIN-HTTP-CONTRACT.md`: Hook 与 Plugin 通信契约。

## 5. Core Objects

- Hook event
- Changed file
- Feedback request
- Problem board response

## 6. Global Constraints

- Hook 通知协议在 MVP 固定为本地 HTTP。
- Hook payload 必须包含文件名、before 内容和 after 内容。
- Hook 不执行 ArchUnit 或 ESLint 命令。
- Hook 不修改用户工程文件。
- Hook 不负责 Problem Board UI。
- Pickles Hook 配置固定使用目标工程 `<repo>/.codex/hooks.json`。
- Pickles Hook 不读取、修改或依赖用户全局 `~/.codex`。
- Pickles Hook 脚本语言固定为 Node.js。
- Pickles Hook 脚本文件固定使用 ESM `.mjs`。
- Pickles Hook MVP 不依赖 npm install，只使用 Node.js built-in modules。
- Pickles Hook 脚本固定放在目标工程 `<repo>/.codex/hooks/`。

## 7. Functional Requirements

### 7.1 Task Lifecycle

Hook 必须接入 Codex task 生命周期检查点。

Hook 必须在任务完成前请求治理反馈。

### 7.2 File Change Capture

Hook 必须捕获任务期间的增量文件变动。

每个变动文件必须包含：

- 文件名
- before 内容
- after 内容

### 7.3 Change Notify

Hook 必须通过本地 HTTP 向 IntelliJ Plugin 发送变动通知。

HTTP endpoint 细节在 MVP 暂不定义。

### 7.4 Feedback Pull

Hook 必须在任务完成前通过本地 HTTP 请求治理反馈。

Hook 必须把治理反馈暴露给 Codex，使 Codex 能修复或汇报问题。

## 8. Key Flows

### 8.1 Change Notify Flow

1. Codex 修改目标工程文件。
2. Hook 捕获文件名、before 内容和 after 内容。
3. Hook 通过本地 HTTP 通知 IntelliJ Plugin。
4. Plugin / Governance Engine 更新 Problem Board。

### 8.2 Finalize Feedback Flow

1. Codex task 进入完成前检查点。
2. Hook 通过本地 HTTP 请求治理反馈。
3. Hook 把治理反馈暴露给 Codex。
4. Codex 修复或汇报问题。

## 9. Non-Functional Requirements

- Hook 失败必须让 Codex 可见。
- Hook 通信失败不得修改用户工程文件。
- Hook 不保存全量语义持久化数据。

## 10. Open Items

- Hook 如何获取本地 HTTP 服务端口。
- Hook event 是否需要 task id。
- HTTP request / response schema。
