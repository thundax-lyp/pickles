# Runtime Tree-Sitter Parser Runbook

## 1. Purpose

本 RUNBOOK 用于将 `pickles-runtime/` 从当前正则 Java 解析实现收敛到文档要求的 tree-sitter Java parser、Java syntax index、parser diagnostic 和验证闭环。

目标是按小步能力变化完成 Runtime parser 能力，不在同一提交中混入 IntelliJ Plugin、Hook、MCP、rules 或 skills 的无关改动。

## 2. Scope

范围内：

- `pickles-runtime/package.json`
- `pickles-runtime/package-lock.json`
- `pickles-runtime/src/**/*.ts`
- `pickles-runtime/test/**/*.ts`
- Runtime 相关验证脚本在确有需要时的最小调整
- 与 Runtime parser 行为直接对应的文档同步
- 根目录 `TODO.md` 中本 RUNBOOK 对应任务的删除、收窄或清理

范围外：

- IntelliJ Plugin UI、Tool Window 和 HTTP contract 改造
- Hook payload、Hook event 和 hook 安装流程改造
- MCP server、tool 和 resource 实现
- `pickles-rules/` 规则包结构改造
- `pickles-skills/` skill 内容实现
- 业务样例工程非验证必需的功能改造

## 3. Bounded Context

Runtime 是规则加载、解析、执行和本地治理运行时能力的所有者。

`JavaSyntaxParser` 固定拥有 tree-sitter API 适配职责。Runtime 其他层只消费 Pickles-owned DTO，不保存、返回或暴露 raw tree-sitter node、tree 或 parser object。

Workspace Index 固定保存进程内文件级索引。MVP 按文件重建 syntax index，不使用 tree-sitter incremental edit API。

Native rule execution 继续通过 `RuleContext` 读取 index、config 和 changed files。tree-sitter 接入不得改变 Plugin、Hook 或 MCP 的稳定契约。

## 4. Module Mapping

- `pickles-runtime/src/config.ts`: 保持 config helper 入口稳定。
- `pickles-runtime/src/types.ts`: 承载 Runtime DTO、Problem、Java syntax DTO 和 parser diagnostic 字段。
- `pickles-runtime/src/java-index.ts`: 收敛为基于 parser DTO 的 Java index 和 query 能力。
- `pickles-runtime/src/index.ts`: 负责 Runtime 检测编排、config 加载、native rule execution 和 Problem 聚合。
- `pickles-runtime/src/stdio.ts`: 保持 stdio JSON contract 稳定。
- `pickles-runtime/test/`: 承载 parser、index、runtime execution 和 stdio 回归测试。

## 5. Core Objects

- `JavaSyntaxParser`: tree-sitter Java parser 适配层。
- `JavaSyntaxFile`: Runtime 自有 Java 文件语法 DTO。
- `JavaTypeDeclaration`: Runtime 自有 Java 类型声明 DTO。
- `JavaMethodDeclaration`: Runtime 自有 Java 方法或构造器 DTO。
- `JavaFieldDeclaration`: Runtime 自有 Java 字段 DTO。
- `ParserDiagnostic`: parser error 转换后的 Runtime diagnostic。
- `WorkspaceIndexService`: 进程内文件级 workspace index。
- `RuleContext`: native rule 访问 index、config 和 changed files 的稳定入口。

## 6. Global Constraints

- Runtime 固定运行在 Node.js 22。
- Runtime 固定使用 TypeScript strict mode 和 ESM。
- Runtime parser 依赖固定使用 `tree-sitter` 与 `tree-sitter-java`。
- Runtime 必须通过 lockfile 固定 npm 依赖版本。
- Runtime 不向 Plugin、Hook、rules、MCP 或 stdio response 暴露 tree-sitter 原生对象。
- Runtime 不执行 Java source code。
- Runtime 不修改业务代码、测试代码或目标工程实现代码。
- Runtime stdout 固定只输出 stdio JSON response。
- Runtime stderr 固定用于日志。
- Java syntax range 固定使用 1-based line 和 1-based column。
- 单文件解析输入上限固定为 2 MiB。
- 单批次 changed files 上限固定为 200。

## 7. Execution Order

### 7.1 Feat 1: 增加 Parser 依赖装配

目标：

- 增加 `tree-sitter` 与 `tree-sitter-java` 依赖。
- 保持 Runtime package 的 TypeScript 与测试入口可运行。
- 不引入 parser 行为变化。

