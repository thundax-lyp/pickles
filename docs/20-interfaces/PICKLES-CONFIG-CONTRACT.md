# Pickles Config Contract

## 1. Purpose

本文档定义目标工程根目录 `.pickles.json` 的 MVP 配置契约。

目标是让 IntelliJ Plugin、Codex Hook 和 Governance Server 使用同一个项目级配置真相源。

## 2. Scope

当前范围：

- `.pickles.json` 文件位置
- MVP 固定字段
- 字段读写归属
- 运行时信息边界

不在范围内：

- 不定义 Plugin UI 的具体交互控件
- 不定义 `.pickles/server.json` 的完整 schema
- 不定义 AGENTS.md 注入块格式

## 3. Bounded Context

`.pickles.json` 固定放在被治理的目标工程根目录。

目标工程指 IntelliJ IDEA 当前打开、且 Codex Agent 正在工作的用户项目。Pickles 插件仓库根目录不放置 `.pickles.json`。Pickles 仓库的 e2e 示例目标工程固定为 `e2e/sample-project/`。

`.pickles.json` 是配置真相源。IntelliJ Plugin、Codex Hook 和 Governance Server 都读取该文件。Plugin 配置界面只展示和修改该文件，不拥有独立配置真相。

## 4. Module Mapping

- `pickles-intellij-plugin/`: 读取、展示、更新 `.pickles.json`。
- `pickles-hooks/`: 读取 `hook.protocol` 和目标工程配置。
- `pickles-runtime/`: 读取 `rules` 和 `problemBoard` 配置并执行检测。
- `e2e/sample-project/`: 保存示例 `.pickles.json`。

## 5. Core Objects

### 5.1 PicklesConfig

固定字段：

- `version`
- `agent`
- `bind`
- `hook`
- `rules`
- `problemBoard`

### 5.2 BindConfig

固定字段：

- `agentsFile`
- `enabled`

### 5.3 HookConfig

固定字段：

- `protocol`

### 5.4 RulesConfig

固定字段：

- `archunit`
- `eslint`
- `scripts`

### 5.5 RuleCommandConfig

固定字段：

- `enabled`
- `command`

### 5.6 ProblemBoardConfig

固定字段：

- `aggregation`

## 6. Global Constraints

- `.pickles.json` 必须是合法 JSON。
- `version` 固定为 `1`。
- `agent` 固定为 `codex`。
- `hook.protocol` 在 MVP 固定为 `http`。
- `problemBoard.aggregation` 在 MVP 固定为 `workspace`。
- 运行时端口、进程号和 server URL 不写入 `.pickles.json`。
- ArchUnit 与 ESLint 的执行命令固定从 `.pickles.json` 读取。
- 命令为空字符串表示尚未配置，不表示禁用。

## 7. Functional Requirements

### 7.1 Minimal Config

MVP 最小配置固定为：

```json
{
  "version": 1,
  "agent": "codex",
  "bind": {
    "agentsFile": "AGENTS.md",
    "enabled": false
  },
  "hook": {
    "protocol": "http"
  },
  "rules": {
    "archunit": {
      "enabled": true,
      "command": ""
    },
    "eslint": {
      "enabled": true,
      "command": ""
    },
    "scripts": []
  },
  "problemBoard": {
    "aggregation": "workspace"
  }
}
```

### 7.2 Rule Command Sync

IntelliJ Plugin 通过 IDEA 获取用户工程使用的 ArchUnit 与 ESLint 命令，并同步到目标工程 `.pickles.json`。

Governance Server 直接调用 `.pickles.json` 中配置的用户工程命令执行检测。

### 7.3 Bind Config

`bind.agentsFile` 固定指向目标工程内的 AGENTS 文件路径。

`bind.enabled` 表达当前工程是否已经绑定 Pickles 治理约束。

Bind 状态检测必须同时检查目标工程 `<repo>/.codex/hooks.json`。

Pickles 配置与 Bind 状态不得读取、写入或依赖用户全局 `~/.codex`。

## 8. Key Flows

### 8.1 Config Read Flow

1. IntelliJ Plugin 定位目标工程根目录。
2. IntelliJ Plugin 读取 `.pickles.json`。
3. Governance Server 读取同一份 `.pickles.json`。
4. Codex Hook 读取同一份 `.pickles.json`。

### 8.2 Config Update Flow

1. Plugin 配置界面展示当前 `.pickles.json`。
2. 用户修改配置。
3. Plugin 写回 `.pickles.json`。
4. Runtime 后续检测使用更新后的配置。

## 9. Non-Functional Requirements

- 配置文件必须可提交到用户工程仓库。
- 配置文件不得包含本机端口、绝对临时路径、进程号或 token。
- 本地 HTTP 端口固定写入目标工程 `<repo>/.pickles/server.json`，不得写入 `.pickles.json`。
- 配置读取失败时，Plugin 必须在 Problem Board 或配置界面展示可理解错误。

## 10. Open Items

- Plugin 如何通过 IDEA 精确识别 ArchUnit 命令。
- Plugin 如何通过 IDEA 精确识别 ESLint 命令。
- `scripts` 指令结构。
