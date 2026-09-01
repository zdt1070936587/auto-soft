package com.autosoft.workflow.exec;

import com.autosoft.workflow.graph.NodeTypes;
import com.autosoft.workflow.graph.WfNode;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * LlmNode执行器。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
@Component
public class LlmNodeExecutor implements NodeExecutor {

    private final WorkflowLlmPort llmPort;
    private final JsonMapper jsonMapper;

    public LlmNodeExecutor(WorkflowLlmPort llmPort, JsonMapper jsonMapper) {
        this.llmPort = llmPort;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public String type() {
        return NodeTypes.LLM;
    }

    @Override
    public Object execute(WfNode node, Map<String, Object> renderedConfig, RunContext context) {
        String prompt = str(renderedConfig.get("prompt"));
        if (prompt == null || prompt.isBlank()) {
            prompt = "请根据输入生成简短结果。";
        }
        Object input = renderedConfig.get("input");
        String contextJson = input == null ? jsonMapper.writeValueAsString(context.input())
                : jsonMapper.writeValueAsString(input);
        WorkflowLlmPort.LlmResult result = llmPort.complete(prompt, contextJson);
        context.addTokens(result.promptTokens(), result.completionTokens());
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("text", result.text());
        return out;
    }

    private static String str(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
