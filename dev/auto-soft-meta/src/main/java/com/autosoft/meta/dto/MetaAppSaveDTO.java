package com.autosoft.meta.dto;

import jakarta.validation.constraints.NotBlank;

public class MetaAppSaveDTO {
    @NotBlank
    private String code;
    @NotBlank
    private String name;
    private String grantRoles;
    private String remark;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getGrantRoles() { return grantRoles; }
    public void setGrantRoles(String grantRoles) { this.grantRoles = grantRoles; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
