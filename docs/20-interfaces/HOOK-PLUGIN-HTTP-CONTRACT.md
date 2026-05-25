# Hook Plugin HTTP Contract

## 1. Purpose

本文档定义 Codex Hook 与 IntelliJ Plugin 之间的本地 HTTP 通信边界、endpoint、request / response schema 和错误结构。

MVP 固定使用本地 HTTP 承载 Hook 事件通知与治理反馈拉取。

## 2. Scope

当前范围：

- Hook 与 Plugin 的职责边界
- 本地 HTTP 作为 MVP 通知协议
- 本地 HTTP 端口发现机制
- Hook event payload 最小内容
- HTTP endpoint 路径
- request / response JSON schema
- HTTP status 规则
- 错误响应结构
- workspace 聚合口径

不在范围内：

- 不定义公网 API
- 不定义鉴权机制
- 不定义 HTTP API 自动生成机制
- 不定义完整 Problem 去重算法

## 3. Bounded Context

Codex Hook 部署在 Codex Runtime 环境中。IntelliJ Plugin 在目标工程所在 IDEA 进程内启动本地 HTTP 服务。

Hook 负责通知与拉取反馈。Plugin / Governance Server 负责聚合、分析和展示。

MVP 不使用 MCP 或 WebSocket 作为 Hook 通知协议。

## 4. Module Mapping

- `pickles-hooks/`: Codex Hook，捕获 task 生命周期和文件变动。
- `pickles-intellij-plugin/`: 本地 HTTP 服务入口。
- `pickles-runtime/`: 处理 Hook 变动集、更新 workspace index、聚合问题。

## 5. Core Objects

### 5.1 CommonEnvelope

Common envelope 是所有 request / response 共享的最小元信息。

MVP 固定包含：

- `schemaVersion`
- `requestId`

`schemaVersion` 固定为 number。MVP 固定为 `1`。

request 中的 `requestId` 固定为 string。Hook 每次请求必须生成 `requestId`。Plugin response 必须回传同一个 `requestId`。没有 request body 的 response 或 Plugin 无法解析 request body 时，response 中的 `requestId` 固定为 `null`。

成功响应不得使用全局 `code` / `message` / `data` wrapper。

错误响应固定使用 `error` object。

### 5.2 HookEvent

Hook event 表达一次 Codex task 生命周期检查点通知。

MVP 固定包含：

- `sessionId`
- `turnId`
- `hookEventName`
- `workspace`
- `idempotencyKey`

`sessionId` 来源于 Codex hook input `session_id`。

`turnId` 来源于 Codex hook input `turn_id`。`SessionStart` 没有 `turn_id` 时，`turnId` 固定为 `null`。

`hookEventName` 固定使用 Codex hook event name，MVP 允许值固定为：

- `SessionStart`
- `PreToolUse`
- `PostToolUse`
- `Stop`

`workspace` 固定为目标工程根目录绝对路径。

Pickles 幂等键固定基于：

- `sessionId`
- `turnId`
- `hookEventName`
- changed file `fileName`

没有 changed file 的事件固定使用 `sessionId`、`turnId` 和 `hookEventName` 作为幂等键。

Hook 必须显式发送 `idempotencyKey`。Plugin / Governance Server 必须按 `idempotencyKey` 处理重复通知。

### 5.3 ChangedFile

Changed file 表达一个文件的 before / after 内容。

MVP 固定包含：

- `fileName`
- `before`
- `after`

`fileName` 固定为相对目标工程根目录的路径，不得使用绝对路径。

`before` 固定为 string 或 `null`。

`after` 固定为 string 或 `null`。

新增文件的 `before` 固定为 `null`。

删除文件的 `after` 固定为 `null`。

### 5.4 Problem

Problem 固定复用 [`PROBLEM-MODEL-CONTRACT.md`](./PROBLEM-MODEL-CONTRACT.md)。

`/feedback` response 中的 `problems` 必须返回 Problem array。

### 5.5 ProblemSource

Problem source 表达问题来源工具。

MVP 固定包含：

- `tool`
- `rule`

`tool` 固定为 string。

`rule` 固定为 string 或 `null`。

### 5.6 FeedbackSummary

Feedback summary 表达 Codex 可直接读取的治理反馈摘要。

MVP 固定包含：

- `errorCount`
- `warnCount`
- `text`

`errorCount` 固定为 number。

`warnCount` 固定为 number。

`text` 固定为 string。

### 5.7 FeedbackStatus

Feedback status 表达治理反馈是否已经接入 runtime。

MVP 固定允许值：

- `ok`
- `unimplemented`

`unimplemented` 固定表示 Plugin 已实现 HTTP contract，但尚未接入 Governance Server。

### 5.8 ApiError

API error 表达统一错误响应。

MVP 固定包含：

- `code`
- `message`
- `details`

`code` 固定允许值：

