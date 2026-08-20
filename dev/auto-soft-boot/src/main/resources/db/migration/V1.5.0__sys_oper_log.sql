-- 阶段 5：操作日志。

CREATE TABLE sys_oper_log (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT,
    username     VARCHAR(64),
    module       VARCHAR(32)  NOT NULL,
    action       VARCHAR(32)  NOT NULL,
    biz_id       VARCHAR(64),
    success      SMALLINT     NOT NULL DEFAULT 1,
    ip           VARCHAR(64),
    cost_ms      INT          NOT NULL DEFAULT 0,
    detail_json  TEXT,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

INSERT INTO sys_menu (parent_id, name, path, component, menu_type, permission, icon, sort, visible, status, created_by, updated_by)
SELECT id, '操作日志', '/system/logs', 'OperLogView', 'MENU', 'system:log:list', 'file-search', 40, 1, 1, 0, 0
FROM sys_menu WHERE path = '/system' AND deleted = 0;

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r CROSS JOIN sys_menu m
WHERE r.code = 'SUPER_ADMIN' AND m.path = '/system/logs'
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = m.id);
