# 本地开发环境

阶段 1 验收：PostgreSQL 可连、能用默认账号登录、侧栏菜单按角色不同、用户/角色页可用。健康检查仍匿名：`GET /api/health`。

## 1. 安装

| 软件 | 版本 |
| --- | --- |
| JDK | 25 |
| Maven | 3.9+ |
| Node.js | 20 LTS+（建议 22） |
| Docker Desktop | 能跑 `postgres:16` 即可 |

验证：

```bash
java -version
mvn -version
node -v
docker -v
```

## 2. 启动数据库

在仓库根目录：

```bash
docker compose up -d
```

默认映射 `5432:5432`，库名 / 用户 / 密码均为 `autosoft`（仅本地开发）。

**5432 被占用**：修改 `docker-compose.yml` 为 `15432:5432`，并把 `dev/auto-soft-boot/src/main/resources/application-dev.yml` 的 URL 改成 `jdbc:postgresql://127.0.0.1:15432/autosoft`。

首次启动 Flyway 会执行 `V1.0.0` 起的迁移（含阶段 6A 的 `V1.8.0__workflow.sql` 与 6C 的 `V1.9.0__wf_schedule.sql`）。

## 3. 启动后端

```bash
cd dev
mvn -pl auto-soft-boot -am spring-boot:run
```

也可在 IDE 运行 `com.autosoft.AutoSoftApplication`，Active profile 为 `dev`。

端口：`8080`。

匿名验收：

```bash
curl http://127.0.0.1:8080/api/health
curl http://127.0.0.1:8080/actuator/health
```

登录（默认密码仅开发库）：

```bash
curl -X POST http://127.0.0.1:8080/api/auth/login ^
  -H "Content-Type: application/json" ^
  -d "{\"username\":\"admin\",\"password\":\"admin123\"}"
```

JWT 密钥在 `application.yml` / `application-dev.yml` 的 `autosoft.jwt.secret`，开发占位可提交。生产用环境变量或 `application-local.yml`（已 gitignore）。

## 4. 启动前端

另开终端：

```bash
cd web
npm install
npm run dev
```

浏览器打开 [http://localhost:5173](http://localhost:5173)，应进入太阳系登录页。Vite 将 `/api` 代理到 `http://127.0.0.1:8080`。

### 默认账号（密码均为 `admin123`）

| 用户名 | 角色 | 可见菜单 |
| --- | --- | --- |
| `admin` | 超级管理员 | 工作台、用户、角色、模型设置、操作日志、建模、工作室、待办 |
| `demo_admin` | 管理员 | 工作台、用户、角色、待办；发布后的动态应用需授权 |
| `demo_dev` | 开发者 | 工作台、应用建模、功能开发、待办 |
| `demo_user` | 普通用户 | 工作台、待办；发布并授权后可见 `/app/...` |

登录后进入 `/dashboard`。健康页：[http://localhost:5173/dev/health](http://localhost:5173/dev/health)。

公开注册关闭：`POST /api/auth/register` 返回 403。

OpenCode / AES 开发占位：`application.yml` 的 `autosoft.opencode.base-url`、`autosoft.crypto.aes-key`。生产务必替换 JWT secret、AES 材料，并用环境变量注入 API Key。

首次启动 Flyway 会执行 `V1.0.0`～`V1.5.0`。

## 5. 常见问题

| 现象 | 处理 |
| --- | --- |
| 后端起不来，连不上库 | 先 `docker compose ps`，确认容器 healthy/running；核对端口 |
| Flyway 校验失败 | 开发空库可 `docker compose down -v` 后重建；**不要改已执行的 SQL 文件** |
| 前端提示无法连接后端 | 确认 8080 已启动；确认 `vite.config.ts` 里 proxy 指向 8080 |
| 浏览器跨域 | 开发走 Vite 代理即可；若直连 8080，framework 已允许 `http://localhost:5173` |
| `mvn` 编译失败、JDK 不对 | `java -version` 必须是 25+。本仓库 enforcer 要求 `[25,)`。若系统 `JAVA_HOME` 仍指向旧 JDK，编译前先切换，例如：`$env:JAVA_HOME="C:\Program Files\Java\JDK25\jdk-25+36"` |
| 前端 Vite 起不来、Node 过旧 | 需要 Node 20+。本仓库 `web/package.json` 已用 Volta 锁定 `20.18.0`，安装 Volta 后在 `web/` 下执行命令即可自动切换 |
| 健康接口 `db=DOWN` 但进程还在 | 启动后 Postgres 被停掉时的预期行为；接口仍返回 `code=0`，用 `data.db` 表达库状态 |
| 登录 401「用户名或密码错误」 | 确认 Flyway `V1.1.0` 已执行、种子用户存在；密码为 `admin123` |
| 登录后无菜单 | 用 `admin` 或对应角色；USER 需应用发布后刷新菜单 |
| Maven ACCESS_VIOLATION（JDK 25） | `$env:MAVEN_OPTS="-Djansi.passthrough=true"` 后再 `mvn -B -DskipTests clean compile` |
| 工作室 SSE 被代理断开 | Vite `/api` proxy 已设长超时；nginx 需关闭缓冲 |

## 6. 模块启动顺序

1. PostgreSQL（`docker compose up -d`）
2. 后端 `cd dev && mvn -pl auto-soft-boot -am spring-boot:run`（JDK 25+）
3. 前端 `cd web && npm run dev`

接口说明：[api.md](./api.md)；操作手册：[user-guide.md](./user-guide.md)。
