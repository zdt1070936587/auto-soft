-- 工作流 HTTP 出站域名白名单（管理端维护）。

CREATE TABLE sys_workflow_http_host (
    id          BIGSERIAL PRIMARY KEY,
    host        VARCHAR(253) NOT NULL,
    remark      VARCHAR(256),
    created_by  BIGINT       NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by  BIGINT       NOT NULL DEFAULT 0,
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted     SMALLINT     NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uk_sys_workflow_http_host_host
    ON sys_workflow_http_host (LOWER(host))
    WHERE deleted = 0;

INSERT INTO sys_menu (parent_id, name, path, component, menu_type, permission, icon, sort, visible, status, created_by, updated_by)
SELECT id, 'HTTP 出站白名单', '/system/workflow-http-hosts', 'WorkflowHttpHostView', 'MENU', 'system:workflow:http:manage', 'global', 35, 1, 1, 0, 0
FROM sys_menu WHERE path = '/system' AND deleted = 0;

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r CROSS JOIN sys_menu m
WHERE r.code = 'SUPER_ADMIN' AND m.path = '/system/workflow-http-hosts'
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = m.id);
