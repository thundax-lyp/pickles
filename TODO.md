# TODO List

## 说明

- `TODO.md` 是未关闭任务面板，不是完成历史。
- 宏观任务必须先讨论边界，再拆解为可执行 TODO。
- 已完成任务必须删除，不在 `TODO.md` 中打勾长期保留。
- 完成历史保留在 commit 或 PR 中。

## 当前任务项

## 待审阅任务项

- [ ] `runtime-stdio-doc-sync`：同步 Runtime stdio 稳定文档
    - 任务类型：执行任务
    - 依据文档：`docs/30-designs/RUNBOOK-RUNTIME-STDIO.md`
    - 范围对象：`docs/30-designs/RUNTIME-DESIGN.md`、`docs/30-designs/INTELLIJ-PLUGIN-DESIGN.md`
    - 处理动作：只沉淀实现后的稳定 stdio 边界
    - 验收点：长期设计文档不复制 RUNBOOK 执行步骤
    - 重要度：7/10
- [ ] `runtime-stdio-closure`：清理 RUNBOOK 和 TODO
    - 任务类型：执行任务
    - 依据文档：`docs/30-designs/RUNBOOK-RUNTIME-STDIO.md`
    - 范围对象：`TODO.md`、`docs/30-designs/RUNBOOK-RUNTIME-STDIO.md`
    - 处理动作：完成实现和验证后删除已完成 TODO 并清理本 RUNBOOK
    - 验收点：`TODO.md` 无已完成项残留，RUNBOOK 已删除或收窄为剩余范围
    - 重要度：8/10

## 待讨论项
