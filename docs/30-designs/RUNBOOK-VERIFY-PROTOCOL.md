# Runbook: Verify Protocol

## 1. Purpose

本文档定义 Pickles 验证协议的一次性建设手册。

目标是把 testcase 运行固定拆成 `Prepare -> Execute -> Assert -> Restore`，并形成可被 PR workflow、AI 和人类稳定复用的验证脚本入口。

## 2. Scope

当前范围：

- 验证协议分层
- testcase 四段式执行模型
- `scripts/verify-*.sh` 脚本拆分
- `HOOK_PLUGIN_CONTRACT` 作为首个落地样例
- PR verify 总入口集成
- 验证文档同步

不在范围内：

- 不重写所有已有测试实现
- 不实现完整 IntelliJ `runIde` 自动 e2e
- 不实现 Runtime testcase
- 不新增外部测试框架
- 不引入第三方 shell 工具依赖
- 不修改业务样例工程语义

## 3. Execution Order

### 3.1 验证协议固定模型

每个可自动化 testcase 固定表达为四段：

1. `Prepare`：准备测试数据、临时目录、fixture、环境变量、端口、server stub 或依赖安装。
2. `Execute`：执行被测命令、脚本、HTTP 调用或构建命令。
3. `Assert`：验证退出码、stdout、stderr、HTTP response、生成文件或数据结构。
4. `Restore`：删除临时目录、恢复被改动文件、停止后台进程、清理环境变量。

脚本必须保证失败路径也执行 `Restore`。

### 3.2 脚本分层

验证脚本固定分两层：

- `scripts/verify-all.sh`：总入口，只负责编排各模块验证。
- `scripts/verify-*.sh`：模块或 testcase 入口，负责自己的 Prepare / Execute / Assert / Restore。

第一阶段固定拆出：

- `scripts/verify-intellij-plugin.sh`
- `scripts/verify-sample-project.sh`
- `scripts/verify-hooks.sh`

`scripts/verify-all.sh` 固定调用上述脚本，不直接展开各模块命令。

### 3.3 脚本行为规则

所有 `scripts/verify-*.sh` 固定满足：

- 使用 `set -euo pipefail`。
- 从脚本位置定位 repo root。
- 输出当前正在验证的模块或 testcase。
- 失败时返回非 0。
- 不依赖调用者当前目录。
- 不依赖用户全局配置。
- 不把临时数据写入未声明的位置。
- 执行完成后恢复由脚本创建或修改的数据。

### 3.4 Hook testcase 首个落地

`HOOK_PLUGIN_CONTRACT` 固定作为首个验证协议落地对象。

`scripts/verify-hooks.sh` 固定执行：

1. `Prepare`：创建临时 git workspace，写入临时 `.pickles/server.json`，启动 fake HTTP server。
2. `Execute`：模拟 `SessionStart`、`PostToolUse`、`Stop` hook stdin。
3. `Assert`：验证 `/health`、`/notify`、`/feedback` 收到请求，验证 notify / feedback schema，验证 server discovery 失败路径。
4. `Restore`：停止 fake HTTP server，删除临时 workspace。

当前 Node test 已覆盖以上行为。脚本第一阶段固定调用：

```bash
node --test pickles-hooks/test/hook-http-contract.test.mjs
```

后续若测试准备逻辑移出 Node test，必须保持四段式语义不变。

### 3.5 Sample project verify

`scripts/verify-sample-project.sh` 固定执行：

1. `Prepare`：进入 `e2e/sample-project/` 并安装依赖。
2. `Execute`：执行 typecheck 与 lint。
3. `Assert`：命令退出码为 0。
4. `Restore`：不删除 `node_modules`，不修改源码；若脚本新增临时文件，必须删除。

### 3.6 IntelliJ Plugin verify

`scripts/verify-intellij-plugin.sh` 固定执行：

1. `Prepare`：读取 `GRADLE_CMD`，调用者可通过环境变量覆盖 Gradle 路径。
2. `Execute`：在 `pickles-intellij-plugin/` 执行 build。
3. `Assert`：Gradle build 退出码为 0。
4. `Restore`：不删除 Gradle cache；若脚本启动后台进程，必须停止。

### 3.7 文档同步

实现完成后必须同步：

- [`../40-readiness/PR-WORKFLOW.md`](../40-readiness/PR-WORKFLOW.md)
- [`../40-readiness/E2E-TEST-CASES.md`](../40-readiness/E2E-TEST-CASES.md)
- [`../00-governance/TODO-RULES.md`](../00-governance/TODO-RULES.md)
- `scripts/verify-all.sh`

若四段式协议成为稳定长期规则，必须从本 RUNBOOK 收敛到治理文档。

## 4. Verification

固定验证命令：

```bash
scripts/verify-hooks.sh
scripts/verify-sample-project.sh
GRADLE_CMD=/tmp/pickles-gradle-8.13/gradle-8.13/bin/gradle scripts/verify-intellij-plugin.sh
GRADLE_CMD=/tmp/pickles-gradle-8.13/gradle-8.13/bin/gradle scripts/verify-all.sh
```

固定断言：

- 每个脚本可从 repo 任意子目录外调用。
- 每个脚本失败时返回非 0。
- `scripts/verify-all.sh` 只编排，不直接包含模块验证细节。
- `HOOK_PLUGIN_CONTRACT` 仍通过。
- PR workflow 文档指向新的脚本分层。

## 5. Open Items

无
