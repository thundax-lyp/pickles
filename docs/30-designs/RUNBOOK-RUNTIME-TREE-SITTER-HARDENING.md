# Runtime Tree-Sitter Hardening Runbook

## 1. Purpose

本 RUNBOOK 用于继续打磨 `pickles-runtime/` 的 tree-sitter Java parser 和 Java syntax index。

目标是补齐当前 parser 主线完成后仍存在的能力缺口：`extends / implements` 解析、Java parser fixture 覆盖、明确的 `WorkspaceIndexService`、parser diagnostic 可读性、本地统一验证入口，以及最终文档和 TODO 收口。

## 2. Scope

范围内：

- `pickles-runtime/src/**/*.ts`
- `pickles-runtime/test/**/*.ts`
- `pickles-runtime/package.json`
- `pickles-runtime/package-lock.json`
- `scripts/verify-*.sh`
- `scripts/verify-all.sh`
- `docs/10-requirements/RUNTIME-REQUIREMENTS.md`
- `docs/30-designs/RUNTIME-DESIGN.md`
- `docs/40-readiness/E2E-TEST-CASES.md`
- 根目录 `TODO.md`

范围外：

- IntelliJ Plugin UI、Tool Window 和 HTTP contract 行为改造
- Hook payload、Hook event 和 hook 安装流程改造
- MCP server、tool 和 resource 实现
- `pickles-rules/` 规则包结构改造
- `pickles-skills/` skill 内容实现
- 目标工程业务代码功能改造

## 3. Bounded Context

Runtime 继续拥有 Java syntax parse、workspace index、native rule execution 和 Problem aggregation。

`JavaSyntaxParser` 固定是 tree-sitter API 的唯一直接使用点。其他 Runtime 层只能消费 Runtime 自有 DTO。

`WorkspaceIndexService` 固定是 Java index 更新、贡献清理和 query 的所有者。Native rule 继续通过 `RuleContext` 访问 index，不直接读取 parser 内部对象。

验证脚本只负责验证，不修改代码格式、不修复实现、不依赖用户全局 shell 状态。

## 4. Module Mapping

- `pickles-runtime/src/types.ts`: 承载 Java syntax DTO、extends / implements 字段、diagnostic 字段。
- `pickles-runtime/src/java-syntax-parser.ts`: 承载 tree-sitter Java parser 适配和 Java syntax DTO 构建。
- `pickles-runtime/src/java-index.ts`: 收敛为 `WorkspaceIndexService` 或围绕该 service 的公开 query facade。
- `pickles-runtime/src/index.ts`: Runtime detection 编排、native rule execution、Problem aggregation。
- `pickles-runtime/test/`: parser fixture、index service、runtime aggregation 和 stdio 回归测试。
- `scripts/`: 总体验证入口和模块验证入口。

## 5. Core Objects

- `JavaSyntaxParser`
- `JavaSyntaxFile`
- `JavaTypeDeclaration`
- `JavaMethodDeclaration`
- `JavaFieldDeclaration`
- `ParserDiagnostic`
- `WorkspaceIndexService`
- `RuleContext`

## 6. Global Constraints

- Runtime 固定运行在 Node.js 22。
- Runtime 固定使用 TypeScript strict mode 和 ESM。
- Runtime 不向 Plugin、Hook、rules、MCP 或 stdio response 暴露 tree-sitter 原生对象。
- Runtime 不执行 Java source code。
- Runtime 不修改目标工程业务代码。
- Java syntax range 固定使用 1-based line 和 1-based column。
- 单文件解析输入上限固定为 2 MiB。
- 单批次 changed files 上限固定为 200。
- 新增验证脚本逻辑不得引入隐藏全局依赖。

## 7. Execution Order

### 7.1 Feat 1: 扩展 Java Type 继承 DTO

目标：

- `JavaTypeDeclaration` 增加 `extendsTypes` 和 `implementsTypes`。
- DTO 字段只保存源码声明名称，不做类型推断或跨文件解析。
- 当前 parser 行为保持不变。

固定动作：

1. 扩展 Runtime Java type DTO。
2. 调整现有测试期望，使新增字段的默认值稳定。
3. 不实现 parser extraction。

验收：

- `npm run typecheck` 通过。
- `npm run test` 通过。
- Runtime public DTO 仍可 JSON 序列化。

### 7.2 Feat 2: 解析 Class Extends 和 Implements

目标：

- `JavaSyntaxParser` 从 `class_declaration` 提取 superclass 和 interfaces。
- `extendsTypes` 包含 class extends 源码名称。
- `implementsTypes` 包含 class implements 源码名称。

固定动作：

