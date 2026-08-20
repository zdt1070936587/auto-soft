# 阶段 4：warm-flow 审批（完整开发计划）

> 文档日期：2026-08-20  
> 对应总计划：[分阶段开发计划.md](./分阶段开发计划.md)  
> 前置：[阶段3-功能开发与OpenCode.md](./阶段3-功能开发与OpenCode.md)、[阶段2-元数据引擎与动态运行时.md](./阶段2-元数据引擎与动态运行时.md)  
> 后续：[阶段5-收口与MVP冻结.md](./阶段5-收口与MVP冻结.md)  
> 工期：5～7 人天  
> 目标：动态实体可挂**单线审批**；USER 提交，指定角色待办通过/驳回；Agent 能生成请假单类需求。不做会签、或签、条件分支。

---

## 1. 阶段目标

1. 引入 `warm-flow-mybatis-plus-sb4-starter`（锁定 **1.8.9**），执行官方 PostgreSQL 脚本。
2. `FlowManager` 封装：启动、待办、通过、驳回、查记录。Controller 不调引擎 API。
3. 实体绑定流程后：`flow_status` 为 `draft` 可编辑并提交；`processing` 只读；`approved` 只读（或按产品允许超管改，MVP **只读**）；`rejected` 可改后再提交。
4. 待办中心：我的待办 / 已办。
5. Agent 工具 `create_simple_flow`、`bind_flow` 真正落地。
6. 验收话术：「请假单：天数、原因，提交后由 ADMIN 审批」全链路通过。

## 2. 本阶段明确不做

| 内容 | 说明 |
| --- | --- |
| 会签 / 或签 / 票签 / 条件分支 / 并行网关 | 非 MVP |
| 仿钉钉复杂设计器给终端用户画图 | 可用官方设计器给超管查看，**USER 不画图** |
| 流程版本灰度、委托、转办、催办 | 非首期（引擎若自带接口也不要在 UI 暴露） |
| 改 warm-flow 源码 | 只封装 |

## 3. 前置条件

- 动态表已有 `flow_status` 列（阶段 2 约定）。若缺失，本阶段 `V1.4.0` 对**新表** DDL 模板补上；旧演示表 `ALTER TABLE ... ADD COLUMN IF NOT EXISTS flow_status`。
- 角色 `ADMIN` 等已存在，审批人按**角色**而非用户（节点办理人为角色下全部用户，谁先办谁占有——MVP 采用「该角色任意用户可办」）。
- 先用官方请假示例在空库验证 starter 能跑，再接动态实体。

## 4. 流程形态（写死模板）

```text
开始 -> 提交人(已办) -> 审批节点(roleCode) -> 结束
```

- 多级：`审批1(roleA) -> 审批2(roleB)`，仍是单线，最多 **3** 级，避免 Agent 生成树。
- 提交即 `insService.start`，提交人不出现在「待办」，在「已办/我发起的」。
- 驳回：回到 `draft` + `rejected`，业务数据保留；再次提交起**新实例**或同一实例 skip（选一种并写进 FlowManager 注释）。**MVP 选定：驳回终止当前实例，记录原因，`flow_status=rejected`，再次提交 start 新实例。**

## 5. 数据库

### 官方脚本

从 warm-flow 仓库取 PostgreSQL `warm-flow-all.sql`，放入 `V1.4.0__warm_flow.sql`（或 `V1.4.0` 只建引擎表，`V1.4.1` 业务绑定）。**不要改官方表结构。**

### meta_entity_flow

| 字段 | 说明 |
| --- | --- |
| entity_id | 唯一绑定一个定义 |
| flow_code | 引擎流程编码 |
| definition_id | 引擎定义 id（发布后） |
| approve_role_codes | JSON 数组，按顺序 |
| enabled | 1/0 |

未绑定的实体：`flow_status` 恒为 `none`，无提交按钮。

## 6. 目录增量

```
dev/auto-soft-flow/
  FlowManager.java          # 对引擎
  EntityFlowService.java    # 绑定与状态机
  web/FlowTodoController.java
  listener/                # 若需节点通过后回写 dyn 表
web/src/views/flow/TodoView.vue
web/src/views/flow/DoneView.vue
```

`auto-soft-boot` 增加 warm-flow sb4 starter。注意与现有 MP 版本一致（3.5.15）。

