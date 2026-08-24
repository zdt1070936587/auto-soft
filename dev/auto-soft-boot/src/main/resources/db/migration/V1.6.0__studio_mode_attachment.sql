-- 工作室：工作级别与附件

ALTER TABLE ai_session
    ADD COLUMN IF NOT EXISTS agent_mode VARCHAR(16) NOT NULL DEFAULT 'develop';

CREATE TABLE IF NOT EXISTS ai_attachment (
    id             BIGSERIAL PRIMARY KEY,
    session_id     BIGINT       NOT NULL,
    message_id     BIGINT,
    file_name      VARCHAR(256) NOT NULL,
    content_type   VARCHAR(128) NOT NULL,
    size_bytes     BIGINT       NOT NULL DEFAULT 0,
    kind           VARCHAR(16)  NOT NULL,
    storage_path   VARCHAR(512) NOT NULL,
    created_by     BIGINT       NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by     BIGINT       NOT NULL DEFAULT 0,
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted        SMALLINT     NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_ai_attachment_session ON ai_attachment (session_id);
