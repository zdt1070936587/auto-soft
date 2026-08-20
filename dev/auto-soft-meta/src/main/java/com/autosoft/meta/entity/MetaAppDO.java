package com.autosoft.meta.entity;

import com.autosoft.system.entity.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("meta_app")
public class MetaAppDO extends BaseDO {

    public static final String DRAFT = "DRAFT";
    public static final String PUBLISHED = "PUBLISHED";

    private String code;
    private String name;
    private String status;
    private Integer version;
    private String grantRoles;
    private String remark;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getGrantRoles() {
        return grantRoles;
    }

    public void setGrantRoles(String grantRoles) {
        this.grantRoles = grantRoles;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
