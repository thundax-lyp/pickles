# IntelliJ Reindex Problem Board Runbook

## 1. Purpose

本文档定义 `feat/intellij-reindex-problem-board` 分支的一次性执行手册。

目标是把 IntelliJ Plugin 从 Hook 驱动的 Problem Board 更新推进到 IDE 内可主动触发的 Runtime 检查闭环。

## 2. Scope

当前范围：

- IntelliJ Plugin 首次 workspace 检查
- 手动 `Reindex` 入口
- Problem Board Header 状态展示
- `Problems` / `Config` tab 分离
- Plugin 到 Runtime 的 changed files 构造
- 相关 Kotlin 单元测试
- 相关 readiness 文档同步

不在范围内：

- 不新增 Runtime HTTP server
- 不新增 MCP 能力
- 不改 Codex Hook 协议
- 不实现 Java 类型解析、classpath 解析或跨文件 symbol resolution
- 不改变 Pickles runtime config 契约
- 不改变 Problem model 契约

## 3. Bounded Context

IntelliJ Plugin 是本任务的流程所有者。Runtime 仍然是规则执行和 Problem 聚合所有者。

Plugin 负责在 IDE 启动后和用户点击 `Reindex` 时收集 workspace 文件内容，并通过既有 stdio JSON request / response 调用 Runtime。

Runtime 不感知 IDE UI，不保存全量语义持久化数据，不执行 HTTP 服务。

Hook 仍然只负责向 Plugin HTTP server 上报增量文件变动。Hook 不直接调用 Runtime。

## 4. Module Mapping

- `pickles-intellij-plugin/`：实现首次检查、手动 `Reindex`、Problem Board UI 状态和 Plugin 到 Runtime 编排测试。
- `pickles-runtime/`：保持既有 stdio 能力；仅在 Plugin 主动检查暴露出契约缺口时补最小测试或修复。
- `docs/40-readiness/E2E-TEST-CASES.md`：同步新增或调整 Plugin 主动检查验证口径。
- `docs/30-designs/RUNBOOK-INTELLIJ-REINDEX-PROBLEM-BOARD.md`：本任务临时执行手册，任务完成后清理。

## 5. Core Objects

- `PicklesProjectService`
- `PicklesRuntimeClient`
- `NodePicklesRuntimeClient`
- `PicklesProblemBoardState`
- `PicklesToolWindowPanel`
- `RuntimeChangedFile`
- `PicklesProblem`
- `IndexStatus`
- `ProblemSummary`

## 6. Global Constraints

- Plugin 不执行 native rules。
- Plugin 不修改业务代码、测试代码或工程实现代码。
- Plugin 可以读取目标工程文件内容并构造 Runtime changed files。
- Plugin 对 Runtime 的调用固定走 stdio JSON request / response。
- 首次 workspace 检查不得阻塞 IDE UI。
- 手动 `Reindex` 不得阻塞 IDE UI。
- Problem Board 展示状态来自 Plugin service 内部状态，不直接读取 Runtime 内部状态。
- UI 删除问题只影响当前展示，不修改 Runtime、Hook 或目标工程文件。
- changed files 传给 Runtime 时使用 repo-relative path。
- Runtime 返回错误时，Plugin 必须保留可理解状态，不清空已有 Problem Board。

## 7. Execution Order

### 7.1 状态模型收敛

1. 在 `PicklesProjectService` 中增加可测试的状态模型。
2. 状态模型固定表达：
    - HTTP server 状态
    - Runtime 状态
    - Index 状态
    - Problem summary
3. `Problem summary` 固定基于当前 `PicklesProblemBoardState` 计算。
4. 状态更新必须触发既有 listener 刷新 UI。

验收点：

- 单元测试能断言空问题、ERROR、WARN 的 summary。
- Runtime 失败时状态可展示，旧 Problem Board 不被清空。

### 7.2 Runtime 主动检查入口

1. 在 `PicklesProjectService` 中增加主动检查方法。
2. 主动检查方法固定由后台线程执行。
3. 主动检查方法固定构造 workspace Java 文件 changed files。
4. Java 文件来源固定为目标工程内 repo-relative `.java` 文件。
5. 主动检查时每个文件使用：
    - `changeType = "modified"`
    - `before = null`
    - `after = 当前文件内容`
6. deleted 文件不进入主动检查输入。
7. Runtime 成功返回后替换当前 Problem Board。
8. Runtime 失败时保留旧 Problem Board 并更新错误状态。

验收点：

- 测试覆盖主动检查会调用 `PicklesRuntimeClient.inspect`。
- 测试覆盖主动检查会把 Runtime 返回的 Problem 写入 Problem Board。
- 测试覆盖 Runtime 失败时旧 Problem Board 保持不变。

