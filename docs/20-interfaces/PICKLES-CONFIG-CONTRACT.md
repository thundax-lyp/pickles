# Pickles Runtime Config Contract

## 1. Purpose

本文档定义目标工程 Pickles runtime config 的 MVP 配置契约。

目标是让 IntelliJ Plugin、Runtime 和 Agent-side skills 使用同一个项目级配置真相源。

## 2. Scope

当前范围：

- runtime config 文件位置
- Agent-side skill 生成物位置
- MVP 固定入口
- 可编程配置字段
- 规则配置边界
- 运行时状态边界

不在范围内：

- 不定义 Plugin UI 的具体交互控件
- 不定义 `.pickles/server.json` 的完整 schema
- 不承载 AGENTS.md 注入块内容
- 不定义 Runtime 内部实现类型

## 3. Bounded Context

Pickles runtime config 固定放在被治理目标工程根目录。

目标工程指 IntelliJ IDEA 当前打开、且 Codex Agent 正在工作的用户项目。Pickles 仓库的 e2e 示例目标工程固定为 `e2e/sample-project/`。

Pickles runtime config 是可提交配置文件。

目标工程 `.pickles/` 目录固定承载 Pickles 项目本地文件，包括本地运行时状态文件和 Agent-side skill 生成的项目本地 rule module。运行时状态文件包括 `server.json`、hook state 和 cache。

Pickles runtime config 是配置真相源。IntelliJ Plugin、Runtime 和 Agent-side skills 都围绕该配置协作。

目标工程 `AGENTS.md` 注入块只承载 Agent 行为提示，不得成为规则真相源。

Agent-side skill 生成 native rule 时，默认将规则声明写入 `pickles.config.ts`、`pickles.config.mjs` 或 `pickles.config.js`。当规则需要独立 rule module 或辅助实现时，生成物默认放入目标工程 `.pickles/rules/`，并由 Pickles runtime config 显式 import。

Runtime 不得自动扫描 `.pickles/` 发现规则。`.pickles/` 不得成为第二个规则真相源。

## 4. Module Mapping

- `pickles-intellij-plugin/`: 展示和更新 Pickles runtime config。
- `pickles-runtime/`: 加载 Pickles runtime config 和 native rules。
- `pickles-hooks/`: 读取本地运行时状态。
- `pickles-rules/`: 提供 native rule authoring contract、规则说明书、规则模板和 plugin rule package。
- `pickles-skills/`: 基于规则说明书生成配置或提示 Agent 运行检查。
- `e2e/sample-project/`: 保存示例 `pickles.config.ts`。

## 5. Core Objects

### 5.1 PicklesRuntimeConfig

固定字段：

- `agent`
- `hook`
- `workspace`
- `rules`
- `problemBoard`

### 5.2 HookConfig

固定字段：

- `protocol`

### 5.3 RuleConfig

### 5.3 WorkspaceConfig

固定字段：

- `ignore`

### 5.4 RuleConfig

固定字段：

- `id`
- `title`
- `message`
- `fixHint`
- `type`
- `severity`

### 5.5 NativeRuleConfig

固定字段：

- `id`
- `title`
- `message`
- `fixHint`
- `type`
- `severity`
- `language`
- `files`
- `options`
- `rule`

### 5.6 ProblemInput

固定字段：

- `title`
- `message`
- `file`
- `position`
- `fixHint`

### 5.7 RuleContext

固定字段：

- `workspaceRoot`
- `changedFiles`
- `files`
- `syntax`
- `java`
- `problem`

### 5.8 SyntaxMatch

固定字段：

- `name`
- `kind`
- `text`
- `range`
- `captures`

### 5.9 SyntaxCapture

固定字段：

- `name`
- `kind`
- `text`
- `range`

### 5.10 ExternalAdapterRuleConfig

固定字段：

- `id`
- `title`
- `type`
- `severity`
- `adapter`
- `command`

### 5.11 ProblemBoardConfig

固定字段：

- `aggregation`

## 6. Global Constraints

