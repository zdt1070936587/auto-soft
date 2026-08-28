package com.autosoft.workflow.web;

import com.autosoft.common.core.R;
import com.autosoft.framework.security.RequiresPermission;
import com.autosoft.workflow.def.WorkflowDefinitionService;
import com.autosoft.workflow.dto.WorkflowCreateDTO;
import com.autosoft.workflow.dto.WorkflowGraphDTO;
import com.autosoft.workflow.dto.WorkflowPublishDTO;
import com.autosoft.workflow.dto.WorkflowRunDTO;
import com.autosoft.workflow.dto.WorkflowScheduleDTO;
import com.autosoft.workflow.dto.WorkflowShareDTO;
import com.autosoft.workflow.exec.WorkflowExecutor;
import com.autosoft.workflow.schedule.WorkflowScheduleService;
import com.autosoft.workflow.share.WorkflowShareService;
import com.autosoft.workflow.vo.WorkflowDefinitionVO;
import com.autosoft.workflow.vo.WorkflowRunVO;
import com.autosoft.workflow.vo.WorkflowShareVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 工作流定义与运行入口。无执行器细节。
 */
@RestController
@RequestMapping("/api/wf")
public class WorkflowController {

    private final WorkflowDefinitionService definitionService;
    private final WorkflowExecutor executor;
    private final WorkflowShareService shareService;
    private final WorkflowScheduleService scheduleService;

    public WorkflowController(WorkflowDefinitionService definitionService, WorkflowExecutor executor,
                              WorkflowShareService shareService, WorkflowScheduleService scheduleService) {
        this.definitionService = definitionService;
        this.executor = executor;
        this.shareService = shareService;
        this.scheduleService = scheduleService;
    }

    @PostMapping
    @RequiresPermission("studio:use")
    public R<Long> create(@Valid @RequestBody WorkflowCreateDTO dto) {
        return R.ok(definitionService.create(dto));
    }

    @GetMapping("/share/{token}")
    public R<WorkflowShareVO> sharePreview(@PathVariable String token) {
        return R.ok(shareService.preview(token));
    }

    @PostMapping("/share/{token}/copy")
    public R<Long> shareCopy(@PathVariable String token) {
        return R.ok(shareService.copy(token));
    }

    @PostMapping("/{id}/share")
    @RequiresPermission("studio:use")
    public R<WorkflowShareVO> share(@PathVariable Long id, @RequestBody(required = false) WorkflowShareDTO dto) {
        return R.ok(shareService.create(id, dto == null ? new WorkflowShareDTO() : dto));
    }

    @PutMapping("/{id}/schedule")
    @RequiresPermission("studio:use")
    public R<Void> schedule(@PathVariable Long id, @RequestBody WorkflowScheduleDTO dto) {
        scheduleService.setEnabled(id, dto.isEnabled());
        return R.ok();
    }

    @GetMapping("/{id}")
    @RequiresPermission("studio:use")
    public R<WorkflowDefinitionVO> get(@PathVariable Long id) {
        return R.ok(definitionService.get(id));
    }

    @GetMapping
    @RequiresPermission("studio:use")
    public R<WorkflowDefinitionVO> getByApp(@RequestParam Long appId) {
        return R.ok(definitionService.getByAppId(appId));
    }

    @PutMapping("/{id}/graph")
    @RequiresPermission("studio:use")
    public R<Void> saveGraph(@PathVariable Long id, @Valid @RequestBody WorkflowGraphDTO dto) {
        definitionService.saveGraph(id, dto.getGraph());
        return R.ok();
    }

    @PostMapping("/{id}/validate")
    @RequiresPermission("studio:use")
    public R<Void> validate(@PathVariable Long id) {
        definitionService.validate(id);
        return R.ok();
    }

    @PostMapping("/{id}/dry-run")
    @RequiresPermission("studio:use")
    public R<WorkflowRunVO> dryRun(@PathVariable Long id, @RequestBody(required = false) WorkflowRunDTO dto) {
        return R.ok(executor.dryRun(id, dto == null ? null : dto.getInput()));
    }

    @PostMapping("/{id}/publish")
    @RequiresPermission("studio:use")
    public R<Void> publish(@PathVariable Long id, @RequestBody WorkflowPublishDTO dto) {
        definitionService.publish(id, dto);
        return R.ok();
    }

    @GetMapping("/by-code/{code}")
    public R<WorkflowDefinitionVO> published(@PathVariable String code) {
        return R.ok(definitionService.getPublishedByCode(code));
    }

    @PostMapping("/{code}/run")
    public R<WorkflowRunVO> run(@PathVariable String code, @RequestBody(required = false) WorkflowRunDTO dto) {
        return R.ok(executor.runPublished(code, dto == null ? null : dto.getInput()));
    }

    @GetMapping("/runs/{runId}")
    public R<WorkflowRunVO> runDetail(@PathVariable Long runId) {
        return R.ok(executor.getRun(runId));
    }
}
