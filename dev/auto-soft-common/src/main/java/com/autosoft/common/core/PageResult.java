package com.autosoft.common.core;

import java.util.Collections;
import java.util.List;

/**
 * 分页查询出参。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
public class PageResult<T> {

    private long total;
    private List<T> records;

    public PageResult() {
        this(0L, Collections.emptyList());
    }

    public PageResult(long total, List<T> records) {
        this.total = total;
        this.records = records == null ? Collections.emptyList() : records;
    }

    public static <T> PageResult<T> empty() {
        return new PageResult<>();
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public List<T> getRecords() {
        return records;
    }

    public void setRecords(List<T> records) {
        this.records = records;
    }
}
