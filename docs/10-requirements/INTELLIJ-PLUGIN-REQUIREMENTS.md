# IntelliJ Plugin Requirements

## 1. Purpose

本文档定义 Pickles IntelliJ Plugin 的 MVP 需求。

目标是明确插件必须提供的用户可见能力，以及插件与 Governance Engine、Codex Hook、目标工程配置之间的边界。

## 2. Scope

当前范围：

- IntelliJ IDEA 插件入口
- Problem Board Tool Window
- `.pickles.json` 配置展示和修改
- AGENTS.md Bind / Unbind
- 本地 HTTP 服务管理
- 文件跳转

不在范围内：

- 不执行 ArchUnit 或 ESLint 命令
- 不实现 Codex Hook
- 不定义 Hook HTTP API 细节
- 不定义 AGENTS.md 注入块格式、marker 和幂等更新细节

## 3. Bounded Context

IntelliJ Plugin 是 Pickles MVP 的主要产品入口。

插件运行在 IntelliJ IDEA 中，目标工程是 IntelliJ IDEA 当前打开、且 Codex Agent 正在工作的用户项目。

插件不修改业务代码、测试代码或工程实现代码。插件可以管理治理文件，例如 `.pickles.json` 和 `AGENTS.md`。

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
- 插件必须读取目标工程根目录 `.pickles.json`。
- 插件配置界面只展示和修改 `.pickles.json`，不拥有独立配置真相。
- 插件负责启动本地 HTTP 服务。
- 插件负责接收 Codex Hook 的增量文件变动通知。
- 插件负责调用 Governance Engine。
- 插件不得执行规则命令；规则命令由 Governance Engine 执行。

## 7. Functional Requirements

### 7.1 Problem Board

插件必须提供嵌入 IntelliJ IDEA 的 Problem Board Tool Window。

MVP 只显示以下字段：

- `title`
- `type`
- `message`

每个问题项必须提供删除按钮。

点击问题项时，插件必须跳转到对应文件位置。

### 7.2 Project Configuration

插件必须读取目标工程根目录 `.pickles.json`。

插件配置界面必须能展示 `.pickles.json` 当前配置。

插件配置界面必须能写回 `.pickles.json`。

插件必须能把 IDEA 识别到的 ArchUnit 与 ESLint 命令同步到 `.pickles.json`。

### 7.3 Bind / Unbind

插件必须检测当前目标工程是否已绑定 Pickles 治理约束。

插件在 Bind 检测时必须同时检查目标工程 `<repo>/.codex/hooks.json`。

插件不得读取、修改或依赖用户全局 `~/.codex` 目录。

- 未绑定时显示绑定按钮。
- 已绑定时显示解除绑定按钮。

Bind / Unbind 的具体注入块格式、marker 和幂等更新细节在 MVP 暂不定义。

### 7.4 Local HTTP Server

插件必须启动本地 HTTP 服务接收 Codex Hook 通知。

本地 HTTP 服务生命周期必须跟随目标工程。

HTTP endpoint 细节在 MVP 暂不定义。

## 8. Key Flows

### 8.1 Problem Display Flow

1. 插件收到 Hook 通知。
2. 插件调用 Governance Engine。
3. Governance Engine 返回 Problem Board 数据。
4. 插件刷新 Tool Window。

### 8.2 Bind Flow

1. 插件检测目标工程绑定状态。
2. 未绑定时展示绑定按钮。
3. 用户点击绑定按钮。
4. 插件检查并管理目标工程内的治理文件。
5. 插件检查并管理目标工程 `<repo>/.codex/hooks.json`。
6. 插件更新 `.pickles.json` 绑定状态。

## 9. Non-Functional Requirements

- 插件 UI 操作不得阻塞 IDEA UI 线程。
- 文件跳转失败时必须展示可理解错误。
- 配置读取失败时必须展示可理解错误。
- 本地 HTTP 端口不得写入 `.pickles.json`。

## 10. Open Items

- 插件实现语言。
- Tool Window 具体布局。
- 本地 HTTP 服务端口发现机制。
- AGENTS.md 注入块格式、marker 和幂等更新细节。
