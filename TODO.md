# TODO List

## 说明

- `TODO.md` 是未关闭任务面板，不是完成历史。
- 宏观任务必须先讨论边界，再拆解为可执行 TODO。
- 已完成任务必须删除，不在 `TODO.md` 中打勾长期保留。
- 完成历史保留在 commit 或 PR 中。

## 当前任务项

## 待审阅任务项

- [ ] `pickles-intellij-plugin/src/main/kotlin/com/pickles/intellij/PicklesHttpContract.kt`：将 Hook notify 接入 Runtime 队列
    - 任务类型：执行任务
    - 依据文档：`docs/30-designs/RUNBOOK-INTELLIJ-REINDEX-NOTIFY-QUEUE.md`
    - 范围对象：`PicklesHttpContractHandler.notify`、`PicklesProjectService.handleNotify`
    - 处理动作：将 Runtime 调用移出 HTTP contract handler，notify 校验成功后由 service 入队并立即返回 `202 accepted`
    - 验收点：测试覆盖 notify 校验失败仍返回错误、成功后不等待 Runtime 完成、与 Reindex 重叠时旧结果失效并补跑 notify
    - 重要度：10/10

- [ ] `pickles-intellij-plugin/`：补充队列状态展示
    - 任务类型：执行任务
    - 依据文档：`docs/30-designs/RUNBOOK-INTELLIJ-REINDEX-NOTIFY-QUEUE.md`
    - 范围对象：`PicklesServiceStatusSnapshot`、`PicklesToolWindowPanel`
    - 处理动作：让 Header 状态表达 Runtime queue running、pending 和 stale 结果状态
    - 验收点：测试覆盖队列运行或 pending 时状态可见，UI 仍能展示 Problem summary
    - 重要度：8/10

- [ ] `docs/40-readiness/E2E-TEST-CASES.md`：同步 Reindex / notify 队列验证口径
    - 任务类型：执行任务
    - 依据文档：`docs/30-designs/RUNBOOK-INTELLIJ-REINDEX-NOTIFY-QUEUE.md`
    - 范围对象：`PLUGIN_WORKSPACE_REINDEX`、`PLUGIN_RUNTIME_FLOW`
    - 处理动作：记录 Reindex 与 notify 共用 Runtime 队列、重叠文件失效补跑和 `/notify` 立即返回的验证断言
    - 验收点：readiness 文档包含可执行验证命令和防漂移点，不改变 HTTP contract schema
    - 重要度：7/10

- [ ] `docs/30-designs/RUNBOOK-INTELLIJ-REINDEX-NOTIFY-QUEUE.md`：完成任务收口并清理 RUNBOOK
    - 任务类型：执行任务
    - 依据文档：`docs/00-governance/TODO-RULES.md`
    - 范围对象：`docs/30-designs/RUNBOOK-INTELLIJ-REINDEX-NOTIFY-QUEUE.md`、`TODO.md`
    - 处理动作：在实现、测试和文档同步完成后删除临时 RUNBOOK 并清理对应 TODO
    - 验收点：`scripts/verify-intellij-plugin.sh`、`scripts/verify-full-flow.sh`、`scripts/verify-all.sh` 通过，RUNBOOK 无残留引用
    - 重要度：8/10

## 待讨论项
