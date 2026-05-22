# Hook Plugin HTTP Contract

## 1. Purpose

本文档定义 Codex Hook 与 IntelliJ Plugin 之间的本地 HTTP 通信边界。

MVP 只固定通信方向、职责和 payload 概念，不定义具体 endpoint。

## 2. Scope

当前范围：

- Hook 与 Plugin 的职责边界
- 本地 HTTP 作为 MVP 通知协议
- Hook event payload 最小内容
- workspace 聚合口径

不在范围内：

- 不定义 HTTP endpoint 路径
- 不定义 request / response JSON schema
- 不定义认证机制
- 不定义端口发现机制

## 3. Bounded Context

Codex Hook 部署在 Codex Runtime 环境中。IntelliJ Plugin 在目标工程所在 IDEA 进程内启动本地 HTTP 服务。

Hook 负责通知与拉取反馈。Plugin / Governance Server 负责聚合、分析和展示。

MVP 不使用 MCP 或 WebSocket 作为 Hook 通知协议。

## 4. Module Mapping

- `pickles-hooks/`: Codex Hook，捕获 task 生命周期和文件变动。
- `pickles-intellij-plugin/`: 本地 HTTP 服务入口。
- `pickles-runtime/`: 处理 Hook 变动集、更新 workspace index、聚合问题。

## 5. Core Objects

### 5.1 HookEvent

Hook event 表达一次 Codex task 生命周期检查点通知。

MVP 固定包含：

- `workspace`
- `files`

### 5.2 ChangedFile

Changed file 表达一个文件的 before / after 内容。

MVP 固定包含：

- `fileName`
- `before`
- `after`

## 6. Global Constraints

- Hook 通知协议在 MVP 固定为本地 HTTP。
- Hook 知道 Codex task 生命周期。
- Hook 事件固定为 `SessionStart`、`PreToolUse`、`PostToolUse` 和 `Stop`。
- Problem Board 按 workspace 聚合，不按单个 task 单独保存。
- Hook payload 固定至少包含文件名、before 内容和 after 内容。
- HTTP API 细节在 MVP 暂不定义。

## 7. Functional Requirements

### 7.1 File Change Notify

Codex Hook 在 `PreToolUse` 根据 `tool_name` 与 `tool_input` 提取候选文件，并读取候选文件 before 内容。

Codex Hook 在 `PostToolUse` 使用 workspace diff 或文件状态扫描确认实际变动文件，读取 after 内容，并向 IntelliJ Plugin 发送变动通知。

通知内容固定表达任务期间的增量文件变动。

`tool_name` 与 `tool_input` 只用于候选提取与解析策略选择，不作为最终文件列表真相源。

新增文件的 before 固定为 `null`。

删除文件的 after 固定为 `null`。

### 7.2 Feedback Request

Codex Hook 在 `Stop` 通过本地 HTTP 请求治理反馈。

返回内容应能让 Codex 判断是否存在 ERROR / WARN 问题。

### 7.3 Session Start Notify

Codex Hook 在 `SessionStart` 读取目标工程 `.pickles.json` 并检查本地 Plugin HTTP 服务是否可用。

### 7.4 Runtime Dispatch

IntelliJ Plugin 收到 Hook 通知后，将变动集交给 Governance Server。

Governance Server 基于变动集增量更新 Incremental Workspace Index。

## 8. Key Flows

### 8.1 Change Notify Flow

1. Codex task 触发 `PreToolUse`。
2. Codex Hook 根据 `tool_name` 与 `tool_input` 提取候选文件。
3. Codex Hook 读取候选文件 before 内容。
4. Codex task 触发 `PostToolUse`。
5. Codex Hook 通过 workspace diff 或文件状态扫描确认实际变动文件。
6. Codex Hook 读取实际变动文件 after 内容。
7. Hook 通过本地 HTTP 通知 IntelliJ Plugin。
8. Plugin 将变动集交给 Governance Server。
9. Governance Server 更新 workspace index 并执行规则检测。

### 8.2 Feedback Flow

1. Codex task 进入 `Stop` 检查点。
2. Hook flush 未上报的 pending workspace diff。
3. Hook 通过本地 HTTP 请求治理反馈。
4. Plugin / Governance Server 返回当前 workspace Problem Board。
5. Codex 修复或汇报问题。

### 8.3 Session Start Flow

1. Codex session 触发 `SessionStart`。
2. Hook 读取目标工程 `.pickles.json`。
3. Hook 检查本地 Plugin HTTP 服务是否可用。
4. Hook 将初始化结果暴露给 Codex。

## 9. Non-Functional Requirements

- 本地 HTTP 服务只服务当前目标工程。
- Hook 失败不得修改用户业务代码。
- 通信失败必须能被 Codex 看到，并允许 Codex 汇报失败原因。

## 10. Open Items

- HTTP endpoint 路径。
- request / response JSON schema。
- 本地 HTTP 端口发现机制。
- 是否需要请求鉴权。
- Hook event 是否需要 task id。
