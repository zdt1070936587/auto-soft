package com.autosoft.workflow.dto;

public class WorkflowPublishDTO {

    private boolean confirm;
    private String grantRoles;

    public boolean isConfirm() { return confirm; }
    public void setConfirm(boolean confirm) { this.confirm = confirm; }
    public String getGrantRoles() { return grantRoles; }
    public void setGrantRoles(String grantRoles) { this.grantRoles = grantRoles; }
}
