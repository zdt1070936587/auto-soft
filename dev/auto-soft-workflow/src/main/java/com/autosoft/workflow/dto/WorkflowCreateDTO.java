package com.autosoft.workflow.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * WorkflowCreate传输对象。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
public class WorkflowCreateDTO {

    @NotBlank
    private String code;
    @NotBlank
    private String name;
    private String grantRoles;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getGrantRoles() { return grantRoles; }
    public void setGrantRoles(String grantRoles) { this.grantRoles = grantRoles; }
}
