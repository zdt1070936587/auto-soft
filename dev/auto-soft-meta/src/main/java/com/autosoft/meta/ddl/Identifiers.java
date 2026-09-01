package com.autosoft.meta.ddl;

import com.autosoft.common.core.ResultCode;
import com.autosoft.common.exception.BizException;
import com.autosoft.common.utils.AssertUtils;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * 动态表/列标识符白名单。禁止把请求参数直接拼进 SQL 标识符。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
public final class Identifiers {

    public static final Pattern CODE = Pattern.compile("^[a-z][a-z0-9_]{1,30}$");
    public static final Set<String> SYSTEM_COLUMNS = Set.of(
            "id", "created_by", "created_at", "updated_by", "updated_at", "deleted", "flow_status");

    private Identifiers() {
    }

    public static void assertCode(String code, String label) {
        AssertUtils.notBlank(code, label + "不能为空");
        if (!CODE.matcher(code).matches()) {
            throw new BizException(ResultCode.BAD_REQUEST, label + "需为小写字母开头的 2-31 位字母数字下划线");
        }
    }

    public static String tableName(String appCode, String entityCode) {
        assertCode(appCode, "appCode");
        assertCode(entityCode, "entityCode");
        return "dyn_" + appCode + "_" + entityCode;
    }

    public static String quoteTable(String table) {
        AssertUtils.notBlank(table, "表名不能为空");
        if (!table.matches("^dyn_[a-z][a-z0-9_]{1,30}_[a-z][a-z0-9_]{1,30}$")) {
            throw new BizException(ResultCode.BAD_REQUEST, "非法表名");
        }
        return "\"" + table + "\"";
    }

    public static String quote(String ident) {
        AssertUtils.notBlank(ident, "标识符不能为空");
        boolean allowed = SYSTEM_COLUMNS.contains(ident) || CODE.matcher(ident).matches();
        if (!allowed) {
            throw new BizException(ResultCode.BAD_REQUEST, "非法标识符");
        }
        return "\"" + ident + "\"";
    }
}
