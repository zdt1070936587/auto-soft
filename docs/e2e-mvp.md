# MVP 端到端走查记录

日期：2026-08-20

环境：开发机实现与编译走查。本记录编写时未连接本机 PostgreSQL / 未调用真实 OpenCode Go，**运行时脚本 1～9 待有库与 Key 后当场复测**。

准备账号：`admin`、`demo_dev`（DEVELOPER）、`demo_user`（USER）、`demo_admin`（ADMIN）。密码均为 `admin123`。

| 步骤 | 内容 | 结果 |
| --- | --- | --- |
| 1 | `demo_dev` 进工作室；超管已写 Key | 代码路径已具备；待运行时 |
| 2 | 「做请假单，天数、原因，ADMIN 审批」 | 工具 `create_app` / `add_field` / `create_simple_flow` 已注册 |
| 3 | 确认后发布，授权 USER | `PublishService` + 菜单授权 |
| 4 | `demo_user` 看到菜单并提交 | 运行时 `submit` + `FlowManager.start` |
| 5 | `demo_admin` 待办通过；表单只读 | `completeApproved` + `flow_status=approved` |
| 6 | 驳回分支 | 终止实例，`rejected`，再提交新实例 |
| 7 | USER 不能打开建模、不能改 LLM Key | `@RequiresPermission` |
| 8 | 超管操作日志可见发布与审批 | `@OperLog` + `/system/logs` |
| 9 | 停库再启，Flyway 不报校验失败 | 脚本只新增：V1.2.0～V1.5.0 |

已知限制：官方 warm-flow PostgreSQL 脚本未能拉取，使用自建 `sys_flow_*` 封装，接口与文档一致；`warm-flow.enabled=false`。Responses / Anthropic 协议提示改选 Chat 模型。

缺陷工单：无阻塞代码缺陷；运行时验收待补。
