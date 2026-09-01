package com.autosoft.workflow.entity;

import com.autosoft.system.entity.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * WfNotice实体。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
@TableName("wf_notice")
public class WfNoticeDO extends BaseDO {

    private Long runId;
    private String toRole;
    private String title;
    private String body;

    public Long getRunId() { return runId; }
    public void setRunId(Long runId) { this.runId = runId; }
    public String getToRole() { return toRole; }
    public void setToRole(String toRole) { this.toRole = toRole; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
}
