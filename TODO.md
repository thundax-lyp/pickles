# TODO List

## 说明

- `TODO.md` 是未关闭任务面板，不是完成历史。
- 宏观任务必须先讨论边界，再拆解为可执行 TODO。
- 已完成任务必须删除，不在 `TODO.md` 中打勾长期保留。
- 完成历史保留在 commit 或 PR 中。

## 当前任务项

## 待审阅任务项

- [ ] `workspace.ignore`：新增 Pickles 专属忽略配置契约
    - 任务类型：拆解任务
    - 依据文档：`docs/20-interfaces/PICKLES-CONFIG-CONTRACT.md`
    - 范围对象：`docs/20-interfaces/PICKLES-CONFIG-CONTRACT.md`、`pickles-runtime/`、`pickles-intellij-plugin/`
    - 处理动作：拆解 `workspace.ignore` 配置契约、Runtime 过滤职责和 Plugin Reindex 调用边界
    - 验收点：形成经审阅的执行 TODO，明确 Plugin 不直接解析 `pickles.config.ts`
    - 重要度：8/10

## 待讨论项
