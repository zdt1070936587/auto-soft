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

## 后续表规划（尚未建表）

详见 [分阶段开发计划.md](./分阶段开发计划.md) 第 10 节。名称预告：

- 用户权限：`sys_user`、`sys_role`、`sys_user_role`、`sys_menu`、`sys_role_menu`
- LLM：`sys_llm_config`、`ai_session`、`ai_message`、`ai_tool_log`
- 元数据：`meta_app`、`meta_entity`、`meta_field`、`meta_page`、`meta_app_menu`
- 工作流：warm-flow 官方 PostgreSQL 脚本 + `meta_entity_flow`

本地空库可重建（会丢数据）：

```bash
docker compose down -v
docker compose up -d
```
