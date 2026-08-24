package com.autosoft.agent.tool;

import com.autosoft.agent.studio.AgentMode;
import com.autosoft.common.core.ResultCode;
import com.autosoft.common.exception.BizException;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 已注册工具表。未知工具名直接失败。
 */
@Component
public class ToolRegistry {

    public static final int RESULT_LIMIT = 8192;

    private final Map<String, AgentTool> tools = new LinkedHashMap<>();
    private final JsonMapper jsonMapper;

    public ToolRegistry(List<AgentTool> agentTools, JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
        for (AgentTool tool : agentTools) {
            tools.put(tool.name(), tool);
        }
    }

    public List<Map<String, Object>> openaiTools() {
        return openaiTools(AgentMode.DEVELOP);
    }

    public List<Map<String, Object>> openaiTools(AgentMode mode) {
        return tools.values().stream()
                .filter(tool -> mode.allowsTool(tool.name()))
                .map(tool -> {
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

    public String execute(String name, ToolContext context, String argumentsJson) {
        return execute(name, context, argumentsJson, AgentMode.DEVELOP);
    }

    public String execute(String name, ToolContext context, String argumentsJson, AgentMode mode) {
        if (!mode.allowsTool(name)) {
            throw new BizException(ResultCode.BAD_REQUEST,
                    "当前工作级别不允许调用工具: " + name + "。请切换到「开发」级别后再执行写操作。");
        }
        AgentTool tool = tools.get(name);
        if (tool == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "未注册的工具: " + name);
        }
        ToolArgs args = ToolArgs.parse(argumentsJson, jsonMapper);
        String result = tool.execute(context, args);
        return truncate(result);
    }

    public static String truncate(String result) {
        if (result == null) {
            return "";
        }
        if (result.length() <= RESULT_LIMIT) {
            return result;
        }
        return result.substring(0, RESULT_LIMIT) + "...(truncated)";
    }

    public static String json(JsonMapper jsonMapper, Object data) {
        return jsonMapper.writeValueAsString(data);
    }
}
