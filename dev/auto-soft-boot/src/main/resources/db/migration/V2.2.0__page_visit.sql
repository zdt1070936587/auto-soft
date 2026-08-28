-- 阶段 7 P4：页面访问埋点（Assistant 浏览历史）。

CREATE TABLE sys_page_visit (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT       NOT NULL,
    path         VARCHAR(256) NOT NULL,
    route_name   VARCHAR(64),
    page_title   VARCHAR(128),
    visited_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_sys_page_visit_user_time ON sys_page_visit (user_id, visited_at DESC);
CREATE INDEX idx_sys_page_visit_user_path ON sys_page_visit (user_id, path, visited_at DESC);
