# E2E Test Cases

## 1. Purpose

本文档定义 Pickles 的端到端测试用例、分段测试用例和多人开发过程中的必测内容。

目标是把真实使用流程、模块边界和防止 AI 意图漂移的关键约束固化为可执行测试口径。

## 2. Scope

当前范围：

- 真实使用全流程 testcase
- Plugin HTTP contract e2e testcase
- Hook 到 Plugin 分段 testcase
- Runtime 到 sample project 分段 testcase
- Plugin 到 Runtime 分段 testcase
- PR 必测内容
- 当前已实现与待实现状态

不在范围内：

- 不定义发布验收流程
- 不定义性能压测
- 不定义云端运行时测试
- 不定义 MCP 或 WebSocket 通知协议测试

## 3. Bounded Context

Pickles 的真实使用流程固定围绕一个目标工程运行。Pickles 仓库自身的端到端目标工程固定为 `e2e/sample-project/`。

完整闭环固定为：

1. IntelliJ IDEA 打开目标工程。
2. Pickles IntelliJ Plugin 启动本地 HTTP server。
3. Plugin 将端口写入 `<repo>/.pickles/server.json`。
4. Codex Hook 从 `<repo>/.pickles/server.json` 发现端口。
5. Codex Hook 在 `PreToolUse` / `PostToolUse` 捕获 before / after 文件变动。
6. Codex Hook 通过 `POST /notify` 上报变动。
7. Plugin 将变动集交给 Governance Runtime。
8. Runtime 读取 `<repo>/pickles.config.ts` 并执行 Pickles native rules。
9. Runtime 聚合 Problem。
10. Plugin 刷新 Problem Board。
11. Codex Hook 在 `Stop` 通过 `POST /feedback` 拉取治理反馈。
12. Codex 根据反馈修复或汇报问题。

当前自动化测试只覆盖完整闭环的一部分。未实现部分必须在本文档中保持明确状态，不得伪装为已覆盖。

## 4. Module Mapping

- `pickles-intellij-plugin/`: Plugin HTTP server、Problem Board service、Plugin contract 单元测试、`runIde` 调试。
- `pickles-hooks/`: Codex Hook 脚本、server discovery、notify / feedback 调用。
- `pickles-runtime/`: Governance Runtime、native rule execution、Problem 聚合。
- `e2e/sample-project/`: 目标工程样例、Pickles runtime config、sample Java native rule。
- `docs/20-interfaces/HOOK-PLUGIN-HTTP-CONTRACT.md`: Hook 与 Plugin HTTP contract。
- `docs/20-interfaces/PROBLEM-MODEL-CONTRACT.md`: Problem model contract。
- `docs/40-readiness/PR-WORKFLOW.md`: PR 必测入口。

## 5. Core Objects

- `E2E_FULL_FLOW`
- `PLUGIN_HTTP_CONTRACT_E2E`
- `HOOK_PLUGIN_CONTRACT`
- `RUNTIME_SAMPLE_PROJECT`
- `PLUGIN_RUNTIME_FLOW`
- `PR_REQUIRED_TESTS`

## 6. Global Constraints

- testcase 必须描述被验证的真实流程，不得只描述实现细节。
- testcase 必须说明覆盖的模块边界。
- testcase 必须说明防止 AI 意图漂移的断言点。
- 已实现 testcase 必须有可运行命令或可重复调试步骤。
- 未实现 testcase 必须标记为待实现，不得进入 PR 必过列表。
- 新增跨模块能力时，必须同步补充或调整本文档。
- PR 自动验证入口固定由 `docs/40-readiness/PR-WORKFLOW.md` 和 `scripts/verify-all.sh` 承载。

## 7. Functional Requirements

### 7.1 PLUGIN_HTTP_CONTRACT_E2E

状态：已实现，手动 e2e 调试已跑通。

目标：验证 IntelliJ Plugin 在目标工程中启动本地 HTTP server，并按 Hook HTTP contract 响应。

覆盖模块：

- `pickles-intellij-plugin/`
- `e2e/sample-project/`
- `docs/20-interfaces/HOOK-PLUGIN-HTTP-CONTRACT.md`

固定步骤：

1. 构建插件：

```bash
cd pickles-intellij-plugin
export JAVA_HOME="$(jenv prefix jetbrains64-17.0.14)"
gradle build
```

2. 启动调试 IDE：

```bash
gradle runIde
```

3. 读取端口：

```bash
PORT=$(node -e "console.log(require('./e2e/sample-project/.pickles/server.json').port)")
```

4. 调用 health：

```bash
curl -i "http://127.0.0.1:$PORT/health"
```

5. 调用 notify：

```bash
curl -i -X POST "http://127.0.0.1:$PORT/notify" \
  -H 'Content-Type: application/json' \
  --data '{
    "schemaVersion": 1,
    "requestId": "e2e-notify-1",
    "event": {
      "sessionId": "e2e-session",
      "turnId": "e2e-turn",
      "hookEventName": "PostToolUse",
      "workspace": "/Users/lizixi/workspace/pickles/e2e/sample-project",
      "idempotencyKey": "e2e-session:e2e-turn:PostToolUse:src/pricing.ts"
    },
    "files": [
      {
        "fileName": "src/pricing.ts",
        "before": "old",
        "after": "new"
      }
    ]
  }'
```

