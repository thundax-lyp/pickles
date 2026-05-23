# PR Workflow

## 1. Purpose

本文档定义 Pickles 的 Pull Request 合并前验证流程。

目标是保证 PR 合并前固定执行统一 verify 入口，并让各项目的验证环节接入同一 PR workflow。

## 2. Scope

当前范围：

- GitHub Pull Request workflow
- 统一 verify 入口
- IntelliJ Plugin verify
- e2e sample project verify

不在范围内：

- 不定义发布流程
- 不定义分支保护配置的 GitHub UI 操作
- 不定义尚未建立构建系统模块的验证命令

## 3. Bounded Context

PR 合并前固定执行 `.github/workflows/pr-verify.yml`。

workflow 固定调用 `scripts/verify-all.sh`。各项目新增或调整验证命令时，必须接入该脚本，避免 GitHub Actions 与本地验证入口分叉。

## 4. Module Mapping

- `.github/workflows/pr-verify.yml`: GitHub PR 触发入口。
- `scripts/verify-all.sh`: 仓库统一 verify 编排入口。
- `pickles-intellij-plugin/`: 通过 `gradle build` 执行插件构建和测试。
- `e2e/sample-project/`: 通过 `npm ci`、`npm run typecheck` 和 `npm run lint` 执行样例工程验证。

## 5. Core Objects

- `pr-verify.yml`
- `scripts/verify-all.sh`
- project verify command

## 6. Global Constraints

- PR 合并前必须通过统一 verify workflow。
- workflow 不直接散落项目验证细节，项目验证细节固定收敛到 `scripts/verify-all.sh`。
- 新增项目验证能力时必须同步接入 `scripts/verify-all.sh`。
- 尚未建立构建系统或验证命令的模块不得在 workflow 中伪造空验证。

## 7. Functional Requirements

### 7.1 PR Verify

`pr-verify.yml` 固定在 pull request 和手动触发时执行。

pull request 目标分支固定覆盖：

- `main`
- `thundax-work`

### 7.2 IntelliJ Plugin Verify

IntelliJ Plugin verify 固定执行：

```bash
cd pickles-intellij-plugin
gradle build
```

### 7.3 e2e Sample Project Verify

e2e sample project verify 固定执行：

```bash
cd e2e/sample-project
npm ci
npm run typecheck
npm run lint
```

## 8. Key Flows

### 8.1 PR Flow

1. 开发者打开或更新 Pull Request。
2. GitHub 触发 `PR Verify` workflow。
3. workflow 准备 Java、Gradle 和 Node.js 环境。
4. workflow 执行 `scripts/verify-all.sh`。
5. 所有项目 verify 通过后，PR 才允许进入合并判断。

## 9. Non-Functional Requirements

- workflow 必须使用可重复安装的依赖入口。
- workflow 必须失败即停止。
- workflow 输出必须能定位失败项目。

## 10. Open Items

无
