package com.autosoft.workflow.entity;

import com.autosoft.system.entity.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.Instant;

@TableName("wf_share")
public class WfShareDO extends BaseDO {

    private Long definitionId;
    private String token;
    private String permission;
    private Instant expireAt;

    public Long getDefinitionId() { return definitionId; }
    public void setDefinitionId(Long definitionId) { this.definitionId = definitionId; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getPermission() { return permission; }
    public void setPermission(String permission) { this.permission = permission; }
    public Instant getExpireAt() { return expireAt; }
    public void setExpireAt(Instant expireAt) { this.expireAt = expireAt; }
}