- Pickles runtime config 文件名固定支持 `pickles.config.ts`、`pickles.config.mjs` 和 `pickles.config.js`。
- 加载优先级固定为 `pickles.config.ts`、`pickles.config.mjs`、`pickles.config.js`。
- `pickles.config.ts` 固定由 Runtime 自身依赖的 `tsx` 加载。
- 目标工程不需要安装 `tsx`。
- Runtime config、native rule 和由 Runtime 调用的 `.pickles/*` TypeScript 文件固定按 Node.js 22 + ESM 语义加载。
- Runtime 不依赖 Codex、Claude Code 或其他 Agent CLI 当前进程内的 Node.js 版本。
- Runtime MVP 不读取目标工程 `tsconfig` 的 path alias 作为 config 加载能力。
- `agent` 固定为 `codex`。
- `hook.protocol` 在 MVP 固定为 `http`。
- `problemBoard.aggregation` 在 MVP 固定为 `workspace`。
- Runtime config 必须可提交到用户工程仓库。
- Runtime config 不得包含本机端口、进程号、server URL、临时绝对路径或 token。
- `workspace` 固定为可选对象。
- `workspace.ignore` 固定为可选 string array。
- `workspace.ignore` 缺省值固定为空 array。
- `workspace.ignore` pattern 固定使用 repo-relative 路径语义。
- `workspace.ignore` 固定由 Runtime 解析和应用。
- IntelliJ Plugin 不得直接解析 `pickles.config.ts` 来读取 `workspace.ignore`。
- IntelliJ Plugin 可以在扫描文件系统时应用 `.gitignore` 和内置目录兜底过滤。
- 本地 HTTP 端口固定写入目标工程 `<repo>/.pickles/server.json`。
- Agent-side skill 生成的项目本地 native rule module 与辅助实现默认放入目标工程 `<repo>/.pickles/rules/`。
- `.pickles/rules/` 内部目录结构在 MVP 不继续细分。
- `.pickles/rules/*` 中需要启用的 rule 必须由 Runtime config 显式 import。
- Runtime 不得自动扫描 `.pickles/` 发现 native rule 或检测脚本。
- `rules` 固定使用数组。
- Rule plugin package 固定通过标准 ESM import 引用。
- Rule plugin package 必须导出 `defineRule({...})` 返回值，或由这些 rule 组成的数组。
- Runtime 只读取 Pickles runtime config 最终导出的 `rules` 数组。
- Runtime 不得自动发现 rule package。
- Runtime 不得读取 rule package metadata 作为规则入口。
- Runtime MVP 不支持字符串式 rule package 引用。
- Runtime MVP 不支持 `plugins` 字段自动展开规则。
- Native rules 固定由 Runtime 执行。
- Runtime config 必须使用 `export default defineConfig({...})`。
- Native rule 必须使用 `defineRule({...})`。
- `defineConfig` 和 `defineRule` 固定由 `@pickles/runtime/config` 提供。
- Rule function 固定返回 `ProblemInput[]` 或 `Promise<ProblemInput[]>`。
- Rule author 调用 `ctx.problem(input)` 时，`input.message` 固定必填。
- `ProblemInput.title` 固定可选，未提供时使用 native rule 的 `title`。
- `ProblemInput.file` 固定为 string 或 `null`。
- `ProblemInput.position` 固定为 `Position` 或 `null`。
- `ProblemInput.fixHint` 固定为 string 或 `null`，未提供时使用 native rule 的 `fixHint`。
- `ProblemInput` 不得覆盖 `severity`。
- `ProblemInput` 不得覆盖 `source`。
- `RuleContext` 不暴露 raw tree-sitter node。
- Parser-specific query helper 固定返回 Pickles-owned DTO。
- Syntax query 结果固定使用 `SyntaxMatch`。
- `SyntaxMatch.captures` 固定使用 `SyntaxCapture[]`。

## 7. Functional Requirements

### 7.1 Minimal Config

MVP 最小配置固定为：

```ts
import { defineConfig } from "@pickles/runtime/config";

export default defineConfig({
    agent: "codex",
    hook: {
        protocol: "http",
    },
    workspace: {
        ignore: [],
    },
    rules: [],
    problemBoard: {
        aggregation: "workspace",
    },
});
```

### 7.2 Workspace Config

`workspace` 固定表达 Runtime workspace 输入处理策略。

`workspace.ignore` 固定表达 Pickles 专属忽略规则。该字段只影响 Runtime 对 changed files 的过滤，不影响 Hook 文件捕获、Plugin 文件扫描和用户项目文件。

`workspace.ignore` 支持以下 MVP pattern：

- `path/to/file.java`：匹配 repo-relative 文件路径。
- `generated/`：匹配 repo-relative 目录前缀。
- `*.generated.java`：匹配文件名后缀。
- `src/**/Generated.java`：匹配 repo-relative glob。

`workspace.ignore` 不支持 `!` negate pattern。需要重新纳入的文件必须通过收窄 ignore pattern 实现。

Runtime 应用 `workspace.ignore` 的固定时机：

1. Runtime 加载 Pickles runtime config。
2. Runtime 校验 `workspace.ignore`。
3. Runtime 在执行 parser 和 native rules 前过滤 changed files。
4. Runtime 只对过滤后的 changed files 执行 parser diagnostic 和 native rule。

IntelliJ Plugin Reindex 的固定边界：

- Plugin 可以读取 `.gitignore` 和使用内置目录兜底忽略减少扫描输入。
- Plugin 不读取 `workspace.ignore`。
- Plugin 不解析、执行或 import `pickles.config.ts`。
- Plugin 将候选 changed files 交给 Runtime 后，由 Runtime 统一应用 `workspace.ignore`。

### 7.3 Native Rule Config

Native rule config 必须能表达 JS / TS 可编程规则。

Native rule 必须通过 Runtime 提供的 `RuleContext` 读取输入。

Native rule 必须使用 `defineRule({...})` 定义。

Native rule 的 `id`、`title`、`message`、`type`、`severity`、`language`、`files` 和 `rule` 固定必填。

Native rule 的 `fixHint` 和 `options` 固定可选。

Native rule 的 `message` 固定作为 Agent-facing 默认规则反馈。

