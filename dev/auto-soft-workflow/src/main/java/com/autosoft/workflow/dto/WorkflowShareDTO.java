package com.autosoft.workflow.dto;

public class WorkflowShareDTO {

    private String permission = "preview";
    private Integer expireDays = 7;

    public String getPermission() { return permission; }
    public void setPermission(String permission) { this.permission = permission; }
    public Integer getExpireDays() { return expireDays; }
    public void setExpireDays(Integer expireDays) { this.expireDays = expireDays; }
}
