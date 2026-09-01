package com.autosoft.agent.web;

import com.autosoft.agent.dto.ChatMessageDTO;
import com.autosoft.agent.dto.UpdateModeDTO;
import com.autosoft.agent.studio.AgentService;
import com.autosoft.agent.studio.StudioAttachmentService;
import com.autosoft.agent.studio.StudioSessionService;
import com.autosoft.agent.vo.AiAttachmentVO;
import com.autosoft.agent.vo.AiMessageVO;
import com.autosoft.agent.vo.AiSessionVO;
import com.autosoft.common.core.R;
import com.autosoft.common.utils.AssertUtils;
import com.autosoft.framework.security.RequiresPermission;
import com.autosoft.meta.vo.PageViewVO;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * 功能开发工作室。无业务逻辑。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
@RestController
@RequestMapping("/api/studio")
public class StudioController {

    private final StudioSessionService sessionService;
    private final AgentService agentService;
    private final StudioAttachmentService attachmentService;

    public StudioController(StudioSessionService sessionService, AgentService agentService,
                            StudioAttachmentService attachmentService) {
        this.sessionService = sessionService;
        this.agentService = agentService;
        this.attachmentService = attachmentService;
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

    @DeleteMapping("/sessions/{id}")
    @RequiresPermission("studio:use")
    public R<Void> delete(@PathVariable Long id) {
        sessionService.delete(id);
        return R.ok();
    }

    @PostMapping("/sessions/{id}/delete")
    @RequiresPermission("studio:use")
    public R<Void> deleteByPost(@PathVariable Long id) {
        sessionService.delete(id);
        return R.ok();
    }

    @GetMapping("/sessions/{id}/messages")
    @RequiresPermission("studio:use")
    public R<List<AiMessageVO>> messages(@PathVariable Long id) {
        return R.ok(sessionService.messages(id));
    }

    @GetMapping("/sessions/{id}/schema")
    @RequiresPermission("studio:use")
    public R<PageViewVO> schema(@PathVariable Long id) {
        return R.ok(sessionService.schema(id));
    }

    @PatchMapping("/sessions/{id}/mode")
    @RequiresPermission("studio:use")
    public R<Void> updateMode(@PathVariable Long id, @RequestBody UpdateModeDTO dto) {
        sessionService.updateMode(id, dto);
        return R.ok();
    }

    @PostMapping("/sessions/{id}/pause")
    @RequiresPermission("studio:use")
    public R<Void> pause(@PathVariable Long id) {
        sessionService.requireOwned(id);
        agentService.requestPause(id);
        return R.ok();
    }

    @PostMapping(value = "/sessions/{id}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequiresPermission("studio:use")
    public R<AiAttachmentVO> upload(@PathVariable Long id, @RequestPart("file") MultipartFile file) {
        sessionService.requireOwned(id);
        return R.ok(attachmentService.upload(id, file));
    }

    @PostMapping(value = "/sessions/{id}/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @RequiresPermission("studio:use")
    public SseEmitter chat(@PathVariable Long id, @RequestBody ChatMessageDTO dto) {
        sessionService.requireOwned(id);
        AssertUtils.notBlank(dto == null ? null : dto.getMessage(), "消息不能为空");
        return agentService.startTurn(id, dto);
    }
}
