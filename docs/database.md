# 数据库约定

## 环境

| 项 | 值 |
| --- | --- |
| 引擎 | PostgreSQL 16+ |
| 库名 | `autosoft` |
| 开发账号 | `autosoft` / `autosoft`（仅本地，见 `docker-compose.yml`） |
| 时区 | 存储 UTC（`TIMESTAMPTZ`） |
| 迁移 | Flyway，脚本位于 `dev/auto-soft-boot/src/main/resources/db/migration/` |

本地启动数据库：

```bash
docker compose up -d
```

若本机 5432 已被占用，将 compose 端口改为 `15432:5432`，并同步修改 `application-dev.yml` 中的 JDBC URL。

## 命名

- 表名、字段名：`snake_case`
- 主键：`id BIGSERIAL`，Java `Long`，不用 UUID 做主键
- 表前缀：
  - `sys_` 系统表
  - `meta_` 元数据
  - `ai_` 对话与模型
  - `dyn_` 用户生成的动态业务表（阶段 2，仅允许此前缀 DDL）

## 公共字段（阶段 1 起强制）

新建业务表必须包含：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `BIGSERIAL` | 主键 |
| `created_by` | `BIGINT` | 创建人 |
| `created_at` | `TIMESTAMPTZ` | 创建时间，默认 `NOW()` |
| `updated_by` | `BIGINT` | 更新人 |
| `updated_at` | `TIMESTAMPTZ` | 更新时间 |
| `deleted` | `SMALLINT` | 逻辑删除，0 否 1 是 |

阶段 0 基线表 `sys_schema_meta` 为迁移探活，不套用完整公共字段。

## Flyway 版本规则

文件名：`V主.次.补丁__英文简述.sql`

- 只新增脚本，**禁止修改已执行过的脚本**
- 阶段 0：`V1.0.0__init.sql`
- 阶段 1：`V1.1.0__sys_user_role.sql`（见 [阶段1-登录用户角色.md](./阶段1-登录用户角色.md) 第 7 节）
- 阶段 2：`V1.2.0__meta_engine.sql`（见 [阶段2-元数据引擎与动态运行时.md](./阶段2-元数据引擎与动态运行时.md)）
- 阶段 3：`V1.3.0__ai_studio.sql`（见 [阶段3-功能开发与OpenCode.md](./阶段3-功能开发与OpenCode.md)）
- 阶段 4：`V1.4.0__warm_flow.sql`（官方脚本 + `meta_entity_flow`，见 [阶段4-warm-flow审批.md](./阶段4-warm-flow审批.md)）
- 阶段 5：`V1.5.0__sys_oper_log.sql`（见 [阶段5-收口与MVP冻结.md](./阶段5-收口与MVP冻结.md)）

## 阶段 0 已有表

### sys_schema_meta

用于验证 Flyway，非业务表。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `BIGSERIAL` | 主键 |
| `phase` | `VARCHAR(32)` | 阶段标识，如 `phase-0` |
| `remark` | `VARCHAR(256)` | 备注 |
| `created_at` | `TIMESTAMPTZ` | 写入时间 |

启动成功后应存在 1 行：`phase-0` / `flyway baseline`。同时存在 Flyway 表 `flyway_schema_history`。

## 阶段 1 已建表（`V1.1.0__sys_user_role.sql`）

公共字段：`created_by` / `created_at` / `updated_by` / `updated_at` / `deleted`（0 否 1 是）。中间表不做逻辑删除。

### sys_user

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `BIGSERIAL` | 主键 |
| `username` | `VARCHAR(64)` | 登录名；部分唯一索引 `WHERE deleted = 0` |
| `password` | `VARCHAR(128)` | BCrypt 密文 |
| `nickname` | `VARCHAR(64)` | 显示名 |
| `status` | `SMALLINT` | 1 启用 0 停用 |
| `last_login_at` | `TIMESTAMPTZ` | 最近登录，可空 |
| 公共字段 | | |

### sys_role

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `BIGSERIAL` | 主键 |
| `code` | `VARCHAR(64)` | 角色编码，未删除范围内唯一。内置：`SUPER_ADMIN` / `ADMIN` / `DEVELOPER` / `USER` |
| `name` | `VARCHAR(64)` | 名称 |
| `remark` | `VARCHAR(256)` | 可空 |
| `sort` | `INT` | 排序 |
| `status` | `SMALLINT` | 1 启用 0 停用 |
| `builtin` | `SMALLINT` | 1 内置不可删 |
| 公共字段 | | |

### sys_user_role

`id`、`user_id`、`role_id`，`UNIQUE(user_id, role_id)`。解绑物理删除。

