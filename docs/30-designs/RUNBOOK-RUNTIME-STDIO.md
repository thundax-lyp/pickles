# Runtime Stdio Runbook

## 1. Purpose

本文档定义 `feat/runtime-stdio` 分支的执行编排。

目标是把 Runtime 从测试内直接调用推进为可验证的 stdio 进程边界，并让 IntelliJ Plugin 通过该边界获得真实 Problem Board 数据。

## 2. Scope

当前范围：

- Runtime stdio request / response 边界。
- Runtime stdio host 入口。
- Runtime stdio 子进程测试。
- IntelliJ Plugin Node Runtime client。
- Plugin `/notify` 通过 Runtime stdio 刷新 Problem Board。
- Plugin `/feedback` 返回真实 Problem summary。
- full-flow e2e 使用 Runtime stdio 子进程。
- 本分支实现后的文档同步和 RUNBOOK 清理。

不在范围内：

- 不接入 `tree-sitter`。
- 不替换当前 Java index 实现。
- 不实现常驻 Runtime 进程。
- 不实现 Runtime 进程池。
- 不实现 Runtime restart protocol。
- 不实现首次 workspace 全量索引。
- 不改变 Hook HTTP request / response schema。
- 不改变 Pickles runtime config contract。
- 不改变 Agent-side skill 行为。

## 3. Bounded Context

当前分支名固定为 `feat/runtime-stdio`。

本分支只关闭 Runtime stdio 边界和 Plugin 调用链，不扩大到 parser、index 持久化或 Runtime lifecycle 的完整产品化。

Runtime 固定拥有规则加载、native rule execution 和 Problem 聚合。Plugin 固定拥有 HTTP server、Problem Board、Runtime 子进程调用和错误展示。

当前阶段 stdio 采用单请求子进程模型。每次 Runtime client 调用启动一个 Node.js 进程，stdin 写入一个 request，stdout 读取一个 response，然后进程退出。

## 4. Module Mapping

- `pickles-runtime/`: Runtime stdio host、request 校验、response 输出和 stdio 测试。
- `pickles-intellij-plugin/`: Node Runtime client、Runtime path 定位、子进程调用、HTTP 错误映射和 Problem Board 更新。
- `e2e/full-flow/`: Hook -> Plugin harness -> Runtime stdio -> feedback 的端到端验证。
- `docs/30-designs/`: 本 RUNBOOK 和实现后的稳定设计同步。
- `TODO.md`: 待审阅执行任务队列。

## 5. Core Objects

- `RuntimeCheckInput`
- `RuntimeCheckResult`
- `RuntimeStdioError`
- `RuntimeStdioHost`
- `RuntimeChangedFile`
- `NodePicklesRuntimeClient`
- `PicklesRuntimeLocator`
- `PicklesProblem`
- `PicklesProblemBoardState`
- `NotifyResponse.processed`
- `FeedbackResponse.status`

## 6. Global Constraints

- Runtime stdout 固定只输出 JSON response。
- Runtime stderr 固定只输出日志和诊断信息。
- Runtime stdio host 每次只处理一个 request。
- Runtime stdio success response 固定兼容 `RuntimeCheckResult`。
- Runtime stdio failure response 固定包含 `error.message`。
- Runtime stdio failure 必须返回非 0 exit code。
- Plugin 不直接 import Runtime TypeScript module。
- Plugin 不执行 native rule。
- Plugin 不读取 Runtime 内部 index。
- Plugin 不依赖 tree-sitter API。
- Plugin Runtime client 必须设置超时。
- Plugin Runtime client 必须区分 invalid JSON、Runtime error、exit code failure 和 timeout。
- Runtime 不可定位时，Plugin 保持明确的未接入状态。
- TypeScript 新增或修改代码必须使用箭头函数形式。

## 7. Execution Order

### 7.1 Runtime Stdio Contract

1. 明确 stdio request 使用 `RuntimeCheckInput`。
2. 明确 stdio success response 使用 `RuntimeCheckResult`。
3. 明确 stdio failure response 使用 `RuntimeStdioError`。
4. 明确 stdout / stderr / exit code 规则。

验收点：

- Runtime stdio host 的输入、输出和失败结构在代码中有稳定类型或局部 DTO。

### 7.2 Runtime Stdio Host

1. 新增 `pickles-runtime/src/stdio.ts`。
2. 从 stdin 读取完整 JSON。
3. 校验 request 至少包含 `workspaceRoot` 和 `changedFiles`。
4. 调用 `runRuntimeCheck`。
5. stdout 输出 JSON response。
6. 捕获异常并输出 `error.message`。
7. 失败时设置非 0 exit code。
8. 在 `pickles-runtime/package.json` 增加 stdio script。

验收点：

- 空 stdin 返回失败。
- 非法 request 返回失败。
- 合法 request 返回 Problem array。
- `npm --prefix pickles-runtime run typecheck` 通过。

### 7.3 Runtime Stdio Test

