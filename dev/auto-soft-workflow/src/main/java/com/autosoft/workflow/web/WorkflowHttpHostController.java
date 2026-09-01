package com.autosoft.workflow.web;

import com.autosoft.common.core.R;
import com.autosoft.framework.security.RequiresPermission;
import com.autosoft.workflow.dto.WorkflowHttpHostCreateDTO;
import com.autosoft.workflow.http.WorkflowHttpHostService;
import com.autosoft.workflow.vo.WorkflowHttpHostVO;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 工作流 HTTP 出站域名白名单。仅超管。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
@Validated
@RestController
@RequestMapping("/api/system/workflow-http-hosts")
public class WorkflowHttpHostController {

    private final WorkflowHttpHostService hostService;

    public WorkflowHttpHostController(WorkflowHttpHostService hostService) {
        this.hostService = hostService;
    }

    @GetMapping
    @RequiresPermission("system:workflow:http:manage")
    public R<List<WorkflowHttpHostVO>> list() {
        return R.ok(hostService.listAll());
    }

    @PostMapping
    @RequiresPermission("system:workflow:http:manage")
    public R<Long> create(@Valid @RequestBody WorkflowHttpHostCreateDTO dto) {
        return R.ok(hostService.create(dto));
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("system:workflow:http:manage")
    public R<Void> delete(@PathVariable Long id) {
        hostService.delete(id);
        return R.ok();
    }
}
