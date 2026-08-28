package com.autosoft.agent.assistant;

import com.autosoft.agent.assistant.dto.AssistantChatDTO;
import com.autosoft.agent.assistant.memory.MemoryService;
import com.autosoft.agent.assistant.tool.AssistantToolContext;
import com.autosoft.agent.assistant.tool.AssistantToolRegistry;
import com.autosoft.agent.entity.AiAssistantMessageDO;
import com.autosoft.agent.entity.AiAssistantSessionDO;
import com.autosoft.agent.entity.AiAssistantToolLogDO;
import com.autosoft.agent.llm.LlmTurn;
import com.autosoft.agent.llm.OpenCodeGoManager;
import com.autosoft.agent.mapper.AiAssistantMessageMapper;
import com.autosoft.agent.mapper.AiAssistantSessionMapper;
import com.autosoft.agent.mapper.AiAssistantToolLogMapper;
import com.autosoft.common.core.ResultCode;
import com.autosoft.common.exception.BizException;
import com.autosoft.common.utils.AssertUtils;
import com.autosoft.framework.security.LoginUser;
import com.autosoft.framework.security.SecurityUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 全局助手对话编排。
 */
@Service
public class AssistantService {

    private static final Logger log = LoggerFactory.getLogger(AssistantService.class);
    public static final int MAX_TOOL_LOOPS = 6;

    private final AiAssistantSessionMapper sessionMapper;
    private final AiAssistantMessageMapper messageMapper;
    private final AiAssistantToolLogMapper toolLogMapper;
    private final OpenCodeGoManager openCodeGoManager;
    private final AssistantToolRegistry toolRegistry;
    private final MemoryService memoryService;
    private final JsonMapper jsonMapper;

    public AssistantService(AiAssistantSessionMapper sessionMapper,
                            AiAssistantMessageMapper messageMapper,
                            AiAssistantToolLogMapper toolLogMapper,
                            OpenCodeGoManager openCodeGoManager,
                            AssistantToolRegistry toolRegistry,
                            MemoryService memoryService,
                            JsonMapper jsonMapper) {
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
        this.toolLogMapper = toolLogMapper;
        this.openCodeGoManager = openCodeGoManager;
        this.toolRegistry = toolRegistry;
        this.memoryService = memoryService;
        this.jsonMapper = jsonMapper;
    }

    public SseEmitter startTurn(Long sessionId, AssistantChatDTO dto) {
        SseEmitter emitter = new SseEmitter(300_000L);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Map<String, String> mdc = org.slf4j.MDC.getCopyOfContextMap();
        Thread.startVirtualThread(() -> {
            if (mdc != null) {
                org.slf4j.MDC.setContextMap(mdc);
            }
            SecurityContextHolder.getContext().setAuthentication(authentication);
            try {
                runTurn(sessionId, dto, emitter);
            } finally {
                SecurityContextHolder.clearContext();
                org.slf4j.MDC.clear();
            }
        });
        return emitter;
    }

    public void runTurn(Long sessionId, AssistantChatDTO dto, SseEmitter emitter) {
        try {
            AiAssistantSessionDO session = loadSession(sessionId);
            LoginUser user = SecurityUtils.requireUser();
            if (!user.isSuperAdmin() && !user.getUserId().equals(session.getUserId())) {
                throw new BizException(ResultCode.FORBIDDEN, "不能查看他人会话");
            }
            AssertUtils.notBlank(dto == null ? null : dto.getMessage(), "消息不能为空");
            String userText = dto.getMessage().trim();
            persistMessage(sessionId, "user", userText, null, null, null);
            maybeTitle(session, userText);

            String hint = AssistantIntentHint.buildHint(userText);
            String memoryContext = memoryService.buildContext(user.getUserId(), userText);
            List<Map<String, Object>> messages = loadModelMessages(sessionId, hint, memoryContext);
            TurnResult turnResult = loopModel(session, user.getUserId(), messages, emitter);
            memoryService.captureChatEpisodeAsync(user.getUserId(), sessionId, userText, turnResult.assistantText());

            emit(emitter, "done", Map.of(
                    "sessionId", sessionId,
                    "tokenInput", nvl(session.getTokenInput()),
                    "tokenOutput", nvl(session.getTokenOutput())));
            emitter.complete();
        } catch (BizException ex) {
            emit(emitter, "error", Map.of("message", ex.getMessage()));
            emitter.complete();
        } catch (Exception ex) {
            log.error("assistant turn failed, sessionId={}", sessionId, ex);
            emit(emitter, "error", Map.of("message", "对话失败，请稍后重试"));
            emitter.completeWithError(ex);
        }
    }

