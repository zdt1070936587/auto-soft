package com.autosoft.workflow.exec;

import com.autosoft.common.utils.AssertUtils;
import com.autosoft.workflow.graph.NodeTypes;
import com.autosoft.workflow.graph.WfNode;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * NotifyNode执行器。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
@Component
public class NotifyNodeExecutor implements NodeExecutor {

    private final WorkflowNotifyPort notifyPort;

    public NotifyNodeExecutor(WorkflowNotifyPort notifyPort) {
        this.notifyPort = notifyPort;
    }

    @Override
    public String type() {
        return NodeTypes.NOTIFY;
    }

    @Override
    public Object execute(WfNode node, Map<String, Object> renderedConfig, RunContext context) {
        String toRole = str(renderedConfig.get("toRole"));
        AssertUtils.notBlank(toRole, "notify 缺少 toRole");
        String title = str(renderedConfig.get("title"));
        if (title == null || title.isBlank()) {
            title = node.getTitle() == null ? "工作流通知" : node.getTitle();
        }
        String body = str(renderedConfig.get("body"));
        notifyPort.send(toRole, title, body, context.runId());
        return Map.of("toRole", toRole, "sent", true);
    }

    private static String str(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
