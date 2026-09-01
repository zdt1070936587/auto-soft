package com.autosoft.agent.assistant.tool;

import com.autosoft.agent.entity.AiAssistantSessionDO;

/**
 * Assistant 工具执行上下文。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
public class AssistantToolContext {

    private final AiAssistantSessionDO session;
    private final Long userId;

    public AssistantToolContext(AiAssistantSessionDO session, Long userId) {
        this.session = session;
        this.userId = userId;
    }

    public AiAssistantSessionDO getSession() {
        return session;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getSessionId() {
        return session.getId();
    }
}