1. 查看 tree-sitter Java class inheritance 节点形态。
2. 实现 class extends extraction。
3. 实现 class implements extraction。
4. 增加 parser tests 覆盖 class extends 和 class implements。

验收：

- `npm run test` 通过。
- class inheritance parser tests 通过。

### 7.3 Feat 3: 解析 Interface Extends 和 Record Implements

目标：

- `JavaSyntaxParser` 从 `interface_declaration` 提取 extended interfaces。
- `JavaSyntaxParser` 从 `record_declaration` 提取 implemented interfaces。
- 不解析继承图，不解析泛型实参语义。

固定动作：

1. 查看 tree-sitter Java interface 和 record inheritance 节点形态。
2. 实现 interface extends extraction。
3. 实现 record implements extraction。
4. 增加 parser tests 覆盖 interface extends 和 record implements。

验收：

- `npm run test` 通过。
- interface / record inheritance parser tests 通过。

### 7.4 Test 4: 覆盖 Import 变体

目标：

- 锁住 static import 与 wildcard import 的 DTO 输出。
- 不改变 import query 的语义，只保证 parser 输出完整源码名称。

固定动作：

1. 增加 normal import、static import、wildcard import fixture。
2. 修正 `parseImports` 对 static 和 wildcard 的名称输出。
3. 确认 sample native rule 行为不变。

验收：

- import fixture tests 通过。
- `scripts/verify-runtime-sample-project.sh` 通过。

### 7.5 Test 5: 覆盖泛型 Type 和 Method

目标：

- parser 能在泛型声明存在时稳定提取 type 和 method 名称。
- 不承诺解析泛型边界或类型参数语义。

固定动作：

1. 增加 generic class fixture。
2. 增加 generic method fixture。
3. 修正 parser 在泛型节点存在时的 name extraction。

验收：

- 泛型 fixture tests 通过。
- `npm run test` 通过。

### 7.6 Test 6: 覆盖 Field 和 Annotation 变体

目标：

- parser 能处理多变量 field、qualified annotation 和 package-private member。
- annotation 名称保持源码声明形态。

固定动作：

1. 增加多变量 field fixture。
2. 增加 qualified annotation fixture。
3. 增加 package-private type 和 member fixture。
4. 修正 field / annotation / modifier extraction。

验收：

- field 和 annotation fixture tests 通过。
- `npm run test` 通过。

### 7.7 Refactor 7: 建立 WorkspaceIndexService 边界

目标：

- 新增 `WorkspaceIndexService` 类型或 class。
- 先建立 service 边界，不改变当前 query 行为。

固定动作：

1. 创建 `WorkspaceIndexService`。
2. 将 index map 初始化逻辑迁入 service。
3. 保留 `createJavaIndex` 兼容 facade。
4. 保持现有 tests 通过。

验收：

- `npm run typecheck` 通过。
- `npm run test` 通过。
- `RuleContext.java.*` 行为不变。

### 7.8 Refactor 8: 将 Index 更新和贡献清理迁入 Service

目标：

- added、modified、deleted、unchanged 更新逻辑归属 `WorkspaceIndexService`。
- `contributionsByPath` 只作为 service 内部状态。

固定动作：

1. 将 `addJavaFile` 迁入 service。
2. 将 `removePathContributions` 迁入 service。
3. 将 change type 分派迁入 service。
4. 保持 `createJavaIndex` facade 稳定。

验收：

- `npm run typecheck` 通过。
- `npm run test` 通过。
- index contribution tests 通过。

### 7.9 Refactor 9: 将 Index Query 迁入 Service

目标：

- Java index query 由 `WorkspaceIndexService` 提供。
- 现有 exported query function 保持兼容或收敛为 service method 调用。

固定动作：

1. 将 `javaFiles` 迁入 service。
2. 将 `findType` 迁入 service。
3. 将 `findTypesByAnnotation` 迁入 service。
4. 将 `findFilesByImport` 迁入 service。
5. 运行 runtime sample 验证。

验收：

- `scripts/verify-runtime-sample-project.sh` 通过。
- tree-sitter API 仍只在 `JavaSyntaxParser` 直接使用。

### 7.10 Feat 10: 稳定 Parser Diagnostic Message

目标：

- parser diagnostic message 更稳定、更可读。
- message 包含 error kind 和 node type。
- 不引入源码片段字段。

固定动作：

1. 为 parser diagnostic 增加稳定 message 格式，包含 node type 或 missing type。
2. 增加 parser diagnostic message tests。
3. 确认 Problem aggregation 输出仍符合 Problem model。

验收：

- syntax error diagnostic 可读且稳定。
- `npm run test` 通过。

### 7.11 Feat 11: 增加 Parser Diagnostic 去重和数量上限

目标：

