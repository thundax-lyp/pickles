# Codex Hooks Official Reference

## 1. Purpose

本文档整理 OpenAI 官方 Codex Hooks 文档中与 Pickles MVP 开发相关的用法。

目标是给 `pickles-hooks/`、`pickles-intellij-plugin/` 和 `pickles-runtime/` 后续实现提供稳定参考入口。

## 2. Scope

当前范围：

- Codex Hooks 基本用途
- Hook 配置位置
- Hook 事件
- matcher 行为
- 输入输出字段
- 当前能力限制
- 与 Codex CLI / Codex app 版本相关的能力记录

不在范围内：

- 不定义 Pickles 自己的 Hook HTTP API
- 不定义 Pickles 的 AGENTS.md 注入格式
- 不复制 OpenAI 官方完整 schema

## 3. Source Attribution

来源：

- OpenAI Developers Codex Hooks: [developers.openai.com/codex/hooks](https://developers.openai.com/codex/hooks)
- OpenAI Developers Codex Changelog: [developers.openai.com/codex/changelog](https://developers.openai.com/codex/changelog)
- OpenAI Developers Codex Feature Maturity: [developers.openai.com/codex/feature-maturity](https://developers.openai.com/codex/feature-maturity)
- OpenAI Codex pull request `18391`: [github.com/openai/codex/pull/18391](https://github.com/openai/codex/pull/18391)

检索日期：2026-05-22。

官方 Hooks 页面声明它是当前 release behavior reference；如果 GitHub schema 与该页面不一致，Pickles 以该页面为准。

## 4. Bounded Context

Codex Hooks 是 Codex 生命周期内运行确定性脚本的扩展机制。Pickles MVP 使用 Codex Hook 在 Codex Runtime 中捕获 task 生命周期与文件变动，并通过本地 HTTP 通知 IntelliJ Plugin。

Pickles 不使用 Codex Hook 直接执行规则命令。规则命令由 Governance Engine 调用。

## 5. Capability Version Map

| 能力 | 官方状态 / 版本记录 | Pickles 采用口径 |
|---|---|---|
| Hooks 稳定能力 | Codex CLI `0.124.0` changelog 写明 Hooks stable，可配置在 `config.toml` 和 managed `requirements.toml`，并可观察 MCP tools、`apply_patch` 与 long-running Bash sessions。 | Pickles Hook MVP 以 Codex CLI `0.124.0+` 作为稳定 Hooks 基线。 |
| Hooks general availability | 2026-05-14 changelog 写明 Hooks general availability。 | Pickles 文档引用该 GA 状态，但实现仍以当前 Hooks 页行为为准。 |
| In-app hook trust review | Codex app `26.506` changelog 写明加入 hooks trust review flow，并让 Hooks settings 在完整配置前可见。 | Pickles 不依赖 Codex app trust UI；仅记录用户环境可能需要 trust review。 |
| Plugin bundled hooks visibility | Codex CLI `0.130.0` changelog 写明 plugin details 显示 bundled hooks。 | Pickles MVP 不使用 plugin-bundled hooks。 |
| Plugin workflow / hooks evolution | Codex CLI `0.131.0` changelog 写明 plugin workflows 包含 default-enabled plugin hooks 等变化。 | 当前 Hooks 页仍写明 plugin-bundled hooks 在本 release 中 opt-in，需要 `[features].plugin_hooks = true`。Pickles 以当前 Hooks 页为准。 |
| MCP tools in hooks | Codex CLI `0.124.0` changelog 写明 hooks 可观察 MCP tools。 | Pickles MVP 不用 MCP 作为 Hook 通知协议，但需要知道 matcher 可匹配 MCP tool names。 |

## 6. Hook Enablement

Hooks 当前默认启用。

关闭 hooks 的 `config.toml` 写法：

```toml
[features]
hooks = false
```

`hooks` 是官方当前 canonical feature key。`codex_hooks` 仍可工作，但已是 deprecated alias。

管理员也可以在 `requirements.toml` 中使用 `[features].hooks = false` 强制关闭 hooks。

## 7. Hook Discovery

Codex 在 active config layers 旁边发现 hooks。

支持两种形式：

- `hooks.json`
- `config.toml` 中 inline `[hooks]` tables

常用位置：

- `~/.codex/hooks.json`
- `~/.codex/config.toml`
- `<repo>/.codex/hooks.json`
- `<repo>/.codex/config.toml`

多个 hook source 会全部加载。高优先级 config layer 不替换低优先级 hooks。

如果同一层同时存在 `hooks.json` 和 inline `[hooks]`，Codex 会合并并在启动时警告。Pickles 固定只使用一种表示方式。

Project-local hooks 只有在项目 `.codex/` layer 被 trust 后才加载。未 trust 的项目中，Codex 仍加载 user 和 system hooks。

## 8. Hook Review And Trust

Codex 在决定哪些 hooks 可运行前会列出 configured hooks。

用户可以在 CLI 中使用 `/hooks`：

- 查看 hook sources
- review 新增或变更的 hooks
- trust hooks
- disable individual non-managed hooks

如果启动时 hooks 需要 review，Codex 会提示用户打开 `/hooks`。

Managed hooks 来自 system、MDM、cloud 或 `requirements.toml` sources。Managed hooks 由 policy trust，不能从用户 hook browser 禁用。

## 9. Hook Configuration Shape

JSON 示例结构：

```json
{
  "hooks": {
    "SessionStart": [
      {
        "matcher": "startup|resume",
        "hooks": [
          {
            "type": "command",
            "command": "python3 ~/.codex/hooks/session_start.py",
            "statusMessage": "Loading session notes"
          }
        ]
      }
    ],
    "PostToolUse": [
      {
        "matcher": "Bash",
        "hooks": [
          {
            "type": "command",
            "command": "/usr/bin/python3 \"$(git rev-parse --show-toplevel)/.codex/hooks/post_tool_use_review.py\"",
            "timeout": 30,
            "statusMessage": "Reviewing Bash output"
          }
        ]
      }
    ],
    "Stop": [
      {
        "hooks": [
          {
            "type": "command",
            "command": "/usr/bin/python3 \"$(git rev-parse --show-toplevel)/.codex/hooks/stop_continue.py\"",
            "timeout": 30
          }
        ]
      }
    ]
  }
}
```

配置规则：

- `timeout` 单位是秒。
- 未设置 `timeout` 时，Codex 使用 `600` 秒。
- `statusMessage` 可选。
- `async` 会被解析，但 async command hooks 当前不支持；`async: true` handler 会被跳过。
- 当前只有 `type: "command"` handler 会运行。
- `prompt` 和 `agent` handlers 会被解析但跳过。
- 命令工作目录是当前 session `cwd`。
- repo-local hooks 应优先从 git root 定位脚本，不依赖相对启动目录。

## 10. Matcher Rules

`matcher` 是 regex string。

以下写法匹配全部支持事件：

- `"*"`
- `""`
- 省略 `matcher`

当前支持 matcher 的事件：

| Event | matcher 过滤对象 |
|---|---|
| `PermissionRequest` | tool name |
| `PostToolUse` | tool name |
| `PreToolUse` | tool name |
| `SessionStart` | start source |
| `UserPromptSubmit` | 不支持 matcher，配置会被忽略 |
| `Stop` | 不支持 matcher，配置会被忽略 |

工具名 matcher 支持：

- `Bash`
- `apply_patch`
- `Edit`
- `Write`
- MCP tool names，例如 `mcp__filesystem__read_file`
- MCP tool regex，例如 `mcp__filesystem__.*`

## 11. Common Input Fields

每个 command hook 在 `stdin` 接收一个 JSON object。

常用公共字段：

- `session_id`: 当前 session 或 thread id
- `transcript_path`: session transcript 路径，可能为 `null`
- `cwd`: session 工作目录
- `hook_event_name`: 当前 hook event name
- `model`: 当前 active model slug，是 Codex-specific extension

Turn-scoped hooks 会包含 `turn_id`。

`SessionStart`、`PreToolUse`、`PermissionRequest`、`PostToolUse`、`UserPromptSubmit` 和 `Stop` 还包含 `permission_mode`。值包括：

- `default`
- `acceptEdits`
- `plan`
- `dontAsk`
- `bypassPermissions`

`transcript_path` 是便利字段，但 transcript 格式不是稳定 hook interface。

## 12. Common Output Fields

`SessionStart`、`UserPromptSubmit` 和 `Stop` 支持公共 JSON 输出字段：

```json
{
  "continue": true,
  "stopReason": "optional",
  "systemMessage": "optional",
  "suppressOutput": false
}
```

输出规则：

- exit `0` 且无输出表示成功，Codex 继续。
- `suppressOutput` 当前会被解析，但尚未实现。
- `PreToolUse` 和 `PermissionRequest` 支持 `systemMessage`，但不支持 `continue`、`stopReason`、`suppressOutput`。
- `PostToolUse` 支持 `systemMessage`、`continue: false` 和 `stopReason`。

## 13. Event Reference

### 13.1 SessionStart

`matcher` 应用于 `source`。

额外输入字段：

- `source`: `startup`、`resume` 或 `clear`

stdout 行为：

- plain text 会作为额外 developer context。
- JSON 可以通过 `hookSpecificOutput.additionalContext` 添加额外 developer context。

Pickles MVP 固定使用 `SessionStart` 执行 session 初始化、本地 Plugin 可用性检查和启动上下文提示。

### 13.2 PreToolUse

`PreToolUse` 可以拦截：

- `Bash`
- `apply_patch`
- MCP tool calls

限制：

- 它是 guardrail，不是完整 enforcement boundary。
- 它不能拦截所有 shell calls。
- 它不拦截 `WebSearch` 或其他非 shell、非 MCP tool calls。

额外输入字段：

- `turn_id`
- `tool_name`
- `tool_use_id`
- `tool_input`

支持输出：

- 使用 `permissionDecision: "deny"` 拒绝支持的 tool call。
- 使用旧格式 `decision: "block"` 也可阻断。
- 使用 exit code `2` 并在 `stderr` 写原因也可阻断。
- 使用 `hookSpecificOutput.additionalContext` 添加 model-visible context。
- 对 Bash 和 `apply_patch`，可以在 `permissionDecision: "allow"` 中返回 `updatedInput.command` 重写输入。

Pickles MVP 固定使用 `PreToolUse` 观察 `apply_patch` 或 shell 操作，提取候选文件并读取 before 内容，但不能把它当作最终文件变动通知边界。

### 13.3 PermissionRequest

`PermissionRequest` 在 Codex 将要请求 approval 时运行，例如 shell escalation 或 managed-network approval。

额外输入字段：

- `turn_id`
- `tool_name`
- `tool_input`
- `tool_input.description`

支持输出：

- `decision.behavior = "allow"`
- `decision.behavior = "deny"` 并带 `message`
- 不作决定时，Codex 使用正常 approval flow

多 hook 返回决策时，任意 `deny` 获胜。否则 `allow` 可以让请求继续而不弹出普通 approval prompt。

Pickles MVP 不依赖 `PermissionRequest`。

### 13.4 PostToolUse

`PostToolUse` 在支持的 tool 产生输出后运行，包括：

- `Bash`
- `apply_patch`
- MCP tool calls

Bash 非零退出也会触发。

限制：

- 已经发生的 tool side effects 不能被撤销。
- 它不能拦截所有 shell calls。
- 它不拦截 `WebSearch` 或其他非 shell、非 MCP tool calls。

额外输入字段：

- `turn_id`
- `tool_name`
- `tool_use_id`
- `tool_input`
- `tool_response`

支持输出：

- `hookSpecificOutput.additionalContext` 添加 developer context。
- `decision: "block"` 不会撤销已完成命令；Codex 会记录反馈，用 hook message 替换 tool result，并继续。
- exit code `2` 可向 `stderr` 写 feedback reason。
- `continue: false` 会停止原始 tool result 的正常处理。

Pickles MVP 固定使用 `PostToolUse`，用于在 `apply_patch` 或 Bash 后通知本地 Plugin。

#### 13.4.1 File Name Extraction Notes

官方 Hooks 文档没有为 `PostToolUse` 定义统一的 `fileName` 字段。

Pickles 在 `PostToolUse` 中必须读取：

- `tool_name`
- `tool_input`
- `tool_response`

Pickles 使用以下规则提取文件名：

| tool_name | 文件名来源 | Pickles 处理规则 |
|---|---|---|
| `apply_patch` | `tool_input.command` 中的 raw patch body | 解析 patch header，提取 add / update / delete / move 涉及的文件路径。 |
| `Bash` | `tool_input.command` 中的 shell command | 只能作为候选线索，不得作为最终变动文件列表。 |
| `mcp__*` | 具体 MCP tool 的参数 schema | 按 tool-specific adapter 提取；没有 adapter 时只能作为候选线索。 |

OpenAI Codex pull request `18391` 显示，`apply_patch` hook 会把 raw patch body 放入 `tool_input.command`，并且 hook stdin 使用 canonical `tool_name` `apply_patch`。Pickles 以此作为 `apply_patch` 文件路径解析依据。

`Bash` 命令可能包含重定向、脚本、子进程、通配符、工具链副作用或间接文件写入。Pickles 不得仅依赖 shell command 文本确定最终变动文件。

Pickles 最终上报的文件列表必须通过 Pickles 自己的 before / after 快照或 workspace diff 确认。`tool_name` 与 `tool_input` 只用于缩小候选范围和选择解析策略。

#### 13.4.2 Stable Before / After Computation

Pickles 稳定计算 before / after 内容的规则：

1. `PreToolUse` 根据 `tool_name` 与 `tool_input` 提取候选文件。
2. `PreToolUse` 在 tool 执行前读取候选文件内容作为 before。
3. `PostToolUse` 根据 `tool_name`、`tool_input` 与 `tool_response` 再次提取候选文件。
4. `PostToolUse` 使用 workspace diff 或文件状态扫描确认实际变动文件。
5. `PostToolUse` 读取实际变动文件内容作为 after。
6. `Stop` 对未上报的 pending workspace diff 执行 flush。

新增文件的 before 固定为 `null`。

删除文件的 after 固定为 `null`。

文件 rename / move 必须表达为旧路径删除与新路径新增，除非后续 HTTP schema 明确定义 rename 结构。

该流程关闭 Pickles before / after 稳定计算问题。后续实现只能细化 diff 算法，不改变 `tool_name` / `tool_input` 不是最终真相源的约束。

### 13.5 UserPromptSubmit

`matcher` 当前不使用。

额外输入字段：

- `turn_id`
- `prompt`

stdout 行为：

- plain text 会作为额外 developer context。
- JSON 可通过 `hookSpecificOutput.additionalContext` 添加额外 developer context。
- 可通过 `decision: "block"` 阻止 prompt。
- exit code `2` 加 `stderr` reason 也可阻止 prompt。

Pickles MVP 不依赖 `UserPromptSubmit`。

### 13.6 Stop

`matcher` 当前不使用。

额外输入字段：

- `turn_id`
- `stop_hook_active`
- `last_assistant_message`

输出规则：

- exit `0` 时 stdout 必须是 JSON，plain text 对该事件无效。
- `decision: "block"` 不拒绝 turn，而是让 Codex 继续，并用 `reason` 创建新的 continuation prompt。
- exit code `2` 加 `stderr` reason 也可以触发 continuation。
- 任意 matching `Stop` hook 返回 `continue: false` 时，优先于其他 continuation decision。

Pickles MVP 应使用 `Stop` 作为任务完成前请求治理反馈的关键事件。

## 14. Pickles MVP Mapping

| Pickles 需要 | Codex Hooks 对应能力 | 采用方式 |
|---|---|---|
| Session 初始化 | `SessionStart` | 用于读取 `.pickles.json`、检查本地 Plugin 可用性并向 Codex 暴露启动上下文。 |
| 捕获 Codex task 生命周期 | `SessionStart`、`PreToolUse`、`PostToolUse`、`Stop`、turn-scoped `turn_id`、公共 `session_id` | `SessionStart` 用于启动初始化；`PreToolUse` / `PostToolUse` 用于变动捕获；`Stop` 用于任务完成前治理反馈。 |
| 捕获文件变动线索 | `PreToolUse` before `apply_patch` / `Bash`; `PostToolUse` after `apply_patch` / `Bash` | `PreToolUse` 提取候选文件并读取 before 内容；`PostToolUse` 根据 `tool_name` / `tool_input` 选择解析策略，并触发向 Plugin 上报。 |
| 在完成前阻止或继续 | `Stop` 的 `decision: "block"` 会创建 continuation prompt | ERROR 存在时，Stop hook 可要求 Codex 继续修复。 |
| 向 Codex 增加上下文 | `additionalContext` / `systemMessage` | WARN 或诊断信息可作为上下文返回。 |
| 处理 approval | `PermissionRequest` | MVP 不依赖。 |
| 本地通知 Plugin | command hook 内自行发起 HTTP 请求 | Codex Hooks 本身不提供 Pickles HTTP 协议；Pickles Hook script 调用本地 HTTP。 |

## 15. Implementation Notes For Pickles

- Pickles repo-local hook 脚本应从 git root 定位，避免 Codex 从子目录启动导致路径失效。
- Pickles MVP 固定使用目标工程 `<repo>/.codex/hooks.json` 配置 Hook。
- Pickles Hook 通过当前 git root 定位目标工程，并读取 `<repo>/.pickles/server.json` 发现本地 HTTP 端口。
- Pickles Hook 使用 `http://127.0.0.1:<port>` 调用本地 Plugin HTTP 服务。
- Pickles Hook 脚本固定使用 Node.js ESM `.mjs`。
- Pickles Hook 脚本固定放在目标工程 `<repo>/.codex/hooks/`。
- Pickles Hook MVP 不依赖 npm install，只使用 Node.js built-in modules。
- Pickles MVP 固定配置 `SessionStart`、`PreToolUse`、`PostToolUse` 和 `Stop`。
- Pickles Bind 不读取、不修改、不依赖用户全局 `~/.codex`。
- Pickles MVP 不使用 plugin-bundled hooks，因为当前 Hooks 页显示 plugin-bundled hooks 需要 `[features].plugin_hooks = true` 才会被发现。
- Project-local hooks 需要项目 `.codex/` layer 被 trust。
- Hook command 的工作目录是 Codex session `cwd`，这与 Pickles 的目标工程根目录定位有关。
- `PostToolUse` 无法撤销副作用，因此 Pickles 必须把它视为通知/反馈点，而不是防护边界。
- `PostToolUse` 没有统一 `fileName` 字段；Pickles 必须根据 `tool_name` 与 `tool_input` 选择解析策略。
- `apply_patch` 可以从 `tool_input.command` 解析 patch 文件路径。
- `Bash` 的 `tool_input.command` 只能作为文件候选线索，最终文件列表必须用 before / after 快照或 workspace diff 确认。
- `PreToolUse` 适合记录 pending tool input、before 内容或变动线索，不承担最终通知。
- Pickles 通过 `PreToolUse` 快照 before、`PostToolUse` 确认 after、`Stop` flush pending diff 稳定计算文件变动。
- `SessionStart` 适合做初始化和可用性检查，不承担文件变动捕获。
- `Stop` hook 的 continuation 语义适合 Pickles 在 ERROR 存在时要求 Codex 继续修复。

## 16. Open Items

无