- `INVALID_REQUEST`
- `WORKSPACE_MISMATCH`
- `CONFIG_ERROR`
- `SERVER_NOT_READY`
- `INTERNAL_ERROR`

`message` 固定为面向 Codex 可读的错误说明。

`details` 固定为 JSON object。无额外细节时固定为 `{}`。

### 5.9 ServerFile

Server file 固定为目标工程 `<repo>/.pickles/server.json`。

MVP 固定包含：

- `port`

## 6. Global Constraints

- Hook 通知协议在 MVP 固定为本地 HTTP。
- 本地 HTTP 端口发现固定使用目标工程 `<repo>/.pickles/server.json`。
- MVP 本地 HTTP 请求不做鉴权。
- Hook event identity 固定使用 Codex `session_id` 与 `turn_id`。
- Hook 知道 Codex task 生命周期。
- Hook 事件固定为 `SessionStart`、`PreToolUse`、`PostToolUse` 和 `Stop`。
- Problem Board 按 workspace 聚合，不按单个 task 单独保存。
- Hook payload 固定至少包含文件名、before 内容和 after 内容。
- Hook API 固定是事件通知与反馈拉取 API，不是规则执行 API。
- `/notify` 固定不承诺同步执行治理。
- `/feedback` 固定不上传文件内容。
- 成功响应固定不使用全局 `code` / `message` / `data` wrapper。
- 错误响应固定使用 `error.code`、`error.message` 和 `error.details`。
- request / response 固定包含 `schemaVersion` 和 `requestId`。
- `ChangedFile.fileName` 固定使用相对目标工程根目录路径，不得使用绝对路径。

## 7. HTTP Endpoints

### 7.1 Health Endpoint

Health endpoint 固定为：

```text
GET /health
```

成功 response 固定返回 HTTP `200`：

```json
{
    "schemaVersion": 1,
    "requestId": null,
    "status": "ok"
}
```

### 7.2 Notify Endpoint

Notify endpoint 固定为：

```text
POST /notify
```

`/notify` 固定用于 Hook 上报生命周期事件和文件变动。

成功 response 固定返回 HTTP `202`。

### 7.3 Feedback Endpoint

Feedback endpoint 固定为：

```text
POST /feedback
```

`/feedback` 固定用于 Hook 在 `Stop` 阶段拉取当前 workspace 的治理反馈。

成功 response 固定返回 HTTP `200`。

## 8. HTTP Schemas

### 8.1 Notify Request

`POST /notify` request body 固定结构：

```json
{
    "schemaVersion": 1,
    "requestId": "request-id",
    "event": {
        "sessionId": "codex-session-id",
        "turnId": "codex-turn-id",
        "hookEventName": "PostToolUse",
        "workspace": "/absolute/path/to/repo",
        "idempotencyKey": "session:turn:event:file-or-event"
    },
    "files": [
        {
            "fileName": "relative/path/File.kt",
            "before": "old content",
            "after": "new content"
        }
    ]
}
```

`files` 固定为 array。没有文件变动的事件固定使用空 array。

### 8.2 Notify Response

`POST /notify` 成功 response body 固定结构：

```json
{
    "schemaVersion": 1,
    "requestId": "request-id",
    "accepted": true,
    "processed": false
}
```

`accepted` 固定表示 Plugin 已接收通知。

`processed` 固定表示 Plugin 是否已同步完成处理。MVP 中 `/notify` 不承诺同步执行治理，默认固定为 `false`。

### 8.3 Feedback Request

`POST /feedback` request body 固定结构：

```json
{
    "schemaVersion": 1,
    "requestId": "request-id",
    "sessionId": "codex-session-id",
    "turnId": "codex-turn-id",
    "workspace": "/absolute/path/to/repo"
}
```

`/feedback` request 不得包含文件内容。Hook 存在未上报 pending workspace diff 时，必须先调用 `/notify` flush，再调用 `/feedback`。

### 8.4 Feedback Response

`POST /feedback` 成功 response body 固定结构：

```json
{
    "schemaVersion": 1,
    "requestId": "request-id",
    "status": "unimplemented",
    "hasBlockingProblems": false,
    "summary": {
        "errorCount": 0,
        "warnCount": 0,
        "text": "Governance feedback is not implemented yet."
    },
    "problems": []
}
```

`hasBlockingProblems` 固定为是否存在 `severity = "ERROR"` 的 Problem。

`status` 为 `unimplemented` 时，`hasBlockingProblems` 固定为 `false`，`problems` 固定为空 array，`summary.errorCount` 与 `summary.warnCount` 固定为 `0`。

`summary.errorCount` 固定为 `severity = "ERROR"` 的 Problem 数量。

`summary.warnCount` 固定为 `severity = "WARN"` 的 Problem 数量。

### 8.5 Error Response

所有失败 response body 固定结构：

```json
{
    "schemaVersion": 1,
    "requestId": "request-id-or-null",
    "error": {
        "code": "INVALID_REQUEST",
        "message": "Missing event.sessionId.",
        "details": {}
    }
}
```

