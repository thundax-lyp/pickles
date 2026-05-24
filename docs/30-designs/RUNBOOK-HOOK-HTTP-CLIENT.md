# Runbook: Hook HTTP Client Contract

## 1. Purpose

本文档定义 `pickles-hooks/` 第一阶段 Hook HTTP client 的一次性执行手册。

目标是在不实现完整 before / after 捕获算法的前提下，先打通 Hook 到 IntelliJ Plugin 的本地 HTTP contract，并实现 [`../40-readiness/E2E-TEST-CASES.md`](../40-readiness/E2E-TEST-CASES.md) 中的 `HOOK_PLUGIN_CONTRACT` testcase。

## 2. Scope

当前范围：

- `pickles-hooks/` Node.js ESM 脚本骨架
- 从当前 git root 定位目标工程
- 读取目标工程 `<repo>/.pickles/server.json`
- 使用 Node.js built-in `fetch` 调用本地 Plugin HTTP server
- 构造 `GET /health`
- 构造 `POST /notify`
- 构造 `POST /feedback`
- 通信失败输出 Codex 可见错误
- fake HTTP server contract test
- `e2e/sample-project/.codex/hooks.json` 绑定样例
- 测试文档同步

不在范围内：

- 不实现 `PreToolUse` before 快照缓存
- 不实现 `PostToolUse` workspace diff 扫描
- 不实现 pending diff flush
- 不解析 `apply_patch` patch body
- 不解析 Bash 命令中的真实变动文件
- 不执行 ArchUnit、ESLint 或其他规则命令
- 不调用 `pickles-runtime/`
- 不修改用户业务代码
- 不读取、修改或依赖用户全局 `~/.codex`
- 不引入 npm install 或第三方 npm package
- 不实现 MCP 或 WebSocket 通知协议

## 3. Execution Order

### 3.1 现状确认

1. 读取 [`../10-requirements/CODEX-HOOKS-REQUIREMENTS.md`](../10-requirements/CODEX-HOOKS-REQUIREMENTS.md)。
2. 读取 [`../20-interfaces/HOOK-PLUGIN-HTTP-CONTRACT.md`](../20-interfaces/HOOK-PLUGIN-HTTP-CONTRACT.md)。
3. 读取 [`../40-readiness/E2E-TEST-CASES.md`](../40-readiness/E2E-TEST-CASES.md) 中 `HOOK_PLUGIN_CONTRACT`。
4. 检查 `pickles-hooks/` 当前文件结构。
5. 检查 `e2e/sample-project/.pickles/server.json` 与 `.codex/hooks.json`。

### 3.2 Hook script ownership

Hook 第一阶段固定在 `pickles-hooks/` 中维护源脚本。

目标工程中的实际脚本固定放置在：

```text
<repo>/.codex/hooks/pickles-hook.mjs
```

本阶段必须明确源脚本与目标工程脚本的同步方式。若采用复制方式，复制脚本必须是显式命令或测试步骤，不得依赖用户全局 `~/.codex`。

### 3.3 Hook HTTP client

Hook HTTP client 固定实现以下能力：

1. 从当前工作目录通过 `git rev-parse --show-toplevel` 定位目标工程根目录。
2. 读取 `<repo>/.pickles/server.json`。
3. 校验 `server.json.port` 是 number。
4. 组装 base URL：`http://127.0.0.1:<port>`。
5. 调用 `GET /health`。
6. 调用 `POST /notify`。
7. 调用 `POST /feedback`。
8. 所有 request 固定包含 `schemaVersion = 1` 与非空 `requestId`。
9. HTTP 非 2xx 或 JSON 解析失败时，错误必须输出到 stderr。
10. 失败时 process exit code 固定非 0。

### 3.4 Hook input mapping

本阶段只实现最小 hook input 映射。

公共字段固定从 Codex hook stdin JSON 读取：

- `session_id`
- `turn_id`
- `hook_event_name`
- `cwd`

映射规则：

- `session_id` -> `sessionId`
- `turn_id` -> `turnId`
- `hook_event_name` -> `hookEventName`
- git root -> `workspace`

`SessionStart` 没有 `turn_id` 时，`turnId` 固定为 `null`。

### 3.5 Notify stub payload

本阶段 `POST /notify` 固定只用于 contract 通路验证。

