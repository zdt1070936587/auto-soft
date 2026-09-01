package com.autosoft.workflow.vo;

import java.time.Instant;

/**
 * WorkflowHttpHost视图对象。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
public class WorkflowHttpHostVO {

    private Long id;
    private String host;
    private String remark;
    private Instant createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