### 7.3 首次 workspace 检查

1. `PicklesStartupActivity` 保持负责启动 Plugin 生命周期。
2. HTTP server 启动后触发首次 workspace 检查。
3. 首次检查必须后台执行。
4. 首次检查重复触发时必须保持幂等，不并发启动多次同类索引。

验收点：

- 测试覆盖 startup 路径或 service 路径会触发首次检查。
- Index 状态能从 idle/running/success/failure 中反映当前结果。

### 7.4 手动 Reindex 入口

1. Tool Window Header 增加 `Reindex` 按钮。
2. `Reindex` 按钮调用主动检查方法。
3. Reindex 运行中按钮必须避免重复触发并发检查。
4. Reindex 完成后刷新 Problem Board。

验收点：

- UI 层或 service 测试覆盖按钮行为对应的 service 方法。
- Reindex 运行中不会并发调用 Runtime。

### 7.5 Problem Board UI 对齐

1. Tool Window 固定拆分为 `Problems` tab 和 `Config` tab。
2. `Problems` tab 固定为默认 tab。
3. Header 固定展示：
    - HTTP server 状态
    - Runtime 状态
    - Index 状态
    - Problem summary
4. Problem row 固定展示：
    - `severity`
    - `title`
    - `type`
    - `message`
    - `file` / `position`
    - `source.rule`
5. Problem row 排序固定为：
    - `ERROR`
    - `WARN`
    - 有 `file` / `position` 的问题
    - Runtime 返回顺序
6. 删除按钮仍只删除当前展示项。
7. 点击问题项仍跳转到对应文件位置。

验收点：

- Kotlin 测试覆盖 Problem 排序。
- Kotlin 测试覆盖删除问题不修改文件。
- 手动打开 IDE 时能看到 tabs、header、problem rows 和 config editor。

### 7.6 文档同步

1. 如果测试入口或验证口径发生变化，同步 `docs/40-readiness/E2E-TEST-CASES.md`。
2. 如果 Plugin 行为口径发生变化，同步 `docs/10-requirements/INTELLIJ-PLUGIN-REQUIREMENTS.md`。
3. 不改变长期规则时，不修改 `docs/00-governance/`。
4. 任务完成后删除本 RUNBOOK。

验收点：

- 文档只记录稳定行为和验证入口。
- 本 RUNBOOK 不在最终收口 commit 中长期保留。

## 8. Key Flows

### 8.1 First Workspace Check Flow

1. 用户打开目标工程。
2. `PicklesStartupActivity` 启动 `PicklesProjectService`。
3. Plugin 启动本地 HTTP server。
4. Plugin 写入 `<repo>/.pickles/server.json`。
5. Plugin 后台触发首次 workspace 检查。
6. Plugin 收集 repo-relative Java 文件和当前内容。
7. Plugin 调用 Runtime stdio。
8. Runtime 读取 `pickles.config.ts` 并执行 native rules。
9. Plugin 保存 Runtime 返回的 Problem。
10. Problem Board 刷新 Header 和 Problems tab。

### 8.2 Manual Reindex Flow

1. 用户点击 `Reindex`。
2. Plugin 将 Index 状态切为 running。
3. Plugin 后台收集 Java 文件。
4. Plugin 调用 Runtime stdio。
5. Runtime 返回 Problem 或 error。
6. Plugin 成功时替换 Problem Board。
7. Plugin 失败时保留旧 Problem Board 并展示错误状态。
8. UI 刷新 Header 和 Problems tab。

### 8.3 Hook Notify Flow

1. Hook 通过 `/notify` 上报增量 changed files。
2. Plugin 按既有 HTTP contract 校验请求。
3. Plugin 调用 Runtime。
4. Runtime 返回 Problem。
5. Plugin 替换 Problem Board。
6. `/feedback` 返回当前 Problem summary。

## 9. Verification

固定验证命令：

```bash
scripts/verify-intellij-plugin.sh
scripts/verify-full-flow.sh
scripts/verify-all.sh
```

手动验证入口：

```bash
cd pickles-intellij-plugin
../scripts/run-intellij-gradle.sh -p . runIde --no-configuration-cache
```

手动验证断言：

- 打开 `e2e/sample-project/` 后，Pickles Tool Window 可见。
- Header 展示 HTTP server、Runtime、Index 和 Problem summary。
- `Problems` tab 为默认 tab。
- `Config` tab 能展示 `pickles.config.ts`。
- 点击 `Reindex` 后 Problem Board 刷新。
- Runtime 失败时 UI 展示错误状态，旧 Problem Board 不被清空。
- 点击有 file/position 的问题项能跳转到目标文件。
- 删除问题只影响当前 Problem Board 展示。

## 10. Open Items

无
