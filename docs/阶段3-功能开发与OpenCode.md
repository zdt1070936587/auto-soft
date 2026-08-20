# 阶段 3：功能开发工作室与 OpenCode Go（完整开发计划）

> 文档日期：2026-08-20  
> 对应总计划：[分阶段开发计划.md](./分阶段开发计划.md)  
> 前置：[阶段2-元数据引擎与动态运行时.md](./阶段2-元数据引擎与动态运行时.md)（手工建模与运行时已跑通）  
> 后续：[阶段4-warm-flow审批.md](./阶段4-warm-flow审批.md)  
> 工期：7～10 人天  
> 目标：DEVELOPER 用自然语言生成 CRUD（实体+字段+列表表单+菜单），右侧所见即所得。审批类工具只留接口形状，**真正建流放到阶段 4**。

---

## 1. 阶段目标

1. 超管可配置 OpenCode Go：API Key（加密存储）、允许模型列表、默认模型（不锁死，设置里选；yml 兜底建议 `kimi-k2.7-code`）。
2. 功能开发工作室：左对话（SSE + 工具进度），右 SchemaRenderer 预览当前草稿 app。
3. Agent 通过 **已注册工具** 调用阶段 2 的 Meta/Publish 能力，禁止任意 SQL、禁止写 Java/Vue 文件。
4. 用户说「做一个客户档案，字段有名称、电话、备注」并确认后，可发布，USER 侧栏出现并可 CRUD。
5. Controller 无业务；LLM HTTP 全部在 `OpenCodeGoManager`；对话编排在 `AgentService` 步骤化。

## 2. 本阶段明确不做

| 内容 | 放到 |
| --- | --- |
| `create_simple_flow` / `bind_flow` 的真实实现 | 阶段 4（本阶段工具可返回「请阶段 4 再开放」或 no-op 说明） |
| Responses 协议（Grok / GPT 5.6 Luna）作为主路径 | 本阶段第二优先，有余量再做 |
| 多会话并发同一 app 的复杂锁 | 简单：一用户一草稿锁，或「当前会话绑定一个 appId」 |
| 用量看板、429 精细化 | 阶段 5 完善；本阶段至少把 429 转成人话 |
| 用户自己填 API Key | MVP 仅系统级配置，避免 Key 散落 |

## 3. 前置条件

- 阶段 2 发布链路可用；`SchemaRenderer` 可独立吃 schema JSON。
- OpenCode Go 账号与 API Key（实施时由超管粘贴，不入库明文）。
- 给 `DEVELOPER` 菜单「功能开发」`/studio`；给 `SUPER_ADMIN` 菜单「模型设置」`/system/llm`。

## 4. OpenCode Go 协议

同一 Key，`Authorization: Bearer`。

| 协议 | URL | 模型举例 | 本阶段优先级 |
| --- | --- | --- | --- |
| OpenAI Chat | `https://opencode.ai/zen/go/v1/chat/completions` | glm-5.x、kimi-k*、deepseek-v4-*、mimo-*、hy3 | **P0**（tool calling 主路径） |
| Anthropic Messages | `https://opencode.ai/zen/go/v1/messages` | minimax-m*、qwen3.* | P1 |
| OpenAI Responses | `https://opencode.ai/zen/go/v1/responses` | grok-4.5、gpt-5.6-luna | P2 |

模型列表：`GET https://opencode.ai/zen/go/v1/models`（可缓存 1 小时）。

`ModelProtocolRouter`：按 `modelId` 选协议。上层只暴露：

```text
streamChat(messages, tools, modelId) -> token/tool_call 事件
```

工具 JSON 使用 OpenAI `tools` / `tool_calls` 形状；Anthropic 路径在 Manager 内转换。

限额（产品文案写入设置页）：约 $12/5h、$30/周、$60/月。默认不要选 `kimi-k3`。

## 5. 数据库（`V1.3.0__ai_studio.sql`）

### sys_llm_config

单行或按 key：`api_key_cipher`、`api_key_iv`、`default_model`、`allowed_models_json`、`updated_by`。  
加密：AES-256-GCM，密钥来自配置 `autosoft.crypto.aes-key`（**不提交生产密钥**，dev 占位）。日志禁止打印解密后的 Key。

### ai_session

`id, user_id, title, app_id(可空), status, token_input, token_output, created_at...`

### ai_message

`id, session_id, role(user/assistant/tool/system), content, tool_name, tool_call_id, tokens, created_at`

### ai_tool_log

`id, session_id, tool_name, arguments_json, result_json(截断), success, error_msg, duration_ms`

## 6. 目录增量

```
dev/auto-soft-agent/
  llm/OpenCodeGoManager.java
  llm/ModelProtocolRouter.java
  llm/ChatCompletionsClient.java
  llm/AnthropicMessagesClient.java
  tool/AgentTool.java          # 接口
  tool/ToolRegistry.java
  tool/impl/*Tool.java         # 每个工具调 meta Service
  studio/AgentService.java
  studio/PromptBuilder.java
  web/StudioController.java    # SSE
  web/LlmConfigController.java
web/src/views/studio/StudioView.vue
web/src/views/system/LlmSettingView.vue
web/src/api/studio.ts
```

