# TODO List

## 说明

- `TODO.md` 是未关闭任务面板，不是完成历史。
- 宏观任务必须先讨论边界，再拆解为可执行 TODO。
- 已完成任务必须删除，不在 `TODO.md` 中打勾长期保留。
- 完成历史保留在 commit 或 PR 中。

## 当前任务项

- [ ] `pickles-intellij-plugin`：验证 Hook HTTP contract stub 并清理 RUNBOOK
  - 任务类型：执行任务
  - 依据文档：`docs/30-designs/RUNBOOK-INTELLIJ-HTTP-SERVER.md`
  - 范围对象：`pickles-intellij-plugin/`、`TODO.md` 与 `docs/30-designs/RUNBOOK-INTELLIJ-HTTP-SERVER.md`
  - 处理动作：在 Gradle 可用后运行插件模块验证并删除已完成 TODO 与一次性 RUNBOOK
  - 验收点：`gradle build` 通过，且任务闭环只保留在 commit 历史中
  - 重要度：9/10

## 待审阅任务项

## 待讨论项
