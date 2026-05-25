# TODO List

## 说明

- `TODO.md` 是未关闭任务面板，不是完成历史。
- 宏观任务必须先讨论边界，再拆解为可执行 TODO。
- 已完成任务必须删除，不在 `TODO.md` 中打勾长期保留。
- 完成历史保留在 commit 或 PR 中。

## 当前任务项

## 待审阅任务项

- [ ] `pickles-runtime`：覆盖 Java import 变体
    - 任务类型：执行任务
    - 依据文档：`docs/30-designs/RUNBOOK-RUNTIME-TREE-SITTER-HARDENING.md`
    - 范围对象：`pickles-runtime/test/**/*.ts`、`pickles-runtime/src/java-syntax-parser.ts`
    - 处理动作：补充 normal import、static import 和 wildcard import parser fixture
    - 验收点：import fixture tests 通过且 `scripts/verify-runtime-sample-project.sh` 通过
    - 重要度：8/10
- [ ] `pickles-runtime`：覆盖 Java 泛型语法样例
    - 任务类型：执行任务
    - 依据文档：`docs/30-designs/RUNBOOK-RUNTIME-TREE-SITTER-HARDENING.md`
    - 范围对象：`pickles-runtime/test/**/*.ts`、`pickles-runtime/src/java-syntax-parser.ts`
    - 处理动作：补充 generic class 和 generic method parser fixture
    - 验收点：泛型 fixture tests 通过且未新增泛型语义解析承诺
    - 重要度：7/10
- [ ] `pickles-runtime`：覆盖 Java field 和 annotation 变体
    - 任务类型：执行任务
    - 依据文档：`docs/30-designs/RUNBOOK-RUNTIME-TREE-SITTER-HARDENING.md`
    - 范围对象：`pickles-runtime/test/**/*.ts`、`pickles-runtime/src/java-syntax-parser.ts`
    - 处理动作：补充多变量 field、qualified annotation、package-private type 和 member fixture
    - 验收点：field 和 annotation fixture tests 通过
    - 重要度：7/10
- [ ] `pickles-runtime`：建立 WorkspaceIndexService 边界
    - 任务类型：执行任务
    - 依据文档：`docs/30-designs/RUNBOOK-RUNTIME-TREE-SITTER-HARDENING.md`
    - 范围对象：`pickles-runtime/src/java-index.ts`、`pickles-runtime/src/**/*.ts`、`pickles-runtime/test/**/*.ts`
    - 处理动作：新增 WorkspaceIndexService 并保留 createJavaIndex 兼容 facade
    - 验收点：RuleContext java query 行为不变且 `npm run test` 通过
    - 重要度：8/10
- [ ] `pickles-runtime`：迁移 index 更新和贡献清理
    - 任务类型：执行任务
    - 依据文档：`docs/30-designs/RUNBOOK-RUNTIME-TREE-SITTER-HARDENING.md`
    - 范围对象：`pickles-runtime/src/java-index.ts`、`pickles-runtime/src/**/*.ts`、`pickles-runtime/test/**/*.ts`
    - 处理动作：将 added、modified、deleted、unchanged 更新和 contributionsByPath 清理迁入 WorkspaceIndexService
    - 验收点：index contribution tests 通过
    - 重要度：8/10
- [ ] `pickles-runtime`：迁移 index query 入口
    - 任务类型：执行任务
    - 依据文档：`docs/30-designs/RUNBOOK-RUNTIME-TREE-SITTER-HARDENING.md`
    - 范围对象：`pickles-runtime/src/java-index.ts`、`pickles-runtime/src/index.ts`、`pickles-runtime/test/**/*.ts`
    - 处理动作：将 javaFiles、findType、findTypesByAnnotation、findFilesByImport 收敛为 WorkspaceIndexService query
    - 验收点：`scripts/verify-runtime-sample-project.sh` 通过且 tree-sitter API 只在 JavaSyntaxParser 直接使用
    - 重要度：8/10
- [ ] `pickles-runtime`：稳定 parser diagnostic message
    - 任务类型：执行任务
    - 依据文档：`docs/30-designs/RUNBOOK-RUNTIME-TREE-SITTER-HARDENING.md`
    - 范围对象：`pickles-runtime/src/java-syntax-parser.ts`、`pickles-runtime/src/index.ts`、`pickles-runtime/test/**/*.ts`
    - 处理动作：让 parser diagnostic message 包含 error kind 和 node type
    - 验收点：diagnostic message tests 通过且 Problem aggregation 输出仍符合 Problem model
    - 重要度：7/10
- [ ] `pickles-runtime`：增加 parser diagnostic 去重和数量上限
    - 任务类型：执行任务
    - 依据文档：`docs/30-designs/RUNBOOK-RUNTIME-TREE-SITTER-HARDENING.md`
    - 范围对象：`pickles-runtime/src/java-syntax-parser.ts`、`pickles-runtime/test/**/*.ts`
    - 处理动作：为 parser diagnostic 增加稳定去重和数量上限
    - 验收点：diagnostic 去重和数量上限测试通过
    - 重要度：7/10
- [ ] `scripts`：明确 verify-intellij-plugin 的 Gradle 解析
    - 任务类型：执行任务
    - 依据文档：`docs/30-designs/RUNBOOK-RUNTIME-TREE-SITTER-HARDENING.md`
    - 范围对象：`scripts/verify-intellij-plugin.sh`
    - 处理动作：缺少系统 gradle 时输出包含安装要求或 GRADLE_CMD 用法的可行动错误
    - 验收点：缺少系统 gradle 时不再出现裸 command not found
    - 重要度：7/10
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
