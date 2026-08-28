-- 阶段 7 P2：助手跨会话记忆（pgvector）。

CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE ai_memory_episode (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT       NOT NULL,
    session_id      BIGINT,
    episode_type    VARCHAR(16)  NOT NULL,
    content_full    TEXT,
    content_summary TEXT         NOT NULL,
    importance      SMALLINT     NOT NULL DEFAULT 5,
    embedding       vector(1536),
    occurred_at     TIMESTAMPTZ  NOT NULL,
    decay_stage     SMALLINT     NOT NULL DEFAULT 0,
    created_by      BIGINT       NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by      BIGINT       NOT NULL DEFAULT 0,
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted         SMALLINT     NOT NULL DEFAULT 0
);

CREATE INDEX idx_ai_mem_ep_user_time ON ai_memory_episode (user_id, occurred_at DESC);
CREATE INDEX idx_ai_mem_ep_embedding ON ai_memory_episode
    USING hnsw (embedding vector_cosine_ops);

CREATE TABLE ai_memory_fact (
    id                BIGSERIAL PRIMARY KEY,
    user_id           BIGINT       NOT NULL,
    category          VARCHAR(32)  NOT NULL,
    fact_key          VARCHAR(64)  NOT NULL,
    fact_value        TEXT         NOT NULL,
    confidence        REAL         NOT NULL DEFAULT 0.8,
    confirmed         SMALLINT     NOT NULL DEFAULT 0,
    source_episode_id BIGINT,
    embedding         vector(1536),
    last_used_at      TIMESTAMPTZ,
    created_by        BIGINT       NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by        BIGINT       NOT NULL DEFAULT 0,
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted           SMALLINT     NOT NULL DEFAULT 0,
    UNIQUE (user_id, category, fact_key)
);

CREATE INDEX idx_ai_mem_fact_user ON ai_memory_fact (user_id, category);
CREATE INDEX idx_ai_mem_fact_embedding ON ai_memory_fact
    USING hnsw (embedding vector_cosine_ops);
