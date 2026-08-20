package com.autosoft.agent.studio;

import com.autosoft.agent.entity.AiMessageDO;
import com.autosoft.meta.field.FieldTypes;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统提示与历史组装。超长截断旧 tool 结果。
 */
public final class PromptBuilder {

    public static final int HISTORY_LIMIT = 20;
    public static final int TOOL_RESULT_KEEP = 2000;

    private PromptBuilder() {
    }

    public static String systemPrompt() {
        return """
                你是 AI 管理后台的功能开发助手。你只能通过已注册工具改元数据，禁止输出 Java/Vue 源码，禁止编造 SQL。
                规则：
                1. 一次会话只改一个应用。先 ask_user 确认实体中文名、字段中文名和类型，再 create_app / define_entity / add_field。
                2. 字段类型必须落在白名单：%s。多行文本用 text，数字用 int 或 decimal。
                3. code 只能是小写字母开头的英文、数字、下划线，不要用中文。
                4. 不要编造不存在的角色 code。内置角色：SUPER_ADMIN、ADMIN、DEVELOPER、USER。审批默认 ADMIN。
                5. 需要审批时调用 create_simple_flow，role_codes 如 ["ADMIN"]。
                6. 发布必须等用户在界面点确认；不要擅自 publish_app，除非用户已确认且 confirm=true。
                7. 改完结构后调用 preview_app 或 get_current_schema 核对。
                """.formatted(String.join("/", FieldTypes.ALL));
    }

    public static List<Map<String, Object>> buildMessages(List<AiMessageDO> history) {
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt()));
        List<AiMessageDO> slice = history;
        if (history.size() > HISTORY_LIMIT) {
            slice = history.subList(history.size() - HISTORY_LIMIT, history.size());
        }
        for (AiMessageDO msg : slice) {
            messages.add(toOpenAi(msg));
        }
        return messages;
    }

    public static Map<String, Object> toOpenAi(AiMessageDO msg) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("role", msg.getRole());
        String content = msg.getContent() == null ? "" : msg.getContent();
        if ("tool".equals(msg.getRole()) && content.length() > TOOL_RESULT_KEEP) {
            content = content.substring(0, TOOL_RESULT_KEEP) + "...(truncated)";
        }
        item.put("content", content);
        if ("tool".equals(msg.getRole())) {
            if (msg.getToolCallId() != null) {
                item.put("tool_call_id", msg.getToolCallId());
            }
            if (msg.getToolName() != null) {
                item.put("name", msg.getToolName());
            }
        }
        return item;
    }
}
