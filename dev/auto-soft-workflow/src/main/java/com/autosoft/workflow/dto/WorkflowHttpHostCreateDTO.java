package com.autosoft.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class WorkflowHttpHostCreateDTO {

    @NotBlank(message = "host 不能为空")
    @Size(max = 253, message = "host 过长")
    private String host;

    @Size(max = 256, message = "备注过长")
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
