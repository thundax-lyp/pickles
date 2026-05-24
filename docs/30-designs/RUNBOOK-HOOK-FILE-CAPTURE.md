# Runbook: Hook File Capture

## 1. Purpose

本文档定义 `pickles-hooks/` 文件变动捕获状态机的一次性执行手册。

目标是在现有 Hook HTTP contract 已打通的基础上，实现真实文件 before / after 捕获，让 `PostToolUse` 能向 Plugin `/notify` 上报实际变动文件。

## 2. Scope

当前范围：

- `PreToolUse` before 快照
- `PostToolUse` workspace diff 确认
- `Stop` pending diff flush
- `<repo>/.pickles/hooks-state/` 状态文件
- git workspace tracked / untracked 文件捕获
- 新增文件、修改文件、删除文件
- rename / move 表达为旧路径删除和新路径新增
- `e2e/sample-project/.codex/hooks.json` 增加 `PreToolUse`
- fake server contract test 扩展
- 验证脚本和 testcase 文档同步

不在范围内：

- 不解析 Bash 命令真实语义作为最终文件列表
- 不把 `tool_name` 或 `tool_input` 当作最终文件列表真相源
- 不执行 ArchUnit、ESLint 或其他规则命令
- 不调用 `pickles-runtime/`
- 不修改用户业务代码
- 不读取、修改或依赖用户全局 `~/.codex`
- 不引入 npm install 或第三方 npm package
- 不实现 MCP 或 WebSocket 通知协议

## 3. Execution Order

### 3.1 现状确认

1. 读取 [`../10-requirements/CODEX-HOOKS-REQUIREMENTS.md`](../10-requirements/CODEX-HOOKS-REQUIREMENTS.md)。
2. 读取 [`../20-interfaces/CODEX-HOOKS-OFFICIAL-REFERENCE.md`](../20-interfaces/CODEX-HOOKS-OFFICIAL-REFERENCE.md)。
3. 读取 [`../20-interfaces/HOOK-PLUGIN-HTTP-CONTRACT.md`](../20-interfaces/HOOK-PLUGIN-HTTP-CONTRACT.md)。
4. 读取 [`../40-readiness/E2E-TEST-CASES.md`](../40-readiness/E2E-TEST-CASES.md) 中 `HOOK_PLUGIN_CONTRACT`。
5. 检查 `pickles-hooks/pickles-hook.mjs` 当前事件分发。
6. 检查 `e2e/sample-project/.codex/hooks.json` 当前 Hook 配置。

### 3.2 State ownership

Hook 状态文件固定放在目标工程：

```text
<repo>/.pickles/hooks-state/
```

状态文件命名固定基于：

```text
<session_id>/<turn_id>/<tool_use_id>.json
```

`SessionStart` 不写入捕获状态。

状态文件固定只保存当前 tool 捕获需要的 before 快照和候选文件，不保存全量语义持久化数据。

`<repo>/.pickles/.gitignore` 必须忽略：

```text
hooks-state/
```

### 3.3 Changed file model

上报到 `/notify` 的 `ChangedFile` 固定满足：

- `fileName` 是相对目标工程根目录路径。
- `fileName` 不得是绝对路径。
- `before` 是 string 或 `null`。
- `after` 是 string 或 `null`。
- 新增文件固定 `before = null`。
- 删除文件固定 `after = null`。
- 修改文件固定同时包含 before string 和 after string。
- rename / move 固定拆为旧路径删除和新路径新增。

### 3.4 PreToolUse before snapshot

`PreToolUse` 固定执行：

1. 读取 hook stdin JSON。
2. 校验 `session_id`、`turn_id`、`tool_use_id`、`tool_name`。
3. 从当前工作目录定位目标工程根目录。
4. 根据 `tool_name` 和 `tool_input` 提取候选文件。
5. 对存在于 workspace 中的候选文件读取 before 内容。
6. 对不存在的候选文件记录 before 为 `null`。
7. 将状态写入 `<repo>/.pickles/hooks-state/<session>/<turn>/<tool>.json`。
8. `PreToolUse` 成功时不调用 `/notify`。

第一阶段候选提取固定支持：

- `apply_patch`：解析 patch header 中的 add / update / delete / move 文件路径。
- `Bash`：只记录空候选，最终文件列表由 `PostToolUse` workspace diff 确认。
- `Edit` / `Write`：从常见 `file_path`、`path` 字段提取候选文件。
- 其他 tool：记录空候选。

