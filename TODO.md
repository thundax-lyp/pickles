# TODO List

## 说明

- `TODO.md` 是未关闭任务面板，不是完成历史。
- 宏观任务必须先讨论边界，再拆解为可执行 TODO。
- 已完成任务必须删除，不在 `TODO.md` 中打勾长期保留。
- 完成历史保留在 commit 或 PR 中。

## 当前任务项

## 待审阅任务项

- [ ] `scripts`：提供可重复本地 Gradle 执行路径
    - 任务类型：执行任务
    - 依据文档：`docs/30-designs/RUNBOOK-RUNTIME-TREE-SITTER-HARDENING.md`
    - 范围对象：`scripts/verify-all.sh`、`scripts/verify-intellij-plugin.sh`、`pickles-intellij-plugin/`、相关文档
    - 处理动作：通过 Gradle wrapper 或明确 fallback 让本地 IntelliJ Plugin 验证路径可重复执行
    - 验收点：可运行环境中 `scripts/verify-all.sh` 通过或最终说明明确缺失条件
    - 重要度：7/10
- [ ] `runtime tree-sitter hardening docs`：同步验证和 Runtime 文档口径
    - 任务类型：执行任务
    - 依据文档：`docs/30-designs/RUNBOOK-RUNTIME-TREE-SITTER-HARDENING.md`
    - 范围对象：`docs/10-requirements/RUNTIME-REQUIREMENTS.md`、`docs/30-designs/RUNTIME-DESIGN.md`、`docs/40-readiness/E2E-TEST-CASES.md`、`docs/40-readiness/PR-WORKFLOW.md`
    - 处理动作：同步 Runtime parser hardening 和验证入口实现口径
    - 验收点：文档没有保留过期实现口径且没有新增未实现承诺
    - 重要度：6/10
- [ ] `runtime tree-sitter hardening runbook`：收口文档、TODO 和 RUNBOOK
    - 任务类型：执行任务
    - 依据文档：`docs/30-designs/RUNBOOK-RUNTIME-TREE-SITTER-HARDENING.md`
    - 范围对象：`docs/10-requirements/RUNTIME-REQUIREMENTS.md`、`docs/30-designs/RUNTIME-DESIGN.md`、`docs/40-readiness/E2E-TEST-CASES.md`、`TODO.md`、`docs/30-designs/RUNBOOK-RUNTIME-TREE-SITTER-HARDENING.md`
    - 处理动作：同步实现口径并清理已完成 TODO 与 RUNBOOK
    - 验收点：完成项不留在 `TODO.md`，已完成 RUNBOOK 被删除或收窄，验证结果已记录在最终交付说明中
    - 重要度：6/10

## 待讨论项
