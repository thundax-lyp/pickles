# IntelliJ Reindex Notify Queue Runbook

## 1. Purpose

本文档定义 `feat/intellij-reindex-notify-queue` 分支的一次性执行手册。

目标是让 IntelliJ Plugin 中的手动 / 首次 `Reindex` 与 Hook `/notify` Runtime 调用形成可解释的队列策略，避免并发 Runtime 调用导致 Problem Board 展示过期结果。

## 2. Scope

当前范围：

- Plugin 内 Runtime 调用队列
- `Reindex` 与 Hook `/notify` 的调度顺序
- 重叠文件导致前序检测失效后的补跑策略
- Problem Board 更新时的运行版本校验
- `/notify` HTTP response 与队列处理边界
- Kotlin 单元测试与 readiness 文档同步

不在范围内：

- 不改变 Hook HTTP contract schema
- 不改变 Runtime stdio request / response schema
- 不新增 Runtime HTTP server
- 不改变 `workspace.ignore` 契约
- 不实现跨进程持久化队列
- 不实现多 workspace 调度

## 3. Bounded Context

IntelliJ Plugin 是 Runtime lifecycle owner，也是本任务的调度所有者。

Runtime 仍然只接收一次 changed files 输入并返回 Problem。Runtime 不感知 Plugin 队列、Hook event 或 Reindex 来源。

Hook 仍然只向 Plugin HTTP server 发送 `/notify` 和 `/feedback`。Hook 不直接调用 Runtime。

Problem Board 只展示当前 workspace 的最新可信结果。被更新输入失效的 Runtime 结果不得替换 Problem Board。

## 4. Module Mapping

- `pickles-intellij-plugin/`：实现 Runtime 调用队列、失效检测、结果落板策略、HTTP notify 调度和单元测试。
- `docs/40-readiness/E2E-TEST-CASES.md`：同步 Reindex 与 Hook notify 队列验证口径。
- `docs/30-designs/RUNBOOK-INTELLIJ-REINDEX-NOTIFY-QUEUE.md`：本任务临时执行手册，任务完成后清理。

## 5. Core Objects

- `PicklesProjectService`
- `PicklesHttpContractHandler`
- `PicklesRuntimeClient`
- `RuntimeChangedFile`
- `PicklesProblemBoardState`
- `PicklesWorkspaceInspection`
- `PicklesRuntimeQueue`
- `RuntimeQueueRequest`
- `RuntimeQueueResult`
- `RuntimeRunVersion`

## 6. Global Constraints

- Plugin 不执行 native rules。
- Plugin 不修改用户业务代码。
- Plugin 不直接读取 Runtime 内部状态。
- Plugin Runtime 调用必须串行执行。
- Hook `/notify` 与 `Reindex` 必须共用同一条 Runtime 队列。
- 队列必须保留最新输入。
- 队列中的后续输入与当前运行输入存在重叠文件时，当前运行结果固定失效。
- 失效的 Runtime 结果不得替换 Problem Board。
- 当前运行结果失效后，队列必须基于最新输入补跑。
- 非重叠输入可以排队顺序执行，但最终 Problem Board 仍只展示最后一次成功的 workspace 结果。
- `/notify` HTTP response 不等待队列全部清空。
- `/notify` 在请求校验通过且成功入队后固定返回 `202 accepted`。
- Runtime 调用失败时必须保留旧 Problem Board，并展示可理解状态。

## 7. Functional Requirements

### 7.1 Runtime Queue Model

Runtime queue 固定维护：

- 当前运行请求
- pending 请求
- 单调递增 run version
- 当前运行是否已失效

Runtime queue request 固定包含：

- request source：`reindex` 或 `notify`
- changed files
- affected file set

`affected file set` 固定使用 repo-relative path。

### 7.2 Reindex Enqueue

`Reindex` 固定执行：

1. Plugin 收集 workspace Java files。
2. Plugin 创建 source 为 `reindex` 的 queue request。
3. Plugin 将 request 入队。
4. 如果没有运行中的 request，立即启动 Runtime 调用。
5. 如果已有运行中的 request，则进入 pending。

### 7.3 Notify Enqueue

Hook `/notify` 固定执行：

1. Plugin 校验 HTTP request。
2. Plugin 将 request 转为 `RuntimeChangedFile`。
3. Plugin 创建 source 为 `notify` 的 queue request。
4. Plugin 将 request 入队。
5. Plugin 返回 `202 accepted`。

Hook `/notify` 不等待 Runtime 调用完成。

### 7.4 Overlap Invalidation

当新 request 入队时：

1. 如果没有运行中的 request，新 request 直接运行。
2. 如果存在运行中的 request，比较新 request 的 affected file set 与当前运行 request 的 affected file set。
3. 两者存在交集时，当前运行 request 标记为失效。
4. 新 request 合并进入 pending。

