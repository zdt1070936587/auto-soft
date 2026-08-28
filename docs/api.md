# API 一览（MVP 冻结）

统一包装 `R<T>`：`code == 0` 成功。HTTP 401/403 时 body 仍为 `R`。响应含 `traceId`（亦在 `X-Trace-Id`）。

权限：`@RequiresPermission`；`SUPER_ADMIN` 绕过权限码。JWT 不含权限，每次查库。

---

## 1. 认证与健康

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| POST | `/api/auth/login` | 匿名 | `{ username, password }` → token + 用户 + 菜单 |
| POST | `/api/auth/register` | 默认关闭 | 返回 403 |
| GET | `/api/auth/me` | 登录 | 当前用户、菜单、权限码 |
| PUT | `/api/auth/password` | 登录 | `{ oldPassword, newPassword }` |
| GET | `/api/health` | 匿名 | 应用名、profile、db、时间 |
| GET | `/actuator/health` | 匿名 | 详情仅授权后可见 |

错误：`401` 未登录 / 用户名密码错误；`403` 无权限；`400` 参数；`404` 资源不存在；`500` 服务器错误。

---

## 2. 用户与角色

| 方法 | 路径 | 权限 |
| --- | --- | --- |
| GET | `/api/users` | `system:user:list` |
| POST | `/api/users` | `system:user:create` |
| PUT | `/api/users/{id}` | `system:user:update` |
| PUT | `/api/users/{id}/password` | `system:user:update` |
| PUT | `/api/users/{id}/status` | `system:user:update` |
| PUT | `/api/users/{id}/roles` | `system:user:update` |
| DELETE | `/api/users/{id}` | `system:user:delete` |
| GET | `/api/roles` | `system:role:list` |
| GET | `/api/roles/options` | `system:user:list` |
| POST | `/api/roles` | `system:role:create` |
| PUT | `/api/roles/{id}` | `system:role:update` |
| DELETE | `/api/roles/{id}` | `system:role:delete` |
| GET/PUT | `/api/roles/{id}/menus` | list / `system:role:grant` |

---

## 3. 元数据建模

权限码 `meta:app:manage`（开发者/超管）。USER 不可访问。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/meta/apps` | 应用列表 |
| GET | `/api/meta/apps/{id}` | 含实体与字段 |
| POST | `/api/meta/apps` | 草稿应用 |
| PUT/DELETE | `/api/meta/apps/{id}` | 更新/删除 |
| POST | `/api/meta/apps/{appId}/entities` | 建实体 |
| PUT/DELETE | `/api/meta/entities/{id}` | |
| POST | `/api/meta/entities/{id}/fields` | 加字段 |
| PUT/DELETE | `/api/meta/fields/{id}` | |
| PUT | `/api/meta/entities/{id}/pages/{type}` | LIST/FORM/DETAIL |
| POST | `/api/meta/apps/{id}/publish` | `{ grantRoles }`；DDL 失败保持 DRAFT |
| POST | `/api/meta/apps/{id}/unpublish` | 隐藏菜单，表与数据保留 |

`code` 正则：`^[a-z][a-z0-9_]{1,30}$`。字段类型：`string/text/int/long/decimal/bool/date/datetime/dict/ref`。

---

## 4. 动态运行时

路径前缀 `/api/runtime/{app}/{entity}`。发布后按 `app:{app}:{entity}:list|create|update|delete|submit` 鉴权。`preview=true` 仅 DEVELOPER。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/schema` | 字段与是否绑流程 |
| GET | `/page` | 分页；未知查询字段忽略；排序列白名单 |
| GET | `/{id}` | 单行 |
| POST | `/` | 新建 |
| PUT | `/{id}` | 修改；审批中/已通过拒绝 |
| DELETE | `/{id}` | 逻辑删除 |
| POST | `/{id}/submit` | 提交审批 |

---

## 5. 工作室与模型

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| GET/PUT | `/api/system/llm-config` | `system:llm:manage` | PUT 时 Key 只写不回显 |
| GET | `/api/system/llm-models` | 同上 | 拉取 Go 模型列表，缓存 1 小时 |
| GET/POST | `/api/studio/sessions` | `studio:use` | 列表 / 新建 |
| GET | `/api/studio/sessions/{id}/messages` | 同上 | 历史 |
| GET | `/api/studio/sessions/{id}/schema` | 同上 | 右栏预览 |
| POST | `/api/studio/sessions/{id}/chat` | 同上 | `text/event-stream`，body `{ message }` |

