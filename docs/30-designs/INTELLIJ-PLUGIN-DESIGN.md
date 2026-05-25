# IntelliJ Plugin Design

## 1. Purpose

本文档定义 `pickles-intellij-plugin/` 的 MVP 设计。

目标是明确 IntelliJ Plugin 是 Pickles 的主要产品入口，并承载 Problem Board UI 与 IDE 侧编排。

## 2. Scope

当前范围：

- Problem Board Tool Window
- Pickles runtime config 配置展示和修改
- AGENTS.md Bind / Unbind
- 本地 HTTP 服务管理
- 文件跳转

不在范围内：

- 不执行 native rules
- 不实现 Codex Hook

## 3. Bounded Context

Plugin 运行在 IntelliJ IDEA 中。Plugin 负责接收 Codex Hook 通知、管理 Runtime 子进程、展示 Problem Board，并管理治理绑定。

Plugin 固定作为 Runtime lifecycle owner。

Plugin 与 Runtime 固定通过 stdio JSON request / response 通信。

Runtime MVP 不暴露 HTTP server。

Plugin 实现语言固定为 Kotlin。

Plugin 不修改业务代码、测试代码或工程实现代码。Plugin 可以管理治理文件。

## 4. Module Mapping

- `pickles-intellij-plugin/`: Plugin UI、Tool Window、配置界面、本地 HTTP 服务和 Runtime 子进程管理。
- `pickles-runtime/`: Node.js Runtime 子进程。
- `pickles-hooks/`: Codex Hook。

## 5. Core Objects

- Problem Board Tool Window
- Bind Button
- Unbind Button
- Local HTTP Server
- Project Configuration View

## 6. Global Constraints

- Problem Board 是嵌入 IntelliJ IDEA 的工具窗口。
- Problem Board 默认放在右侧 Tool Window 区域，底部区域优先留给 Terminal / Codex 交互。
- 用户可以使用 IntelliJ IDEA Tool Window 的内置移动能力调整 Problem Board 位置。
- MVP UI 只显示 `title`、`type`、`message`。
- 每个问题项提供删除按钮。
- 点击问题项跳转到对应文件位置。
- Plugin 配置界面只展示和修改 Pickles runtime config。
- Plugin 不拥有独立配置真相。

## 7. Functional Requirements

### 7.1 Problem Board

Problem Board 固定显示 workspace 当前问题。

每个问题项固定展示：

- `title`
- `type`
- `message`

### 7.2 Problem Delete

用户点击删除按钮后，该问题从当前 Problem Board 展示中移除。

删除不修改用户业务代码。

### 7.3 File Navigation

用户点击问题项后，Plugin 根据 Problem 的 `file` 和 `position` 跳转到对应文件位置。

### 7.4 Bind / Unbind

Plugin 检测当前目标工程是否已经绑定 Pickles 治理约束。

Bind 检测同时覆盖目标工程 `<repo>/.codex/hooks.json`。

Plugin 不读取、不修改、不依赖用户全局 `~/.codex` 目录。

- 未绑定时显示绑定按钮。
- 已绑定时显示解除绑定按钮。

AGENTS.md 注入块格式、marker 和幂等更新细节在 MVP 暂不定义。

### 7.5 Local HTTP Server

Plugin 启动本地 HTTP 服务接收 Codex Hook 通知。

Plugin 将本地 HTTP 服务端口写入目标工程 `<repo>/.pickles/server.json`。

HTTP endpoint 与 request / response schema 固定由 [`../20-interfaces/HOOK-PLUGIN-HTTP-CONTRACT.md`](../20-interfaces/HOOK-PLUGIN-HTTP-CONTRACT.md) 定义。

### 7.6 Runtime Process

Plugin 启动独立 Node.js Runtime 子进程。

Runtime 子进程生命周期跟随目标工程。

Plugin 通过 stdio JSON request / response 调用 Runtime。

Runtime stdout 固定用于 JSON response。

Runtime stderr 固定用于日志。

Runtime 子进程异常退出时，Plugin 必须展示可理解错误，并可以尝试重启。

## 8. Key Flows

### 8.1 Problem Display Flow

1. Plugin 接收 Hook 通知。
2. Plugin 通过 stdio 调用 Runtime 子进程。
3. Plugin 接收 Problem Board 数据。
4. Tool Window 刷新问题列表。

### 8.2 Config Flow

1. Plugin 定位目标工程根目录。
2. Plugin 读取 Pickles runtime config。
3. 用户在配置界面修改配置。
4. Plugin 写回 Pickles runtime config。

## 9. Non-Functional Requirements

- Plugin UI 操作不得阻塞 IDEA UI 线程。
- 本地 HTTP 服务生命周期跟随目标工程。
- 本地 HTTP 服务端口固定写入目标工程 `<repo>/.pickles/server.json`。
- 文件跳转失败时必须展示可理解错误。

## 10. Open Items

- Tool Window 具体布局。
- AGENTS.md 注入块格式、marker 和幂等更新细节。
