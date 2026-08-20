package com.autosoft.agent.web;

import com.autosoft.agent.dto.ChatMessageDTO;
import com.autosoft.agent.studio.AgentService;
import com.autosoft.agent.studio.StudioSessionService;
import com.autosoft.agent.vo.AiMessageVO;
import com.autosoft.agent.vo.AiSessionVO;
import com.autosoft.common.core.R;
import com.autosoft.common.utils.AssertUtils;
import com.autosoft.framework.security.RequiresPermission;
import com.autosoft.meta.vo.RuntimeSchemaVO;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * 功能开发工作室。无业务逻辑。
 */
@RestController
@RequestMapping("/api/studio")
public class StudioController {

    private final StudioSessionService sessionService;
    private final AgentService agentService;

    public StudioController(StudioSessionService sessionService, AgentService agentService) {
        this.sessionService = sessionService;
        this.agentService = agentService;
    }

    @GetMapping("/sessions")
    @RequiresPermission("studio:use")
    public R<List<AiSessionVO>> sessions() {
        return R.ok(sessionService.listMine());
    }

    @PostMapping("/sessions")
    @RequiresPermission("studio:use")
    public R<Long> create() {
        return R.ok(sessionService.create());
    }

    @GetMapping("/sessions/{id}/messages")
    @RequiresPermission("studio:use")
    public R<List<AiMessageVO>> messages(@PathVariable Long id) {
        return R.ok(sessionService.messages(id));
    }

    @GetMapping("/sessions/{id}/schema")
    @RequiresPermission("studio:use")
    public R<RuntimeSchemaVO> schema(@PathVariable Long id) {
        return R.ok(sessionService.schema(id));
    }

    @PostMapping(value = "/sessions/{id}/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @RequiresPermission("studio:use")
    public SseEmitter chat(@PathVariable Long id, @RequestBody ChatMessageDTO dto) {
        sessionService.requireOwned(id);
        AssertUtils.notBlank(dto == null ? null : dto.getMessage(), "消息不能为空");
        return agentService.startTurn(id, dto.getMessage());
    }
}
