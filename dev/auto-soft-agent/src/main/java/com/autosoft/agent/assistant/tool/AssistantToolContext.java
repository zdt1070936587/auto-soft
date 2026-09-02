package com.autosoft.agent.assistant.tool;

import com.autosoft.agent.assistant.action.vo.ActionDraftVO;
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
    private ActionDraftVO lastDraft;
    private boolean askUser;
    private String askQuestion;

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

    public ActionDraftVO getLastDraft() {
        return lastDraft;
    }

    public void setLastDraft(ActionDraftVO lastDraft) {
        this.lastDraft = lastDraft;
    }

    public boolean isAskUser() {
        return askUser;
    }

    public void setAskUser(boolean askUser) {
        this.askUser = askUser;
    }

    public String getAskQuestion() {
        return askQuestion;
    }

    public void setAskQuestion(String askQuestion) {
        this.askQuestion = askQuestion;
    }
}
