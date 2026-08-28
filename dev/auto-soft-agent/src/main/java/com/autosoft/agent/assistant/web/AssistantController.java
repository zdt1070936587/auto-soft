package com.autosoft.agent.assistant.web;

import com.autosoft.agent.assistant.AssistantMemoryService;
import com.autosoft.agent.assistant.AssistantService;
import com.autosoft.agent.assistant.AssistantSessionService;
import com.autosoft.agent.assistant.dto.AssistantChatDTO;
import com.autosoft.agent.assistant.memory.EpisodeSearchHit;
import com.autosoft.agent.assistant.vo.AiAssistantMessageVO;
import com.autosoft.agent.assistant.vo.AiAssistantSessionVO;
import com.autosoft.agent.assistant.vo.AiMemoryFactVO;
import com.autosoft.common.core.R;
import com.autosoft.common.utils.AssertUtils;
import com.autosoft.framework.security.RequiresPermission;
import com.autosoft.framework.security.SecurityUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * 全局 AI 助手 API。无业务逻辑。
 */
@RestController
@RequestMapping("/api/assistant")
public class AssistantController {

    private final AssistantSessionService sessionService;
    private final AssistantService assistantService;
    private final AssistantMemoryService memoryService;

    public AssistantController(AssistantSessionService sessionService,
                               AssistantService assistantService,
                               AssistantMemoryService memoryService) {
        this.sessionService = sessionService;
        this.assistantService = assistantService;
        this.memoryService = memoryService;
    }

    @GetMapping("/sessions")
    @RequiresPermission("assistant:use")
    public R<List<AiAssistantSessionVO>> sessions() {
        return R.ok(sessionService.listMine());
    }

    @PostMapping("/sessions")
    @RequiresPermission("assistant:use")
    public R<Long> create() {
        return R.ok(sessionService.create());
    }

    @DeleteMapping("/sessions/{id}")
    @RequiresPermission("assistant:use")
    public R<Void> delete(@PathVariable Long id) {
        sessionService.delete(id);
        return R.ok();
    }

    @GetMapping("/sessions/{id}/messages")
    @RequiresPermission("assistant:use")
    public R<List<AiAssistantMessageVO>> messages(@PathVariable Long id) {
        return R.ok(sessionService.messages(id));
    }

    @PostMapping(value = "/sessions/{id}/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @RequiresPermission("assistant:use")
    public SseEmitter chat(@PathVariable Long id, @RequestBody AssistantChatDTO dto) {
        sessionService.requireOwned(id);
        AssertUtils.notBlank(dto == null ? null : dto.getMessage(), "消息不能为空");
        return assistantService.startTurn(id, dto);
    }

    @GetMapping("/memory/facts")
    @RequiresPermission("assistant:use")
    public R<List<AiMemoryFactVO>> listFacts() {
        return R.ok(memoryService.listFacts(SecurityUtils.requireUser().getUserId()));
    }

    @DeleteMapping("/memory/facts/{id}")
    @RequiresPermission("assistant:use")
    public R<Void> deleteFact(@PathVariable Long id) {
        memoryService.deleteFact(SecurityUtils.requireUser().getUserId(), id);
        return R.ok();
    }

    @PostMapping("/memory/facts/{id}/confirm")
    @RequiresPermission("assistant:use")
    public R<Void> confirmFact(@PathVariable Long id) {
        memoryService.confirmFact(SecurityUtils.requireUser().getUserId(), id);
        return R.ok();
    }

    @GetMapping("/memory/episodes")
    @RequiresPermission("assistant:use")
    public R<List<EpisodeSearchHit>> listEpisodes(@RequestParam(defaultValue = "20") int limit) {
        return R.ok(memoryService.listEpisodes(SecurityUtils.requireUser().getUserId(), limit));
    }
}
