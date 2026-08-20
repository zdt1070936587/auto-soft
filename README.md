# auto-soft

AI 管理后台：用户通过自然语言与系统对话，即可生成自己需要的后台功能（动态 CRUD + 审批流）。

当前进度：阶段 0（脚手架与规范）。登录、用户权限、功能开发工作室尚未实现。

## 目录

| 目录 | 说明 |
| --- | --- |
| `docs/` | 设计文档与开发规范 |
| `dev/` | Java / Spring Boot 4 后端 |
| `web/` | Vue 3 前端 |

## 快速开始

需已安装 JDK 21、Maven 3.9+、Node.js 20+、Docker。

```bash
# 1. 启动 PostgreSQL（开发库，密码仅用于本地）
docker compose up -d

# 2. 启动后端（http://127.0.0.1:8080）
cd dev
mvn -pl auto-soft-boot -am spring-boot:run

# 3. 另开终端启动前端（http://localhost:5173）
cd web
npm install
npm run dev
```

完整步骤与排障见 [docs/dev-setup.md](docs/dev-setup.md)。整体计划见 [docs/分阶段开发计划.md](docs/分阶段开发计划.md)。