`PostToolUse` 触发时，Hook 必须发送合法 notify payload。

`files` 固定使用空 array，除非测试显式传入 `PICKLES_TEST_CHANGED_FILE`。

测试模式下 `PICKLES_TEST_CHANGED_FILE` 固定 JSON 结构：

```json
{
  "fileName": "src/pricing.ts",
  "before": "old",
  "after": "new"
}
```

测试模式不得进入正式 Codex Hook 配置。

### 3.6 Feedback pull

`Stop` 触发时，Hook 必须调用 `POST /feedback`。

Hook 必须把 feedback response 以 Codex 可见方式输出。

当前 Plugin 返回 `status = "unimplemented"` 时，Hook 必须成功退出，并输出该状态。

### 3.7 SessionStart health check

`SessionStart` 触发时，Hook 必须调用 `GET /health`。

health 成功时，Hook 必须成功退出。

health 失败时，Hook 必须输出 stderr 并返回非 0 exit code。

### 3.8 Hook configuration sample

`e2e/sample-project/.codex/hooks.json` 固定配置目标工程本地 hook。

本阶段只配置：

- `SessionStart`
- `PostToolUse`
- `Stop`

`PreToolUse` 在完整 before 快照阶段再配置。

hook command 必须从当前 git root 定位 `.codex/hooks/pickles-hook.mjs`，不得写死 Pickles 仓库路径。

## 4. Verification

### 4.1 Unit contract test

必须新增 fake HTTP server 测试。

固定断言：

- fake server 收到 `GET /health`。
- fake server 收到 `POST /notify`。
- fake server 收到 `POST /feedback`。
- notify request 包含 `schemaVersion = 1`。
- notify request 包含 `requestId`。
- notify request 包含 `event.sessionId`。
- notify request 包含 `event.hookEventName = "PostToolUse"`。
- notify request 包含 `event.workspace`。
- notify request 包含 `event.idempotencyKey`。
- notify request 的 `files` 是 array。
- feedback request 不包含文件内容。
- server.json 缺失时脚本失败并输出 stderr。
- server.json port 非 number 时脚本失败并输出 stderr。

### 4.2 e2e contract debug

最小手动 e2e 固定步骤：

1. 启动 IntelliJ Plugin：

```bash
cd pickles-intellij-plugin
export JAVA_HOME="$(jenv prefix jetbrains64-17.0.14)"
gradle runIde
```

2. 在 `e2e/sample-project/` 执行 hook 脚本模拟 `SessionStart`。
3. 在 `e2e/sample-project/` 执行 hook 脚本模拟 `PostToolUse`。
4. 在 `e2e/sample-project/` 执行 hook 脚本模拟 `Stop`。

固定断言：

- `SessionStart` 调用 `/health` 成功。
- `PostToolUse` 调用 `/notify` 成功。
- `Stop` 调用 `/feedback` 成功并输出 `unimplemented`。

### 4.3 Project verification

若 `pickles-hooks/` 新增 Node 测试入口，必须能在无 npm install 前提下运行。

可用验证命令固定写入最终响应。若新增 `package.json`，不得引入第三方依赖。

## 5. Documentation Sync

实现完成后必须检查：

- [`../10-requirements/CODEX-HOOKS-REQUIREMENTS.md`](../10-requirements/CODEX-HOOKS-REQUIREMENTS.md)
- [`../20-interfaces/HOOK-PLUGIN-HTTP-CONTRACT.md`](../20-interfaces/HOOK-PLUGIN-HTTP-CONTRACT.md)
- [`../40-readiness/E2E-TEST-CASES.md`](../40-readiness/E2E-TEST-CASES.md)
- `e2e/sample-project/.codex/hooks.json`

若 `HOOK_PLUGIN_CONTRACT` 自动化完成，必须将其状态从待实现更新为已实现，并同步 PR workflow 是否接入。

## 6. Closure

任务完成时必须：

1. 删除或收窄 `TODO.md` 中对应任务。
2. 若 RUNBOOK 已执行完毕且无剩余价值，删除本 RUNBOOK。
3. 将代码、测试、配置样例、文档同步、`TODO.md` 清理和 RUNBOOK 清理放入同一个语义完整 commit。

## 7. Open Items

无
