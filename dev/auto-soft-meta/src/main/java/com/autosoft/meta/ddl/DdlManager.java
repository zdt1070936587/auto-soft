package com.autosoft.meta.ddl;

import com.autosoft.common.core.ResultCode;
import com.autosoft.common.exception.BizException;
import com.autosoft.meta.entity.MetaFieldDO;
import com.autosoft.meta.field.FieldTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 仅允许 dyn_ 前缀建表与 ADD COLUMN。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
@Component
public class DdlManager {

    private static final Logger log = LoggerFactory.getLogger(DdlManager.class);

    private final JdbcTemplate jdbcTemplate;

    public DdlManager(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void ensureTable(String appCode, String entityCode, List<MetaFieldDO> fields) {
        String table = Identifiers.tableName(appCode, entityCode);
        String quoted = Identifiers.quoteTable(table);
        if (!tableExists(table)) {
            jdbcTemplate.execute(buildCreateSql(quoted, fields));
            log.info("created dynamic table {}", table);
        }
        Set<String> existing = existingColumns(table);
        for (MetaFieldDO field : fields) {
            Identifiers.assertCode(field.getCode(), "fieldCode");
            if (Identifiers.SYSTEM_COLUMNS.contains(field.getCode())) {
                throw new BizException(ResultCode.BAD_REQUEST, "字段不能与系统列重名: " + field.getCode());
            }
            if (!existing.contains(field.getCode())) {
                String col = Identifiers.quote(field.getCode());
                String type = FieldTypes.pgType(field.getFieldType(), field.getLength());
                jdbcTemplate.execute("ALTER TABLE " + quoted + " ADD COLUMN " + col + " " + type);
                log.info("added column {}.{}", table, field.getCode());
            }
        }
    }

    private boolean tableExists(String table) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = current_schema() AND table_name = ?",
                Integer.class, table);
        return count != null && count > 0;
    }

    private Set<String> existingColumns(String table) {
        List<String> names = jdbcTemplate.query(
                "SELECT column_name FROM information_schema.columns WHERE table_schema = current_schema() AND table_name = ?",
                (rs, i) -> rs.getString(1), table);
        Set<String> set = new HashSet<>();
        for (String name : names) {
            set.add(name.toLowerCase(Locale.ROOT));
        }
        return set;
    }

    private String buildCreateSql(String quotedTable, List<MetaFieldDO> fields) {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE IF NOT EXISTS ").append(quotedTable).append(" (");
        sql.append("\"id\" BIGSERIAL PRIMARY KEY,");
        for (MetaFieldDO field : fields) {
            Identifiers.assertCode(field.getCode(), "fieldCode");
            sql.append(Identifiers.quote(field.getCode())).append(" ")
                    .append(FieldTypes.pgType(field.getFieldType(), field.getLength())).append(",");
        }
        sql.append("\"created_by\" BIGINT NOT NULL DEFAULT 0,");
        sql.append("\"created_at\" TIMESTAMPTZ NOT NULL DEFAULT NOW(),");
        sql.append("\"updated_by\" BIGINT NOT NULL DEFAULT 0,");
        sql.append("\"updated_at\" TIMESTAMPTZ NOT NULL DEFAULT NOW(),");
        sql.append("\"deleted\" SMALLINT NOT NULL DEFAULT 0,");
        sql.append("\"flow_status\" VARCHAR(32) NOT NULL DEFAULT 'none'");
        sql.append(")");
        return sql.toString();
    }
}