## 7. Agent 工具（必须注册，模型不能发明别的）

| 工具 | 作用 | 内部调用 |
| --- | --- | --- |
| `ask_user` | 澄清：实体中文名、字段列表、是否列表查询 | 不写库，返回给模型 |
| `create_app` / `update_app` | 草稿 app | MetaAppService |
| `define_entity` | 建实体 | MetaEntityService |
| `add_field` / `update_field` | 字段 | MetaFieldService |
| `define_page` | LIST/FORM schema，可为空表示用默认 | MetaPageService |
| `bind_menu` | 指定授权角色，默认 USER | 可先记在 app 扩展字段，发布时用 |
| `get_current_schema` | 给模型看当前草稿 | Meta schema |
| `preview_app` | 标记预览，前端刷新右栏 | 无 DDL |
| `publish_app` | 调阶段 2 PublishService | 必须用户在 UI 点确认或工具前二次确认 |

系统提示词硬约束：

- 一次会话只改一个 app
- 先 `ask_user` 再动结构（字段无中文名则追问）
- 字段类型必须落在白名单
- 不要输出源码文件
- 不要编造不存在的角色 code

`AgentService.runTurn` 步骤：

1. `loadSessionAndDraftApp`
2. `buildMessages`（系统提示 + 历史，超长截断旧 tool 结果）
3. `streamModel`
4. 若 tool_call：`validateArgs` → `ToolRegistry.execute` → 写 `ai_tool_log` → 结果回灌（截断 8KB）→ 再请求模型（循环上限 **8** 次）
5. 持久化 assistant 消息与 token
6. SSE 事件：`text` / `tool_start` / `tool_end` / `schema_updated` / `error` / `done`

循环超限：结束并提示「请缩短需求或手动在应用建模里改」。

## 8. API

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET/PUT | `/api/system/llm-config` | 超管；PUT 时 Key 只写不回显，回显掩码 |
| GET | `/api/system/llm-models` | 拉取 Go 模型列表 |
| GET/POST | `/api/studio/sessions` | 会话列表/新建 |
| GET | `/api/studio/sessions/{id}/messages` | 历史 |
| POST | `/api/studio/sessions/{id}/chat` | `text/event-stream`，body `{ message }` |
| GET | `/api/studio/sessions/{id}/schema` | 右栏预览 |

Security：`/api/studio/**` 需 `DEVELOPER` 或超管；llm-config 仅超管。SSE 注意：Filter 不要缓冲；超时拉长（如 5 分钟）。

## 9. 前端工作室

布局：左 40% 对话（Markdown 渲染用户/助手；工具调用显示「正在添加字段 qty…」）；右 60% `SchemaRenderer`（preview 模式，可空表）。底栏：发布按钮（二次确认）、新开会话。

设置页：Key 密码框、默认模型 Select（options 来自 `/llm-models`）、说明额度与协议（Chat 模型优先）。

`http.ts`：SSE 用 `fetch` + `ReadableStream`，不要走 axios 拦截器解包。

## 10. 任务拆解

| 任务 | 内容 | 工期 |
| --- | --- | --- |
| A | 表、AES、llm 设置页、拉模型 | 1d |
| B | Chat Completions + SSE 通「回声」 | 1d |
| C | ToolRegistry + 只读 `get_current_schema` | 0.5d |
| D | 写库类工具接 MetaService | 2d |
| E | 循环、截断、发布确认、右栏刷新 | 1.5d |
| F | Anthropic 路径（若默认模型只需 Chat 可缩） | 1d |
| G | 验收话术走查、提示词打磨 | 1d |

合计 **7～10 人天**。若 Key 未就绪，可用 MockTool 先跑 UI，但验收必须真模型。

## 11. 验收清单

- [ ] 超管保存 Key 后库中非明文；接口不回传完整 Key
- [ ] 指定默认模型后工作室用该模型（请求日志可打 modelId，禁止打 Key）
- [ ] 话术生成客户档案三字段，右栏出现表单/表头
- [ ] 发布后 USER 登录可见菜单并能 CRUD
- [ ] 工具循环不超过上限；非法 field_type 被工具拒绝并让模型改
- [ ] 429 时用户看到「额度或限流，请更换模型或稍后」而非堆栈
- [ ] 无 Java/Vue 文件写入仓库；无 warm-flow 依赖

## 12. 风险

| 风险 | 处理 |
| --- | --- |
| 模型乱调工具 | 严格 JSON Schema；未知 tool 名直接错误回灌 |
| 协议分叉 | Router 隔离；P0 只保证 Chat Completions |
| 提示词注入 | 工具层再校验 code 正则与类型白名单，不信任模型参数 |
| SSE 被代理缓冲 | nginx 文档阶段 5 再写；dev 直连 Vite 代理 `timeout: 0` |

## 13. 交给阶段 4 的接口

- 同一 `ToolRegistry` 增加 `create_simple_flow`、`bind_flow`
- 草稿 app 与 session 绑定关系保持
- 发布后实体 id 可查，供挂流程定义
