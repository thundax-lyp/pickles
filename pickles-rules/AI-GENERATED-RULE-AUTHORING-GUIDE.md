# AI-Generated Rule Authoring Guide

## 1. Purpose

本文档定义 AI 生成 Pickles native rule 时必须遵守的作者侧约束。

目标是让 AI 能把用户的规则意图稳定生成到 Pickles runtime config、项目本地 rule module 或 rule package，并保证 Runtime 可以按 lint-style native rule 执行。

## 2. Scope

当前范围：

- rule intent 到 rule spec 的整理方式
- template 选择规则
- `defineRule({...})` 必填字段
- `RuleContext` 使用边界
- `ProblemInput` 输出边界
- config 内联规则与 `.pickles/rules/*` rule module 生成规则
- 生成物形态自检

不在范围内：

- 不执行 Pickles native rule
- 不判断目标工程是否违规
- 不直接解析 Java syntax tree
- 不生成 ArchUnit、ESLint 或 external adapter 规则
- 不生成 auto-fix 或 code modification 逻辑

## 3. Workflow

AI 生成规则固定按以下流程：

1. 将用户规则意图整理为 rule spec。
2. 在 file-level native rule、Java syntax query rule 和 workspace-level native rule 模板中选择一个。
3. 生成或更新 Pickles runtime config、`.pickles/rules/*` 或 rule package。
4. 对生成物做形态自检。
5. 将复检交给 Runtime、CLI、MCP 或 Plugin 的稳定检查入口。
6. 后续维护必须按 rule id 定位并收窄修改。

## 4. Rule Spec

生成 rule 前必须先明确：

- rule id
- rule title
- Agent-facing message
- optional fix hint
- problem type
- severity
- language
- file scope
- required Runtime query capability
- expected Problem location

Rule id 必须稳定，并适合在后续维护时定位同一条规则。

## 5. Template Selection

MVP 只使用以下模板类别：

- file-level native rule：检查单个文件或变动文件内容。
- Java syntax query rule：使用 `ctx.syntax.query(...)` 或 `ctx.java.query(...)` 查询 Java syntax DTO。
- workspace-level native rule：使用 Java workspace index 查询 type、annotation 或 import。

MVP 不生成 external adapter、ArchUnit、ESLint、TypeScript、Python、auto-fix 或 package publishing 模板。

## 6. Rule Placement

短规则可以直接写入 `pickles.config.ts`、`pickles.config.mjs` 或 `pickles.config.js`。

当规则需要独立 module 或辅助实现时，默认写入目标工程 `.pickles/rules/`。

`.pickles/rules/` 内部目录结构在 MVP 不继续细分。

`pickles.config.*` 必须显式 import `.pickles/rules/*` 中需要启用的 rule。

Runtime 不扫描 `.pickles/` 自动发现规则。

`.pickles/` 不得成为第二个规则真相源。

## 7. Rule Shape

Native rule 必须使用 `defineRule({...})`。

`defineRule({...})` 必须包含：

- `id`
- `title`
- `message`
- `type`
- `severity`
- `language`
- `files`
- `rule`

`fixHint` 和 `options` 可选。

`options` 必须可 JSON 序列化。

`rule(ctx)` 必须返回 `ProblemInput[]` 或 `Promise<ProblemInput[]>`。

示例：

```ts
import { defineRule } from "@pickles/runtime/config";

export const noControllerToRepository = defineRule({
    id: "java-no-controller-to-repository",
    title: "Controller must not depend on repository",
    message: "Controller classes must not import repository classes directly.",
    fixHint: "Move repository access behind a service layer and let the controller depend on the service.",
    type: "architecture",
    severity: "ERROR",
    language: "java",
    files: ["src/main/java/**/*.java"],
    rule(ctx) {
        return [];
    },
});
```

## 8. Runtime Context

Native rule 只能通过 `RuleContext` 和 JSON-serializable `options` 获取输入。

MVP 可使用：

- `ctx.files.changed(language?)`
- `ctx.files.byGlob(patterns)`
- `ctx.files.read(file)`
- `ctx.syntax.query(file, query)`
- `ctx.java.files()`
- `ctx.java.changedFiles()`
- `ctx.java.findType(qualifiedName)`
- `ctx.java.findTypesByAnnotation(annotationName)`
- `ctx.java.findFilesByImport(importTarget)`
- `ctx.java.query(file, query)`
- `ctx.problem(input)`

当 rule 的 `language` 为 `java` 时，Runtime 保证 `ctx.java` 可用。

Rule 不得直接使用 raw tree-sitter node、tree 或 parser object。

## 9. Problem Output

Rule author 必须通过 `ctx.problem(input)` 创建 ProblemInput。

`input.message` 必填。

可选字段：

- `title`
- `file`
- `position`
- `fixHint`

`ProblemInput` 不得覆盖 `severity`。

`ProblemInput` 不得覆盖 `source`。

Runtime 自动补齐 `source.tool` 和 `source.rule`。

示例：

```ts
return [
    ctx.problem({
        message: "OrderController imports OrderRepository directly.",
        file: file.path,
        position: match.range.start,
    }),
];
```

## 10. Side-Effect Rules

Native rule 固定为 lint-style 纯检查函数。

Native rule 和 helper 不得：

- 修改文件
- 创建目录
- 启动外部命令
- 访问网络
- 依赖 Agent、IDE、Hook 或 MCP 内部状态
- 直接使用 parser 原生对象
- 读取隐藏全局状态
- 保存跨 invocation 状态

Native rule 可以：

- import 纯函数 helper
- 使用当前 invocation 内的局部变量
- 读取 Runtime 通过 `RuleContext` 暴露的 DTO
- 返回 `ProblemInput[]`

## 11. Shape Self-Check

生成后必须检查：

- 是否使用 `defineRule({...})`
- 是否包含 `id`、`title`、`message`、`type`、`severity`、`language`、`files` 和 `rule`
- 是否通过 `ctx.problem(...)` 输出 ProblemInput
- `ctx.problem(...)` 是否提供 `message`
- 是否没有覆盖 `severity` 或 `source`
- 是否没有直接 import tree-sitter
- 是否没有使用文件写入、子进程、网络或跨 invocation 缓存
- 如果 rule 位于 `.pickles/rules/*`，`pickles.config.*` 是否显式 import 该 rule

形态自检不替代 Runtime 检测。

## 12. Maintenance

修改已有规则时必须优先按 rule id 定位。

维护时不得重写无关规则。

维护时必须保留用户手写逻辑，除非用户明确要求替换。

修改范围必须收窄到当前规则的 metadata、options、query、helper 或 rule body。
