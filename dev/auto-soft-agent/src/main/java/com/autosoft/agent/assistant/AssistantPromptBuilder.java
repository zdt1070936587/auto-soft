package com.autosoft.agent.assistant;

import com.autosoft.agent.entity.AiAssistantMessageDO;
import com.autosoft.agent.studio.PromptBuilder;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 全局助手系统提示与历史组装。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
public final class AssistantPromptBuilder {

    public static final int HISTORY_LIMIT = PromptBuilder.HISTORY_LIMIT;
    public static final int TOOL_RESULT_KEEP = PromptBuilder.TOOL_RESULT_KEEP;

    private AssistantPromptBuilder() {
    }

    public static String systemPrompt() {
        return """
                你是 AI 管理后台的全局使用助手，帮助用户找到菜单入口、回顾自己的操作历史与页面浏览记录，并进行友好闲聊。
                规则：
                1. 你不是功能开发助手，禁止修改应用、实体、工作流；操作类意图通过 prepare_action_draft 生成计划，实际写库由用户在目标页保存。
                2. 导航问题：必须先调用 search_menus，只返回工具结果中的路径；无结果时明确告知，禁止编造菜单路径。
                3. 写操作历史：必须先调用 query_my_operations 或 get_operation_timeline；无记录时友好说明「没有找到相关记录」，禁止编造。
                4. 写操作历史仅覆盖已记录的系统写操作（如 USER.CREATE）。
                5. 页面浏览历史：用户问「打开/浏览/访问/进入某页面」时，必须先调用 query_my_page_visits；无记录时明确说明「没有找到浏览记录」，禁止编造。
                6. 写操作与页面浏览不可混用：「新增用户」查 oper log；「打开用户管理页」查 page visit。
                7. module/action 对照：新增用户 USER+CREATE，修改用户 USER+UPDATE，删除用户 USER+DELETE，发布应用 META+PUBLISH，运行工作流 WORKFLOW+RUN。
                8. 闲聊问题直接回答，不必强行调用工具。
                9. 用户主动告知姓名、职责、团队或偏好时，调用 remember_fact 写入记忆。
                10. 用户询问「记得吗/之前说过/我叫什么」时，调用 recall_user_memory；与 [用户画像] 冲突时以已确认 fact 为准。
                11. 操作类意图（新建/添加/创建用户或业务记录）：必须先 search_capabilities，再 get_capability_schema，再 prepare_action_draft；禁止未调用工具时声称操作已成功。
                12. 操作类与历史查询区分：「新增用户」走 prepare_action_draft；「有没有新增过用户」走 query_my_operations，二者不可混用。
                13. search_capabilities 返回 ambiguous=true 时，必须 ask_user 让用户选择具体功能。
                14. prepare_action_draft 返回 status=draft 时表示缺必填或存在未知字段，应继续追问或等待用户补全，不可说已创建成功。
                15. 只有 prepare_action_draft 返回 status=ready 且用户确认前往目标页保存后，才算操作完成。
                16. 回答简洁清晰，使用简体中文。
                """;
    }

    public static List<Map<String, Object>> buildMessages(String hint, String memoryContext,
                                                            List<AiAssistantMessageDO> history,
                                                            JsonMapper jsonMapper) {
        List<AiAssistantMessageDO> slice = history;
        if (history.size() > HISTORY_LIMIT) {
            slice = history.subList(history.size() - HISTORY_LIMIT, history.size());
        }
        List<Map<String, Object>> messages = new ArrayList<>();
        StringBuilder system = new StringBuilder(systemPrompt());
        if (memoryContext != null && !memoryContext.isBlank()) {
            system.append("\n\n").append(memoryContext.trim());
        }
        if (hint != null && !hint.isBlank()) {
            system.append("\n").append(hint.trim());
        }
        messages.add(Map.of("role", "system", "content", system.toString()));
        for (AiAssistantMessageDO msg : slice) {
            if ("__tool_calls".equals(msg.getToolName())) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("role", "assistant");
                item.put("content", "");
                item.put("tool_calls", jsonMapper.readValue(msg.getContent(),
                        new TypeReference<List<Map<String, Object>>>() {
                        }));
                messages.add(item);
            } else {
                messages.add(toOpenAi(msg));
            }
        }
        return messages;
    }

    public static Map<String, Object> toOpenAi(AiAssistantMessageDO msg) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("role", msg.getRole());
        String content = msg.getContent() == null ? "" : msg.getContent();
        if ("tool".equals(msg.getRole()) && content.length() > TOOL_RESULT_KEEP) {
            content = content.substring(0, TOOL_RESULT_KEEP) + "…(已截断)";
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
