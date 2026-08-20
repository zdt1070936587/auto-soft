package com.autosoft.framework.mybatis;

import com.autosoft.framework.security.SecurityUtils;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * 插入/更新时填充审计字段。匿名请求（如登录）用户 ID 为 0。
 */
@Component
public class AuditMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        Instant now = Instant.now();
        Long userId = SecurityUtils.currentUserId();
        strictInsertFill(metaObject, "createdAt", Instant.class, now);
        strictInsertFill(metaObject, "updatedAt", Instant.class, now);
        strictInsertFill(metaObject, "createdBy", Long.class, userId);
        strictInsertFill(metaObject, "updatedBy", Long.class, userId);
        strictInsertFill(metaObject, "deleted", Integer.class, 0);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        strictUpdateFill(metaObject, "updatedAt", Instant.class, Instant.now());
        strictUpdateFill(metaObject, "updatedBy", Long.class, SecurityUtils.currentUserId());
    }
}