### 8.6 HTTP Status

HTTP status 固定规则：

| Status | 使用场景                                       |
| ------ | ---------------------------------------------- |
| `200`  | `/health` 成功；`/feedback` 成功               |
| `202`  | `/notify` 已接收                               |
| `400`  | request JSON 非法、schema 不匹配或必填字段缺失 |
| `404`  | request workspace 不属于当前 Plugin 服务       |
| `409`  | 幂等键冲突或 session / workspace 状态冲突      |
| `503`  | Plugin 服务尚未准备好                          |
| `500`  | 未预期服务端错误                               |

## 9. Functional Requirements

### 9.1 File Change Notify

Codex Hook 在 `PreToolUse` 根据 `tool_name` 与 `tool_input` 提取候选文件，并读取候选文件 before 内容。

Codex Hook 在 `PostToolUse` 使用 workspace diff 或文件状态扫描确认实际变动文件，读取 after 内容，并向 IntelliJ Plugin 发送变动通知。

通知内容固定表达任务期间的增量文件变动。

`tool_name` 与 `tool_input` 只用于候选提取与解析策略选择，不作为最终文件列表真相源。

新增文件的 before 固定为 `null`。

删除文件的 after 固定为 `null`。

### 9.2 Feedback Request

Codex Hook 在 `Stop` 通过本地 HTTP 请求治理反馈。

返回内容应能让 Codex 判断是否存在 ERROR / WARN 问题。

### 9.3 Session Start Notify

Codex Hook 在 `SessionStart` 通过当前 git root 定位目标工程根目录。

Codex Hook 读取目标工程 `.pickles/config.json`。

Codex Hook 读取目标工程 `.pickles/server.json`。

Codex Hook 使用 `http://127.0.0.1:<port>` 检查本地 Plugin HTTP 服务是否可用。

### 9.4 Port Discovery

IntelliJ Plugin 启动本地 HTTP 服务后，必须写入目标工程 `<repo>/.pickles/server.json`。

`server.json` MVP 最小结构：

```json
{
    "port": 0
}
```

Codex Hook 必须从当前 git root 定位目标工程根目录，并读取 `<repo>/.pickles/server.json`。

Codex Hook 必须使用 `server.json` 中的 `port` 组装本地地址 `http://127.0.0.1:<port>`。

### 9.5 Runtime Dispatch

IntelliJ Plugin 收到 Hook 通知后，将变动集交给 Governance Server。

Governance Server 基于变动集增量更新 Incremental Workspace Index。

## 10. Key Flows

### 10.1 Change Notify Flow

1. Codex task 触发 `PreToolUse`。
2. Codex Hook 根据 `tool_name` 与 `tool_input` 提取候选文件。
3. Codex Hook 读取候选文件 before 内容。
4. Codex task 触发 `PostToolUse`。
5. Codex Hook 通过 workspace diff 或文件状态扫描确认实际变动文件。
6. Codex Hook 读取实际变动文件 after 内容。
7. Hook 通过 `POST /notify` 通知 IntelliJ Plugin。
8. Plugin 将变动集交给 Governance Server。
9. Governance Server 更新 workspace index 并执行规则检测。

### 10.2 Feedback Flow

1. Codex task 进入 `Stop` 检查点。
2. Hook flush 未上报的 pending workspace diff。
3. Hook 通过 `POST /feedback` 请求治理反馈。
4. Plugin / Governance Server 返回当前 workspace Problem Board。
5. Codex 修复或汇报问题。

### 10.3 Session Start Flow

1. Codex session 触发 `SessionStart`。
2. Hook 通过当前 git root 定位目标工程根目录。
3. Hook 读取目标工程 `.pickles/config.json`。
4. Hook 读取目标工程 `.pickles/server.json`。
5. Hook 使用 `http://127.0.0.1:<port>` 检查本地 Plugin HTTP 服务是否可用。
6. Hook 将初始化结果暴露给 Codex。

## 11. AI Implementation Rules

- 不得引入全局 `code` / `message` / `data` 成功响应 wrapper。
- 不得让 `/notify` 同步执行治理并返回 Problem Board。
- 不得在 `/feedback` request 中发送文件内容。
- 不得在 `ChangedFile.fileName` 中使用绝对路径。
- 不得在 MVP 中加入鉴权。
- 不得在本契约中用 MCP 或 WebSocket 替代本地 HTTP。
- 不得把 `tool_name` 或 `tool_input` 当作最终文件列表真相源。
- 不得让 Hook 修改用户业务代码。

## 12. Non-Functional Requirements

- 本地 HTTP 服务只服务当前目标工程。
- Hook 失败不得修改用户业务代码。
- 通信失败必须能被 Codex 看到，并允许 Codex 汇报失败原因。
- Plugin / Governance Server 必须按 Hook event 幂等键处理重复通知。

## 13. Open Items

无
