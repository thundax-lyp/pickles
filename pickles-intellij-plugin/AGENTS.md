# IntelliJ Plugin Agent

只给 AI / harness 读。目标：把 IntelliJ IDEA plugin 作为 Pickles 的主要产品入口处理。

## Scope

- IntelliJ 平台集成
- 编辑器与项目视图交互
- Problem Board UI
- PSI / VFS 观察与事件适配
- 本地 Governance Server 管理入口
- Agent Bind 相关 IDE 侧流程

## Working Rules

- Plugin 永远不修改用户代码。
- IDE 侧负责观察、展示、编排和触发，不承载规则执行真相。
- 规则执行与问题聚合优先放在 `../pickles-runtime/`。
- MCP tool、resource 和协议适配优先放在 `../pickles-mcp/`。
- Agent hook 安装与生命周期触发优先放在 `../pickles-hooks/`。
- 规则声明、策略和可复用治理规范优先放在 `../pickles-rules/`。

## Documentation

- 插件需求进入 `../docs/10-requirements/`。
- 插件与 runtime / MCP / hooks 的契约进入 `../docs/20-interfaces/`。
- 跨模块设计和一次性迁移进入 `../docs/30-designs/`。
- 人类阅读材料进入 `../docs/60-human/`，不作为 AI 默认执行规则。

## Validation

- 模块建立构建系统后，在本文件补充固定验证命令。
- 修改 IDE 行为、事件监听或 UI 状态时，必须同步考虑测试或手动验证路径。
