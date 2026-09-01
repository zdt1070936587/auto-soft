package com.autosoft.flow.entity;

import com.autosoft.system.entity.BaseDO;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * FlowTask实体。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
@TableName("sys_flow_task")
public class FlowTaskDO extends BaseDO {
    private Long instanceId;
    private Integer levelNo;
    private String roleCode;
    private String status;
    private Long assigneeId;
    @TableField("comment_text")
    private String commentText;

    public Long getInstanceId() { return instanceId; }
    public void setInstanceId(Long instanceId) { this.instanceId = instanceId; }
    public Integer getLevelNo() { return levelNo; }
    public void setLevelNo(Integer levelNo) { this.levelNo = levelNo; }
    public String getRoleCode() { return roleCode; }
    public void setRoleCode(String roleCode) { this.roleCode = roleCode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getAssigneeId() { return assigneeId; }
    public void setAssigneeId(Long assigneeId) { this.assigneeId = assigneeId; }
    public String getCommentText() { return commentText; }
    public void setCommentText(String commentText) { this.commentText = commentText; }
}
