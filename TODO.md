# TODO List

## 说明

- `TODO.md` 是未关闭任务面板，不是完成历史。
- 宏观任务必须先讨论边界，再拆解为可执行 TODO。
- 已完成任务必须删除，不在 `TODO.md` 中打勾长期保留。
- 完成历史保留在 commit 或 PR 中。

## 当前任务项

## 待审阅任务项

- [ ] `pickles-intellij-plugin/`：调整 Reindex 与 Runtime ignore 的职责边界
    - 任务类型：执行任务
    - 依据文档：`docs/20-interfaces/PICKLES-CONFIG-CONTRACT.md`
    - 范围对象：`PicklesWorkspaceInspection`、`PicklesRuntimeClient`、readiness 文档
    - 处理动作：保留 Plugin 的 `.gitignore` 和内置扫描兜底过滤，确认 Pickles 专属 `workspace.ignore` 只由 Runtime 应用
    - 验收点：Plugin 测试不解析 `pickles.config.ts`，readiness 文档记录 Reindex 与 Runtime ignore 的边界
    - 重要度：7/10

## 待讨论项
