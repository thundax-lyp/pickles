# IntelliJ Plugin Requirements

## 1. Purpose

本文档定义 Pickles IntelliJ Plugin 的 MVP 需求。

目标是明确插件必须提供的用户可见能力，以及插件与 Governance Engine、Codex Hook、目标工程配置之间的边界。

## 2. Scope

当前范围：

- IntelliJ IDEA 插件入口
- Problem Board Tool Window
- Pickles runtime config 展示和修改
- AGENTS.md Bind / Unbind
- 本地 HTTP 服务管理
- 文件跳转

不在范围内：

- 不执行 Pickles native rules、ArchUnit、ESLint 或用户业务命令
- 不实现 Codex Hook

## 3. Bounded Context

IntelliJ Plugin 是 Pickles MVP 的主要产品入口。

插件运行在 IntelliJ IDEA 中，目标工程是 IntelliJ IDEA 当前打开、且 Codex Agent 正在工作的用户项目。

插件实现语言固定为 Kotlin。

插件不修改业务代码、测试代码或工程实现代码。插件可以管理治理文件，例如 Pickles runtime config 和 `AGENTS.md`。

## 4. Module Mapping

- `pickles-intellij-plugin/`: 插件实现、Problem Board UI、本地 HTTP 服务管理、配置界面、Bind / Unbind。
- `pickles-runtime/`: Governance Engine，负责检测执行和问题聚合。
- `pickles-hooks/`: Codex Hook，负责向插件发送变动通知。
- `docs/20-interfaces/`: 插件依赖的配置、HTTP、Problem 模型契约。

## 5. Core Objects

- Problem Board
- Problem item
- Project configuration view
- Bind button
- Unbind button
- Local HTTP server

## 6. Global Constraints

- 插件固定运行在 IntelliJ IDEA。
- 插件必须读取目标工程 Pickles runtime config。
- 插件配置界面只展示和修改 Pickles runtime config，不拥有独立配置真相。
- 插件负责启动本地 HTTP 服务。
- 插件负责接收 Codex Hook 的增量文件变动通知。
- 插件负责调用 Governance Engine。
- 插件负责启动并管理独立 Node.js Runtime 子进程。
- 插件与 Runtime 固定通过 stdio JSON request / response 通信。
- Runtime MVP 不暴露 HTTP server。
- Codex Hook 不直接调用 Runtime。
- 插件不得执行 native rule；native rule 由 Runtime 执行。

## 7. Functional Requirements

### 7.1 Problem Board

插件必须提供嵌入 IntelliJ IDEA 的 Problem Board Tool Window。

Tool Window MVP 固定采用：

- Status Header
- Problems tab
- Config tab

Problems tab 固定为默认 tab。

Status Header 必须展示：

- HTTP server 状态
- Runtime 状态
- Index 状态
- Problem summary

Status Header 必须提供：

- Refresh
- Reindex
- Bind / Unbind

Problem row 必须展示：

- `severity`
- `title`
- `type`
- `message`
- `file` / `position`
- `source.rule`

Problem row 排序固定为：

1. `ERROR`
2. `WARN`
3. 有 `file` / `position` 的问题
4. Runtime 返回顺序

每个问题项必须提供删除按钮。

点击问题项时，插件必须跳转到对应文件位置。

Index 正在运行时，Problem Board 必须展示当前结果可能不完整的状态。

### 7.2 Project Configuration

插件必须读取目标工程 Pickles runtime config。

插件配置界面必须能展示 Pickles runtime config 当前配置。

插件配置界面必须能写回 Pickles runtime config。

插件不通过 IDEA 自动识别 ArchUnit、ESLint 或用户业务命令。

插件配置界面必须能展示和写回 Pickles runtime config。

### 7.3 Bind / Unbind

插件必须检测当前目标工程是否已绑定 Pickles 治理约束。

插件在 Bind 时必须管理目标工程 `AGENTS.md` 中的单个 Pickles 注入块。

Pickles 注入块 marker 固定为：

```md
<!-- PICKLES:BEGIN -->
<!-- PICKLES:END -->
```

Pickles 注入块只承载 Agent 行为提示，不承载规则真相。

规则真相固定保留在 Pickles runtime config。

Bind 规则固定为：

- `AGENTS.md` 不存在时创建文件。
- marker 不存在时，在文件末尾追加 Pickles 注入块。
- 存在一组完整 marker 时，替换 marker 内内容。
- marker 外内容永不修改。

Unbind 规则固定为：

- 只删除 Pickles marker 包围的注入块。
- 不删除 `AGENTS.md` 文件本身。
- marker 不存在时视为已解绑。
- marker 不成对、重复或嵌套时不得自动修复，必须展示可理解冲突状态。

插件在 Bind 检测时必须同时检查目标工程 `<repo>/.codex/hooks.json`。

插件不得读取、修改或依赖用户全局 `~/.codex` 目录。

- 未绑定时显示绑定按钮。
- 已绑定时显示解除绑定按钮。

### 7.4 Local HTTP Server

插件必须启动本地 HTTP 服务接收 Codex Hook 通知。

本地 HTTP 服务生命周期必须跟随目标工程。

插件必须把本地 HTTP 服务端口写入目标工程 `<repo>/.pickles/server.json`。

`server.json` 是运行时状态文件。

HTTP endpoint 与 request / response schema 固定由 [`../20-interfaces/HOOK-PLUGIN-HTTP-CONTRACT.md`](../20-interfaces/HOOK-PLUGIN-HTTP-CONTRACT.md) 定义。

### 7.5 Runtime Lifecycle

插件必须启动并管理独立 Node.js Runtime 子进程。

插件必须在目标工程打开、HTTP server 启动且 Runtime 子进程可用后，后台触发 Runtime 首次 workspace 全量索引。

首次 workspace 全量索引不得阻塞 IDE UI。

Hook `SessionStart` 不触发 Runtime 全量索引。

插件必须提供手动 Refresh / Reindex 入口，允许用户重新触发 workspace 全量索引。

## 8. Key Flows

### 8.1 Problem Display Flow

1. 插件收到 Hook 通知。
2. 插件调用 Runtime。
3. Runtime 返回 Problem Board 数据。
4. 插件刷新 Tool Window。

### 8.2 Bind Flow

1. 插件检测目标工程绑定状态。
2. 未绑定时展示绑定按钮。
3. 用户点击绑定按钮。
4. 插件检查并管理目标工程内的治理文件。
5. 插件检查并管理目标工程 `<repo>/.codex/hooks.json`。
6. 插件刷新绑定状态。

## 9. Non-Functional Requirements

- 插件 UI 操作不得阻塞 IDEA UI 线程。
- 文件跳转失败时必须展示可理解错误。
- 配置读取失败时必须展示可理解错误。
- 本地 HTTP 端口固定写入目标工程 `<repo>/.pickles/server.json`。

## 10. Open Items

无
