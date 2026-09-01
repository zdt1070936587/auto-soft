package com.autosoft.agent.tool;

import com.autosoft.agent.entity.AiSessionDO;
import com.autosoft.agent.mapper.AiSessionMapper;
import com.autosoft.common.core.ResultCode;
import com.autosoft.common.exception.BizException;

/**
 * 工具执行上下文。一次会话只绑定一个 app。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
public class ToolContext {

    private final AiSessionDO session;
    private final AiSessionMapper sessionMapper;
    private boolean schemaUpdated;

    public ToolContext(AiSessionDO session, AiSessionMapper sessionMapper) {
        this.session = session;
        this.sessionMapper = sessionMapper;
    }

    public Long sessionId() {
        return session.getId();
    }

    public Long appId() {
        return session.getAppId();
    }

    public Long requireAppId() {
        if (session.getAppId() == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "请先 create_app");
        }
        return session.getAppId();
    }

    public void bindApp(Long appId) {
        if (session.getAppId() != null && !session.getAppId().equals(appId)) {
            throw new BizException(ResultCode.BAD_REQUEST, "一次会话只能绑定一个应用");
        }
        session.setAppId(appId);
        sessionMapper.updateById(session);
        markSchemaUpdated();
    }

    public void markSchemaUpdated() {
        this.schemaUpdated = true;
    }

    public boolean isSchemaUpdated() {
        return schemaUpdated;
    }

    public AiSessionDO session() {
        return session;
    }
}
