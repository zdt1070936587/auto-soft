package com.autosoft.agent.tool.impl;

import com.autosoft.agent.tool.AgentTool;
import com.autosoft.agent.tool.ToolArgs;
import com.autosoft.agent.tool.ToolContext;
import com.autosoft.common.core.ResultCode;
import com.autosoft.common.exception.BizException;
import com.autosoft.meta.dto.PublishDTO;
import com.autosoft.meta.publish.PublishService;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * PublishApp工具。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
@Component
public class PublishAppTool implements AgentTool {

    private final PublishService publishService;

    public PublishAppTool(PublishService publishService) {
        this.publishService = publishService;
    }

    @Override
    public String name() {
        return "publish_app";
    }

    @Override
    public String description() {
        return "发布当前草稿。必须 confirm=true，且用户已在界面二次确认。会执行 DDL 并生成菜单。";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("confirm", AgentTool.prop("boolean", "必须为 true"));
        props.put("grant_roles", AgentTool.prop("string", "授权角色，可空则用应用上已保存的值"));
        return AgentTool.objectSchema(props, List.of("confirm"));
    }

    @Override
    public String execute(ToolContext context, ToolArgs args) {
        if (!args.bool("confirm")) {
            throw new BizException(ResultCode.BAD_REQUEST, "发布需用户确认，请等待用户点击发布");
        }
        PublishDTO dto = new PublishDTO();
        dto.setGrantRoles(args.str("grant_roles"));
        publishService.publish(context.requireAppId(), dto);
        context.markSchemaUpdated();
        return "{\"published\":true}";
    }
}
