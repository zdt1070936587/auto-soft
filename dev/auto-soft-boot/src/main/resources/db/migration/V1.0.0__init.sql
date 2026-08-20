-- 阶段 0 基线：仅用于验证 Flyway。业务表从阶段 1 的 V1.1.0 开始。
CREATE TABLE IF NOT EXISTS sys_schema_meta (
    id          BIGSERIAL PRIMARY KEY,
    phase       VARCHAR(32)  NOT NULL,
    remark      VARCHAR(256) NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE sys_schema_meta IS 'schema 迁移备注，非业务表';

INSERT INTO sys_schema_meta (phase, remark)
VALUES ('phase-0', 'flyway baseline');
