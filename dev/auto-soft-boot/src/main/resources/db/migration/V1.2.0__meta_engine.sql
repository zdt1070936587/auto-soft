-- 阶段 2：元数据引擎。演示 app 以 DRAFT 写入，发布由接口完成。

CREATE TABLE meta_app (
    id           BIGSERIAL PRIMARY KEY,
    code         VARCHAR(32)  NOT NULL,
    name         VARCHAR(64)  NOT NULL,
    status       VARCHAR(16)  NOT NULL DEFAULT 'DRAFT',
    version      INT          NOT NULL DEFAULT 0,
    grant_roles  VARCHAR(256) NOT NULL DEFAULT 'USER',
    remark       VARCHAR(256),
    created_by   BIGINT       NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by   BIGINT       NOT NULL DEFAULT 0,
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted      SMALLINT     NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uk_meta_app_code_alive ON meta_app (code) WHERE deleted = 0;

CREATE TABLE meta_entity (
    id          BIGSERIAL PRIMARY KEY,
    app_id      BIGINT       NOT NULL,
    code        VARCHAR(32)  NOT NULL,
    name        VARCHAR(64)  NOT NULL,
    remark      VARCHAR(256),
    created_by  BIGINT       NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by  BIGINT       NOT NULL DEFAULT 0,
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted     SMALLINT     NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uk_meta_entity_app_code_alive ON meta_entity (app_id, code) WHERE deleted = 0;

CREATE TABLE meta_field (
    id             BIGSERIAL PRIMARY KEY,
    entity_id      BIGINT       NOT NULL,
    code           VARCHAR(32)  NOT NULL,
    name           VARCHAR(64)  NOT NULL,
    field_type     VARCHAR(16)  NOT NULL,
    length         INT,
    nullable_flag  SMALLINT     NOT NULL DEFAULT 1,
    default_value  VARCHAR(256),
    options_json   TEXT,
    ref_app        VARCHAR(32),
    ref_entity     VARCHAR(32),
    sort           INT          NOT NULL DEFAULT 0,
    queryable      SMALLINT     NOT NULL DEFAULT 0,
    listed         SMALLINT     NOT NULL DEFAULT 1,
    required_flag  SMALLINT     NOT NULL DEFAULT 0,
    created_by     BIGINT       NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by     BIGINT       NOT NULL DEFAULT 0,
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted        SMALLINT     NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uk_meta_field_entity_code_alive ON meta_field (entity_id, code) WHERE deleted = 0;

CREATE TABLE meta_page (
    id           BIGSERIAL PRIMARY KEY,
    entity_id    BIGINT       NOT NULL,
    page_type    VARCHAR(16)  NOT NULL,
    schema_json  TEXT,
    created_by   BIGINT       NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by   BIGINT       NOT NULL DEFAULT 0,
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted      SMALLINT     NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uk_meta_page_entity_type_alive ON meta_page (entity_id, page_type) WHERE deleted = 0;

CREATE TABLE meta_app_menu (
    id          BIGSERIAL PRIMARY KEY,
    app_id      BIGINT       NOT NULL,
    menu_id     BIGINT       NOT NULL,
    created_by  BIGINT       NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by  BIGINT       NOT NULL DEFAULT 0,
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted     SMALLINT     NOT NULL DEFAULT 0
);

COMMENT ON TABLE meta_app IS '元数据应用';
COMMENT ON TABLE meta_entity IS '元数据实体';
COMMENT ON TABLE meta_field IS '元数据字段';
COMMENT ON COLUMN meta_field.nullable_flag IS '1 可空 0 否';
COMMENT ON COLUMN meta_field.required_flag IS '1 表单必填';
COMMENT ON TABLE meta_page IS '页面 schema';
COMMENT ON TABLE meta_app_menu IS '发布时写入的 sys_menu 记录';

INSERT INTO meta_app (code, name, status, version, grant_roles, remark)
VALUES ('demo', '演示', 'DRAFT', 0, 'USER', '物品登记，验收时点发布');

INSERT INTO meta_entity (app_id, code, name, remark)
SELECT id, 'item', '物品登记', '阶段 2 演示实体' FROM meta_app WHERE code = 'demo' AND deleted = 0;

INSERT INTO meta_field (entity_id, code, name, field_type, length, nullable_flag, sort, queryable, listed, required_flag)
SELECT e.id, 'name', '名称', 'string', 128, 0, 10, 1, 1, 1
FROM meta_entity e JOIN meta_app a ON a.id = e.app_id WHERE a.code = 'demo' AND e.code = 'item';

INSERT INTO meta_field (entity_id, code, name, field_type, length, nullable_flag, sort, queryable, listed, required_flag)
SELECT e.id, 'qty', '数量', 'int', NULL, 1, 20, 0, 1, 0
FROM meta_entity e JOIN meta_app a ON a.id = e.app_id WHERE a.code = 'demo' AND e.code = 'item';

INSERT INTO meta_field (entity_id, code, name, field_type, length, nullable_flag, sort, queryable, listed, required_flag)
SELECT e.id, 'remark', '备注', 'text', NULL, 1, 30, 0, 1, 0
FROM meta_entity e JOIN meta_app a ON a.id = e.app_id WHERE a.code = 'demo' AND e.code = 'item';

INSERT INTO sys_menu (parent_id, name, path, component, menu_type, permission, icon, sort, visible, status, created_by, updated_by)
VALUES (0, '应用建模', '/meta/apps', 'MetaAppView', 'MENU', 'meta:app:manage', 'appstore', 30, 1, 1, 0, 0);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r
CROSS JOIN sys_menu m
WHERE r.code IN ('SUPER_ADMIN', 'DEVELOPER')
  AND m.path = '/meta/apps'
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = m.id
  );