- diagnostic 数量有固定上限，避免异常文件刷屏。
- 重复 diagnostic 被稳定去重。

固定动作：

1. 定义 diagnostic 数量上限。
2. 实现 diagnostic 去重。
3. 增加数量上限测试。
4. 增加重复 diagnostic 测试。

验收：

- diagnostic 数量上限测试通过。
- diagnostic 去重测试通过。
- `npm run test` 通过。

### 7.12 Fix 12: 明确 Verify-Intellij-Plugin 的 Gradle 解析

目标：

- 缺少系统 `gradle` 时，`scripts/verify-intellij-plugin.sh` 输出可行动错误。
- 不在本切片引入 wrapper。

固定动作：

1. 在 `scripts/verify-intellij-plugin.sh` 中显式检测 `GRADLE_CMD` 或 `gradle`。
2. 缺失时输出包含安装要求或 `GRADLE_CMD` 用法的错误。
3. 保持 CI 中 setup-gradle 行为不变。

验收：

- 缺少系统 `gradle` 时不再出现裸 `command not found`。
- Runtime 和 full-flow 验证继续通过。

### 7.13 Fix 13: 提供可重复本地 Gradle 执行路径

目标：

- `scripts/verify-all.sh` 在本地具备可重复执行路径。
- IntelliJ Plugin 验证不依赖不可发现的系统 `gradle`。

固定动作：

1. 判断采用 Gradle wrapper 还是验证脚本 fallback。
2. 若采用 wrapper，新增 wrapper 文件并调整脚本优先使用 wrapper。
3. 若采用固定 fallback，记录为什么不新增 wrapper。
4. 更新相关文档中的本地验证说明。

验收：

- 本地 IntelliJ Plugin 验证路径可重复执行。
- 可运行环境中 `scripts/verify-all.sh` 通过。

### 7.14 Docs 14: 同步验证和 Runtime 文档口径

目标：

- Runtime parser hardening 实现与文档一致。
- 验证入口文档与脚本行为一致。

固定动作：

1. 检查 `RUNTIME-REQUIREMENTS.md`。
2. 检查 `RUNTIME-DESIGN.md`。
3. 检查 `E2E-TEST-CASES.md`。
4. 检查 `PR-WORKFLOW.md`。

验收：

- 文档没有保留过期实现口径。
- 文档没有新增未实现承诺。

### 7.15 Closure: TODO 和 RUNBOOK 收口

目标：

- 本 RUNBOOK 完成后不作为长期文档保留。
- 对应 TODO 完成后删除或收窄。
- 文档与实现口径一致。

固定动作：

1. 删除本 RUNBOOK 或将未完成范围收窄为新的待审阅任务。
2. 清理 `TODO.md` 中已经完成的任务项。
3. 记录最终验证结果。

验收：

- 已完成任务不留在 `TODO.md`。
- 已完成 RUNBOOK 不留在 `docs/30-designs/`。
- `git status --short` 只显示本任务相关改动。

## 8. Verification

每个 Runtime feat 的最小验证：

- 在 `pickles-runtime/` 运行 `npm run typecheck`。
- 在 `pickles-runtime/` 运行 `npm run test`。

Runtime 阶段收口验证：

- 在仓库根目录运行 `scripts/verify-runtime-sample-project.sh`。
- 在仓库根目录运行 `scripts/verify-full-flow.sh`。

验证脚本阶段收口验证：

- 在仓库根目录运行 `scripts/verify-all.sh`。

本机如果缺少 IntelliJ Plugin 所需 JDK、Gradle 或 wrapper 条件，必须在最终说明中明确失败命令、失败原因和补跑条件。

## 9. Commit Plan

提交固定按工程判断拆分：

- `Feat(runtime): 扩展 Java 继承声明模型`
- `Feat(runtime): 解析 Java 继承和接口声明`
- `Feat(runtime): 解析 Java 接口和记录继承声明`
- `Test(runtime): 覆盖 Java 导入语法变体`
- `Test(runtime): 覆盖 Java 泛型语法样例`
- `Test(runtime): 覆盖 Java 字段和注解变体`
- `Refactor(runtime): 建立工作区索引服务边界`
- `Refactor(runtime): 收敛索引更新和贡献清理`
- `Refactor(runtime): 收敛索引查询入口`
- `Feat(runtime): 稳定解析诊断消息`
- `Feat(runtime): 限制解析诊断数量`
- `Fix(scripts): 明确 Gradle 验证入口错误`
- `Fix(scripts): 提供本地 Gradle 验证路径`
- `Docs(runtime): 同步 tree-sitter 强化文档`
- `Docs(runtime): 收口 tree-sitter 强化任务`

提交前必须确认没有混入与当前 feat 无关的文件。

## 10. Open Items

无
