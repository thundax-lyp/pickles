# TODO List

## 说明

- `TODO.md` 是未关闭任务面板，不是完成历史。
- 宏观任务必须先讨论边界，再拆解为可执行 TODO。
- 已完成任务必须删除，不在 `TODO.md` 中打勾长期保留。
- 完成历史保留在 commit 或 PR 中。

## 当前任务项

## 待审阅任务项

- [ ] `e2e`：配置 sample project `PreToolUse` Hook
  - 任务类型：配置任务
  - 依据文档：`docs/30-designs/RUNBOOK-HOOK-FILE-CAPTURE.md`
  - 范围对象：`e2e/sample-project/.codex/hooks.json`
  - 处理动作：为 sample project 增加 `PreToolUse` 本地 Hook 绑定
  - 验收点：`PreToolUse` matcher 覆盖 `Bash`、`apply_patch`、`Edit`、`Write`；command 从当前 git root 定位 `.codex/hooks/pickles-hook.mjs`
  - 重要度：8/10

- [ ] `docs`：同步文件捕获文档并清理 RUNBOOK
  - 任务类型：收尾任务
  - 依据文档：`docs/30-designs/RUNBOOK-HOOK-FILE-CAPTURE.md`
  - 范围对象：`docs/10-requirements/CODEX-HOOKS-REQUIREMENTS.md`、`docs/20-interfaces/HOOK-PLUGIN-HTTP-CONTRACT.md`、`docs/40-readiness/E2E-TEST-CASES.md`、`TODO.md`、`docs/30-designs/RUNBOOK-HOOK-FILE-CAPTURE.md`
  - 处理动作：同步真实文件捕获状态、测试口径和配置口径，删除已完成 RUNBOOK 并清理对应 TODO
  - 验收点：`HOOK_PLUGIN_CONTRACT` 固定断言包含真实文件捕获；最终验证命令通过；RUNBOOK 删除；相关 TODO 清空或收窄
  - 重要度：8/10

## 待讨论项