### 3.5 PostToolUse actual diff

`PostToolUse` 固定执行：

1. 读取 hook stdin JSON。
2. 从 `<repo>/.pickles/hooks-state/` 读取对应 before 状态。
3. 使用 workspace diff 或文件状态扫描确认实际变动文件。
4. 读取实际变动文件 after 内容。
5. 合并 before 状态与 after 内容生成 `ChangedFile[]`。
6. 调用 `POST /notify`。
7. `/notify` 成功后删除对应状态文件。

实际变动文件确认第一阶段固定使用 git：

```bash
git status --porcelain --untracked-files=all
```

文件内容读取规则：

- 工作区存在文件：读取当前文件内容作为 after。
- 工作区不存在文件：after 固定为 `null`。
- before 状态存在：使用状态中的 before。
- before 状态缺失：从 `git show HEAD:<file>` 读取 tracked 文件 before；读取失败时 before 固定为 `null`。

### 3.6 Stop pending flush

`Stop` 固定执行：

1. 扫描当前 session / turn 下未清理的 pending state。
2. 对 pending workspace diff 执行一次 `/notify` flush。
3. flush 成功后删除 pending state。
4. 再调用 `POST /feedback`。

没有 pending diff 时，`Stop` 只调用 `POST /feedback`。

`/feedback` request 固定不得包含文件内容。

### 3.7 Hook configuration sample

`e2e/sample-project/.codex/hooks.json` 固定增加 `PreToolUse`。

第一阶段配置事件固定为：

- `SessionStart`
- `PreToolUse`
- `PostToolUse`
- `Stop`

`PreToolUse` matcher 固定覆盖：

- `Bash`
- `apply_patch`
- `Edit`
- `Write`

hook command 必须从当前 git root 定位 `.codex/hooks/pickles-hook.mjs`，不得写死 Pickles 仓库绝对路径。

## 4. Verification

### 4.1 Unit contract test

必须扩展 `pickles-hooks/test/hook-http-contract.test.mjs`。

固定断言：

- 修改已有 tracked 文件时，`/notify` 收到 before 和 after。
- 新增文件时，`before = null`。
- 删除文件时，`after = null`。
- `ChangedFile.fileName` 是相对路径。
- `ChangedFile.fileName` 不是绝对路径。
- `PreToolUse` 写入 state。
- `PostToolUse` 成功 notify 后清理 state。
- `PostToolUse` 缺失 state 时仍能用 git fallback 生成 before。
- `Stop` 在 pending state 存在时先 flush `/notify`，再调用 `/feedback`。
- `/feedback` request 不包含文件内容。

### 4.2 Hook config verification

固定断言：

- `e2e/sample-project/.codex/hooks.json` 包含 `PreToolUse`。
- `PreToolUse` command 使用目标工程本地 `.codex/hooks/pickles-hook.mjs`。
- `PreToolUse` 不读取或依赖用户全局 `~/.codex`。

### 4.3 Project verification

固定验证命令：

```bash
scripts/verify-hooks.sh
GRADLE_CMD=/tmp/pickles-gradle-8.13/gradle-8.13/bin/gradle scripts/verify-all.sh
```

## 5. Documentation Sync

实现完成后必须检查：

- [`../10-requirements/CODEX-HOOKS-REQUIREMENTS.md`](../10-requirements/CODEX-HOOKS-REQUIREMENTS.md)
- [`../20-interfaces/CODEX-HOOKS-OFFICIAL-REFERENCE.md`](../20-interfaces/CODEX-HOOKS-OFFICIAL-REFERENCE.md)
- [`../20-interfaces/HOOK-PLUGIN-HTTP-CONTRACT.md`](../20-interfaces/HOOK-PLUGIN-HTTP-CONTRACT.md)
- [`../40-readiness/E2E-TEST-CASES.md`](../40-readiness/E2E-TEST-CASES.md)
- `e2e/sample-project/.codex/hooks.json`
- `e2e/sample-project/.pickles/.gitignore`

若 `HOOK_PLUGIN_CONTRACT` 已覆盖真实文件捕获，必须更新其固定断言。

## 6. Closure

任务完成时必须：

1. 删除或收窄 `TODO.md` 中对应任务。
2. 删除本 RUNBOOK。
3. 将代码、测试、配置样例、文档同步、`TODO.md` 清理和 RUNBOOK 清理收敛到阶段 PR。

## 7. Open Items

无
