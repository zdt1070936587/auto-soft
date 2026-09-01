package com.autosoft.agent.assistant.tool;

import com.autosoft.agent.tool.ToolArgs;
import com.autosoft.common.core.ResultCode;
import com.autosoft.common.exception.BizException;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Assistant 工具注册表。与 Studio ToolRegistry 完全分离。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
@Component
public class AssistantToolRegistry {

    public static final int RESULT_LIMIT = 8192;

    private final Map<String, AssistantTool> tools = new LinkedHashMap<>();
    private final JsonMapper jsonMapper;

    public AssistantToolRegistry(List<AssistantTool> assistantTools, JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
        for (AssistantTool tool : assistantTools) {
            tools.put(tool.name(), tool);
        }
    }

    public List<Map<String, Object>> openaiTools() {
        return tools.values().stream().map(tool -> {
            Map<String, Object> function = new LinkedHashMap<>();
            function.put("name", tool.name());
            function.put("description", tool.description());
            function.put("parameters", tool.parametersSchema());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("type", "function");
            item.put("function", function);
            return item;
        }).toList();
    }

    public String execute(String name, AssistantToolContext context, String argumentsJson) {
        AssistantTool tool = tools.get(name);
        if (tool == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "未知工具: " + name);
        }
        ToolArgs args = ToolArgs.parse(argumentsJson, jsonMapper);
        String result = tool.execute(context, args.map());
        return truncate(result);
    }

    public static String truncate(String text) {
        if (text == null) {
            return "";
        }
        return text.length() <= RESULT_LIMIT ? text : text.substring(0, RESULT_LIMIT) + "…";
    }

    public static String json(JsonMapper jsonMapper, Object value) {
        return jsonMapper.writeValueAsString(value);
    }
}