固定动作：

1. 更新 `pickles-runtime/package.json` 和 `pickles-runtime/package-lock.json`。
2. 确认依赖版本由 lockfile 固定。
3. 不修改 Runtime parser、index 或 rule execution 行为。

验收：

- `npm run typecheck` 通过。
- `npm run test` 通过。

### 7.2 Feat 2: 建立 JavaSyntaxParser 边界与 DTO

目标：

- 新增 `JavaSyntaxParser` 适配层。
- 定义 Runtime 自有 Java syntax DTO。
- 固定 tree-sitter API 只允许在 parser adapter 层直接使用。

固定动作：

1. 新增 `JavaSyntaxParser` 文件。
2. 在 `pickles-runtime/src/types.ts` 中新增或补齐 parser DTO。
3. 增加最小 parser construction smoke test。
4. 不替换 `java-index.ts` 当前行为。

验收：

- `npm run typecheck` 通过。
- 最小 parser smoke test 通过。
- 仓库内除 `JavaSyntaxParser` 外没有新增 tree-sitter API 直接使用点。

### 7.3 Feat 3: 解析 Java Package、Import 和 Type

目标：

- `JavaSyntaxParser` 输出 package declaration、import declaration 和 top-level type declaration。
- 覆盖 class、interface、enum、annotation 和 record。
- 保持 index 仍可暂时使用旧实现。

固定动作：

1. 实现 package extraction。
2. 实现 import extraction。
3. 实现 top-level type extraction。
4. 增加 parser 测试覆盖普通 class、interface、enum、annotation 和 record。

验收：

- `npm run typecheck` 通过。
- parser tests 覆盖 package、import 和 top-level type。
- 当前 sample project runtime tests 继续通过。

### 7.4 Feat 4: 解析 Java Member、Annotation 和 Range

目标：

- `JavaSyntaxParser` 输出 method、constructor、field、annotation、modifiers 和 source range。
- Runtime 内部 range 固定使用 1-based line 和 1-based column。

固定动作：

1. 实现 method extraction。
2. 实现 constructor extraction。
3. 实现 field extraction。
4. 实现 annotation 与 modifier extraction。
5. 实现 parser DTO source range。
6. 增加 parser 测试覆盖 nested type、annotated method、constructor、field 和 range。

验收：

- `npm run typecheck` 通过。
- parser tests 覆盖 member、annotation、modifier 和 1-based range。

### 7.5 Feat 5: 将 Java Index 接到 Parser DTO

目标：

- `createJavaIndex` 基于 `JavaSyntaxParser` 输出更新 index。
- 保持现有 native rule query 能力稳定。
- 删除 Java package、import、type 提取中的正则解析职责。

固定动作：

1. 调整 `pickles-runtime/src/java-index.ts`，让 Java index 消费 parser DTO。
2. 实现或补齐按 path、qualified type、annotation、import target 的 query。
3. 保持 native rule context 入口兼容现有 sample rule。
4. 删除或停止使用 Java index 内部 package、import、type 正则解析。

验收：

- `npm run typecheck` 通过。
- `npm run test` 中 sample project native rule 测试通过。
- `scripts/verify-runtime-sample-project.sh` 通过。

### 7.6 Feat 6: 实现 Index 增删改贡献清理

目标：

- Workspace Index 正确处理 added、modified、deleted 和 unchanged。
- `contributionsByPath` 能清理旧 qualified type、annotation 和 import 索引 key。

固定动作：

1. 补齐 index 更新路径。
2. 实现 modified 前清理旧贡献。
3. 实现 deleted 清理旧贡献并删除 index 项。
4. 实现 unchanged 不更新 index。
5. 增加 index tests 覆盖 added、modified、deleted 和 unchanged。

验收：

- index tests 覆盖四种 change type。
- 重复修改同一文件不会留下旧反向索引。
- `npm run test` 通过。

### 7.7 Feat 7: 增加 Parser Diagnostic

目标：

- tree-sitter syntax error 转换为 parser diagnostic。
- 单文件 parse failure 不阻断整批检测。
- Parser diagnostic 能进入 Problem aggregation。

固定动作：

1. 定义或补齐 `ParserDiagnostic` 到 `Problem` 的转换。
2. 将 Java parser diagnostic 的 `source.tool` 固定为 `tree-sitter-java`。
3. 将 parser diagnostic severity 固定为 `WARN`。
4. 将 parser diagnostic fixHint 固定为 `null`。
5. 增加 syntax error 与同批其他文件继续处理测试。

