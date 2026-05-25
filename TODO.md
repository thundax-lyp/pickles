# TODO List

## 说明

- `TODO.md` 是未关闭任务面板，不是完成历史。
- 宏观任务必须先讨论边界，再拆解为可执行 TODO。
- 已完成任务必须删除，不在 `TODO.md` 中打勾长期保留。
- 完成历史保留在 commit 或 PR 中。

## 当前任务项

## 待审阅任务项

- [ ] `pickles-runtime`：补齐 runtime 分层测试和 full-flow 回归
    - 任务类型：执行任务
    - 依据文档：`docs/30-designs/RUNBOOK-RUNTIME-TREE-SITTER-PARSER.md`
    - 范围对象：`pickles-runtime/test/**/*.ts`、`e2e/full-flow/full-flow.test.mjs`
    - 处理动作：补齐 parser、index、native rule、Problem aggregation 分层验证并回归 full-flow
    - 验收点：`scripts/verify-runtime-sample-project.sh` 和 `scripts/verify-full-flow.sh` 通过
    - 重要度：8/10
- [ ] `runtime parser runbook`：收口文档、TODO 和 RUNBOOK
    - 任务类型：执行任务
    - 依据文档：`docs/30-designs/RUNBOOK-RUNTIME-TREE-SITTER-PARSER.md`
    - 范围对象：`docs/10-requirements/RUNTIME-REQUIREMENTS.md`、`docs/30-designs/RUNTIME-DESIGN.md`、`docs/40-readiness/E2E-TEST-CASES.md`、`TODO.md`、`docs/30-designs/RUNBOOK-RUNTIME-TREE-SITTER-PARSER.md`
    - 处理动作：同步 Runtime parser 实现口径并清理已完成 TODO 与 RUNBOOK
    - 验收点：完成项不留在 `TODO.md`，已完成 RUNBOOK 被删除或收窄，相关验证结果已记录在最终交付说明中
    - 重要度：7/10

## 待讨论项
