package com.autosoft.meta.entity;

import com.autosoft.system.entity.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("meta_entity")
public class MetaEntityDO extends BaseDO {

    private Long appId;
    private String code;
    private String name;
    private String remark;

    public Long getAppId() {
        return appId;
    }

    public void setAppId(Long appId) {
        this.appId = appId;
    }

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

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
