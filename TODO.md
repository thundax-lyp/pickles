# TODO List

## 说明

- `TODO.md` 是未关闭任务面板，不是完成历史。
- 宏观任务必须先讨论边界，再拆解为可执行 TODO。
- 已完成任务必须删除，不在 `TODO.md` 中打勾长期保留。
- 完成历史保留在 commit 或 PR 中。

## 当前任务项

## 待审阅任务项

- [ ] `docs/40-readiness/E2E-TEST-CASES.md`：同步主动检查验证口径
    - 任务类型：执行任务
    - 依据文档：`docs/30-designs/RUNBOOK-INTELLIJ-REINDEX-PROBLEM-BOARD.md`
    - 范围对象：`docs/40-readiness/E2E-TEST-CASES.md`
    - 处理动作：记录首次 workspace 检查、手动 Reindex 和 Problem Board 状态的验证入口
    - 验收点：文档包含可执行验证命令和手动验证断言，不改变既有 Hook / Runtime 边界
    - 重要度：7/10

- [ ] `docs/30-designs/RUNBOOK-INTELLIJ-REINDEX-PROBLEM-BOARD.md`：完成任务收口并清理 RUNBOOK
    - 任务类型：执行任务
    - 依据文档：`docs/00-governance/TODO-RULES.md`
    - 范围对象：`docs/30-designs/RUNBOOK-INTELLIJ-REINDEX-PROBLEM-BOARD.md`、`TODO.md`
    - 处理动作：在实现、测试和文档同步完成后删除临时 RUNBOOK 并清理对应 TODO
    - 验收点：`scripts/verify-intellij-plugin.sh`、`scripts/verify-full-flow.sh`、`scripts/verify-all.sh` 通过，RUNBOOK 无残留引用
    - 重要度：8/10

## 待讨论项
