package com.autosoft.meta.runtime;

import com.autosoft.common.core.PageQuery;
import com.autosoft.common.core.PageResult;
import com.autosoft.common.core.ResultCode;
import com.autosoft.common.exception.BizException;
import com.autosoft.common.utils.AssertUtils;
import com.autosoft.framework.security.SecurityUtils;
import com.autosoft.meta.ddl.Identifiers;
import com.autosoft.meta.entity.MetaFieldDO;
import com.autosoft.meta.field.FieldTypes;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 动态 CRUD SQL。列名/排序仅来自白名单，值全部 bind。
 */
@Component
public class RuntimeSqlManager {

    private final NamedParameterJdbcTemplate jdbc;

    public RuntimeSqlManager(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public PageResult<Map<String, Object>> page(String table, List<MetaFieldDO> fields, PageQuery page,
                                                Map<String, String> query, String orderBy, String orderDir) {
        String quoted = Identifiers.quoteTable(table);
        MapSqlParameterSource params = new MapSqlParameterSource();
        StringBuilder where = new StringBuilder(" WHERE ").append(Identifiers.quote("deleted")).append(" = 0 ");
        appendQuery(where, params, fields, query);
        String order = buildOrder(fields, orderBy, orderDir);
        Integer total = jdbc.queryForObject("SELECT COUNT(*) FROM " + quoted + where, params, Integer.class);
        String sql = "SELECT * FROM " + quoted + where + " ORDER BY " + order
                + " LIMIT :limit OFFSET :offset";
        params.addValue("limit", page.getSize());
        params.addValue("offset", page.offset());
        List<Map<String, Object>> records = jdbc.queryForList(sql, params);
        return new PageResult<>(total == null ? 0 : total, records);
    }

    public Map<String, Object> get(String table, Long id) {
        MapSqlParameterSource params = new MapSqlParameterSource("id", id);
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT * FROM " + Identifiers.quoteTable(table)
                        + " WHERE " + Identifiers.quote("id") + " = :id AND "
                        + Identifiers.quote("deleted") + " = 0", params);
        if (rows.isEmpty()) {
            throw new BizException(ResultCode.NOT_FOUND, "记录不存在");
        }
        return rows.get(0);
    }

    public Long insert(String table, List<MetaFieldDO> fields, Map<String, Object> body, String flowStatus) {
        Long userId = SecurityUtils.currentUserId();
        Instant now = Instant.now();
        List<String> cols = new ArrayList<>();
        MapSqlParameterSource params = new MapSqlParameterSource();
        for (MetaFieldDO field : fields) {
            if (!body.containsKey(field.getCode())) {
                continue;
            }
            cols.add(Identifiers.quote(field.getCode()));
            params.addValue(field.getCode(), convert(field, body.get(field.getCode())));
        }
        cols.add(Identifiers.quote("created_by"));
        cols.add(Identifiers.quote("created_at"));
        cols.add(Identifiers.quote("updated_by"));
        cols.add(Identifiers.quote("updated_at"));
        cols.add(Identifiers.quote("deleted"));
        cols.add(Identifiers.quote("flow_status"));
        params.addValue("created_by", userId);
        params.addValue("created_at", Timestamp.from(now));
        params.addValue("updated_by", userId);
        params.addValue("updated_at", Timestamp.from(now));
        params.addValue("deleted", 0);
        params.addValue("flow_status", flowStatus);
        StringBuilder sql = new StringBuilder("INSERT INTO ").append(Identifiers.quoteTable(table)).append(" (");
        sql.append(String.join(",", cols)).append(") VALUES (");
        List<String> holders = new ArrayList<>();
        holders.addAll(fields.stream().filter(f -> body.containsKey(f.getCode())).map(f -> ":" + f.getCode()).toList());
        holders.add(":created_by");
        holders.add(":created_at");
        holders.add(":updated_by");
        holders.add(":updated_at");
        holders.add(":deleted");
        holders.add(":flow_status");
        sql.append(String.join(",", holders)).append(")");
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(sql.toString(), params, keyHolder, new String[]{"id"});
        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    public void update(String table, List<MetaFieldDO> fields, Long id, Map<String, Object> body) {
        get(table, id);
        List<String> sets = new ArrayList<>();
        MapSqlParameterSource params = new MapSqlParameterSource("id", id);
        for (MetaFieldDO field : fields) {
            if (!body.containsKey(field.getCode())) {
                continue;
            }
            sets.add(Identifiers.quote(field.getCode()) + " = :" + field.getCode());
            params.addValue(field.getCode(), convert(field, body.get(field.getCode())));
        }
        sets.add(Identifiers.quote("updated_by") + " = :updated_by");
        sets.add(Identifiers.quote("updated_at") + " = :updated_at");
        params.addValue("updated_by", SecurityUtils.currentUserId());
        params.addValue("updated_at", Timestamp.from(Instant.now()));
        if (sets.size() == 2) {
            return;
        }
        jdbc.update("UPDATE " + Identifiers.quoteTable(table) + " SET " + String.join(",", sets)
                + " WHERE " + Identifiers.quote("id") + " = :id AND " + Identifiers.quote("deleted") + " = 0", params);
    }

    public void logicDelete(String table, Long id) {
        get(table, id);
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("id", id);
        params.addValue("updated_by", SecurityUtils.currentUserId());
        params.addValue("updated_at", Timestamp.from(Instant.now()));
        jdbc.update("UPDATE " + Identifiers.quoteTable(table) + " SET "
                + Identifiers.quote("deleted") + " = 1, "
                + Identifiers.quote("updated_by") + " = :updated_by, "
                + Identifiers.quote("updated_at") + " = :updated_at WHERE "
                + Identifiers.quote("id") + " = :id", params);
    }

    public void updateFlowStatus(String table, Long id, String status) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("id", id);
        params.addValue("status", status);
        params.addValue("updated_at", Timestamp.from(Instant.now()));
        jdbc.update("UPDATE " + Identifiers.quoteTable(table) + " SET "
                + Identifiers.quote("flow_status") + " = :status, "
                + Identifiers.quote("updated_at") + " = :updated_at WHERE "
                + Identifiers.quote("id") + " = :id AND " + Identifiers.quote("deleted") + " = 0", params);
    }

