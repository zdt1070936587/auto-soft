package com.autosoft.agent.studio;

import com.autosoft.agent.entity.AiAttachmentDO;
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

    private static final String JSON_TOOL_SCHEMA_EXAMPLE = """
            {
              "version": 1,
              "title": "JSON 工具",
              "layout": "admin",
              "state": { "source": "", "result": "" },
              "blocks": [
                { "type": "textarea", "id": "source", "label": "输入 JSON", "bind": "source", "rows": 12 },
                { "type": "toolbar", "items": [
                  { "label": "格式化", "action": "json.format", "from": "source", "to": "result" },
                  { "label": "压缩", "action": "json.minify", "from": "source", "to": "result" },
                  { "label": "排序键", "action": "json.sortKeys", "from": "source", "to": "result" }
                ]},
                { "type": "textarea", "id": "result", "label": "结果", "bind": "result", "readonly": true, "rows": 12 }
              ]
            }
            """;

    private PromptBuilder() {
    }

    public static String systemPrompt(AgentMode mode) {
        String base = """
                你是 AI 管理后台的功能开发助手。你只能通过已注册工具改元数据，禁止输出 Java/Vue 源码，禁止编造 SQL。
                规则：
                1. 一次会话只改一个应用。先 ask_user 确认需求类型，再 create_app。
                2. 应用类型 app_kind：
                   - admin：后台 CRUD（请假单、订单管理等），需 define_entity + add_field，禁止为纯工具建实体。
                   - frontend：纯前端工具页（JSON 格式化、计算器、转换器等），用 define_page(page_type=PAGE)，禁止建实体。
                   - h5：移动端落地页/工具，同 frontend 但 layout=h5。
                3. 低代码 PAGE：define_page 必填 page_code、layout、schema_json。内置 block：textarea/text/button/toolbar/card/divider；内置动作：json.format/json.minify/json.sortKeys/json.validate/state.copy/state.clear。
                4. JSON 工具 schema 示例：
                %s
                5. 字段类型白名单：%s。多行文本用 text，数字用 int 或 decimal。
                6. code 只能是小写字母开头的英文、数字、下划线，不要用中文。
                7. 不要编造不存在的角色 code。内置角色：SUPER_ADMIN、ADMIN、DEVELOPER、USER。审批默认 ADMIN。
                8. 需要审批时调用 create_simple_flow，role_codes 如 ["ADMIN"]。
                9. 发布必须等用户在界面点确认；不要擅自 publish_app，除非用户已确认且 confirm=true。
                10. 改完结构后调用 preview_app 或 get_current_schema 核对。
                """.formatted(JSON_TOOL_SCHEMA_EXAMPLE.trim(), String.join("/", FieldTypes.ALL));
        return base + "\n" + modePrompt(mode);
    }

    private static String modePrompt(AgentMode mode) {
        return switch (mode) {
            case DISCUSS -> """
                    当前工作级别：讨论。
                    - 只澄清需求、解读现有 schema、回答疑问。
                    - 禁止调用任何会写库的元数据工具（create_app、define_entity、add_field、define_page 等）。
                    - 可用工具：ask_user、get_current_schema、preview_app。
                    """;
            case PLAN -> """
                    当前工作级别：计划。
                    - 产出分步骤实施计划，区分 CRUD 与低代码工具/H5，但不写库。
                    - 必须用 ask_user 给出计划清单并请用户确认；确认后提示用户切换到「开发」级别再执行。
                    - 禁止调用任何会写库的元数据工具。
                    - 可用工具：ask_user、get_current_schema、preview_app。
                    """;
            case DEVELOP -> """
                    当前工作级别：开发。
                    - 可直接调用全部元数据工具修改草稿应用。
                    - 仍应先 ask_user 确认关键字段后再落库。
                    - 工具/H5 需求：create_app(app_kind=frontend|h5) + define_page(PAGE)，不要 define_entity。
                    - CRUD 需求：create_app(app_kind=admin) + define_entity + add_field。
                    """;
        };
    }

    public static List<Map<String, Object>> buildMessages(List<AiMessageDO> history) {
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt(AgentMode.DEVELOP)));
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
