# TODO List

## 说明

- `TODO.md` 是未关闭任务面板，不是完成历史。
- 宏观任务必须先讨论边界，再拆解为可执行 TODO。
- 已完成任务必须删除，不在 `TODO.md` 中打勾长期保留。
- 完成历史保留在 commit 或 PR 中。

## 当前任务项

## 待审阅任务项

- [ ] `pickles-intellij-plugin/src/main/kotlin/com/pickles/intellij/PicklesModels.kt`：结构化 Problem source
  - 任务类型：执行任务
  - 依据文档：`docs/30-designs/RUNBOOK-INTELLIJ-HTTP-SERVER.md`
  - 范围对象：`pickles-intellij-plugin/src/main/kotlin/com/pickles/intellij/PicklesModels.kt`
  - 处理动作：将 `PicklesProblem.source` 从 string 调整为包含 `tool` 与 `rule` 的结构化对象
  - 验收点：`PicklesProblem` 与 `PROBLEM-MODEL-CONTRACT.md` 中 `source` 固定结构一致
  - 重要度：8/10

- [ ] `pickles-intellij-plugin/src/main/kotlin/com/pickles/intellij/PicklesHttpContract.kt`：新增 Hook HTTP contract 模型
  - 任务类型：执行任务
  - 依据文档：`docs/30-designs/RUNBOOK-INTELLIJ-HTTP-SERVER.md`
  - 范围对象：`pickles-intellij-plugin/src/main/kotlin/com/pickles/intellij/PicklesHttpContract.kt`
  - 处理动作：新增 HTTP request、response、summary、error 与 result data class
  - 验收点：模型字段覆盖 `schemaVersion`、`requestId`、`HookEvent`、`ChangedFile`、notify、feedback 和 error response
  - 重要度：9/10

- [ ] `pickles-intellij-plugin/src/main/kotlin/com/pickles/intellij/PicklesHttpContract.kt`：实现 Hook HTTP contract 校验器
  - 任务类型：执行任务
  - 依据文档：`docs/30-designs/RUNBOOK-INTELLIJ-HTTP-SERVER.md`
  - 范围对象：`pickles-intellij-plugin/src/main/kotlin/com/pickles/intellij/PicklesHttpContract.kt`
  - 处理动作：实现 JSON 解析、公共字段校验、workspace mismatch 校验和 changed file 校验
  - 验收点：非法 JSON、错误 `schemaVersion`、空 `requestId`、缺失必填字段、绝对 `fileName`、双空 before/after 和 workspace mismatch 都返回契约错误
  - 重要度：9/10

- [ ] `pickles-intellij-plugin/src/main/kotlin/com/pickles/intellij/PicklesProjectService.kt`：接入 `/health` 契约响应
  - 任务类型：执行任务
  - 依据文档：`docs/30-designs/RUNBOOK-INTELLIJ-HTTP-SERVER.md`
  - 范围对象：`pickles-intellij-plugin/src/main/kotlin/com/pickles/intellij/PicklesProjectService.kt`
  - 处理动作：让 `/health` 返回包含 `schemaVersion`、`requestId: null` 和 `status: ok` 的 JSON
  - 验收点：`GET /health` 返回 HTTP `200` 与契约 body，非 GET 返回统一错误结构
  - 重要度：7/10

- [ ] `pickles-intellij-plugin/src/main/kotlin/com/pickles/intellij/PicklesProjectService.kt`：接入 `/notify` 契约响应
  - 任务类型：执行任务
  - 依据文档：`docs/30-designs/RUNBOOK-INTELLIJ-HTTP-SERVER.md`
  - 范围对象：`pickles-intellij-plugin/src/main/kotlin/com/pickles/intellij/PicklesProjectService.kt`
  - 处理动作：让 `/notify` 委托 contract 校验并返回 accepted stub
  - 验收点：合法 `POST /notify` 返回 HTTP `202`、`accepted: true`、`processed: false`，且不调用 runtime、不刷新 Problem Board
  - 重要度：9/10

- [ ] `pickles-intellij-plugin/src/main/kotlin/com/pickles/intellij/PicklesProjectService.kt`：接入 `/feedback` unimplemented 响应
  - 任务类型：执行任务
  - 依据文档：`docs/30-designs/RUNBOOK-INTELLIJ-HTTP-SERVER.md`
  - 范围对象：`pickles-intellij-plugin/src/main/kotlin/com/pickles/intellij/PicklesProjectService.kt`
  - 处理动作：让 `/feedback` 委托 contract 校验并返回 unimplemented feedback stub
  - 验收点：合法 `POST /feedback` 返回 HTTP `200`、`status: unimplemented`、空 `problems` 和 0 计数 summary
  - 重要度：9/10

- [ ] `pickles-intellij-plugin/src/test/kotlin/com/pickles/intellij`：补充 Hook HTTP contract 单元测试
  - 任务类型：执行任务
  - 依据文档：`docs/30-designs/RUNBOOK-INTELLIJ-HTTP-SERVER.md`
  - 范围对象：`pickles-intellij-plugin/src/test/kotlin/com/pickles/intellij`
  - 处理动作：为 contract handler 和结构化 Problem source 增加窄测试
  - 验收点：测试覆盖 health、notify 正常、notify 缺 session、workspace mismatch、feedback unimplemented、method not allowed 和无 `code/message/data` 成功 wrapper
  - 重要度：9/10

- [ ] `docs/`：同步 Hook HTTP contract 实现口径
  - 任务类型：执行任务
  - 依据文档：`docs/30-designs/RUNBOOK-INTELLIJ-HTTP-SERVER.md`
  - 范围对象：`docs/10-requirements/INTELLIJ-PLUGIN-REQUIREMENTS.md`、`docs/20-interfaces/HOOK-PLUGIN-HTTP-CONTRACT.md`、`docs/30-designs/INTELLIJ-PLUGIN-DESIGN.md`、`docs/30-designs/MVP-IMPLEMENTATION-DESIGN.md`
  - 处理动作：清理旧的 HTTP API 未定义口径，并记录 `/feedback` stub 的 `unimplemented` 状态
  - 验收点：默认实现入口不再保留“HTTP endpoint 细节暂不定义”或与 stub 行为冲突的描述
  - 重要度：7/10

- [ ] `pickles-intellij-plugin`：运行 IntelliJ Plugin 验证
  - 任务类型：执行任务
  - 依据文档：`docs/30-designs/RUNBOOK-INTELLIJ-HTTP-SERVER.md`
  - 范围对象：`pickles-intellij-plugin/`
  - 处理动作：使用 JDK 17 运行插件模块最小验证
  - 验收点：`gradle build` 通过；若本机缺少 Gradle 或 JDK 17，最终响应明确记录阻塞原因
  - 重要度：9/10

- [ ] `TODO.md`：清理 HTTP server RUNBOOK 执行现场
  - 任务类型：执行任务
  - 依据文档：`docs/30-designs/RUNBOOK-INTELLIJ-HTTP-SERVER.md`
  - 范围对象：`TODO.md` 与 `docs/30-designs/RUNBOOK-INTELLIJ-HTTP-SERVER.md`
  - 处理动作：实现、测试和验证完成后删除已完成 TODO 与一次性 RUNBOOK
  - 验收点：任务闭环只保留在 commit 历史中，`TODO.md` 不长期保留完成项
  - 重要度：9/10

## 待讨论项