SSE 事件：`text` / `tool_start` / `tool_end` / `schema_updated` / `error` / `done`。无 Key / 429 / 模型错误返回中文 `error.message`。

### 全局 AI 助手（阶段 7 P0～P3）

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| GET/POST | `/api/assistant/sessions` | `assistant:use` | 列表 / 新建 |
| GET | `/api/assistant/sessions/{id}/messages` | 同上 | 历史 |
| DELETE | `/api/assistant/sessions/{id}` | 同上 | 删除会话 |
| POST | `/api/assistant/sessions/{id}/chat` | 同上 | `text/event-stream`，body `{ message }` |
| GET | `/api/assistant/memory/facts` | 同上 | 当前用户 fact 列表 |
| DELETE | `/api/assistant/memory/facts/{id}` | 同上 | 逻辑删除 fact |
| POST | `/api/assistant/memory/facts/{id}/confirm` | 同上 | 确认 fact |
| GET | `/api/assistant/memory/episodes` | 同上 | 最近 episode 摘要（`?limit=20`） |

SSE 事件：`text` / `tool_start` / `tool_end` / `structured`（`nav_link` / `oper_timeline`）/ `error` / `done`。与 Studio 会话隔离。

助手工具（内部）：`search_menus`、`query_my_operations`、`get_operation_timeline`、`query_my_page_visits`、`recall_user_memory`、`remember_fact`。操作历史与页面浏览均强制 `user_id = 当前用户`。

### 页面访问埋点（阶段 7 P4）

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| POST | `/api/telemetry/page-visits` | 登录即可 | body `{ visits: [{ path, routeName?, pageTitle?, visitedAt? }] }`，返回 `{ inserted }` |

前端 AdminLayout 路由 `afterEach` 批量上报；同 path 60s 内去重。关闭：`autosoft.telemetry.page-visit.enabled=false` 与 `VITE_PAGE_VISIT_ENABLED=false`。

---

## 6. 审批

登录即可访问；办理时校验角色。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/flow/todo` | 我的待办（角色命中） |
| GET | `/api/flow/done` | 我已办 |
| POST | `/api/flow/todo/{taskId}/complete` | `{ comment }` |
| POST | `/api/flow/todo/{taskId}/reject` | `{ comment }` 必填 |

猜 taskId 且角色不匹配 → 403。

---

## 7. 操作日志

| 方法 | 路径 | 权限 |
| --- | --- | --- |
| GET | `/api/system/logs` | `system:log:list` |

查询：`current/size/module/username`。详情已脱敏，不含密码与 Key。

---

## 8. 自动化工作流（阶段 6A/B/C）

权限：建模接口 `studio:use`；运行已发布流需 `wf:{code}:run`（开发者可试跑草稿）。分享接口需登录。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/wf` | 建草稿（同时建 `app_kind=workflow` 应用） |
| GET | `/api/wf/{id}` | 定义 + graph |
| GET | `/api/wf?appId=` | 按应用取定义 |
| PUT | `/api/wf/{id}/graph` | 保存 IR（画布） |
| POST | `/api/wf/{id}/validate` | 只校验 |
| POST | `/api/wf/{id}/dry-run` | 草稿试跑，`{ input }`；approval 不真人审 |
| POST | `/api/wf/{id}/publish` | `{ confirm: true }` |
| POST | `/api/wf/{id}/share` | `{ permission: preview\|copy, expireDays }`，操作日志 SHARE |
| GET | `/api/wf/share/{token}` | 只读图，剥离 secret/header/apiKey |
| POST | `/api/wf/share/{token}/copy` | 复制为当前用户草稿 |
| PUT | `/api/wf/{id}/schedule` | 超管关停/启用定时 `{ enabled }` |
| GET | `/api/wf/by-code/{code}` | 已发布定义 |
| POST | `/api/wf/{code}/run` | 按发布快照运行 |
| GET | `/api/wf/runs/{runId}` | 实例与步骤 |

节点：`start` / `end` / `meta.query` / `llm` / `notify` / `condition` / `approval` / `meta.upsert` / `http`。`condition` 必须 `when=true` 与 `when=false` 各一条出边。非 condition 可额外 `when=error`。`http` 的 host 须在 `autosoft.workflow.http.allowed-hosts`，并拒绝 localhost/私网/元数据 IP。

SSE 增加 `graph_updated`。工作流会话禁止 `define_entity` 等 CRUD 工具。
