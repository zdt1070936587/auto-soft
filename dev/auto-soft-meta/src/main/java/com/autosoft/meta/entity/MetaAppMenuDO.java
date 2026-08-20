package com.autosoft.meta.entity;

import com.autosoft.system.entity.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("meta_app_menu")
public class MetaAppMenuDO extends BaseDO {

    private Long appId;
    private Long menuId;

    public Long getAppId() {
        return appId;
    }

    public void setAppId(Long appId) {
        this.appId = appId;
    }

    public Long getMenuId() {
        return menuId;
    }

    public void setMenuId(Long menuId) {
        this.menuId = menuId;
    }
}
