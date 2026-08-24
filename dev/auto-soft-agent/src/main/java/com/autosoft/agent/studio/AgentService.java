package com.autosoft.agent.studio;

import com.autosoft.agent.dto.ChatMessageDTO;
import com.autosoft.agent.entity.AiAttachmentDO;
import com.autosoft.agent.entity.AiMessageDO;
import com.autosoft.agent.entity.AiSessionDO;
import com.autosoft.agent.entity.AiToolLogDO;
import com.autosoft.agent.llm.LlmTurn;
import com.autosoft.agent.llm.OpenCodeGoManager;
import com.autosoft.agent.mapper.AiMessageMapper;
import com.autosoft.agent.mapper.AiSessionMapper;
import com.autosoft.agent.mapper.AiToolLogMapper;
import com.autosoft.agent.tool.ToolContext;
import com.autosoft.agent.tool.ToolRegistry;
import com.autosoft.common.core.ResultCode;
import com.autosoft.common.exception.BizException;
import com.autosoft.common.utils.AssertUtils;
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
import java.util.stream.Collectors;

/**
 * 对话编排。主方法只写步骤。
 */
@Service
public class AgentService {

    private static final Logger log = LoggerFactory.getLogger(AgentService.class);
    public static final int MAX_TOOL_LOOPS = 8;

    private final AiSessionMapper sessionMapper;
    private final AiMessageMapper messageMapper;
    private final AiToolLogMapper toolLogMapper;
    private final OpenCodeGoManager openCodeGoManager;
    private final ToolRegistry toolRegistry;
    private final JsonMapper jsonMapper;
    private final TurnPauseRegistry pauseRegistry;
    private final StudioAttachmentService attachmentService;

    public AgentService(AiSessionMapper sessionMapper, AiMessageMapper messageMapper, AiToolLogMapper toolLogMapper,
                        OpenCodeGoManager openCodeGoManager, ToolRegistry toolRegistry, JsonMapper jsonMapper,
                        TurnPauseRegistry pauseRegistry, StudioAttachmentService attachmentService) {
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
        this.toolLogMapper = toolLogMapper;
        this.openCodeGoManager = openCodeGoManager;
        this.toolRegistry = toolRegistry;
        this.jsonMapper = jsonMapper;
        this.pauseRegistry = pauseRegistry;
        this.attachmentService = attachmentService;
    }

    public SseEmitter startTurn(Long sessionId, ChatMessageDTO dto) {
        SseEmitter emitter = new SseEmitter(300_000L);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        java.util.Map<String, String> mdc = org.slf4j.MDC.getCopyOfContextMap();
        Thread.startVirtualThread(() -> {
            if (mdc != null) {
                org.slf4j.MDC.setContextMap(mdc);
            }
            SecurityContextHolder.getContext().setAuthentication(authentication);
            try {
                runTurn(sessionId, dto, emitter);
            } finally {
                pauseRegistry.clear(sessionId);
                SecurityContextHolder.clearContext();
                org.slf4j.MDC.clear();
            }
        });
        return emitter;
    }

    public void requestPause(Long sessionId) {
        pauseRegistry.requestPause(sessionId);
    }

    public void runTurn(Long sessionId, ChatMessageDTO dto, SseEmitter emitter) {
        try {
            AiSessionDO session = loadSession(sessionId);
            AssertUtils.notBlank(dto == null ? null : dto.getMessage(), "消息不能为空");
            pauseRegistry.clear(sessionId);
            applyMode(session, dto == null ? null : dto.getAgentMode());
            AgentMode mode = currentMode(session);

            List<Long> attachmentIds = dto == null || dto.getAttachmentIds() == null
                    ? List.of() : dto.getAttachmentIds();
            List<AiAttachmentDO> attachments = attachmentService.requireForSend(sessionId, attachmentIds);
            String userText = dto.getMessage().trim();
            String persistContent = attachmentService.buildPersistContent(userText, attachments);
            Long userMessageId = persistMessage(sessionId, "user", persistContent, null, null);
            attachmentService.linkToMessage(sessionId, userMessageId, attachmentIds);
            maybeTitle(session, userText);

            List<Map<String, Object>> messages = loadModelMessages(sessionId, mode);
            TurnOutcome outcome = loopModel(session, mode, messages, emitter);
            if (outcome == TurnOutcome.PAUSED) {
                return;
            }
            if (outcome == TurnOutcome.INCOMPLETE) {
                emit(emitter, "error", Map.of("message", "请缩短需求或手动在应用建模里改"));
            }
            emit(emitter, "done", Map.of(
                    "sessionId", sessionId,
                    "tokenInput", nvl(session.getTokenInput()),
                    "tokenOutput", nvl(session.getTokenOutput()),
                    "agentMode", mode.code()));
            emitter.complete();
        } catch (BizException ex) {
            emit(emitter, "error", Map.of("message", ex.getMessage()));
            emitter.complete();
        } catch (Exception ex) {
            log.error("studio turn failed, sessionId={}", sessionId, ex);
            emit(emitter, "error", Map.of("message", "对话失败，请稍后重试"));
            emitter.completeWithError(ex);
        }
    }

