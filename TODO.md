# TODO List

## 说明

- `TODO.md` 是未关闭任务面板，不是完成历史。
- 宏观任务必须先讨论边界，再拆解为可执行 TODO。
- 已完成任务必须删除，不在 `TODO.md` 中打勾长期保留。
- 完成历史保留在 commit 或 PR 中。

## 当前任务项

## 待审阅任务项

- [ ] `pickles-hooks`：按 RUNBOOK 拆解 Hook HTTP client contract 实现任务
  - 任务类型：拆解任务
  - 依据文档：`docs/30-designs/RUNBOOK-HOOK-HTTP-CLIENT.md`
  - 范围对象：`pickles-hooks/`、`e2e/sample-project/.codex/hooks.json`、`docs/40-readiness/E2E-TEST-CASES.md`
  - 处理动作：将 RUNBOOK 拆解为可独立验收的 Hook HTTP client 实现任务
  - 验收点：拆解后的执行任务覆盖脚本骨架、server discovery、health、notify、feedback、fake server 测试、e2e 配置、文档同步和 RUNBOOK 清理
  - 重要度：9/10

## 待讨论项
