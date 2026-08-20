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
