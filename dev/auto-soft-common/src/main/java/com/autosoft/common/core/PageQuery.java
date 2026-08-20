package com.autosoft.common.core;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 分页查询入参。current 从 1 开始，size 上限 200。
 */
public class PageQuery {

    public static final int DEFAULT_CURRENT = 1;
    public static final int DEFAULT_SIZE = 10;
    public static final int MAX_SIZE = 200;

    @Min(1)
    private Integer current = DEFAULT_CURRENT;

    @Min(1)
    @Max(MAX_SIZE)
    private Integer size = DEFAULT_SIZE;

    public Integer getCurrent() {
        return current == null ? DEFAULT_CURRENT : current;
    }

    public void setCurrent(Integer current) {
        this.current = current;
    }

    public Integer getSize() {
        return size == null ? DEFAULT_SIZE : size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    /**
     * 计算 SQL OFFSET。
     */
    public int offset() {
        return (getCurrent() - 1) * getSize();
    }
}
