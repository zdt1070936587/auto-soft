package com.autosoft.agent.tool;

import com.autosoft.agent.studio.AgentMode;
import com.autosoft.common.core.ResultCode;
import com.autosoft.common.exception.BizException;
import com.autosoft.meta.app.AppKind;
import com.autosoft.meta.app.MetaCatalogService;
import com.autosoft.meta.entity.MetaAppDO;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 已注册工具表。未知工具名直接失败。按 app_kind 互斥。
 */
@Component
public class ToolRegistry {

    public static final int RESULT_LIMIT = 8192;

    public static final Set<String> CRUD_TOOLS = Set.of(
            "create_app", "update_app", "define_entity", "add_field", "update_field",
            "define_page", "bind_menu", "create_simple_flow", "bind_flow", "publish_app");

    public static final Set<String> WORKFLOW_TOOLS = Set.of(
            "create_workflow", "set_trigger", "add_node", "update_node", "remove_node",
            "connect_nodes", "get_workflow_graph", "validate_workflow", "dry_run_workflow",
            "publish_workflow");

    private final Map<String, AgentTool> tools = new LinkedHashMap<>();
    private final JsonMapper jsonMapper;
    private final MetaCatalogService catalogService;

    public ToolRegistry(List<AgentTool> agentTools, JsonMapper jsonMapper, MetaCatalogService catalogService) {
        this.jsonMapper = jsonMapper;
        this.catalogService = catalogService;
        for (AgentTool tool : agentTools) {
            tools.put(tool.name(), tool);
        }
    }

    public List<Map<String, Object>> openaiTools() {
        return openaiTools(AgentMode.DEVELOP, null);
    }

    public List<Map<String, Object>> openaiTools(AgentMode mode) {
        return openaiTools(mode, null);
    }

    public List<Map<String, Object>> openaiTools(AgentMode mode, String appKind) {
        return tools.values().stream()
                .filter(tool -> mode.allowsTool(tool.name()))
                .filter(tool -> allowedForKind(tool.name(), appKind))
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
        String kind = resolveKind(context);
        if (!allowedForKind(name, kind)) {
            throw new BizException(ResultCode.BAD_REQUEST, kindMessage(name, kind));
        }
        AgentTool tool = tools.get(name);
        if (tool == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "未注册的工具: " + name);
        }
        ToolArgs args = ToolArgs.parse(argumentsJson, jsonMapper);
        String result = tool.execute(context, args);
        return truncate(result);
    }

    public String resolveKind(ToolContext context) {
        if (context.appId() == null) {
            return null;
        }
        MetaAppDO app = catalogService.requireApp(context.appId());
        return AppKind.from(app.getAppKind()).code();
    }

    private boolean allowedForKind(String name, String kind) {
        if (kind == null) {
            if (CRUD_TOOLS.contains(name) && !"create_app".equals(name)) {
                return false;
            }
            if (WORKFLOW_TOOLS.contains(name) && !"create_workflow".equals(name)) {
                return false;
            }
            return true;
        }
        if (AppKind.WORKFLOW.code().equals(kind)) {
            if (CRUD_TOOLS.contains(name)) {
                return false;
            }
            return true;
        }
        return !WORKFLOW_TOOLS.contains(name);
    }

    private static String kindMessage(String name, String kind) {
        if (AppKind.WORKFLOW.code().equals(kind)) {
            return "工作流会话不能调用 " + name + "，请使用工作流工具改图";
        }
        if (kind == null) {
            return "请先 create_app 或 create_workflow";
        }
        return "当前应用不是工作流，不能调用 " + name;
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
