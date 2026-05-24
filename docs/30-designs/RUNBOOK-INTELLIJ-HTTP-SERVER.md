# Runbook: IntelliJ HTTP Server Contract Stub

## 1. Purpose

本文档定义在 `pickles-intellij-plugin/` 中实现本地 HTTP server 与 Hook API stub 的一次性执行手册。

目标是在 IntelliJ Plugin 中按 [`../20-interfaces/HOOK-PLUGIN-HTTP-CONTRACT.md`](../20-interfaces/HOOK-PLUGIN-HTTP-CONTRACT.md) 固定 endpoint、request / response schema、错误结构和基础校验，并让 `/feedback` 先返回 `unimplemented` 状态。

## 2. Scope

当前范围：

- `pickles-intellij-plugin/` 本地 HTTP server 结构整理
- `/health`、`/notify`、`/feedback` endpoint 实现
- Hook HTTP request / response data class
- JSON 解析与基础字段校验
- 统一错误响应
- workspace mismatch 检查
- narrow tests 或最小可运行验证
- 相关文档同步

不在范围内：

- 不接入 `pickles-runtime/` 规则执行
- 不执行 ArchUnit 或 ESLint
- 不实现 Codex Hook 脚本
- 不实现 MCP 或 WebSocket 通知协议
- 不实现 Problem Board 自动刷新
- 不新增鉴权机制
- 不引入全局 `code` / `message` / `data` 成功响应 wrapper

## 3. Execution Order

### 3.1 准备与现状确认

1. 读取 `pickles-intellij-plugin/AGENTS.md`。
2. 读取 [`../20-interfaces/HOOK-PLUGIN-HTTP-CONTRACT.md`](../20-interfaces/HOOK-PLUGIN-HTTP-CONTRACT.md)。
3. 检查 `PicklesProjectService` 中现有 `HttpServer`、`/health`、`/notify`、`/feedback` 实现。
4. 检查 `PicklesModels.kt` 中现有 `PicklesProblem` 与 Problem contract 是否一致。

### 3.2 HTTP contract model

1. 在 IntelliJ Plugin 模块中新增或扩展 HTTP contract data class。
2. 固定模型名称与契约字段一致：
   - `schemaVersion`
   - `requestId`
   - `HookEvent`
   - `ChangedFile`
   - `NotifyRequest`
   - `NotifyResponse`
   - `FeedbackRequest`
   - `FeedbackResponse`
   - `FeedbackSummary`
   - `ApiError`
   - `ApiErrorResponse`
3. 将 `PicklesProblem.source` 从未结构化 string 收敛为 object，字段固定为 `tool` 与 `rule`。
4. 不新增跨模块共享 helper；只有两个以上模块真实复用时再抽出共享契约。

### 3.3 HTTP response helpers

1. 在 `PicklesProjectService` 内收敛 JSON response 写出逻辑。
2. 固定所有 response 的 `Content-Type` 为 `application/json; charset=utf-8`。
3. 成功响应不得使用全局 `code` / `message` / `data` wrapper。
4. 错误响应固定使用：

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

### 3.4 Endpoint behavior

1. `GET /health` 固定返回 HTTP `200`：

```json
{
  "schemaVersion": 1,
  "requestId": null,
  "status": "ok"
}
```

2. `POST /notify` 固定解析 `NotifyRequest` 并执行基础校验。
3. `/notify` 校验通过后固定返回 HTTP `202`：

```json
{
  "schemaVersion": 1,
  "requestId": "request-id",
  "accepted": true,
  "processed": false
}
```

4. `/notify` 在本 RUNBOOK 阶段不调用 runtime，不刷新 Problem Board，不同步返回 Problem Board。
5. `POST /feedback` 固定解析 `FeedbackRequest` 并执行基础校验。
6. `/feedback` 校验通过后固定返回 HTTP `200`，`status` 固定为 `unimplemented`，`problems` 固定为空 array，`summary` 固定为 0 计数。
7. `/feedback` request 不接收文件内容。

### 3.5 Validation rules

基础校验固定覆盖：

- request body 必须是合法 JSON。
- `schemaVersion` 必须等于 `1`。
- request 中的 `requestId` 必须是非空 string。
- `/notify` 必须包含 `event.sessionId`。
- `/notify` 必须包含 `event.hookEventName`，且值属于 `SessionStart`、`PreToolUse`、`PostToolUse`、`Stop`。
- `/notify` 必须包含 `event.workspace`，且必须匹配当前目标工程根目录。
- `/notify` 的 `files` 必须是 array。
- `ChangedFile.fileName` 不得是绝对路径。
- `ChangedFile.before` 与 `ChangedFile.after` 不得同时为 `null`。
- `/feedback` 必须包含 `sessionId`、`requestId` 和 `workspace`。
- `/feedback.workspace` 必须匹配当前目标工程根目录。

错误状态固定使用契约中的 HTTP status：

- `400`：JSON 非法、schema 不匹配或必填字段缺失。
- `404`：workspace 不属于当前 Plugin 服务。
- `500`：未预期服务端错误。

## 4. Verification

最小验证固定包括：

- 在 `pickles-intellij-plugin/` 运行 `gradle build`。
- 使用 JDK 17；本机使用 jenv 时先执行：

```bash
export JAVA_HOME="$(jenv prefix jetbrains64-17.0.14)"
```

- 若模块已有 HTTP handler 测试入口，新增或更新以下测试：
  - `/health` 返回 `schemaVersion`、`requestId: null`、`status: ok`。
  - `/notify` 非 POST 返回 `405`。
  - `/notify` 缺少 `event.sessionId` 返回 `400` 和 `INVALID_REQUEST`。
  - `/notify` workspace mismatch 返回 `404` 和 `WORKSPACE_MISMATCH`。
  - `/notify` 正常请求返回 `202`、`accepted: true`、`processed: false`。
  - `/feedback` 正常请求返回 `status: unimplemented`、空 `problems` 和 0 计数 summary。
- 若当前模块没有合适测试入口，必须在最终响应中说明只完成了 `gradle build` 验证。

## 5. Documentation Sync

实现完成后必须检查：

- [`../20-interfaces/HOOK-PLUGIN-HTTP-CONTRACT.md`](../20-interfaces/HOOK-PLUGIN-HTTP-CONTRACT.md) 是否仍与代码一致。
- [`../20-interfaces/PROBLEM-MODEL-CONTRACT.md`](../20-interfaces/PROBLEM-MODEL-CONTRACT.md) 是否仍与 `PicklesProblem` 一致。
- [`../10-requirements/INTELLIJ-PLUGIN-REQUIREMENTS.md`](../10-requirements/INTELLIJ-PLUGIN-REQUIREMENTS.md) 是否仍与本地 HTTP server 行为一致。

## 6. Closure

任务完成时必须：

1. 删除或收窄 `TODO.md` 中对应任务。
2. 若 RUNBOOK 已执行完毕且无剩余价值，删除本 RUNBOOK。
3. 将代码、测试、文档同步、`TODO.md` 清理和 RUNBOOK 清理放入同一个语义完整 commit。

## 7. Open Items

无