    private TurnResult loopModel(AiAssistantSessionDO session, Long userId,
                                 List<Map<String, Object>> messages, SseEmitter emitter) {
        List<Map<String, Object>> tools = toolRegistry.openaiTools();
        String lastStructured = null;
        for (int i = 0; i < MAX_TOOL_LOOPS; i++) {
            LlmTurn turn = openCodeGoManager.chat(messages, tools);
            addTokens(session, turn);
            if (!turn.hasToolCalls()) {
                String text = turn.getContent() == null ? "" : turn.getContent();
                persistMessage(session.getId(), "assistant", text, null, null, lastStructured);
                emit(emitter, "text", Map.of("content", text));
                if (lastStructured != null) {
                    emitStructured(emitter, lastStructured);
                }
                return new TurnResult(text, lastStructured);
            }
            persistToolCalls(session.getId(), turn);
            messages.add(assistantToolCallMessage(turn));
            AssistantToolContext context = new AssistantToolContext(session, userId);
            for (LlmTurn.ToolCall call : turn.getToolCalls()) {
                String structured = executeOneTool(session, context, call, messages, emitter);
                if (structured != null) {
                    lastStructured = structured;
                }
            }
        }
        emit(emitter, "error", Map.of("message", "工具调用次数过多，请简化问题后重试"));
        return new TurnResult("", lastStructured);
    }

    /**
     * @return structured payload JSON if applicable
     */
    private String executeOneTool(AiAssistantSessionDO session, AssistantToolContext context,
                                    LlmTurn.ToolCall call, List<Map<String, Object>> messages,
                                    SseEmitter emitter) {
        emit(emitter, "tool_start", Map.of("tool", nz(call.getName()), "arguments", nz(call.getArgumentsJson())));
        long start = System.currentTimeMillis();
        boolean ok = true;
        String error = null;
        String result;
        try {
            result = toolRegistry.execute(call.getName(), context, call.getArgumentsJson());
        } catch (BizException ex) {
            ok = false;
            error = ex.getMessage();
            result = "{\"error\":\"" + escape(ex.getMessage()) + "\"}";
        } catch (Exception ex) {
            ok = false;
            error = "工具执行失败";
            result = "{\"error\":\"工具执行失败\"}";
            log.warn("assistant tool failed, name={}", call.getName());
        }
        saveToolLog(session.getId(), call, result, ok, error, (int) (System.currentTimeMillis() - start));
        persistMessage(session.getId(), "tool", result, call.getName(), call.getId(), null);
        messages.add(toolMessage(call, result));
        emit(emitter, "tool_end", Map.of("tool", nz(call.getName()), "success", ok,
                "result", AssistantToolRegistry.truncate(result)));
        return extractStructuredPayload(call.getName(), result);
    }

    private String extractStructuredPayload(String toolName, String result) {
        if (result == null || result.isBlank()) {
            return null;
        }
        try {
            Map<String, Object> map = jsonMapper.readValue(result, new TypeReference<Map<String, Object>>() {
            });
            if ("search_menus".equals(toolName) && map.containsKey("items")) {
                map.putIfAbsent("type", "nav_link");
                return jsonMapper.writeValueAsString(map);
            }
            if ("get_operation_timeline".equals(toolName)) {
                map.putIfAbsent("type", "oper_timeline");
                return jsonMapper.writeValueAsString(map);
            }
        } catch (RuntimeException ignored) {
            // not structured
        }
        return null;
    }

    private void emitStructured(SseEmitter emitter, String payloadJson) {
        try {
            Map<String, Object> data = jsonMapper.readValue(payloadJson, new TypeReference<Map<String, Object>>() {
            });
            emit(emitter, "structured", data);
        } catch (RuntimeException ex) {
            log.warn("structured emit failed");
        }
    }

