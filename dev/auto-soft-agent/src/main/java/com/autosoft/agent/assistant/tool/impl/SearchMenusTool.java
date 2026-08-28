package com.autosoft.agent.assistant.tool.impl;

import com.autosoft.agent.assistant.tool.AssistantTool;
import com.autosoft.agent.assistant.tool.AssistantToolContext;
import com.autosoft.agent.assistant.tool.AssistantToolRegistry;
import com.autosoft.system.menu.MenuService;
import com.autosoft.system.vo.MenuSearchHit;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class SearchMenusTool implements AssistantTool {

    private final MenuService menuService;
    private final JsonMapper jsonMapper;

    public SearchMenusTool(MenuService menuService, JsonMapper jsonMapper) {
        this.menuService = menuService;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public String name() {
        return "search_menus";
    }

    @Override
    public String description() {
        return "按关键词搜索当前用户有权访问的菜单入口，返回名称与路径。";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("keyword", AssistantTool.prop("string", "搜索关键词，如「用户管理」"));
        props.put("limit", AssistantTool.prop("integer", "最多返回条数，默认 5"));
        return AssistantTool.objectSchema(props, List.of("keyword"));
    }

    @Override
    public String execute(AssistantToolContext context, Map<String, Object> args) {
        String keyword = args.get("keyword") == null ? "" : String.valueOf(args.get("keyword"));
        int limit = 5;
        Object limitObj = args.get("limit");
        if (limitObj instanceof Number number) {
            limit = number.intValue();
        }
        List<MenuSearchHit> hits = menuService.searchMine(context.getUserId(), keyword, limit);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "nav_link");
        payload.put("items", hits);
        return AssistantToolRegistry.json(jsonMapper, payload);
    }
}
