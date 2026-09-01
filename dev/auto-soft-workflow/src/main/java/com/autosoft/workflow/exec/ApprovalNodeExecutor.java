package com.autosoft.workflow.exec;

import com.autosoft.common.utils.AssertUtils;
import com.autosoft.meta.runtime.FlowStartPort;
import com.autosoft.workflow.graph.NodeTypes;
import com.autosoft.workflow.graph.WfNode;
import com.autosoft.workflow.graph.WorkflowGraphValidator;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ApprovalNode执行器。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
@Component
public class ApprovalNodeExecutor implements NodeExecutor {

    private final FlowStartPort flowStartPort;

    public ApprovalNodeExecutor(FlowStartPort flowStartPort) {
        this.flowStartPort = flowStartPort;
    }

    @Override
    public String type() {
        return NodeTypes.APPROVAL;
    }

    @Override
    public Object execute(WfNode node, Map<String, Object> renderedConfig, RunContext context) {
        String app = str(renderedConfig.get("app"));
        String entity = str(renderedConfig.get("entity"));
        String idRaw = str(renderedConfig.get("id"));
        AssertUtils.notBlank(idRaw, "approval 缺少 id");
        Long bizId;
        try {
            bizId = Long.parseLong(idRaw.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("approval id 不是数字");
        }
        List<String> roles = WorkflowGraphValidator.roleCodes(renderedConfig.get("role_codes"));
        if (context.dryRun()) {
            Map<String, Object> skipped = new LinkedHashMap<>();
            skipped.put("skipped", true);
            skipped.put("dry_run_assumed_approved", true);
            skipped.put("bizId", bizId);
            return skipped;
        }
        Long instanceId = flowStartPort.startSimple(app, entity, bizId, roles);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("flowInstanceId", instanceId);
        output.put("bizId", bizId);
        output.put("app", app);
        output.put("entity", entity);
        output.put("status", "paused");
        context.markPause(app, entity, bizId, instanceId);
        throw new WorkflowPausedException(node.getId());
    }

    private static String str(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
