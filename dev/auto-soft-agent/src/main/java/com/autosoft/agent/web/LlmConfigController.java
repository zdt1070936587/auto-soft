package com.autosoft.agent.web;

import com.autosoft.agent.dto.LlmConfigSaveDTO;
import com.autosoft.agent.studio.LlmConfigService;
import com.autosoft.agent.vo.LlmConfigVO;
import com.autosoft.agent.vo.LlmModelVO;
import com.autosoft.common.core.R;
import com.autosoft.framework.security.RequiresPermission;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 模型设置。仅超管。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
@RestController
@RequestMapping("/api/system")
public class LlmConfigController {

    private final LlmConfigService llmConfigService;

    public LlmConfigController(LlmConfigService llmConfigService) {
        this.llmConfigService = llmConfigService;
    }

    @GetMapping("/llm-config")
    @RequiresPermission("system:llm:manage")
    public R<LlmConfigVO> get() {
        return R.ok(llmConfigService.get());
    }

    @PutMapping("/llm-config")
    @RequiresPermission("system:llm:manage")
    public R<Void> save(@RequestBody LlmConfigSaveDTO dto) {
        llmConfigService.save(dto);
        return R.ok();
    }

    @GetMapping("/llm-models")
    @RequiresPermission("system:llm:manage")
    public R<List<LlmModelVO>> models() {
        return R.ok(llmConfigService.listModelVos());
    }
}
