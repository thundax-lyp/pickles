# TODO List

## 说明

- `TODO.md` 是未关闭任务面板，不是完成历史。
- 宏观任务必须先讨论边界，再拆解为可执行 TODO。
- 已完成任务必须删除，不在 `TODO.md` 中打勾长期保留。
- 完成历史保留在 commit 或 PR 中。

## 当前任务项

## 待审阅任务项

- [ ] `docs/30-designs/RUNBOOK-INTELLIJ-REINDEX-PROBLEM-BOARD.md`：完成任务收口并清理 RUNBOOK
    - 任务类型：执行任务
    - 依据文档：`docs/00-governance/TODO-RULES.md`
    - 范围对象：`docs/30-designs/RUNBOOK-INTELLIJ-REINDEX-PROBLEM-BOARD.md`、`TODO.md`
    - 处理动作：在实现、测试和文档同步完成后删除临时 RUNBOOK 并清理对应 TODO
    - 验收点：`scripts/verify-intellij-plugin.sh`、`scripts/verify-full-flow.sh`、`scripts/verify-all.sh` 通过，RUNBOOK 无残留引用
    - 重要度：8/10

## 待讨论项
