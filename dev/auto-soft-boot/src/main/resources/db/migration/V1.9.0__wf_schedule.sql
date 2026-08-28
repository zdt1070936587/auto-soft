-- 阶段 6C：工作流定时触发。同一 definition 同时只允许一个运行中实例（应用层约束）。

CREATE TABLE wf_schedule (
    id              BIGSERIAL PRIMARY KEY,
    definition_id   BIGINT       NOT NULL,
    cron            VARCHAR(64)  NOT NULL,
    enabled         SMALLINT     NOT NULL DEFAULT 1,
    last_run_at     TIMESTAMPTZ,
    created_by      BIGINT       NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by      BIGINT       NOT NULL DEFAULT 0,
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted         SMALLINT     NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uk_wf_schedule_def_alive ON wf_schedule (definition_id) WHERE deleted = 0;