### sys_menu

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `BIGSERIAL` | 主键 |
| `parent_id` | `BIGINT` | 0 表示根 |
| `name` | `VARCHAR(64)` | 标题 |
| `path` | `VARCHAR(128)` | 前端路由，按钮可空 |
| `component` | `VARCHAR(128)` | 前端组件名，目录可空 |
| `menu_type` | `VARCHAR(16)` | `DIR` / `MENU` / `BUTTON` |
| `permission` | `VARCHAR(128)` | 权限码，按钮必填 |
| `icon` | `VARCHAR(64)` | 可空 |
| `sort` | `INT` | |
| `visible` | `SMALLINT` | 1 显示 0 隐藏（隐藏仍可授权接口） |
| `status` | `SMALLINT` | 1 启用 |
| 公共字段 | | |

阶段 1 菜单：`/dashboard`、`/system`（DIR）、`/system/users`、`/system/roles`，以及对应用户/角色按钮权限。

### sys_role_menu

`id`、`role_id`、`menu_id`，`UNIQUE(role_id, menu_id)`。解绑物理删除。

种子用户（密码均为 `admin123`，仅开发库）：`admin`（SUPER_ADMIN）、`demo_admin`、`demo_dev`、`demo_user`。

## 阶段 2 已建表（`V1.2.0__meta_engine.sql`）

动态业务表名：`dyn_{appCode}_{entityCode}`，仅允许此前缀 DDL，无 DROP。

### meta_app

`code`、`name`、`status`（DRAFT/PUBLISHED）、`version`、`grant_roles`、`remark` + 公共字段。未删除范围内 `code` 唯一。

### meta_entity

`app_id`、`code`、`name`、`remark` + 公共字段。同一 app 下 `code` 唯一。

### meta_field

`entity_id`、`code`、`name`、`field_type`、`length`、`nullable_flag`、`default_value`、`options_json`、`ref_app`、`ref_entity`、`sort`、`queryable`、`listed`、`required_flag` + 公共字段。

### meta_page

`entity_id`、`page_type`（LIST/FORM/DETAIL）、`schema_json` + 公共字段。

### meta_app_menu

`app_id`、`menu_id`，记录发布生成的菜单以便取消发布时隐藏。

演示草稿：`demo` / 实体 `item`（name/qty/remark），需调用发布接口后 USER 才可见。

## 阶段 3 已建表（`V1.3.0__ai_studio.sql`）

### sys_llm_config

`api_key_cipher`、`api_key_iv`、`default_model`、`allowed_models_json` + 公共字段。Key 不以明文存储。

### ai_session

`user_id`、`title`、`app_id`、`status`、`token_input`、`token_output` + 公共字段。

### ai_message

`session_id`、`role`、`content`、`tool_name`、`tool_call_id`、`tokens` + 公共字段。

### ai_tool_log

`session_id`、`tool_name`、`arguments_json`、`result_json`（截断）、`success`、`error_msg`、`duration_ms` + 公共字段。

## 阶段 4 已建表（`V1.4.0__warm_flow.sql`）

官方 warm-flow 脚本未能从仓库拉取；业务使用自建表，接口契约不变。starter 坐标锁定 1.8.9，`warm-flow.enabled=false`。

### meta_entity_flow

`entity_id`（未删除唯一）、`flow_code`、`definition_id`、`approve_role_codes`、`enabled` + 公共字段。

### sys_flow_definition / sys_flow_instance / sys_flow_task

单线审批运行表：定义、实例、待办任务（`comment_text`、办理角色、状态 pending/done/rejected）。

动态表示例列 `flow_status`：`none/draft/processing/approved/rejected`。

## 阶段 5 已建表（`V1.5.0__sys_oper_log.sql`）

### sys_oper_log

无逻辑删除。`user_id`、`username`、`module`、`action`、`biz_id`、`success`、`ip`、`cost_ms`、`detail_json`（脱敏）、`created_at`。

## 阶段 6A 已建表（`V1.8.0__workflow.sql`）

前缀 `wf_`。公共字段同阶段 1。`wf_share` 已建表，B 暴露分享 API。

### wf_definition

`app_id`（与 `meta_app` 一对一）、`code`、`name`、`status`（DRAFT/PUBLISHED）、`graph_json`、`version`、`grant_roles`、`visibility` + 公共字段。

### wf_definition_version

发布快照：`definition_id`、`version`、`graph_json`。

### wf_run / wf_run_step

运行实例与逐步日志。`dry_run`、`status`（running/succeeded/failed/paused）、`trigger_json`、步骤摘要脱敏。paused 时 `trigger_json` 含上下文快照。

### wf_notice

站内通知：`run_id`、`to_role`、`title`、`body`。

### wf_share

分享令牌：`definition_id`、`token`、`permission`（preview/copy）、`expire_at`。

## 阶段 6C 已建表（`V1.9.0__wf_schedule.sql`）

### wf_schedule

`definition_id`、`cron`、`enabled`、`last_run_at`。同一 definition 同时只允许一个 running/paused 运行；最小间隔由 `autosoft.workflow.schedule.min-interval-ms` 控制（默认 5 分钟）。

本地空库可重建（会丢数据）：

```bash
docker compose down -v
docker compose up -d
```
