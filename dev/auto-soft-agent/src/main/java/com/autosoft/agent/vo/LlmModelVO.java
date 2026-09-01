package com.autosoft.agent.vo;

/**
 * LlmModel视图对象。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
public class LlmModelVO {

    private String id;

    public LlmModelVO() {
    }

    public LlmModelVO(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
