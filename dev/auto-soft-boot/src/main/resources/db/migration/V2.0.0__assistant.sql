-- 阶段 7 P0：全局 AI 助手会话与权限。

CREATE TABLE ai_assistant_session (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT       NOT NULL,
    title         VARCHAR(128) NOT NULL DEFAULT '新对话',
    status        VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    token_input   BIGINT       NOT NULL DEFAULT 0,
    token_output  BIGINT       NOT NULL DEFAULT 0,
    created_by    BIGINT       NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by    BIGINT       NOT NULL DEFAULT 0,
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted       SMALLINT     NOT NULL DEFAULT 0
);

CREATE INDEX idx_ai_asst_session_user ON ai_assistant_session (user_id, created_at DESC);

CREATE TABLE ai_assistant_message (
    id            BIGSERIAL PRIMARY KEY,
    session_id    BIGINT       NOT NULL,
    role          VARCHAR(16)  NOT NULL,
    content       TEXT,
    payload_json  TEXT,
    tool_name     VARCHAR(64),
    tool_call_id  VARCHAR(64),
    tokens        INT          NOT NULL DEFAULT 0,
    created_by    BIGINT       NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by    BIGINT       NOT NULL DEFAULT 0,
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted       SMALLINT     NOT NULL DEFAULT 0
);

CREATE INDEX idx_ai_asst_msg_session ON ai_assistant_message (session_id, id);

CREATE TABLE ai_assistant_tool_log (
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

-- 权限：assistant:use（隐藏按钮，FAB 即入口）
INSERT INTO sys_menu (parent_id, name, path, component, menu_type, permission, icon, sort, visible, status, created_by, updated_by)
VALUES (0, 'AI助手', NULL, NULL, 'BUTTON', 'assistant:use', NULL, 999, 0, 1, 0, 0);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r CROSS JOIN sys_menu m
WHERE r.code IN ('SUPER_ADMIN', 'ADMIN', 'DEVELOPER', 'USER') AND m.permission = 'assistant:use'
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = m.id);
