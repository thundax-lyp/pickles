# Problem Model Contract

## 1. Purpose

本文档定义 Pickles MVP 的问题模型契约。

目标是让 Problem Board UI、Runtime 和 Codex Hook 使用同一套问题字段。

## 2. Scope

当前范围：

- Problem 最小字段
- Problem type 枚举
- severity 规则
- UI 展示字段
- 文件跳转字段

不在范围内：

- 不定义问题持久化格式
- 不定义完整 Repair-Oriented Summary 结构

## 3. Bounded Context

Problem 是 Pickles native rule、parser diagnostic 或 external adapter 结果经过 Runtime 聚合后的最小展示单元。

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
- `fixHint`

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

### 5.4 ProblemType

固定枚举值：

- `architecture`
- `style`
- `security`
- `maintainability`
- `parser`
- `adapter`

## 6. Global Constraints

- `severity` 固定使用规则工具返回的级别。
- MVP 只使用 `ERROR` 和 `WARN`。
- Pickles 不在 MVP 中新增独立 severity 体系。
- `type` 固定使用 `ProblemType` 枚举。
- `source` 固定使用 object，不得使用未结构化 string。
- `file` 固定为 string 或 `null`。
- `position` 固定为 `Position` 或 `null`。
- `fixHint` 固定为 string 或 `null`。
- UI 只显示 `title`、`type`、`message`。
- 文件跳转只在 `file` 和 `position` 同时存在时启用。

## 7. Functional Requirements

### 7.1 Native Rule Problem Creation

Runtime 执行 Pickles native rule 后，必须将 `ProblemInput` 归一化为 Problem。

Rule-level `id`、`title`、`type` 和 `severity` 可以作为 `ProblemInput` 的默认值。

Rule-level `fixHint` 可以作为 `ProblemInput.fixHint` 的默认值。

Rule author 调用 `ctx.problem(input)` 时，`input.message` 固定必填。

`ProblemInput` 可以覆盖 `title`、`message`、`file`、`position` 和 `fixHint`。

`ProblemInput` 不得覆盖 `severity`。

`ProblemInput` 不得覆盖 `source`。

Runtime 必须自动补齐 `source.tool` 和 `source.rule`。

Native rule Problem 的 `source.tool` 固定为 `pickles-native`。

### 7.2 Parser Diagnostic Problem Creation

Runtime 生成 parser diagnostic 后，必须将 parser diagnostic 归一化为 Problem。

转换后必须保留来源工具信息。

### 7.3 Problem Board Display

Problem Board 是嵌入 IntelliJ IDEA 的工具窗口。

MVP 只显示以下字段：

- `title`
- `type`
- `message`

### 7.4 Problem Delete

每个问题项提供删除按钮。

删除只影响当前 workspace Problem Board 展示，不修改用户业务代码。

### 7.5 File Navigation

点击问题项时，Plugin 跳转到对应文件位置。

`file` 和 `position` 同时存在时，Plugin 必须支持跳转。

`file` 或 `position` 为 `null` 时，Plugin 不展示跳转失败状态。

### 7.6 Problem Deduplication

Runtime 聚合 Problem 时必须去重。

MVP dedupe key 固定由以下字段组成：

- `source.tool`
- `source.rule`
- `file`
- `position.line`
- `position.column`
- `message`

`file` 为 `null` 时，dedupe key 中使用空值。

`position` 为 `null` 时，`position.line` 和 `position.column` 使用空值。

MVP 只删除 dedupe key 完全相同的重复 Problem。

MVP 不做跨 rule 合并。

MVP 不做跨 tool 合并。

MVP 不做相似 message 合并。

`title` 不参与去重。

`fixHint` 不参与去重。

重复 Problem 固定保留第一次出现的 Problem。

## 8. Key Flows

### 8.1 Display Flow

1. Runtime 生成 Problem。
2. IntelliJ Plugin 刷新 Problem Board。
3. UI 展示 `title`、`type`、`message`。

### 8.2 Navigation Flow

1. 用户点击 Problem Board 中的问题项。
2. Plugin 根据 `file` 和 `position` 定位目标文件。
3. IDEA 打开文件并跳转到对应位置。

## 9. Non-Functional Requirements

- Problem 字段必须适合 JSON 序列化。
- UI 删除问题不得删除底层规则结果来源。
- 文件不存在且跳转字段存在时，Plugin 必须展示可理解的跳转失败状态。

## 10. Open Items

无
