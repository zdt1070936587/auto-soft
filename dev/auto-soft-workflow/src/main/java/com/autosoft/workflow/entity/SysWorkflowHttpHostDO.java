package com.autosoft.workflow.entity;

import com.autosoft.system.entity.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * SysWorkflowHttpHost实体。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
@TableName("sys_workflow_http_host")
public class SysWorkflowHttpHostDO extends BaseDO {

    private String host;
    private String remark;

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
}
