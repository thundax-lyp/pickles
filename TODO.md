# TODO List

## 说明

- `TODO.md` 是未关闭任务面板，不是完成历史。
- 宏观任务必须先讨论边界，再拆解为可执行 TODO。
- 已完成任务必须删除，不在 `TODO.md` 中打勾长期保留。
- 完成历史保留在 commit 或 PR 中。

## 当前任务项

## 待审阅任务项

- [ ] `scripts`：新增 IntelliJ Plugin 验证脚本
  - 任务类型：执行任务
  - 依据文档：`docs/30-designs/RUNBOOK-VERIFY-PROTOCOL.md`
  - 范围对象：`scripts/verify-intellij-plugin.sh`
  - 处理动作：将 IntelliJ Plugin build 验证从 `verify-all.sh` 拆为独立脚本
  - 验收点：脚本使用 `set -euo pipefail`；从脚本位置定位 repo root；支持 `GRADLE_CMD` 覆盖；可从任意目录调用；build 失败返回非 0
  - 重要度：9/10

- [ ] `scripts`：新增 sample project 验证脚本
  - 任务类型：执行任务
  - 依据文档：`docs/30-designs/RUNBOOK-VERIFY-PROTOCOL.md`
  - 范围对象：`scripts/verify-sample-project.sh`
  - 处理动作：将 `e2e/sample-project/` 依赖安装、typecheck 和 lint 验证从 `verify-all.sh` 拆为独立脚本
  - 验收点：脚本使用 `set -euo pipefail`；从脚本位置定位 repo root；执行 `npm ci`、`npm run typecheck`、`npm run lint`；可从任意目录调用；失败返回非 0
  - 重要度：9/10

- [ ] `scripts`：新增 Hook contract 验证脚本
  - 任务类型：执行任务
  - 依据文档：`docs/30-designs/RUNBOOK-VERIFY-PROTOCOL.md`
  - 范围对象：`scripts/verify-hooks.sh`
  - 处理动作：将 `HOOK_PLUGIN_CONTRACT` Node test 验证从 `verify-all.sh` 拆为独立脚本
  - 验收点：脚本使用 `set -euo pipefail`；从脚本位置定位 repo root；执行 `node --test pickles-hooks/test/hook-http-contract.test.mjs`；可从任意目录调用；失败返回非 0
  - 重要度：9/10

- [ ] `scripts`：收窄 `verify-all.sh` 为验证编排入口
  - 任务类型：执行任务
  - 依据文档：`docs/30-designs/RUNBOOK-VERIFY-PROTOCOL.md`
  - 范围对象：`scripts/verify-all.sh`
  - 处理动作：改造 `verify-all.sh`，只调用 `verify-intellij-plugin.sh`、`verify-sample-project.sh` 和 `verify-hooks.sh`
  - 验收点：`verify-all.sh` 不直接展开模块验证细节；可从任意目录调用；任一子验证失败时总入口返回非 0
  - 重要度：8/10

- [ ] `docs`：同步验证协议文档并清理 RUNBOOK
  - 任务类型：收尾任务
  - 依据文档：`docs/30-designs/RUNBOOK-VERIFY-PROTOCOL.md`
  - 范围对象：`docs/00-governance/TODO-RULES.md`、`docs/40-readiness/PR-WORKFLOW.md`、`docs/40-readiness/E2E-TEST-CASES.md`、`TODO.md`、`docs/30-designs/RUNBOOK-VERIFY-PROTOCOL.md`
  - 处理动作：将四段式验证协议收敛为稳定治理和 readiness 文档，删除已完成 RUNBOOK 并清理对应 TODO
  - 验收点：文档明确 Prepare / Execute / Assert / Restore；PR workflow 指向脚本分层；最终验证命令通过；RUNBOOK 删除；相关 TODO 清空或收窄
  - 重要度：8/10

## 待讨论项
