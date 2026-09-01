package com.autosoft.meta.field;

import com.autosoft.common.core.ResultCode;
import com.autosoft.common.exception.BizException;

import java.util.Locale;
import java.util.Set;

/**
 * 字段类型白名单。DDL 只允许 switch 本枚举，禁止拼接 field_type 原文。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
public final class FieldTypes {

    public static final String STRING = "string";
    public static final String TEXT = "text";
    public static final String INT = "int";
    public static final String LONG = "long";
    public static final String DECIMAL = "decimal";
    public static final String BOOL = "bool";
    public static final String DATE = "date";
    public static final String DATETIME = "datetime";
    public static final String DICT = "dict";
    public static final String REF = "ref";

    public static final Set<String> ALL = Set.of(
            STRING, TEXT, INT, LONG, DECIMAL, BOOL, DATE, DATETIME, DICT, REF);

    private FieldTypes() {
    }

    public static String require(String fieldType) {
        if (fieldType == null || !ALL.contains(fieldType.toLowerCase(Locale.ROOT))) {
            throw new BizException(ResultCode.BAD_REQUEST, "不支持的字段类型");
        }
        return fieldType.toLowerCase(Locale.ROOT);
    }

    public static String pgType(String fieldType, Integer length) {
        return switch (require(fieldType)) {
            case STRING -> {
                int n = length == null ? 255 : length;
                if (n < 1 || n > 1024) {
                    throw new BizException(ResultCode.BAD_REQUEST, "string 长度需 1-1024");
                }
                yield "VARCHAR(" + n + ")";
            }
            case TEXT -> "TEXT";
            case DICT -> "VARCHAR(64)";
            case INT -> "INTEGER";
            case LONG, REF -> "BIGINT";
            case DECIMAL -> "NUMERIC(18,2)";
            case BOOL -> "SMALLINT";
            case DATE -> "DATE";
            case DATETIME -> "TIMESTAMPTZ";
            default -> throw new BizException(ResultCode.BAD_REQUEST, "不支持的字段类型");
        };
    }
}