    private AiAssistantSessionDO loadSession(Long sessionId) {
        AiAssistantSessionDO session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BizException(ResultCode.NOT_FOUND, "会话不存在");
        }
        return session;
    }

    private List<Map<String, Object>> loadModelMessages(Long sessionId, String hint, String memoryContext) {
        List<AiAssistantMessageDO> rows = messageMapper.selectList(new LambdaQueryWrapper<AiAssistantMessageDO>()
                .eq(AiAssistantMessageDO::getSessionId, sessionId)
                .orderByAsc(AiAssistantMessageDO::getId));
        return AssistantPromptBuilder.buildMessages(hint, memoryContext, rows, jsonMapper);
    }

    private void persistMessage(Long sessionId, String role, String content,
                                String toolName, String toolCallId, String payloadJson) {
        AiAssistantMessageDO row = new AiAssistantMessageDO();
        row.setSessionId(sessionId);
        row.setRole(role);
        row.setContent(content);
        row.setPayloadJson(payloadJson);
        row.setToolName(toolName);
        row.setToolCallId(toolCallId);
        row.setTokens(0);
        messageMapper.insert(row);
    }

    private void persistToolCalls(Long sessionId, LlmTurn turn) {
        List<Map<String, Object>> payload = new ArrayList<>();
        for (LlmTurn.ToolCall call : turn.getToolCalls()) {
            Map<String, Object> fn = new LinkedHashMap<>();
            fn.put("name", call.getName());
            fn.put("arguments", call.getArgumentsJson() == null ? "{}" : call.getArgumentsJson());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", call.getId());
            item.put("type", "function");
            item.put("function", fn);
            payload.add(item);
        }
        persistMessage(sessionId, "assistant", jsonMapper.writeValueAsString(payload), "__tool_calls", null, null);
    }

    private Map<String, Object> assistantToolCallMessage(LlmTurn turn) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("role", "assistant");
        item.put("content", "");
        List<Map<String, Object>> calls = new ArrayList<>();
        for (LlmTurn.ToolCall call : turn.getToolCalls()) {
            Map<String, Object> fn = new LinkedHashMap<>();
            fn.put("name", call.getName());
            fn.put("arguments", call.getArgumentsJson() == null ? "{}" : call.getArgumentsJson());
            Map<String, Object> one = new LinkedHashMap<>();
            one.put("id", call.getId());
            one.put("type", "function");
            one.put("function", fn);
            calls.add(one);
        }
        item.put("tool_calls", calls);
        return item;
    }

    private Map<String, Object> toolMessage(LlmTurn.ToolCall call, String result) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("role", "tool");
        item.put("tool_call_id", call.getId());
        item.put("name", call.getName());
        item.put("content", result);
        return item;
    }

    private void saveToolLog(Long sessionId, LlmTurn.ToolCall call, String result,
                             boolean ok, String error, int cost) {
        AiAssistantToolLogDO row = new AiAssistantToolLogDO();
        row.setSessionId(sessionId);
        row.setToolName(call.getName());
        row.setArgumentsJson(AssistantToolRegistry.truncate(call.getArgumentsJson()));
        row.setResultJson(AssistantToolRegistry.truncate(result));
        row.setSuccess(ok ? 1 : 0);
        row.setErrorMsg(error);
        row.setDurationMs(cost);
        toolLogMapper.insert(row);
    }

    private void addTokens(AiAssistantSessionDO session, LlmTurn turn) {
        session.setTokenInput(nvl(session.getTokenInput()) + turn.getPromptTokens());
        session.setTokenOutput(nvl(session.getTokenOutput()) + turn.getCompletionTokens());
        sessionMapper.updateById(session);
    }

    private void maybeTitle(AiAssistantSessionDO session, String userText) {
        if (session.getTitle() != null && !"新对话".equals(session.getTitle())) {
            return;
        }
        session.setTitle(userText.length() > 24 ? userText.substring(0, 24) : userText);
        sessionMapper.updateById(session);
    }

    private void emit(SseEmitter emitter, String event, Object data) {
        try {
            emitter.send(SseEmitter.event().name(event).data(data));
        } catch (IOException ex) {
            log.warn("assistant sse emit failed, event={}", event);
        }
    }

    private static long nvl(Long value) {
        return value == null ? 0L : value;
    }

    private static String nz(String value) {
        return value == null ? "" : value;
    }

    private static String escape(String text) {
        return text == null ? "" : text.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private record TurnResult(String assistantText, String structuredPayload) {
    }
}
