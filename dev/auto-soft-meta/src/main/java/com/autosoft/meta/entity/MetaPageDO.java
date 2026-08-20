package com.autosoft.meta.entity;

import com.autosoft.system.entity.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("meta_page")
public class MetaPageDO extends BaseDO {

    private Long entityId;
    private String pageType;
    private String schemaJson;

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public String getPageType() {
        return pageType;
    }

    public void setPageType(String pageType) {
        this.pageType = pageType;
    }

    public String getSchemaJson() {
        return schemaJson;
    }

    public void setSchemaJson(String schemaJson) {
        this.schemaJson = schemaJson;
    }
}
