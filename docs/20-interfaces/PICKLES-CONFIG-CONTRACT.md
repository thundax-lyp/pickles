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
- 不定义 AGENTS.md 注入块格式
- 不定义 Runtime 内部实现类型

## 3. Bounded Context

Pickles runtime config 固定放在被治理目标工程根目录。

目标工程指 IntelliJ IDEA 当前打开、且 Codex Agent 正在工作的用户项目。Pickles 仓库的 e2e 示例目标工程固定为 `e2e/sample-project/`。

Pickles runtime config 是可提交配置文件。

目标工程 `.pickles/` 目录固定承载 Pickles 项目本地文件，包括本地运行时状态文件和 Agent-side skill 生成的检测脚本或辅助实现。运行时状态文件包括 `server.json`、hook state 和 cache。

Pickles runtime config 是配置真相源。IntelliJ Plugin、Runtime 和 Agent-side skills 都围绕该配置协作。

Agent-side skill 生成 native rule 时，默认将规则声明写入 `pickles.config.ts`、`pickles.config.mjs` 或 `pickles.config.js`。当规则需要额外检测脚本或辅助实现时，生成物必须放入目标工程 `.pickles/*`，并由 Pickles runtime config 显式引用。

Runtime 不得自动扫描 `.pickles/` 发现规则。`.pickles/` 不得成为第二个规则真相源。

## 4. Module Mapping

- `pickles-intellij-plugin/`: 展示和更新 Pickles runtime config。
- `pickles-runtime/`: 加载 Pickles runtime config、native rules 和 external adapters。
- `pickles-hooks/`: 读取本地运行时状态。
- `pickles-rules/`: 提供 native rule authoring contract、规则说明书、规则模板和 plugin rule package。
- `pickles-skills/`: 基于规则说明书生成配置或提示 Agent 运行检查。
- `e2e/sample-project/`: 保存示例 `pickles.config.ts`。

## 5. Core Objects

### 5.1 PicklesRuntimeConfig

固定字段：

- `agent`
- `hook`
- `rules`
- `problemBoard`

### 5.2 HookConfig

固定字段：

- `protocol`

### 5.3 RuleConfig

固定字段：

- `id`
- `title`
- `type`
- `severity`

### 5.4 NativeRuleConfig

固定字段：

- `id`
- `title`
- `type`
- `severity`
- `language`
- `files`
- `options`
- `rule`

### 5.5 ProblemInput

固定字段：

- `title`
- `message`
- `file`
- `position`
- `source`

### 5.6 RuleContext

固定字段：

- `workspaceRoot`
- `changedFiles`
- `files`
- `java`
- `problem`

### 5.7 SyntaxMatch

固定字段：

- `name`
- `kind`
- `text`
- `range`
- `captures`

### 5.8 SyntaxCapture

固定字段：

- `name`
- `kind`
- `text`
- `range`

### 5.9 ExternalAdapterRuleConfig

固定字段：

- `id`
- `title`
- `type`
- `severity`
- `adapter`
- `command`

### 5.10 ProblemBoardConfig

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
- 本地 HTTP 端口固定写入目标工程 `<repo>/.pickles/server.json`。
- Agent-side skill 生成的检测脚本或辅助实现固定放入目标工程 `<repo>/.pickles/*`。
- `.pickles/*` 中的检测脚本或辅助实现必须由 Runtime config 显式引用。
- Runtime 不得自动扫描 `.pickles/` 发现 native rule、external adapter 或检测脚本。
- `rules` 固定使用数组。
- Rule plugin package 固定通过标准 ESM import 引用。
- Rule plugin package 必须导出 `defineRule({...})` 返回值，或由这些 rule 组成的数组。
- Runtime 只读取 Pickles runtime config 最终导出的 `rules` 数组。
- Runtime 不得自动发现 rule package。
- Runtime 不得读取 rule package metadata 作为规则入口。
- Runtime MVP 不支持字符串式 rule package 引用。
- Runtime MVP 不支持 `plugins` 字段自动展开规则。
- Native rules 固定由 Runtime 执行。
- ArchUnit 与 ESLint 固定作为 external adapter rule。
- Runtime config 必须使用 `export default defineConfig({...})`。
- Native rule 必须使用 `defineRule({...})`。
- `defineConfig` 和 `defineRule` 固定由 `@pickles/runtime/config` 提供。
- Rule function 固定返回 `ProblemInput[]` 或 `Promise<ProblemInput[]>`。
- `ProblemInput.file` 固定为 string 或 `null`。
- `ProblemInput.position` 固定为 `Position` 或 `null`。
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
    rules: [
        {
            id: "sample-eslint",
            title: "Sample TypeScript validation",
            type: "external-adapter",
            severity: "ERROR",
            adapter: "eslint",
            command: "npm run lint",
        },
    ],
    problemBoard: {
        aggregation: "workspace",
    },
});
```

### 7.2 Native Rule Config

Native rule config 必须能表达 JS / TS 可编程规则。

Native rule 必须通过 Runtime 提供的 `RuleContext` 读取输入。

Native rule 必须使用 `defineRule({...})` 定义。

Native rule 的 options 必须可 JSON 序列化。

Native rule 的 `rule(ctx)` 必须返回 `ProblemInput[]` 或 `Promise<ProblemInput[]>`。

Native rule 的输出必须能归一化为 Problem。

`RuleContext` 必须提供稳定 index query、parser-specific query helper 和 problem factory。

`RuleContext` 必须至少提供：

- `workspaceRoot`
- `changedFiles`
- `files.changed(language?)`
- `java.files()`
- `java.changedFiles()`
- `java.findType(qualifiedName)`
- `java.findTypesByAnnotation(annotationName)`
- `java.findFilesByImport(importTarget)`
- `java.query(file, query)`
- `problem(input)`

`java.query(file, query)` 必须返回 `SyntaxMatch[]`。

`SyntaxMatch.captures` 必须保留 tree-sitter query capture 信息。

`SyntaxCapture` 必须由 Runtime 创建，不得暴露 raw tree-sitter node。

`SyntaxMatch` 必须由 Runtime 创建，不得暴露 raw tree-sitter node。

Native rule 示例：

```ts
import { defineConfig, defineRule } from "@pickles/runtime/config";

const noControllerToRepository = defineRule({
    id: "java-no-controller-to-repository",
    title: "Controller must not depend on repository",
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
    rules: [noControllerToRepository],
});
```

Rule plugin package 示例：

```ts
import { defineConfig } from "@pickles/runtime/config";
import { javaArchitectureRules } from "@pickles/rules-java-architecture";

export default defineConfig({
    agent: "codex",
    rules: [...javaArchitectureRules],
});
```

### 7.3 External Adapter Config

External adapter rule 必须能表达外部命令。

MVP 固定 adapter：

- `archunit`
- `eslint`

External adapter execution 和输出归一化由后续 adapter design 定义。

当 Agent-side skill 为 external adapter 生成检测脚本时，脚本必须放入目标工程 `.pickles/*`。

External adapter rule 必须在 Pickles runtime config 中通过 `command` 显式引用该脚本。

### 7.4 Agent-Side Skill Generated Files

`pickles-rule-authoring-skill` 生成规则时必须遵守以下边界：

- Native rule 声明默认写入 Pickles runtime config。
- 检测脚本、external adapter wrapper 或较长辅助实现写入目标工程 `.pickles/*`。
- Pickles runtime config 必须显式引用 `.pickles/*` 中参与检测的文件。
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
4. Runtime 后续检测使用更新后的配置。

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

- External adapter execution design。