Native rule 的 `fixHint` 固定作为 Agent-facing 默认修复建议。

Native rule 的 options 必须可 JSON 序列化。

Native rule 固定为 lint-style 纯检查函数。

Native rule 只能通过 `RuleContext` 和 JSON-serializable `options` 获取输入。

Native rule 的 `rule(ctx)` 必须返回 `ProblemInput[]` 或 `Promise<ProblemInput[]>`。

Native rule helper 必须遵守同样的无副作用约束。

Native rule 不得修改文件、创建目录、启动外部命令、访问网络、依赖 Agent / IDE / Hook / MCP 内部状态、直接使用 parser 原生对象、读取隐藏全局状态或保存跨 invocation 状态。

Native rule 的输出必须能归一化为 Problem。

`RuleContext` 固定分为通用层和 language-specific helper。

通用层必须提供文件查询、受控 syntax query 和 problem factory。

Language-specific helper 按语言扩展。MVP 固定只实现 Java helper。

Native rule 的 `language` 字段必须为 string。Runtime MVP 只支持 `java`，遇到未支持 language 必须返回配置错误。

`RuleContext` 必须至少提供：

- `workspaceRoot`
- `changedFiles`
- `files.changed(language?)`
- `files.byGlob(patterns)`
- `files.read(file)`
- `syntax.query(file, query)`
- `java.files()`
- `java.changedFiles()`
- `java.findType(qualifiedName)`
- `java.findTypesByAnnotation(annotationName)`
- `java.findFilesByImport(importTarget)`
- `java.query(file, query)`
- `problem(input)`

当 native rule 的 `language` 为 `java` 时，Runtime 必须保证 `ctx.java` 可用。

`syntax.query(file, query)` 和 `java.query(file, query)` 必须返回 `SyntaxMatch[]`。

`SyntaxMatch.captures` 必须保留 tree-sitter query capture 信息。

`SyntaxCapture` 必须由 Runtime 创建，不得暴露 raw tree-sitter node。

`SyntaxMatch` 必须由 Runtime 创建，不得暴露 raw tree-sitter node。

Native rule 示例：

```ts
import { defineConfig, defineRule } from "@pickles/runtime/config";

const noControllerToRepository = defineRule({
    id: "java-no-controller-to-repository",
    title: "Controller must not depend on repository",
    message: "Controller classes must not import repository classes directly.",
    fixHint:
        "Move repository access behind a service layer and let the controller depend on the service.",
    type: "architecture",
    severity: "ERROR",
    language: "java",
    files: ["src/main/java/**/*.java"],
    rule(ctx) {
        return [
            ctx.problem({
                message: "Controller imports repository directly.",
                file: null,
                position: null,
            }),
        ];
    },
});

export default defineConfig({
    agent: "codex",
    hook: {
        protocol: "http",
    },
    rules: [noControllerToRepository],
    problemBoard: {
        aggregation: "workspace",
    },
});
```

Rule plugin package 示例：

```ts
import { defineConfig } from "@pickles/runtime/config";
import { javaArchitectureRules } from "@pickles/rules-java-architecture";

export default defineConfig({
    agent: "codex",
    hook: {
        protocol: "http",
    },
    rules: [...javaArchitectureRules],
    problemBoard: {
        aggregation: "workspace",
    },
});
```

### 7.4 Agent-Side Skill Generated Files

`pickles-rule-authoring-skill` 生成规则时必须遵守以下边界：

- Native rule 声明默认写入 Pickles runtime config。
- 当 native rule 需要独立 module 或辅助实现时，默认写入目标工程 `.pickles/rules/`。
- `.pickles/rules/` 内部目录结构在 MVP 不继续细分。
- Pickles runtime config 必须显式 import `.pickles/rules/*` 中需要启用的 rule。
- Skill 不得要求 Runtime 加载 skill。
- Skill 不得要求 Runtime 扫描 `.pickles/`。

### 7.5 Config Read Flow

1. IntelliJ Plugin 定位目标工程根目录。
2. IntelliJ Plugin 展示 Pickles runtime config。
3. Runtime 加载 Pickles runtime config。
4. Codex Hook 读取本地运行时状态。
5. Agent-side skills 基于 Pickles runtime config 提示规则创作或规则检查。

### 7.6 Config Update Flow

1. Plugin 配置界面展示当前 Pickles runtime config。
2. 用户修改配置。
3. Plugin 写回 Pickles runtime config。
4. Runtime 下一次检测使用更新后的配置。

## 8. Key Flows

### 8.1 Native Rule Flow

1. Runtime 加载 Pickles runtime config。
2. Runtime 加载 native rule。
3. Runtime 构造 `RuleContext`。
4. Runtime 执行 native rule。
5. Runtime 将 rule result 归一化为 Problem。

## 9. Non-Functional Requirements

- 配置读取失败时，Plugin 必须在 Problem Board 或配置界面展示可理解错误。
- Runtime config 必须适合 AI 生成和维护。
- Runtime config 必须适合 rule package 复用。
- Runtime config 必须支持 TypeScript 类型提示。

## 10. Open Items

无
