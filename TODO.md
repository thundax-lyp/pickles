# TODO List

## 说明

- `TODO.md` 是未关闭任务面板，不是完成历史。
- 宏观任务必须先讨论边界，再拆解为可执行 TODO。
- 已完成任务必须删除，不在 `TODO.md` 中打勾长期保留。
- 完成历史保留在 commit 或 PR 中。

## 当前任务项

## 待审阅任务项

## 待讨论项

- [ ] 确定首次 workspace 全量索引触发时机
    - 任务类型：待讨论项
    - 关联任务：`Runtime-Plugin Boundary`
    - 决策要求：确认打开项目、首次检测、Hook 首次通知或手动刷新中的触发时机
    - 重要度：9/10

- [ ] 设计 Tool Window 具体布局
    - 任务类型：待讨论项
    - 关联任务：`IntelliJ Plugin`
    - 决策要求：确认 Tool Window 的区域、列表字段、操作入口和错误状态展示
    - 重要度：7/10

- [ ] 确定 AGENTS.md 注入块格式、marker 和幂等更新细节
    - 任务类型：待讨论项
    - 关联任务：`AGENTS.md Injection`
    - 决策要求：确认 Pickles 注入块 marker、重复更新、删除和冲突处理规则
    - 重要度：8/10
