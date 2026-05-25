# TODO List

## 说明

- `TODO.md` 是未关闭任务面板，不是完成历史。
- 宏观任务必须先讨论边界，再拆解为可执行 TODO。
- 已完成任务必须删除，不在 `TODO.md` 中打勾长期保留。
- 完成历史保留在 commit 或 PR 中。

## 当前任务项

## 待审阅任务项

- [ ] `pickles-runtime`：解析 Java member、annotation 和 range
    - 任务类型：执行任务
    - 依据文档：`docs/30-designs/RUNBOOK-RUNTIME-TREE-SITTER-PARSER.md`
    - 范围对象：`pickles-runtime/src/**/*.ts`、`pickles-runtime/test/**/*.ts`
    - 处理动作：让 JavaSyntaxParser 输出 method、constructor、field、annotation、modifier 和 1-based range
    - 验收点：parser tests 覆盖 nested type、annotated method、constructor、field 和 range
    - 重要度：8/10
- [ ] `pickles-runtime`：将 Java index 接到 parser DTO
    - 任务类型：执行任务
    - 依据文档：`docs/30-designs/RUNBOOK-RUNTIME-TREE-SITTER-PARSER.md`
    - 范围对象：`pickles-runtime/src/java-index.ts`、`pickles-runtime/src/types.ts`、`pickles-runtime/test/**/*.ts`
    - 处理动作：将 Java index 更新为消费 JavaSyntaxParser 输出并停止使用内部正则解析
    - 验收点：现有 native rule sample 测试和 `scripts/verify-runtime-sample-project.sh` 通过
    - 重要度：9/10
- [ ] `pickles-runtime`：实现 index 增删改贡献清理
    - 任务类型：执行任务
    - 依据文档：`docs/30-designs/RUNBOOK-RUNTIME-TREE-SITTER-PARSER.md`
    - 范围对象：`pickles-runtime/src/java-index.ts`、`pickles-runtime/test/**/*.ts`
    - 处理动作：实现 added、modified、deleted、unchanged 的 index 更新和反向索引贡献清理
    - 验收点：index tests 覆盖四种 change type 且重复修改不会留下旧索引
    - 重要度：8/10
- [ ] `pickles-runtime`：增加 parser diagnostic
    - 任务类型：执行任务
    - 依据文档：`docs/30-designs/RUNBOOK-RUNTIME-TREE-SITTER-PARSER.md`
    - 范围对象：`pickles-runtime/src/**/*.ts`、`pickles-runtime/test/**/*.ts`
    - 处理动作：把 tree-sitter syntax error 转换为 parser diagnostic 并纳入 Problem aggregation
    - 验收点：syntax error 返回可展示诊断且同批其他文件继续处理
    - 重要度：8/10
- [ ] `pickles-runtime`：增加 Runtime 输入限制
    - 任务类型：执行任务
    - 依据文档：`docs/30-designs/RUNBOOK-RUNTIME-TREE-SITTER-PARSER.md`
    - 范围对象：`pickles-runtime/src/**/*.ts`、`pickles-runtime/test/**/*.ts`
    - 处理动作：实现 2 MiB 单文件限制和 200 changed files 批次限制
    - 验收点：超大文件和超批次测试通过且错误输出可解释
    - 重要度：8/10
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
