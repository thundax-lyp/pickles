# TODO List

## 说明

- `TODO.md` 是未关闭任务面板，不是完成历史。
- 宏观任务必须先讨论边界，再拆解为可执行 TODO。
- 已完成任务必须删除，不在 `TODO.md` 中打勾长期保留。
- 完成历史保留在 commit 或 PR 中。

## 当前任务项

## 待审阅任务项

- [ ] `pickles-intellij-plugin/src/main/kotlin/com/pickles/intellij/PicklesProjectService.kt`：收敛 Problem Board 状态模型
    - 任务类型：执行任务
    - 依据文档：`docs/30-designs/RUNBOOK-INTELLIJ-REINDEX-PROBLEM-BOARD.md`
    - 范围对象：`PicklesProjectService`、`PicklesProblemBoardState`
    - 处理动作：增加可测试的 HTTP server、Runtime、Index 和 Problem summary 状态模型
    - 验收点：单元测试能断言空问题、ERROR、WARN summary，Runtime 失败时旧 Problem Board 不被清空
    - 重要度：9/10

- [ ] `pickles-intellij-plugin/src/main/kotlin/com/pickles/intellij/PicklesProjectService.kt`：实现 Runtime 主动检查入口
    - 任务类型：执行任务
    - 依据文档：`docs/30-designs/RUNBOOK-INTELLIJ-REINDEX-PROBLEM-BOARD.md`
    - 范围对象：`PicklesProjectService`、`PicklesRuntimeClient`、`RuntimeChangedFile`
    - 处理动作：增加后台收集 repo-relative Java 文件并调用 Runtime stdio 的主动检查方法
    - 验收点：测试覆盖主动检查调用 Runtime、写入 Problem Board、失败时保留旧 Problem Board
    - 重要度：10/10

- [ ] `pickles-intellij-plugin/src/main/kotlin/com/pickles/intellij/PicklesStartupActivity.kt`：接入首次 workspace 检查
    - 任务类型：执行任务
    - 依据文档：`docs/30-designs/RUNBOOK-INTELLIJ-REINDEX-PROBLEM-BOARD.md`
    - 范围对象：`PicklesStartupActivity`、`PicklesProjectService`
    - 处理动作：在 Plugin 启动 HTTP server 后后台触发一次幂等 workspace 检查
    - 验收点：测试覆盖首次检查触发路径，重复触发不会并发启动多次同类索引
    - 重要度：9/10

- [ ] `pickles-intellij-plugin/src/main/kotlin/com/pickles/intellij/PicklesToolWindowPanel.kt`：对齐 Problem Board UI
    - 任务类型：执行任务
    - 依据文档：`docs/30-designs/RUNBOOK-INTELLIJ-REINDEX-PROBLEM-BOARD.md`
    - 范围对象：`PicklesToolWindowPanel`、Problem row rendering
    - 处理动作：增加 `Reindex` 入口、Header 状态展示、`Problems` / `Config` tab 和 Problem row 排序展示
    - 验收点：测试覆盖 Problem 排序，手动验证能看到 tabs、header、problem rows 和 config editor
    - 重要度：9/10

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