pending 合并规则固定为：

- 相同 file path 使用最新 request 的 file 内容。
- 不同 file path 保留在 pending request 中。
- pending request source 可使用 `mixed` 内部值，但不得暴露到 HTTP contract。

### 7.5 Result Apply Rule

Runtime 返回结果后：

1. 如果 run version 已失效，不更新 Problem Board。
2. 如果 run version 未失效，使用 Runtime 返回 Problems 替换 Problem Board。
3. 如果存在 pending request，立即启动下一次 Runtime 调用。
4. 如果不存在 pending request，将 Index 状态切换为 succeeded 或 failed。

### 7.6 Feedback Rule

`/feedback` 固定读取当前 Problem Board。

当 Runtime queue 正在运行或存在 pending request 时，Problem Board Header 必须能展示当前结果可能不是最新结果。

MVP 中 `/feedback` response schema 不增加字段。

## 8. Execution Order

### 8.1 抽出可测试 Runtime 队列对象

1. 新增 `PicklesRuntimeQueue`。
2. 队列对象不依赖 IntelliJ `Project`。
3. 队列对象负责入队、合并、失效标记和 next request 选择。
4. 补 Kotlin 单元测试覆盖空队列、串行队列、重叠失效和 pending 合并。

验收点：

- 单元测试能证明重叠文件入队会标记当前运行失效。
- 单元测试能证明 pending 中同一路径保留最新内容。

### 8.2 将 Reindex 接入 Runtime 队列

1. `reindexWorkspace()` 不再直接调用 Runtime。
2. `reindexWorkspace()` 收集文件后入队。
3. 队列启动 Runtime 调用。
4. 成功且未失效时替换 Problem Board。
5. 失败时保留旧 Problem Board。

验收点：

- 测试覆盖 Reindex 入队后会调用 Runtime。
- 测试覆盖失效 Reindex 结果不会落板。

### 8.3 将 Hook notify 接入 Runtime 队列

1. `PicklesHttpContractHandler.notify` 只负责校验和构造 request。
2. Runtime 调用从 HTTP contract handler 中移出。
3. `PicklesProjectService` 负责将 notify request 入队。
4. `/notify` 校验成功并入队后返回 `202 accepted`。

验收点：

- 测试覆盖 `/notify` 校验失败仍返回错误。
- 测试覆盖 `/notify` 校验成功后不等待 Runtime 完成。
- 测试覆盖 notify 与 Reindex 重叠时 Reindex 结果失效并补跑 notify。

### 8.4 状态与文档同步

1. Header 状态能表达 running / pending / stale。
2. readiness 文档补充队列验证口径。
3. 任务完成后删除本 RUNBOOK。

验收点：

- `scripts/verify-intellij-plugin.sh` 通过。
- `scripts/verify-full-flow.sh` 通过。
- `scripts/verify-all.sh` 通过。
- RUNBOOK 无残留引用。

## 9. Key Flows

### 9.1 Reindex Running Then Notify Same File

1. 用户触发 `Reindex`。
2. Plugin 启动 Runtime run version 1，affected files 包含 `src/App.java`。
3. Hook `/notify` 到达，changed files 包含 `src/App.java`。
4. Plugin 将 version 1 标记为失效。
5. Plugin 将 notify request 合并为 pending。
6. version 1 返回 Problems。
7. Plugin 丢弃 version 1 结果。
8. Plugin 启动 version 2。
9. version 2 返回 Problems。
10. Plugin 使用 version 2 结果刷新 Problem Board。

### 9.2 Reindex Running Then Notify Different File

1. 用户触发 `Reindex`。
2. Hook `/notify` 到达，changed files 不与当前运行 files 重叠。
3. Plugin 保留当前运行结果可落板资格。
4. Plugin 将 notify request 放入 pending。
5. 当前运行完成后按结果落板。
6. pending request 随后运行并刷新 Problem Board。

### 9.3 Multiple Notify Same File

1. Hook `/notify` 到达，changed files 包含 `src/App.java` after A。
2. 第二个 Hook `/notify` 到达，changed files 包含 `src/App.java` after B。
3. pending request 中 `src/App.java` 固定保留 after B。
4. Runtime 只对最新 pending 输入补跑。

## 10. Verification

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

- Reindex 运行中触发文件修改后，最终 Problem Board 使用最新 notify 输入结果。
- Reindex 运行中同一文件 notify 到达时，旧 Reindex 结果不得覆盖 notify 结果。
- Reindex 运行中不同文件 notify 到达时，notify 会在 Reindex 后继续执行。
- Runtime 失败时旧 Problem Board 保留。

## 11. Open Items

无
