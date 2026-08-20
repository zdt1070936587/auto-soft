-- 阶段 4：实体流程绑定与单线审批运行表。
-- 官方 warm-flow PostgreSQL 脚本实施时未能从仓库拉取；FlowManager 封装本表，接口契约与文档一致。
-- 引入 starter 坐标 org.dromara.warm:warm-flow-mybatis-plus-sb4-starter:1.8.9，默认不启用自动配置以免缺官方表导致无法启动。

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = current_schema() AND table_name = 'dyn_demo_item'
    ) THEN
        ALTER TABLE dyn_demo_item ADD COLUMN IF NOT EXISTS flow_status VARCHAR(32) NOT NULL DEFAULT 'none';
    END IF;
END $$;

CREATE TABLE meta_entity_flow (
    id                  BIGSERIAL PRIMARY KEY,
    entity_id           BIGINT       NOT NULL,
    flow_code           VARCHAR(64)  NOT NULL,
    definition_id       BIGINT,
    approve_role_codes  TEXT         NOT NULL,
    enabled             SMALLINT     NOT NULL DEFAULT 1,
    created_by          BIGINT       NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by          BIGINT       NOT NULL DEFAULT 0,
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted             SMALLINT     NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uk_meta_entity_flow_entity ON meta_entity_flow (entity_id) WHERE deleted = 0;

CREATE TABLE sys_flow_definition (
    id                  BIGSERIAL PRIMARY KEY,
    flow_code           VARCHAR(64)  NOT NULL,
    name                VARCHAR(128) NOT NULL,
    approve_role_codes  TEXT         NOT NULL,
    enabled             SMALLINT     NOT NULL DEFAULT 1,
    created_by          BIGINT       NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by          BIGINT       NOT NULL DEFAULT 0,
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted             SMALLINT     NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uk_sys_flow_definition_code ON sys_flow_definition (flow_code) WHERE deleted = 0;

CREATE TABLE sys_flow_instance (
    id              BIGSERIAL PRIMARY KEY,
    definition_id   BIGINT       NOT NULL,
    app_code        VARCHAR(32)  NOT NULL,
    entity_code     VARCHAR(32)  NOT NULL,
    biz_id          BIGINT       NOT NULL,
    status          VARCHAR(16)  NOT NULL,
    start_user_id   BIGINT       NOT NULL,
    current_level   INT          NOT NULL DEFAULT 0,
    created_by      BIGINT       NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by      BIGINT       NOT NULL DEFAULT 0,
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted         SMALLINT     NOT NULL DEFAULT 0
);

CREATE TABLE sys_flow_task (
    id             BIGSERIAL PRIMARY KEY,
    instance_id    BIGINT       NOT NULL,
    level_no       INT          NOT NULL,
    role_code      VARCHAR(64)  NOT NULL,
    status         VARCHAR(16)  NOT NULL,
    assignee_id    BIGINT,
    comment_text   VARCHAR(512),
    created_by     BIGINT       NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by     BIGINT       NOT NULL DEFAULT 0,
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted        SMALLINT     NOT NULL DEFAULT 0
);

INSERT INTO sys_menu (parent_id, name, path, component, menu_type, permission, icon, sort, visible, status, created_by, updated_by)
VALUES (0, '待办中心', '/flow', NULL, 'DIR', NULL, 'audit', 50, 1, 1, 0, 0);

INSERT INTO sys_menu (parent_id, name, path, component, menu_type, permission, icon, sort, visible, status, created_by, updated_by)
SELECT id, '我的待办', '/flow/todo', 'TodoView', 'MENU', 'flow:todo:list', 'check', 10, 1, 1, 0, 0
FROM sys_menu WHERE path = '/flow' AND deleted = 0;

INSERT INTO sys_menu (parent_id, name, path, component, menu_type, permission, icon, sort, visible, status, created_by, updated_by)
SELECT id, '我的已办', '/flow/done', 'DoneView', 'MENU', 'flow:done:list', 'history', 20, 1, 1, 0, 0
FROM sys_menu WHERE path = '/flow' AND deleted = 0;

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r CROSS JOIN sys_menu m
WHERE m.path IN ('/flow', '/flow/todo', '/flow/done')
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = m.id);
