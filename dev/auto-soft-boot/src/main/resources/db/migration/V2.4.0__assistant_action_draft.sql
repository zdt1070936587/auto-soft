-- 阶段 8：助手操作草稿
CREATE TABLE ai_assistant_action_draft (
    id              UUID PRIMARY KEY,
    session_id      BIGINT       NOT NULL REFERENCES ai_assistant_session(id) ON DELETE CASCADE,
    user_id         BIGINT       NOT NULL,
    capability_id   VARCHAR(128) NOT NULL,
    status          VARCHAR(16)  NOT NULL DEFAULT 'draft',
    target_path     VARCHAR(256) NOT NULL,
    target_type     VARCHAR(32)  NOT NULL,
    modal_key       VARCHAR(64),
    values_json     JSONB        NOT NULL DEFAULT '{}',
    display_json    JSONB        NOT NULL DEFAULT '{}',
    missing_json    JSONB        NOT NULL DEFAULT '[]',
    unknown_json    JSONB        NOT NULL DEFAULT '[]',
    expires_at      TIMESTAMPTZ  NOT NULL,
    consumed_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ai_action_draft_session ON ai_assistant_action_draft (session_id, status);
CREATE INDEX idx_ai_action_draft_user ON ai_assistant_action_draft (user_id, created_at DESC);
CREATE INDEX idx_ai_action_draft_expires ON ai_assistant_action_draft (expires_at)
    WHERE status IN ('draft', 'ready');
