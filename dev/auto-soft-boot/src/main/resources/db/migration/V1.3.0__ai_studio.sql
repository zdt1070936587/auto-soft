-- 阶段 3：LLM 配置与工作室会话。

CREATE TABLE sys_llm_config (
    id                   BIGSERIAL PRIMARY KEY,
    api_key_cipher       TEXT,
    api_key_iv           VARCHAR(64),
    default_model        VARCHAR(128) NOT NULL DEFAULT 'kimi-k2.7-code',
    allowed_models_json  TEXT,
    created_by           BIGINT       NOT NULL DEFAULT 0,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by           BIGINT       NOT NULL DEFAULT 0,
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted              SMALLINT     NOT NULL DEFAULT 0
);

INSERT INTO sys_llm_config (default_model) VALUES ('kimi-k2.7-code');

CREATE TABLE ai_session (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT       NOT NULL,
    title         VARCHAR(128) NOT NULL DEFAULT '新会话',
    app_id        BIGINT,
    status        VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    token_input   BIGINT       NOT NULL DEFAULT 0,
    token_output  BIGINT       NOT NULL DEFAULT 0,
    created_by    BIGINT       NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by    BIGINT       NOT NULL DEFAULT 0,
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted       SMALLINT     NOT NULL DEFAULT 0
);

CREATE TABLE ai_message (
    id            BIGSERIAL PRIMARY KEY,
    session_id    BIGINT       NOT NULL,
    role          VARCHAR(16)  NOT NULL,
    content       TEXT,
    tool_name     VARCHAR(64),
    tool_call_id  VARCHAR(64),
    tokens        INT          NOT NULL DEFAULT 0,
    created_by    BIGINT       NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by    BIGINT       NOT NULL DEFAULT 0,
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted       SMALLINT     NOT NULL DEFAULT 0
);

CREATE TABLE ai_tool_log (
    id              BIGSERIAL PRIMARY KEY,
    session_id      BIGINT       NOT NULL,
    tool_name       VARCHAR(64)  NOT NULL,
    arguments_json  TEXT,
    result_json     TEXT,
    success         SMALLINT     NOT NULL DEFAULT 1,
    error_msg       VARCHAR(512),
    duration_ms     INT          NOT NULL DEFAULT 0,
    created_by      BIGINT       NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by      BIGINT       NOT NULL DEFAULT 0,
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted         SMALLINT     NOT NULL DEFAULT 0
);

INSERT INTO sys_menu (parent_id, name, path, component, menu_type, permission, icon, sort, visible, status, created_by, updated_by)
VALUES (0, '功能开发', '/studio', 'StudioView', 'MENU', 'studio:use', 'robot', 40, 1, 1, 0, 0);

INSERT INTO sys_menu (parent_id, name, path, component, menu_type, permission, icon, sort, visible, status, created_by, updated_by)
SELECT id, '模型设置', '/system/llm', 'LlmSettingView', 'MENU', 'system:llm:manage', 'cloud', 30, 1, 1, 0, 0
FROM sys_menu WHERE path = '/system' AND deleted = 0;

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r CROSS JOIN sys_menu m
WHERE r.code IN ('SUPER_ADMIN', 'DEVELOPER') AND m.path = '/studio'
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = m.id);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r CROSS JOIN sys_menu m
WHERE r.code = 'SUPER_ADMIN' AND m.path = '/system/llm'
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = m.id);
