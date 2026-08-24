-- 低代码多类型页面：应用类型 + 页面编码/布局

ALTER TABLE meta_app
    ADD COLUMN IF NOT EXISTS app_kind VARCHAR(16) NOT NULL DEFAULT 'admin';

ALTER TABLE meta_page
    ADD COLUMN IF NOT EXISTS app_id BIGINT,
    ADD COLUMN IF NOT EXISTS page_code VARCHAR(32),
    ADD COLUMN IF NOT EXISTS layout VARCHAR(16) DEFAULT 'admin';

ALTER TABLE meta_page
    ALTER COLUMN entity_id DROP NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_meta_page_app_code_alive
    ON meta_page (app_id, page_code) WHERE deleted = 0 AND page_code IS NOT NULL;