1. 在 `runtime-sample-project.test.ts` 增加 stdio 子进程测试。
2. 使用 `node --import tsx src/stdio.ts`。
3. stdin 写入 sample project 的 `RuntimeCheckInput`。
4. assert exit code 为 0。
5. assert stdout 是 JSON。
6. assert response 包含 sample rule Problem。
7. 增加 Runtime error path 测试。

验收点：

- `npm --prefix pickles-runtime test` 覆盖成功和失败路径。

### 7.4 Plugin Runtime DTO Alignment

1. 确认 Kotlin `PicklesProblem` 覆盖 Problem contract 字段。
2. 补齐缺失字段，例如 `fixHint`。
3. 确认 `RuntimeChangedFile` 可映射为 Runtime `ChangedFile`。
4. 确认 deleted / added / modified 的 `changeType` 推导规则。

验收点：

- Kotlin DTO 能解析 Runtime stdout 中的 Problem JSON。
- DTO 变化有单元测试覆盖。

### 7.5 Runtime Path Location

1. 增加 `PicklesRuntimeLocator`。
2. 支持系统属性覆盖 Runtime 目录。
3. 支持 Pickles repo 内 e2e sample project 的相对定位。
4. Runtime 不可定位时返回 `null`，不抛隐藏异常。

验收点：

- Runtime root 可定位测试覆盖成功路径。
- Runtime root 不可定位测试覆盖 fallback 行为。

### 7.6 Node Runtime Client Process

1. 增加 `NodePicklesRuntimeClient`。
2. 使用 `ProcessBuilder` 调用 Node.js。
3. 固定命令为 Node.js + `--import tsx` + Runtime stdio host。
4. stdin 写入 Runtime request。
5. stdout 读取 Runtime response。
6. stderr 保留为错误诊断。
7. 设置调用超时。
8. timeout 时销毁进程。

验收点：

- 子进程调用参数可测试。
- timeout、non-zero exit 和 stderr message 可映射为失败。

### 7.7 Node Runtime Client Response Parsing

1. 解析 stdout JSON。
2. success response 转为 `List<PicklesProblem>`。
3. `error.message` response 转为失败。
4. invalid JSON response 转为失败。
5. 空 stdout 转为失败。

验收点：

- stdout success、runtime error、invalid JSON 和 empty stdout 都有测试覆盖。

### 7.8 Plugin Service Integration

1. `PicklesProjectService` 延迟创建 Runtime client。
2. Runtime 可定位时向 `PicklesHttpContractHandler` 注入 client 和 Problem Board。
3. Runtime 不可定位时不注入 client，保持未接入语义。
4. Runtime client 初始化不得阻塞 UI 线程。

验收点：

- Runtime 可用时 `/notify processed = true`。
- Runtime 不可用时 `/feedback status = unimplemented`。

### 7.9 HTTP Notify Failure Mapping

1. Runtime client 成功时替换 Problem Board。
2. Runtime client 失败时返回 `INTERNAL_ERROR`。
3. Runtime client 失败时不得把 Problem Board 替换为空。
4. 错误 message 必须可读。

验收点：

- `PicklesHttpContractTest` 或 `PicklesRuntimeFlowTest` 覆盖 Runtime failure。

### 7.10 Feedback Semantics

1. Runtime 已接入且 Problem Board 存在时，`/feedback status = ok`。
2. Runtime 未接入时，`/feedback status = unimplemented`。
3. `hasBlockingProblems` 由 `ERROR` Problem 决定。
4. summary 使用当前 Problem Board 统计。

验收点：

- feedback ok 路径和 unimplemented 路径都有测试覆盖。

### 7.11 Full Flow Stdio E2E

1. `e2e/full-flow/full-flow.test.mjs` 移除 direct import `runRuntimeCheck`。
2. Plugin harness 使用 Runtime stdio 子进程执行检测。
3. 保持 Hook 输入和 HTTP contract 不变。
4. assert `/feedback` 输出 blocking problem。

验收点：

- `./scripts/verify-full-flow.sh` 证明 Hook 到 Runtime stdio 到 feedback 闭环成立。

### 7.12 Documentation Sync

1. 实现完成后检查 `RUNTIME-DESIGN.md`。
2. 实现完成后检查 `INTELLIJ-PLUGIN-DESIGN.md`。
3. 只沉淀稳定边界，不复制 RUNBOOK 执行步骤。
4. 不修改无关需求文档。

验收点：

- 长期文档只记录稳定 stdio 边界。

### 7.13 Closure

1. 运行验证入口。
2. 删除已完成 TODO。
3. 清理本 RUNBOOK。
4. 检查残留引用。
5. 提交最终收口 commit。

验收点：

- `TODO.md` 不保留已完成项。
- 本 RUNBOOK 被删除，或只保留未完成剩余范围。
- 工作区干净。

## 8. Verification

分步验证：

- `npm run lint`
- `npm --prefix pickles-runtime run typecheck`
- `npm --prefix pickles-runtime test`
- `./scripts/verify-intellij-plugin.sh`
- `./scripts/verify-full-flow.sh`

收口验证：

- `./scripts/verify-all.sh`
- `git diff --check`

## 9. Open Items

无