6. 调用 feedback：

```bash
curl -i -X POST "http://127.0.0.1:$PORT/feedback" \
  -H 'Content-Type: application/json' \
  --data '{
    "schemaVersion": 1,
    "requestId": "e2e-feedback-1",
    "sessionId": "e2e-session",
    "turnId": "e2e-turn",
    "workspace": "/Users/lizixi/workspace/pickles/e2e/sample-project"
  }'
```

固定断言：

- `<repo>/.pickles/server.json` 被 Plugin 重写为当前本地 HTTP 端口。
- `GET /health` 返回 HTTP `200`。
- health response 包含 `schemaVersion = 1`、`requestId = null`、`status = "ok"`。
- `POST /notify` 返回 HTTP `202`。
- notify response 包含 `accepted = true` 与 `processed = false`。
- `POST /feedback` 返回 HTTP `200`。
- feedback response 包含 `status = "unimplemented"`、空 `problems`、`errorCount = 0`、`warnCount = 0`。
- 成功响应不得包含全局 `code` / `message` / `data` wrapper。

防漂移点：

- Plugin 必须使用本地 HTTP，不得改成 MCP 或 WebSocket。
- `/notify` 不得同步执行治理并返回 Problem Board。
- `/feedback` request 不得包含文件内容。
- `ChangedFile.fileName` 必须是相对路径。

### 7.2 PLUGIN_HTTP_CONTRACT_UNIT

状态：已实现，已接入 `gradle build`。

目标：用单元测试锁住 Plugin HTTP contract 的核心响应和错误结构。

固定命令：

```bash
cd pickles-intellij-plugin
../scripts/verify-intellij-plugin.sh
```

固定断言：

- `healthReturnsContractEnvelope`
- `notifyReturnsAcceptedStubForValidRequest`
- `notifyRejectsMissingSessionId`
- `notifyRejectsWorkspaceMismatch`
- `feedbackReturnsUnimplementedStub`
- `methodNotAllowedUsesErrorEnvelope`
- `responseObjectsSerializeWithoutGlobalCodeMessageDataWrapper`

防漂移点：

- contract handler 必须独立于 IDE UI 可测试。
- 错误响应必须使用 `error` object。
- `requestId: null` 必须显式序列化。

### 7.3 E2E_SAMPLE_PROJECT_VERIFY

状态：已实现，已纳入 PR workflow 口径。

目标：验证 `e2e/sample-project/` 作为目标工程样例保持可构建。

固定命令：

```bash
cd e2e/sample-project
../../scripts/verify-sample-project.sh
```

固定断言：

- TypeScript 类型检查通过。
- lint 命令通过。
- Pickles runtime config 使用 Pickles native rule，不使用 external adapter。

防漂移点：

- e2e sample project 必须持续代表真实目标工程。
- 样例工程验证失败不得被 Plugin 测试掩盖。

### 7.4 HOOK_PLUGIN_CONTRACT

状态：已实现，已接入 `scripts/verify-all.sh`。

目标：验证 `pickles-hooks/` 的 Node ESM hook 脚本能读取 `server.json` 并调用 Plugin HTTP contract。

覆盖模块：

- `pickles-hooks/`
- `pickles-intellij-plugin/`
- `e2e/sample-project/`

固定命令：

```bash
scripts/verify-hooks.sh
```

固定断言：

- Hook 从当前 git root 定位目标工程。
- Hook 读取 `<repo>/.pickles/server.json`。
- Hook 使用 `http://127.0.0.1:<port>`。
- `PreToolUse` 读取候选文件 before 并写入 hook state。
- `SessionStart` 触发 `GET /health`。
- `PostToolUse` 触发 `POST /notify`。
- `Stop` 触发 `POST /feedback`。
- 修改已有文件时，`/notify` 包含 before 和 after。
- 新增文件时，`before = null`。
- 删除文件时，`after = null`。
- 缺失 hook state 时，`PostToolUse` 使用 git fallback 计算 before。
- `Stop` 在 pending state 存在时先 flush `/notify`，再调用 `/feedback`。
- Hook 通信失败能被 Codex 看见。
- `server.json` 缺失时 Hook 非 0 退出并输出 stderr。
- `server.json.port` 非 number 时 Hook 非 0 退出并输出 stderr。
- `e2e/sample-project/.codex/hooks.json` 绑定 `SessionStart`、`PreToolUse`、`PostToolUse` 和 `Stop`。

防漂移点：

- Hook 不读取或修改用户全局 `~/.codex`。
- Hook 不执行 native rules。
- Hook 不修改用户业务代码。
- `tool_name` 与 `tool_input` 不得作为最终文件列表真相源。
- `ChangedFile.fileName` 不得使用绝对路径。