    private enum TurnOutcome {
        COMPLETE, PAUSED, INCOMPLETE
    }

    private TurnOutcome loopModel(AiSessionDO session, AgentMode mode, List<Map<String, Object>> messages,
                                  SseEmitter emitter) {
        List<Map<String, Object>> tools = toolRegistry.openaiTools(mode);
        for (int i = 0; i < MAX_TOOL_LOOPS; i++) {
            if (pauseRegistry.consumePause(session.getId())) {
                finishPaused(session, emitter);
                return TurnOutcome.PAUSED;
            }
            LlmTurn turn = openCodeGoManager.chat(messages, tools);
            addTokens(session, turn);
            if (!turn.hasToolCalls()) {
                String text = turn.getContent() == null ? "" : turn.getContent();
                persistMessage(session.getId(), "assistant", text, null, null);
                emit(emitter, "text", Map.of("content", text));
                return TurnOutcome.COMPLETE;
            }
            persistToolCalls(session.getId(), turn);
            messages.add(assistantToolCallMessage(turn));
            ToolContext context = new ToolContext(session, sessionMapper);
            boolean askedUser = false;
            for (LlmTurn.ToolCall call : turn.getToolCalls()) {
                if (executeOneTool(session, mode, context, call, messages, emitter)) {
                    askedUser = true;
                }
            }
            if (context.isSchemaUpdated()) {
                emit(emitter, "schema_updated", Map.of("appId", nvl(session.getAppId())));
            }
            if (askedUser) {
                return TurnOutcome.COMPLETE;
            }
            if (pauseRegistry.consumePause(session.getId())) {
                finishPaused(session, emitter);
                return TurnOutcome.PAUSED;
            }
        }
        return TurnOutcome.INCOMPLETE;
    }

    private void finishPaused(AiSessionDO session, SseEmitter emitter) {
        String text = "已暂停。当前步骤内已完成的修改已保留，你可以补充意见后继续。";
        persistMessage(session.getId(), "assistant", text, null, null);
        emit(emitter, "paused", Map.of("message", text));
        emitter.complete();
    }

    /**
     * @return true 表示本工具为 ask_user，调用方应结束本轮
     */
    private boolean executeOneTool(AiSessionDO session, AgentMode mode, ToolContext context, LlmTurn.ToolCall call,
                                   List<Map<String, Object>> messages, SseEmitter emitter) {
        emit(emitter, "tool_start", Map.of("tool", nz(call.getName()), "arguments", nz(call.getArgumentsJson())));
        long start = System.currentTimeMillis();
        boolean ok = true;
        String error = null;
        String result;
        try {
            result = toolRegistry.execute(call.getName(), context, call.getArgumentsJson(), mode);
        } catch (BizException ex) {
            ok = false;
            error = ex.getMessage();
            result = "{\"error\":\"" + escape(ex.getMessage()) + "\"}";
        } catch (Exception ex) {
            ok = false;
            error = "工具执行失败";
            result = "{\"error\":\"工具执行失败\"}";
            log.warn("tool failed, name={}", call.getName());
        }
        saveToolLog(session.getId(), call, result, ok, error, (int) (System.currentTimeMillis() - start));
        persistMessage(session.getId(), "tool", result, call.getName(), call.getId());
        messages.add(toolMessage(call, result));
        emit(emitter, "tool_end", Map.of("tool", nz(call.getName()), "success", ok, "result", ToolRegistry.truncate(result)));
        if (!"ask_user".equals(call.getName())) {
            return false;
        }
        String question = extractAskUserQuestion(call.getArgumentsJson());
        persistMessage(session.getId(), "assistant", question, "ask_user", null);
        emit(emitter, "ask_user", Map.of("question", question));
        return true;
    }

    private void applyMode(AiSessionDO session, String requestedMode) {
        if (requestedMode == null || requestedMode.isBlank()) {
            return;
        }
        AgentMode mode = AgentMode.from(requestedMode);
        session.setAgentMode(mode.code());
        sessionMapper.updateById(session);
    }