## 7. 后端设计

### 7.1 FlowManager

步骤示例 `completeTask`：

1. `loadTaskAndCheckAssignee`（当前用户角色是否匹配）
2. `completeOrReject`
3. `syncBusinessStatus`（更新 `dyn_*`.flow_status）
4. `writeComment`

办理人：用 warm-flow 权限处理器对接 `sys_user` / `sys_role`（官方 SPI）。实施时对照 1.8.9 文档实现 `PermissionHandler`，把 `userId`、`roleCode` 提供给引擎。**禁止**把办理人写成死用户 id。

### 7.2 与运行时集成

`RuntimeService` 增加：

- 写操作：若绑定流程且 status 为 `processing`/`approved` → 拒绝修改删除
- `POST /api/runtime/{app}/{entity}/{id}/submit`
- 列表可筛 `flow_status`

提交步骤：

1. 校验记录存在且 status 为 `draft` 或 `rejected`
2. `FlowManager.start(entity, bizId, vars)`（vars 含摘要字段，避免引擎库塞大 JSON）
3. 更新 `flow_status=processing`

### 7.3 待办 API

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/flow/todo` | 我的待办（角色命中） |
| GET | `/api/flow/done` | 我已办 |
| GET | `/api/flow/initiated` | 我发起的（可选） |
| POST | `/api/flow/todo/{taskId}/complete` | `{ comment }` |
| POST | `/api/flow/todo/{taskId}/reject` | `{ comment }` 必填 |

权限：登录即可进待办；complete 时 Manager 再校验任务归属。

菜单：所有角色「待办中心」`/flow/todo`；ADMIN 等审批角色才能看到待办条目。

### 7.4 Agent 工具

`create_simple_flow(entityCode, roleCodes[])`：

1. 校验角色存在  
2. 按模板生成并发布 warm-flow 定义（名称=实体中文+审批）  
3. 写 `meta_entity_flow`

`bind_flow`：若定义已存在则只绑 entity。

系统提示词补充：有审批需求必须问审批角色；角色必须是已有 `code`。

请假单演示也可手工绑，不强制 Agent，但验收至少一条 Agent 或手工绑定 + 一条 Agent 路径（时间不够则手工绑定验收，Agent 工具单测/接口测）。

## 8. 前端

- 运行时页：有绑定时显示状态 Tag、提交按钮、只读态禁用表单
- 待办表：标题、发起人、时间、办理；点开抽屉只读表单 + 通过/驳回
- 驳回必须填意见

SchemaRenderer 增加 `readonly` 与 `onSubmit` 插槽，不要复制一套页面。

## 9. 任务拆解

| 任务 | 内容 | 工期 |
| --- | --- | --- |
| A | starter + 官方 SQL + 官方示例跑通 | 1d |
| B | PermissionHandler 对接用户角色 | 0.5～1d |
| C | FlowManager + meta_entity_flow + 状态回写 | 1.5d |
| D | Runtime submit + 只读规则 | 1d |
| E | 待办 UI | 1d |
| F | Agent 两工具 + 请假单验收 | 1d |

合计 **5～7 人天**。

## 10. 验收清单

- [ ] 应用能启动，warm-flow 表存在
- [ ] 请假单（天数 int、原因 text）发布并绑定 ADMIN 审批
- [ ] USER 提交后不能改；ADMIN 待办可见；通过后 `approved`；驳回后 USER 可改再提
- [ ] 未绑定流程的物品登记行为与阶段 2 一致（无提交按钮）
- [ ] 非办理角色 complete 返回 403
- [ ] Controller 不出现 warm-flow API 类型
- [ ] 无会签配置入口

## 11. 风险

| 风险 | 处理 |
| --- | --- |
| sb4 starter 与 Boot 4.1 不兼容 | 锁定 1.8.9；第一天必须跑通官方示例，失败再查发行说明，不降 Boot |
| 办理人模型不匹配 | 文档写清「角色内任一人可办」；Handler 单测 |
| 动态表回写失败引擎已往前走 | 同一事务能则同事务；不能则先更业务再 complete，失败补偿记日志（阶段 5 再补） |

## 12. 交给阶段 5 的接口

- 流程关键动作（提交/通过/驳回）要能打点到操作日志
- 待办与运行时权限模型稳定，阶段 5 做渗透式复查（越权、篡改 taskId）
