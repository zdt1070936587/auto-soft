package com.autosoft.agent.assistant.tool.impl;

import com.autosoft.agent.assistant.memory.MemoryService;
import com.autosoft.agent.assistant.tool.AssistantTool;
import com.autosoft.agent.assistant.tool.AssistantToolContext;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * RecallUserMemory工具。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
@Component
public class RecallUserMemoryTool implements AssistantTool {

    private final MemoryService memoryService;

    public RecallUserMemoryTool(MemoryService memoryService) {
        this.memoryService = memoryService;
    }

    @Override
    public String name() {
        return "recall_user_memory";
    }

    @Override
    public String description() {
        return "回忆用户过往说过的事或画像信息。";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("query", AssistantTool.prop("string", "回忆关键词或问题"));
        props.put("top_k", AssistantTool.prop("integer", "最多返回条数，默认 5"));
        return AssistantTool.objectSchema(props, List.of("query"));
    }

    @Override
    public String execute(AssistantToolContext context, Map<String, Object> args) {
        String query = args.get("query") == null ? "" : String.valueOf(args.get("query"));
        int topK = 5;
        Object topKObj = args.get("top_k");
        if (topKObj instanceof Number number) {
            topK = number.intValue();
        }
        return memoryService.recallJson(context.getUserId(), query, topK);
    }
}