    private void appendQuery(StringBuilder where, MapSqlParameterSource params, List<MetaFieldDO> fields,
                             Map<String, String> query) {
        if (query == null) {
            return;
        }
        Map<String, MetaFieldDO> index = new LinkedHashMap<>();
        for (MetaFieldDO field : fields) {
            if (field.getQueryable() != null && field.getQueryable() == 1) {
                index.put(field.getCode(), field);
            }
        }
        if (query.containsKey("flow_status") && query.get("flow_status") != null && !query.get("flow_status").isBlank()) {
            where.append(" AND ").append(Identifiers.quote("flow_status")).append(" = :flow_status ");
            params.addValue("flow_status", query.get("flow_status"));
        }
        for (Map.Entry<String, String> entry : query.entrySet()) {
            if ("current".equals(entry.getKey()) || "size".equals(entry.getKey())
                    || "orderBy".equals(entry.getKey()) || "orderDir".equals(entry.getKey())
                    || "preview".equals(entry.getKey()) || "flow_status".equals(entry.getKey())) {
                continue;
            }
            MetaFieldDO field = index.get(entry.getKey());
            if (field == null || entry.getValue() == null || entry.getValue().isBlank()) {
                continue;
            }
            String type = FieldTypes.require(field.getFieldType());
            if (FieldTypes.STRING.equals(type) || FieldTypes.TEXT.equals(type) || FieldTypes.DICT.equals(type)) {
                where.append(" AND ").append(Identifiers.quote(field.getCode()))
                        .append(" LIKE :q_").append(field.getCode()).append(" ");
                params.addValue("q_" + field.getCode(), "%" + entry.getValue() + "%");
            } else {
                where.append(" AND ").append(Identifiers.quote(field.getCode()))
                        .append(" = :q_").append(field.getCode()).append(" ");
                params.addValue("q_" + field.getCode(), convert(field, entry.getValue()));
            }
        }
    }

    private String buildOrder(List<MetaFieldDO> fields, String orderBy, String orderDir) {
        String col = orderBy == null || orderBy.isBlank() ? "id" : orderBy;
        if (!"id".equals(col)) {
            boolean listed = fields.stream().anyMatch(f -> col.equals(f.getCode())
                    && f.getListed() != null && f.getListed() == 1);
            AssertUtils.isTrue(listed, "排序字段不在白名单");
        } else {
            Identifiers.quote("id");
        }
        String dir = orderDir == null ? "DESC" : orderDir.toUpperCase(Locale.ROOT);
        AssertUtils.isTrue("ASC".equals(dir) || "DESC".equals(dir), "排序方向非法");
        return Identifiers.quote(col) + " " + dir;
    }

    private Object convert(MetaFieldDO field, Object raw) {
        if (raw == null || (raw instanceof String s && s.isBlank())) {
            return null;
        }
        String type = FieldTypes.require(field.getFieldType());
        String text = String.valueOf(raw);
        return switch (type) {
            case FieldTypes.INT -> Integer.valueOf(text);
            case FieldTypes.LONG, FieldTypes.REF -> Long.valueOf(text);
            case FieldTypes.DECIMAL -> new java.math.BigDecimal(text);
            case FieldTypes.BOOL -> {
                if ("true".equalsIgnoreCase(text) || "1".equals(text)) {
                    yield 1;
                }
                yield 0;
            }
            default -> text;
        };
    }
}
