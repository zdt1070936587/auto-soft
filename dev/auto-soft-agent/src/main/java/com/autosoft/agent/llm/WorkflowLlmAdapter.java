package com.autosoft.agent.llm;

import com.autosoft.workflow.exec.WorkflowLlmPort;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * WorkflowLlm适配器。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
@Component
public class WorkflowLlmAdapter implements WorkflowLlmPort {

    private final OpenCodeGoManager openCodeGoManager;

    public WorkflowLlmAdapter(OpenCodeGoManager openCodeGoManager) {
        this.openCodeGoManager = openCodeGoManager;
    }

    @Override
    public LlmResult complete(String prompt, String contextJson) {
        List<Map<String, Object>> messages = new ArrayList<>();
        Map<String, Object> system = new LinkedHashMap<>();
        system.put("role", "system");
        system.put("content", "你是工作流中的文本生成节点。只输出对用户有用的正文，不要输出密钥。");
        messages.add(system);
        Map<String, Object> user = new LinkedHashMap<>();
        user.put("role", "user");
        user.put("content", prompt + "\n\n上下文 JSON:\n" + (contextJson == null ? "{}" : contextJson));
        messages.add(user);
        LlmTurn turn = openCodeGoManager.chat(messages, List.of());
        String text = turn.getContent() == null ? "" : turn.getContent();
        return new LlmResult(text, turn.getPromptTokens(), turn.getCompletionTokens());
    }
}
