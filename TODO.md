# TODO List

## 说明

- `TODO.md` 是未关闭任务面板，不是完成历史。
- 宏观任务必须先讨论边界，再拆解为可执行 TODO。
- 已完成任务必须删除，不在 `TODO.md` 中打勾长期保留。
- 完成历史保留在 commit 或 PR 中。

## 当前任务项

## 待审阅任务项

- [ ] `pickles-hooks`：完成 Hook HTTP client contract 文档同步与 RUNBOOK 收尾
  - 任务类型：收尾任务
  - 依据文档：`docs/30-designs/RUNBOOK-HOOK-HTTP-CLIENT.md`
  - 范围对象：`TODO.md`、`docs/10-requirements/CODEX-HOOKS-REQUIREMENTS.md`、`docs/20-interfaces/HOOK-PLUGIN-HTTP-CONTRACT.md`、`docs/40-readiness/E2E-TEST-CASES.md`、`docs/30-designs/RUNBOOK-HOOK-HTTP-CLIENT.md`
  - 处理动作：实现完成后同步相关文档，更新 `HOOK_PLUGIN_CONTRACT` 状态，删除或收窄已完成 TODO，删除已完成且无剩余价值的 RUNBOOK
  - 验收点：最终 commit 包含代码、测试、配置样例、文档同步、TODO 清理和 RUNBOOK 清理；最终响应写明验证命令
  - 重要度：8/10

## 待讨论项
