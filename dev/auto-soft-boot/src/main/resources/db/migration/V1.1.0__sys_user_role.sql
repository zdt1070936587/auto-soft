-- 阶段 1：用户、角色、菜单与种子数据。
-- 默认密码均为 admin123，哈希由 BCryptPasswordEncoder 兼容的 bcryptjs 生成，仅开发库使用。
-- BCrypt(admin123) = $2b$10$1ps496QsJd6ozsD97nL4IO2Xg1Si.BdwXgP8AJ2qME38uvtCLi7g.

CREATE TABLE sys_user (
    id             BIGSERIAL PRIMARY KEY,
    username       VARCHAR(64)  NOT NULL,
    password       VARCHAR(128) NOT NULL,
    nickname       VARCHAR(64)  NOT NULL,
    status         SMALLINT     NOT NULL DEFAULT 1,
    last_login_at  TIMESTAMPTZ,
    created_by     BIGINT       NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by     BIGINT       NOT NULL DEFAULT 0,
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted        SMALLINT     NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uk_sys_user_username_alive ON sys_user (username) WHERE deleted = 0;
COMMENT ON TABLE sys_user IS '系统用户';
COMMENT ON COLUMN sys_user.password IS 'BCrypt 密文，禁止明文';
COMMENT ON COLUMN sys_user.status IS '1 启用 0 停用';

CREATE TABLE sys_role (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(64)  NOT NULL,
    name        VARCHAR(64)  NOT NULL,
    remark      VARCHAR(256),
    sort        INT          NOT NULL DEFAULT 0,
    status      SMALLINT     NOT NULL DEFAULT 1,
    builtin     SMALLINT     NOT NULL DEFAULT 0,
    created_by  BIGINT       NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by  BIGINT       NOT NULL DEFAULT 0,
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted     SMALLINT     NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uk_sys_role_code_alive ON sys_role (code) WHERE deleted = 0;
COMMENT ON TABLE sys_role IS '系统角色';
COMMENT ON COLUMN sys_role.builtin IS '1 内置不可删';

CREATE TABLE sys_user_role (
    id       BIGSERIAL PRIMARY KEY,
    user_id  BIGINT NOT NULL,
    role_id  BIGINT NOT NULL,
    CONSTRAINT uk_sys_user_role UNIQUE (user_id, role_id)
);

COMMENT ON TABLE sys_user_role IS '用户角色关联，解绑物理删除';

CREATE TABLE sys_menu (
    id          BIGSERIAL PRIMARY KEY,
    parent_id   BIGINT       NOT NULL DEFAULT 0,
    name        VARCHAR(64)  NOT NULL,
    path        VARCHAR(128),
    component   VARCHAR(128),
    menu_type   VARCHAR(16)  NOT NULL,
    permission  VARCHAR(128),
    icon        VARCHAR(64),
    sort        INT          NOT NULL DEFAULT 0,
    visible     SMALLINT     NOT NULL DEFAULT 1,
    status      SMALLINT     NOT NULL DEFAULT 1,
    created_by  BIGINT       NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by  BIGINT       NOT NULL DEFAULT 0,
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted     SMALLINT     NOT NULL DEFAULT 0
);

COMMENT ON TABLE sys_menu IS '系统菜单与按钮权限';
COMMENT ON COLUMN sys_menu.menu_type IS 'DIR / MENU / BUTTON';
COMMENT ON COLUMN sys_menu.visible IS '1 显示 0 隐藏（隐藏仍可授权接口）';

CREATE TABLE sys_role_menu (
    id       BIGSERIAL PRIMARY KEY,
    role_id  BIGINT NOT NULL,
    menu_id  BIGINT NOT NULL,
    CONSTRAINT uk_sys_role_menu UNIQUE (role_id, menu_id)
);

COMMENT ON TABLE sys_role_menu IS '角色菜单关联，解绑物理删除';

INSERT INTO sys_role (id, code, name, remark, sort, status, builtin, created_by, updated_by)
OVERRIDING SYSTEM VALUE
VALUES
    (1, 'SUPER_ADMIN', '超级管理员', '内置，鉴权绕过权限码', 10, 1, 1, 0, 0),
    (2, 'ADMIN', '管理员', '用户与角色维护', 20, 1, 1, 0, 0),
    (3, 'DEVELOPER', '开发者', '阶段 1 仅工作台', 30, 1, 1, 0, 0),
    (4, 'USER', '普通用户', '阶段 1 仅工作台', 40, 1, 1, 0, 0);

SELECT setval('sys_role_id_seq', 4);

INSERT INTO sys_user (id, username, password, nickname, status, created_by, updated_by)
OVERRIDING SYSTEM VALUE
VALUES
    (1, 'admin', '$2b$10$1ps496QsJd6ozsD97nL4IO2Xg1Si.BdwXgP8AJ2qME38uvtCLi7g.', '超级管理员', 1, 0, 0),
    (2, 'demo_admin', '$2b$10$1ps496QsJd6ozsD97nL4IO2Xg1Si.BdwXgP8AJ2qME38uvtCLi7g.', '演示管理员', 1, 0, 0),
    (3, 'demo_dev', '$2b$10$1ps496QsJd6ozsD97nL4IO2Xg1Si.BdwXgP8AJ2qME38uvtCLi7g.', '演示开发者', 1, 0, 0),
    (4, 'demo_user', '$2b$10$1ps496QsJd6ozsD97nL4IO2Xg1Si.BdwXgP8AJ2qME38uvtCLi7g.', '演示普通用户', 1, 0, 0);

SELECT setval('sys_user_id_seq', 4);

INSERT INTO sys_user_role (user_id, role_id) VALUES
    (1, 1),
    (2, 2),
    (3, 3),
    (4, 4);

-- 菜单：1 工作台；2 系统管理；3 用户；4-7 用户按钮；8 角色；9-13 角色按钮
INSERT INTO sys_menu (id, parent_id, name, path, component, menu_type, permission, icon, sort, visible, status, created_by, updated_by)
OVERRIDING SYSTEM VALUE
VALUES
    (1, 0, '工作台', '/dashboard', 'WorkbenchView', 'MENU', NULL, 'dashboard', 10, 1, 1, 0, 0),
    (2, 0, '系统管理', '/system', NULL, 'DIR', NULL, 'setting', 20, 1, 1, 0, 0),
    (3, 2, '用户管理', '/system/users', 'UserView', 'MENU', 'system:user:list', 'user', 10, 1, 1, 0, 0),
    (4, 3, '查询用户', NULL, NULL, 'BUTTON', 'system:user:list', NULL, 10, 0, 1, 0, 0),
    (5, 3, '新建用户', NULL, NULL, 'BUTTON', 'system:user:create', NULL, 20, 0, 1, 0, 0),
    (6, 3, '修改用户', NULL, NULL, 'BUTTON', 'system:user:update', NULL, 30, 0, 1, 0, 0),
    (7, 3, '删除用户', NULL, NULL, 'BUTTON', 'system:user:delete', NULL, 40, 0, 1, 0, 0),
    (8, 2, '角色管理', '/system/roles', 'RoleView', 'MENU', 'system:role:list', 'team', 20, 1, 1, 0, 0),
    (9, 8, '查询角色', NULL, NULL, 'BUTTON', 'system:role:list', NULL, 10, 0, 1, 0, 0),
    (10, 8, '新建角色', NULL, NULL, 'BUTTON', 'system:role:create', NULL, 20, 0, 1, 0, 0),
    (11, 8, '修改角色', NULL, NULL, 'BUTTON', 'system:role:update', NULL, 30, 0, 1, 0, 0),
    (12, 8, '删除角色', NULL, NULL, 'BUTTON', 'system:role:delete', NULL, 40, 0, 1, 0, 0),
    (13, 8, '分配菜单', NULL, NULL, 'BUTTON', 'system:role:grant', NULL, 50, 0, 1, 0, 0);

SELECT setval('sys_menu_id_seq', 13);

-- SUPER_ADMIN：全部菜单
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu WHERE deleted = 0;

-- ADMIN：工作台 + 系统管理下用户/角色（含按钮）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 2, id FROM sys_menu WHERE id IN (1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13);

-- DEVELOPER / USER：仅工作台
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (3, 1), (4, 1);
