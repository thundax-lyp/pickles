# IntelliJ Plugin Design

## 1. Purpose

本文档定义 `pickles-intellij-plugin/` 的 MVP 设计。

目标是明确 IntelliJ Plugin 是 Pickles 的主要产品入口，并承载 Problem Board UI 与 IDE 侧编排。

## 2. Scope

当前范围：

- Problem Board Tool Window
- `.pickles.json` 配置展示和修改
- AGENTS.md Bind / Unbind
- 本地 HTTP 服务管理
- 文件跳转

不在范围内：

- 不执行规则命令
- 不实现 Codex Hook
- 不定义 HTTP API 细节

## 3. Bounded Context

Plugin 运行在 IntelliJ IDEA 中。Plugin 负责接收 Codex Hook 通知、调用 Governance Server、展示 Problem Board，并管理治理绑定。

Plugin 不修改业务代码、测试代码或工程实现代码。Plugin 可以管理治理文件。

## 4. Module Mapping

- `pickles-intellij-plugin/`: Plugin UI、Tool Window、配置界面、本地 HTTP 服务。
- `pickles-runtime/`: Governance Server。
- `pickles-hooks/`: Codex Hook。

## 5. Core Objects

- Problem Board Tool Window
- Bind Button
- Unbind Button
- Local HTTP Server
- Project Configuration View

## 6. Global Constraints

- Problem Board 是嵌入 IntelliJ IDEA 的工具窗口。
- MVP UI 只显示 `title`、`type`、`message`。
- 每个问题项提供删除按钮。
- 点击问题项跳转到对应文件位置。
- Plugin 配置界面只展示和修改 `.pickles.json`。
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

HTTP endpoint 细节在 MVP 暂不定义。

## 8. Key Flows

### 8.1 Problem Display Flow

1. Plugin 接收 Hook 通知。
2. Plugin 调用 Governance Server。
3. Plugin 接收 Problem Board 数据。
4. Tool Window 刷新问题列表。

### 8.2 Config Flow

1. Plugin 定位目标工程根目录。
2. Plugin 读取 `.pickles.json`。
3. 用户在配置界面修改配置。
4. Plugin 写回 `.pickles.json`。

## 9. Non-Functional Requirements

- Plugin UI 操作不得阻塞 IDEA UI 线程。
- 本地 HTTP 服务生命周期跟随目标工程。
- 文件跳转失败时必须展示可理解错误。

## 10. Open Items

- Plugin 实现语言。
- Tool Window 具体布局。
- 本地 HTTP 服务端口发现机制。
- AGENTS.md 注入块格式、marker 和幂等更新细节。
