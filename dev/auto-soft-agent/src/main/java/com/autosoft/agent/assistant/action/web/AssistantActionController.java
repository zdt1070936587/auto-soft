package com.autosoft.agent.assistant.action.web;

import com.autosoft.agent.assistant.action.ActionDraftService;
import com.autosoft.agent.assistant.action.CapabilityDiscoveryService;
import com.autosoft.agent.assistant.action.model.CapabilitySearchResult;
import com.autosoft.agent.assistant.action.vo.ActionDraftVO;
import com.autosoft.common.core.R;
import com.autosoft.framework.security.RequiresPermission;
import com.autosoft.framework.security.SecurityUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 助手操作 Copilot REST API。
 *
 * @author zhaodt
 * @since 2026-09-02
 */
@RestController
@RequestMapping("/api/assistant")
public class AssistantActionController {

    private final CapabilityDiscoveryService discoveryService;
    private final ActionDraftService draftService;

    public AssistantActionController(CapabilityDiscoveryService discoveryService,
                                     ActionDraftService draftService) {
        this.discoveryService = discoveryService;
        this.draftService = draftService;
    }

    @GetMapping("/capabilities/search")
    @RequiresPermission("assistant:use")
    public R<CapabilitySearchResult> searchCapabilities(@RequestParam String keyword,
                                                        @RequestParam(defaultValue = "create") String intent,
                                                        @RequestParam(defaultValue = "5") int limit) {
        Long userId = SecurityUtils.requireUser().getUserId();
        return R.ok(discoveryService.search(keyword, intent, limit, userId));
    }

    @GetMapping("/action-drafts/{id}")
    @RequiresPermission("assistant:use")
    public R<ActionDraftVO> getDraft(@PathVariable("id") String id) {
        Long userId = SecurityUtils.requireUser().getUserId();
        return R.ok(draftService.get(id, userId));
    }

    @PostMapping("/action-drafts/{id}/consume")
    @RequiresPermission("assistant:use")
    public R<Map<String, Object>> consume(@PathVariable("id") String id) {
        Long userId = SecurityUtils.requireUser().getUserId();
        draftService.consume(id, userId);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("consumed", true);
        return R.ok(payload);
    }

    @PostMapping("/action-drafts/{id}/cancel")
    @RequiresPermission("assistant:use")
    public R<Map<String, Object>> cancel(@PathVariable("id") String id) {
        Long userId = SecurityUtils.requireUser().getUserId();
        draftService.cancel(id, userId);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("cancelled", true);
        return R.ok(payload);
    }
}
