# Problem Model Contract

## 1. Purpose

本文档定义 Pickles MVP 的问题模型契约。

目标是让 Problem Board UI、Governance Server 和 Codex Hook 使用同一套问题字段。

## 2. Scope

当前范围：

- Problem 最小字段
- severity 规则
- UI 展示字段
- 文件跳转字段

不在范围内：

- 不定义问题持久化格式
- 不定义问题去重算法
- 不定义完整 Repair-Oriented Summary 结构

## 3. Bounded Context

Problem 是规则检测工具返回结果经过 Pickles 聚合后的最小展示单元。

Problem Board 按 workspace 聚合。MVP 不按 task 单独保存 Problem Board。

## 4. Module Mapping

- `pickles-runtime/`: 生成和聚合 Problem。
- `pickles-intellij-plugin/`: 展示 Problem Board，处理删除和文件跳转。
- `pickles-hooks/`: 在任务完成前读取治理反馈。

## 5. Core Objects

### 5.1 Problem

固定字段：

- `title`
- `type`
- `message`
- `severity`
- `source`
- `file`
- `position`

### 5.2 Position

固定字段：

- `line`
- `column`

### 5.3 Source

固定字段：

- `tool`
- `rule`

`tool` 固定为 string。

`rule` 固定为 string 或 `null`。

## 6. Global Constraints

- `severity` 固定使用规则工具返回的级别。
- MVP 只使用 `ERROR` 和 `WARN`。
- Pickles 不在 MVP 中新增独立 severity 体系。
- `source` 固定使用 object，不得使用未结构化 string。
- UI 只显示 `title`、`type`、`message`。
- 文件跳转依赖 `file` 和 `position`。

## 7. Functional Requirements

### 7.1 Problem Creation

Governance Server 调用 ArchUnit 与 ESLint 命令后，将工具输出转换为 Problem。

转换后必须保留来源工具信息。

### 7.2 Problem Board Display

Problem Board 是嵌入 IntelliJ IDEA 的工具窗口。

MVP 只显示以下字段：

- `title`
- `type`
- `message`

### 7.3 Problem Delete

每个问题项提供删除按钮。

删除只影响当前 workspace Problem Board 展示，不修改用户业务代码。

### 7.4 File Navigation

点击问题项时，Plugin 跳转到对应文件位置。

跳转依赖 `file` 和 `position` 字段。

## 8. Key Flows

### 8.1 Display Flow

1. Governance Server 生成 Problem。
2. IntelliJ Plugin 刷新 Problem Board。
3. UI 展示 `title`、`type`、`message`。

### 8.2 Navigation Flow

1. 用户点击 Problem Board 中的问题项。
2. Plugin 根据 `file` 和 `position` 定位目标文件。
3. IDEA 打开文件并跳转到对应位置。

## 9. Non-Functional Requirements

- Problem 字段必须适合 JSON 序列化。
- UI 删除问题不得删除底层规则结果来源。
- 文件不存在时，Plugin 必须展示可理解的跳转失败状态。

## 10. Open Items

- `type` 的固定枚举值。
- position 是否允许为空。
- 问题去重规则。
