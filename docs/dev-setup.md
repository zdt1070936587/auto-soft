# 本地开发环境

阶段 0 验收：PostgreSQL 可连、后端健康检查返回 `db=UP`、前端页面能展示同一结果。

## 1. 安装

| 软件 | 版本 |
| --- | --- |
| JDK | 21 |
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

## 3. 启动后端

```bash
cd dev
mvn -pl auto-soft-boot -am spring-boot:run
```

也可在 IDE 运行 `com.autosoft.AutoSoftApplication`，Active profile 为 `dev`。

端口：`8080`。

验收：

```bash
curl http://127.0.0.1:8080/api/health
curl http://127.0.0.1:8080/actuator/health
```

`/api/health` 成功时应类似：

```json
{
  "code": 0,
  "msg": "ok",
  "data": {
    "appName": "auto-soft",
    "profile": "dev",
    "db": "UP",
    "now": "2026-08-20T06:00:00Z"
  }
}
```

首次启动 Flyway 会执行 `V1.0.0__init.sql`，库中应有 `flyway_schema_history` 与 `sys_schema_meta`。

本地覆盖配置请使用 `application-local.yml`（已 gitignore），不要把真实密钥提交进仓库。

## 4. 启动前端

另开终端：

```bash
cd web
npm install
npm run dev
```

浏览器打开 [http://localhost:5173](http://localhost:5173)。Vite 将 `/api` 代理到 `http://127.0.0.1:8080`。页面应显示应用名、profile、数据库状态（绿色 UP / 红色 DOWN）和服务器时间。

## 5. 常见问题

| 现象 | 处理 |
| --- | --- |
| 后端起不来，连不上库 | 先 `docker compose ps`，确认容器 healthy/running；核对端口 |
| Flyway 校验失败 | 开发空库可 `docker compose down -v` 后重建；**不要改已执行的 SQL 文件** |
| 前端提示无法连接后端 | 确认 8080 已启动；确认 `vite.config.ts` 里 proxy 指向 8080 |
| 浏览器跨域 | 开发走 Vite 代理即可；若直连 8080，framework 已允许 `http://localhost:5173` |
| `mvn` 编译失败、JDK 不对 | `java -version` 必须是 21+。本仓库 enforcer 要求 `[21,)`。若系统 `JAVA_HOME` 仍指向 JDK 8，编译前先切换，例如：`$env:JAVA_HOME="C:\Program Files\Java\JDK25\jdk-25+36"` |
| 前端 Vite 起不来、Node 过旧 | 需要 Node 20+。本仓库 `web/package.json` 已用 Volta 锁定 `20.18.0`，安装 Volta 后在 `web/` 下执行命令即可自动切换 |
| 健康接口 `db=DOWN` 但进程还在 | 启动后 Postgres 被停掉时的预期行为；接口仍返回 `code=0`，用 `data.db` 表达库状态 |

## 6. 阶段 0 不包含

登录页、JWT、用户管理、OpenCode、warm-flow。不要在本阶段往仓库加这些依赖或页面。