    private AgentMode currentMode(AiSessionDO session) {
        return AgentMode.from(session.getAgentMode());
    }

    private String extractAskUserQuestion(String argumentsJson) {
        if (argumentsJson == null || argumentsJson.isBlank()) {
            return "请确认以上方案，或提出修改意见。";
        }
        try {
            Map<String, Object> args = jsonMapper.readValue(argumentsJson, new TypeReference<Map<String, Object>>() {
            });
            Object question = args.get("question");
            if (question != null && !String.valueOf(question).isBlank()) {
                return String.valueOf(question);
            }
        } catch (Exception ignored) {
            // 解析失败时把整段参数当作确认文案
        }
        return argumentsJson;
    }

    private AiSessionDO loadSession(Long sessionId) {
        AiSessionDO session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BizException(ResultCode.NOT_FOUND, "会话不存在");
        }
        return session;
    }

    private List<Map<String, Object>> loadModelMessages(Long sessionId, AgentMode mode) {
        List<AiMessageDO> rows = messageMapper.selectList(new LambdaQueryWrapper<AiMessageDO>()
                .eq(AiMessageDO::getSessionId, sessionId).orderByAsc(AiMessageDO::getId));
        if (rows.size() > PromptBuilder.HISTORY_LIMIT) {
            rows = rows.subList(rows.size() - PromptBuilder.HISTORY_LIMIT, rows.size());
        }
        List<Long> userMessageIds = rows.stream()
                .filter(msg -> "user".equals(msg.getRole()))
                .map(AiMessageDO::getId)
                .toList();
        Map<Long, List<AiAttachmentDO>> attachmentsByMessage = attachmentService.listByMessageIds(userMessageIds)
                .stream()
                .collect(Collectors.groupingBy(AiAttachmentDO::getMessageId));

        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", PromptBuilder.systemPrompt(mode)));
        for (AiMessageDO msg : rows) {
            if ("__tool_calls".equals(msg.getToolName())) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("role", "assistant");
                item.put("content", "");
                item.put("tool_calls", jsonMapper.readValue(msg.getContent(), new TypeReference<List<Map<String, Object>>>() {
                }));
                messages.add(item);
            } else if ("user".equals(msg.getRole())) {
                List<AiAttachmentDO> linked = attachmentsByMessage.getOrDefault(msg.getId(), List.of());
                messages.add(attachmentService.buildUserMessageFromStored(msg.getContent(), linked));
            } else {
                messages.add(PromptBuilder.toOpenAi(msg));
            }
        }
        return messages;
    }

    private Long persistMessage(Long sessionId, String role, String content, String toolName, String toolCallId) {
        AiMessageDO row = new AiMessageDO();
        row.setSessionId(sessionId);
        row.setRole(role);
        row.setContent(content);
        row.setToolName(toolName);
        row.setToolCallId(toolCallId);
        row.setTokens(0);
        messageMapper.insert(row);
        return row.getId();
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
        persistMessage(sessionId, "assistant", jsonMapper.writeValueAsString(payload), "__tool_calls", null);
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

    private void saveToolLog(Long sessionId, LlmTurn.ToolCall call, String result, boolean ok, String error, int cost) {
        AiToolLogDO row = new AiToolLogDO();
        row.setSessionId(sessionId);
        row.setToolName(call.getName());
        row.setArgumentsJson(ToolRegistry.truncate(call.getArgumentsJson()));
        row.setResultJson(ToolRegistry.truncate(result));
        row.setSuccess(ok ? 1 : 0);
        row.setErrorMsg(error);
        row.setDurationMs(cost);
        toolLogMapper.insert(row);
    }

    private void addTokens(AiSessionDO session, LlmTurn turn) {
        session.setTokenInput(nvl(session.getTokenInput()) + turn.getPromptTokens());
        session.setTokenOutput(nvl(session.getTokenOutput()) + turn.getCompletionTokens());
        sessionMapper.updateById(session);
    }

    private void maybeTitle(AiSessionDO session, String userText) {
        if (session.getTitle() != null && !"新会话".equals(session.getTitle())) {
            return;
        }
        session.setTitle(userText.length() > 24 ? userText.substring(0, 24) : userText);
        sessionMapper.updateById(session);
    }

    private void emit(SseEmitter emitter, String event, Object data) {
        try {
            emitter.send(SseEmitter.event().name(event).data(data));
        } catch (IOException ex) {
            log.warn("sse emit failed, event={}", event);
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
}