### 7.5 RUNTIME_SAMPLE_PROJECT

状态：已自动化。

目标：验证 `pickles-runtime/` 能基于 changed files 和 Pickles runtime config 执行样例工程 native rule 并生成 Problem。

覆盖模块：

- `pickles-runtime/`
- `e2e/sample-project/`
- `docs/20-interfaces/PROBLEM-MODEL-CONTRACT.md`

固定命令：

```bash
scripts/verify-runtime-sample-project.sh
```

固定断言：

- Runtime 读取 `<repo>/pickles.config.ts`。
- Runtime 基于 changed files 更新 workspace index。
- Runtime 执行 Pickles native rule。
- Runtime 将 `ProblemInput` 归一化为 Problem。
- Problem 包含 `title`、`type`、`message`、`severity`、`source`、`file`、`position`。
- `source` 固定为 object，包含 `tool` 与 `rule`。
- `source.tool` 固定为 `pickles-native`。

防漂移点：

- Runtime 不拥有 UI。
- Runtime 不修改业务代码。
- Runtime 不保存全量语义持久化数据。
- Runtime 不执行 ArchUnit、ESLint 或用户业务命令。

### 7.6 PLUGIN_RUNTIME_FLOW

状态：已自动化。

目标：验证 Plugin 收到 `/notify` 后能将 changed files 交给 Runtime，并刷新 Problem Board service 数据。

覆盖模块：

- `pickles-intellij-plugin/`
- `pickles-runtime/`

固定命令：

```bash
scripts/verify-intellij-plugin.sh
```

固定断言：

- Plugin 接收合法 `NotifyRequest`。
- Plugin 调用 Runtime。
- Runtime 返回 Problem array。
- Plugin 保存当前 workspace Problem Board 数据。
- UI 删除问题只影响当前展示，不修改用户代码。

防漂移点：

- Plugin 不执行 native rules。
- Plugin 不把 Problem Board 按 task 单独保存。
- Plugin 不从 Runtime 内部状态读取临时数据。

### 7.7 E2E_FULL_FLOW

状态：已自动化。

目标：覆盖真实用户流程的完整闭环。

固定命令：

```bash
scripts/verify-full-flow.sh
```

固定流程：

1. 启动测试内 Plugin HTTP / Problem Board harness。
2. Plugin 写入 `.pickles/server.json`。
3. 模拟 Codex `SessionStart`。
4. 模拟一次文件修改。
5. Hook 在 `PostToolUse` 上报 before / after。
6. Plugin 接收 `/notify`。
7. Runtime 执行 sample project native rule。
8. Runtime 生成 Problem。
9. Plugin 更新 Problem Board。
10. Hook 在 `Stop` 调用 `/feedback`。
11. feedback 返回 Problem summary。

固定断言：

- `.pickles/server.json` 存在且端口可连接。
- Hook notify 到达 Plugin。
- Runtime 至少生成一个可解释 Problem。
- Feedback 中包含同一 workspace 的 Problem summary。
- Problem Board service 中存在同一 Problem。

防漂移点：

- 全流程必须体现 Hook、Plugin、Runtime 三者边界。
- 任一模块不得越界替代其他模块职责。
- 该自动化不启动真实 IntelliJ UI；真实 IDE 启动由 Plugin 验证入口覆盖。

## 8. Key Flows

### 8.1 多人开发必测流

1. 修改 HTTP contract 或 Plugin server：必须运行 `PLUGIN_HTTP_CONTRACT_UNIT`，必要时运行 `PLUGIN_HTTP_CONTRACT_E2E`。
2. 修改 Hook 脚本：必须运行 `HOOK_PLUGIN_CONTRACT`。
3. 修改 Runtime 规则执行或 Problem 聚合：必须运行 `RUNTIME_SAMPLE_PROJECT`。
4. 修改 Plugin 到 Runtime 编排：必须运行 `PLUGIN_RUNTIME_FLOW`。
5. 修改跨模块闭环：必须运行 `E2E_FULL_FLOW`。
6. 修改 e2e sample project：必须运行 `E2E_SAMPLE_PROJECT_VERIFY`。

### 8.2 PR 必测流

当前 PR 必测固定为：

1. `scripts/verify-intellij-plugin.sh`
2. `scripts/verify-sample-project.sh`
3. `scripts/verify-hooks.sh`
4. `scripts/verify-runtime-sample-project.sh`
5. `scripts/verify-full-flow.sh`
6. `scripts/verify-all.sh`

完整验证入口固定为 `scripts/verify-all.sh`。

## 9. Non-Functional Requirements

- testcase 名称必须稳定，便于 PR、TODO 和 commit 引用。
- testcase 必须失败即定位到模块边界。
- 手动 e2e testcase 必须记录实际命令和断言。
- 自动化 e2e testcase 必须接入统一 verify 入口。
- 当前未实现 testcase 不得作为 PR 必过项。
- 已实现 testcase 若失败，不得通过降低断言规避失败。

## 10. Open Items

无
