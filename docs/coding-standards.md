# 编码规范

依据《阿里巴巴 Java 开发手册》，结合本仓库约定。Code Review 按本节逐项检查。

阶段 0 以文档 + Review 为准；推荐在 IDE 安装 Alibaba Java Coding Guidelines（P3C）插件。

## 1. 分层

| 层 | 允许 | 禁止 |
| --- | --- | --- |
| Controller | 取登录上下文、`@Validated` 参数校验、调 Service、返回 `R<T>` | 业务分支、算费、拼 SQL、直接调 Mapper |
| Service | 业务编排。主方法只写步骤，每步下沉到私有方法或协作类 | 直接输出 HTTP、堆 150 行以上的过程代码 |
| Manager | 封装第三方（LLM、工作流、动态 DDL）、多 DAO 组合、缓存 | 暴露给 Controller |
| Mapper | 数据访问 | 业务判断 |

Controller 不直接暴露 DO。对象分层：DO / DTO / Query / VO。

示例（健康检查，阶段 0 已落地）：

- `HealthController` 只调用 `HealthService.check()`
- `HealthService` 按步骤填充 VO
- `DbHealthManager` 执行 `SELECT 1`

## 2. 方法长度

- 主方法只写步骤（校验 → 落库 → 刷新缓存）。
- 单方法超过约 80 行建议拆；**超过 150 行必须拆**。

## 3. 命名

- 类名：UpperCamelCase（`HealthService`）
- 方法 / 变量：lowerCamelCase（`fillDbStatus`）
- 常量：全大写下划线（`MAX_PAGE_SIZE`）
- 包名：全小写，根包 `com.autosoft`
- 前端组件文件：PascalCase（`HealthView.vue`）
- 前端 API 模块按领域拆分（`api/health.ts`）

## 4. 魔法值

状态、类型、错误码用枚举（如 `ResultCode`）。禁止在业务里散落 `"UP"` / `1` 而不定义常量。探活结果等对外字段可与枚举的 `code` 对齐后输出。

## 5. 异常

- 业务失败抛 `BizException`，带 `ResultCode`。
- 禁止 `e.printStackTrace()`。
- 禁止空 `catch` 吞异常。
- `GlobalExceptionHandler` 记录完整栈，**响应体不返回堆栈**。
- 校验失败返回 400 语义的业务码，拼接字段错误信息。

## 6. 日志

- 使用 SLF4J，`{}` 占位，禁止字符串拼接拼日志。
- **禁止**输出密码、token、身份证号等敏感信息。
- 异常日志带上业务关键参数（用户 ID、实体编码），相当于保护案发现场。

## 7. API

- 统一包装 `com.autosoft.common.core.R`：`code == 0` 成功。
- Controller 禁止返回裸 `Map`。
- URL：阶段 0 为 `/api/health`；阶段 1 起资源风格，如 `/api/users`。
- 时间：库用 `TIMESTAMPTZ`，Java 用 `Instant`，JSON 为 ISO-8601（Spring Boot 4 使用 Jackson 3，定制入口为 `JsonMapperBuilderCustomizer`，不要再写 Jackson 2 的 `ObjectMapper`）。

## 8. 注释

- 类与 public 方法用中文说明「做什么」。
- 禁止无意义注释（如 `// get name`）。
- 复杂步骤在 Service 主方法用序号注释标出步骤即可。

## 9. 前端

- 组件 PascalCase；组合与工具 camelCase。
- HTTP 经 `api/http.ts` 解包 `R<T>`，`code !== 0` 时提示 `msg`。
- 阶段 0 不携带 token；阶段 1 在拦截器中统一附加。

## 10. Git

提交说明用中文或英文祈使句，写清「为什么」。禁止 `update`、`fix` 这种空信息。

示例：`补齐健康检查分层，避免 Controller 直接访问数据源`。