验收：

- 单个 Java 文件 parser error 返回可展示 Problem 或 diagnostic。
- 同一批次其他文件继续处理。
- Problem aggregation tests 覆盖 parser diagnostic。

### 7.8 Feat 8: 增加 Runtime 输入限制

目标：

- 实现 2 MiB 单文件限制。
- 实现 200 changed files 批次限制。
- 错误输出稳定且可解释。

固定动作：

1. 在 Runtime detection 入口实施单文件大小限制。
2. 在 Runtime detection 入口实施 changed files 批次数量限制。
3. 增加超大文件测试。
4. 增加超批次测试。

验收：

- 超过固定输入限制时 Runtime 返回可解释错误或 Problem。
- `npm run test` 通过。

### 7.9 Test: 补齐 Runtime 分层测试和 Full-Flow 回归

目标：

- Runtime parser、index、native rule、Problem aggregation 分层测试闭环。
- full-flow 保持 Hook、Plugin contract stub 和 Runtime 三者边界不变。

固定动作：

1. 检查 parser tests 是否覆盖 `RUNTIME-DESIGN.md` 指定 Java 结构。
2. 检查 index tests 是否覆盖新增、修改、删除。
3. 检查 native rule tests 是否覆盖可编程规则加载、执行和 Problem 归一化。
4. 检查 Problem aggregation tests 是否覆盖 parser diagnostic。
5. 运行 Runtime 和 full-flow 验证。

验收：

- `scripts/verify-runtime-sample-project.sh` 通过。
- `scripts/verify-full-flow.sh` 通过。
- Runtime parser 变更没有要求 Plugin 或 Hook contract 变化。

### 7.10 Closure: 文档、TODO 和 RUNBOOK 收口

目标：

- 本 RUNBOOK 完成后不作为长期文档保留。
- 对应 TODO 完成后删除或收窄。
- 文档与实现口径一致。

固定动作：

1. 检查 `docs/10-requirements/RUNTIME-REQUIREMENTS.md` 是否仍与实现一致。
2. 检查 `docs/30-designs/RUNTIME-DESIGN.md` 是否仍与实现一致。
3. 检查 `docs/40-readiness/E2E-TEST-CASES.md` 是否需要同步测试覆盖状态。
4. 删除本 RUNBOOK 或将未完成范围收窄为新的待审阅任务。
5. 清理 `TODO.md` 中已经完成的任务项。

验收：

- 已完成任务不留在 `TODO.md`。
- 已完成 RUNBOOK 不留在 `docs/30-designs/`。
- `git status --short` 只显示本任务相关改动。

## 8. Verification

每个 feat 的最小验证：

- 在 `pickles-runtime/` 运行 `npm run typecheck`。
- 在 `pickles-runtime/` 运行 `npm run test`。

Runtime parser 阶段收口验证：

- 在仓库根目录运行 `scripts/verify-runtime-sample-project.sh`。
- 在仓库根目录运行 `scripts/verify-full-flow.sh`。

PR 前验证：

- 在仓库根目录运行 `npm run format:check`。
- 在仓库根目录运行 `scripts/verify-all.sh`。

本地如果缺少 `gradle`，`scripts/verify-all.sh` 会在 IntelliJ Plugin 验证阶段失败。该环境问题不得被记录为 Runtime parser 实现失败；PR 前必须在具备 Gradle 8.13+ 与 JDK 17 的环境中补跑统一验证。

## 9. Commit Plan

提交固定按工程判断拆分：

- `Feat(runtime): 增加 parser 依赖装配`
- `Feat(runtime): 建立 Java 语法解析边界`
- `Feat(runtime): 解析 Java 包导入和类型`
- `Feat(runtime): 解析 Java 成员和源码范围`
- `Feat(runtime): 使用语法解析结果更新 Java 索引`
- `Feat(runtime): 实现 Java 索引贡献清理`
- `Feat(runtime): 增加解析诊断`
- `Feat(runtime): 增加运行时输入限制`
- `Test(runtime): 补齐语法索引回归验证`
- `Docs(runtime): 收口 tree-sitter 解析任务`

提交前必须确认没有混入与当前 feat 无关的文件。

## 10. Open Items

无
