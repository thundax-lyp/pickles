# MVP Implementation Design

## 1. Purpose

本文档定义 Pickles MVP 的实现闭环。

目标是把当前 MVP 从人类阅读材料收敛为可执行的工程设计入口。

## 2. Scope

当前范围：

- MVP 模块职责
- 实现顺序
- e2e sample project
- 暂不实现范围

不在范围内：

- 不定义 AGENTS.md 注入块格式
- 不定义 MCP 实现

## 3. Bounded Context

MVP 固定运行在 IntelliJ IDEA + Codex 环境中。

Pickles 检测对象是用户项目，也就是 Agent 工作目录下的项目。Pickles 仓库自身的 e2e 目标工程固定为 `e2e/sample-project/`。

## 4. Module Mapping

- `pickles-intellij-plugin/`: IntelliJ Plugin、Problem Board UI、Bind / Unbind、HTTP server 管理。
- `pickles-runtime/`: Governance Server、Incremental Workspace Index、native rule execution、问题聚合。
- `pickles-hooks/`: Codex Hook、文件变动捕获、本地 HTTP 通知。
- `pickles-rules/`: Pickles native rule authoring contract、规则说明书和 rule templates。
- `pickles-mcp/`: MVP 暂缓。
- `e2e/sample-project/`: 被治理目标工程样例。

## 5. Core Objects

- Pickles runtime config
- Hook event
- Changed file
- Incremental Workspace Index
- Problem
- Problem Board

## 6. Global Constraints

- Plugin 不修改业务代码、测试代码或工程实现代码。
- IntelliJ Plugin 实现语言固定为 Kotlin。
- Pickles 可以管理治理文件，例如注入或更新 `AGENTS.md` 中的治理约束。
- Pickles runtime config 固定放在目标工程根目录。
- Hook 通知协议在 MVP 固定为本地 HTTP。
- 本地 HTTP 端口发现固定使用目标工程 `<repo>/.pickles/server.json`。
- Hook 事件在 MVP 固定为 `SessionStart`、`PreToolUse`、`PostToolUse` 和 `Stop`。
- Hook event 幂等固定绑定 Codex `session_id` 与 `turn_id`。
- Rule detection 在 MVP 固定为 Pickles native rules。
- Problem Board 按 workspace 聚合。
- Hook HTTP endpoint 与 request / response schema 固定由 [`../20-interfaces/HOOK-PLUGIN-HTTP-CONTRACT.md`](../20-interfaces/HOOK-PLUGIN-HTTP-CONTRACT.md) 定义。

## 7. Functional Requirements

### 7.1 MVP Closure

MVP 最小闭环固定为：

1. Plugin 打开目标工程。
2. Plugin 读取目标工程 Pickles runtime config。
3. Plugin 启动本地 HTTP 服务并写入 `<repo>/.pickles/server.json`。
4. Codex Hook 在 `SessionStart` 读取 `.pickles/server.json` 并检查本地 Plugin 可用性。
5. Codex Hook 在 `PreToolUse` 提取候选文件并读取 before 内容。
6. Codex Hook 在 `PostToolUse` 通过 workspace diff 或文件状态扫描确认实际变动文件。
7. Codex Hook 读取实际变动文件 after 内容。
8. Hook 通过本地 HTTP 通知 Plugin。
9. Plugin 将变动集交给 Governance Server。
10. Governance Server 更新 Incremental Workspace Index。
11. Governance Server 执行 Pickles runtime config 中的 native rules。
12. Governance Server 聚合 Problem。
13. Problem Board 展示 `title`、`type`、`message`。
14. Hook 在 `Stop` flush pending diff 并请求治理反馈。

### 7.2 Implementation Order

固定实现顺序：

1. `e2e/sample-project/` 最小目标工程。
2. Pickles runtime config 读取与校验。
3. Problem model。
4. Governance Server native rule execution 与问题聚合。
5. IntelliJ Problem Board UI。
6. Plugin 本地 HTTP 服务。
7. Codex Hook 通知。
8. AGENTS.md Bind / Unbind。

### 7.3 Deferred Capabilities

MVP 暂不实现：

- MCP Hook 通知协议。
- WebSocket Hook 通知协议。
- Cloud Runtime。
- Auto Repair。
- Full semantic persistence。
- Multi-Agent Orchestration。

## 8. Key Flows

### 8.1 Workspace Problem Flow

1. Hook 在 `PreToolUse` 提取候选文件并读取 before 内容。
2. Hook 在 `PostToolUse` 确认实际变动文件并读取 after 内容。
3. Hook 提交包含 before / after 的文件变动。
4. Runtime 更新 workspace index。
5. Runtime 执行 native rules。
6. Runtime 聚合 Problem。
7. Plugin 刷新 Problem Board。

### 8.2 Bind Flow

1. Plugin 检测目标工程是否已绑定 Pickles。
2. Plugin 同时检查目标工程 `<repo>/.codex/hooks.json`。
3. Plugin 不检查、不写入用户全局 `~/.codex`。
4. 未绑定时显示绑定按钮。
5. 已绑定时显示解除绑定按钮。
6. 具体注入块格式在 MVP 暂不定义。

## 9. Non-Functional Requirements

- MVP 优先保证单 workspace 闭环可运行。
- 失败状态必须能在 Plugin UI 或 Codex 输出中被看见。
- Runtime 不保存全量语义持久化数据。
- 本地 HTTP 端口固定写入目标工程 `<repo>/.pickles/server.json`。

## 10. Open Items

- AGENTS.md 注入块格式、marker 和幂等更新细节。
