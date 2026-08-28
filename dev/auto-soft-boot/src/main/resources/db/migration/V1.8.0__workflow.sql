-- 阶段 6A：自动化工作流定义、运行、站内通知。wf_share 建表但不暴露 API。

CREATE TABLE wf_definition (
    id              BIGSERIAL PRIMARY KEY,
    app_id          BIGINT       NOT NULL,
    code            VARCHAR(32)  NOT NULL,
    name            VARCHAR(128) NOT NULL,
    status          VARCHAR(16)  NOT NULL DEFAULT 'DRAFT',
    graph_json      TEXT         NOT NULL,
    version         INT          NOT NULL DEFAULT 0,
    grant_roles     VARCHAR(256) NOT NULL DEFAULT 'USER',
    visibility      VARCHAR(16)  NOT NULL DEFAULT 'private',
    created_by      BIGINT       NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by      BIGINT       NOT NULL DEFAULT 0,
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted         SMALLINT     NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uk_wf_definition_app_alive ON wf_definition (app_id) WHERE deleted = 0;
CREATE UNIQUE INDEX uk_wf_definition_code_alive ON wf_definition (code) WHERE deleted = 0;

CREATE TABLE wf_definition_version (
    id              BIGSERIAL PRIMARY KEY,
    definition_id   BIGINT       NOT NULL,
    version         INT          NOT NULL,
    graph_json      TEXT         NOT NULL,
    created_by      BIGINT       NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by      BIGINT       NOT NULL DEFAULT 0,
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted         SMALLINT     NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uk_wf_definition_version ON wf_definition_version (definition_id, version) WHERE deleted = 0;

CREATE TABLE wf_run (
    id              BIGSERIAL PRIMARY KEY,
    definition_id   BIGINT       NOT NULL,
    version         INT          NOT NULL DEFAULT 0,
    dry_run         SMALLINT     NOT NULL DEFAULT 0,
    status          VARCHAR(16)  NOT NULL,
    trigger_json    TEXT,
    current_node_id VARCHAR(32),
    token_input     BIGINT       NOT NULL DEFAULT 0,
    token_output    BIGINT       NOT NULL DEFAULT 0,
    error_msg       VARCHAR(512),
    start_user_id   BIGINT       NOT NULL DEFAULT 0,
    created_by      BIGINT       NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by      BIGINT       NOT NULL DEFAULT 0,
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted         SMALLINT     NOT NULL DEFAULT 0
);

CREATE TABLE wf_run_step (
    id              BIGSERIAL PRIMARY KEY,
    run_id          BIGINT       NOT NULL,
    node_id         VARCHAR(32)  NOT NULL,
    node_type       VARCHAR(32)  NOT NULL,
    status          VARCHAR(16)  NOT NULL,
    input_summary   TEXT,
    output_summary  TEXT,
    error_msg       VARCHAR(512),
    duration_ms     INT          NOT NULL DEFAULT 0,
    created_by      BIGINT       NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by      BIGINT       NOT NULL DEFAULT 0,
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted         SMALLINT     NOT NULL DEFAULT 0
);

CREATE TABLE wf_notice (
    id              BIGSERIAL PRIMARY KEY,
    run_id          BIGINT,
    to_role         VARCHAR(64)  NOT NULL,
    title           VARCHAR(128) NOT NULL,
    body            TEXT,
    created_by      BIGINT       NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by      BIGINT       NOT NULL DEFAULT 0,
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted         SMALLINT     NOT NULL DEFAULT 0
);

CREATE TABLE wf_share (
    id              BIGSERIAL PRIMARY KEY,
    definition_id   BIGINT       NOT NULL,
    token           VARCHAR(64)  NOT NULL,
    permission      VARCHAR(16)  NOT NULL,
    expire_at       TIMESTAMPTZ,
    created_by      BIGINT       NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by      BIGINT       NOT NULL DEFAULT 0,
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted         SMALLINT     NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uk_wf_share_token_alive ON wf_share (token) WHERE deleted = 0;

INSERT INTO sys_menu (parent_id, name, path, component, menu_type, permission, icon, sort, visible, status, created_by, updated_by)
SELECT id, '工作流', '/studio?kind=workflow', 'StudioView', 'MENU', 'studio:use', 'apartment', 45, 1, 1, 0, 0
FROM sys_menu WHERE path = '/studio' AND deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_menu x WHERE x.path = '/studio?kind=workflow' AND x.deleted = 0);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r
CROSS JOIN sys_menu m
WHERE m.path = '/studio?kind=workflow' AND m.deleted = 0
  AND r.code IN ('SUPER_ADMIN', 'DEVELOPER')
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = m.id);
