package com.autosoft.workflow.exec;

import com.autosoft.workflow.graph.NodeTypes;
import com.autosoft.workflow.graph.WfNode;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class EndNodeExecutor implements NodeExecutor {

    @Override
    public String type() {
        return NodeTypes.END;
    }

    @Override
    public Object execute(WfNode node, Map<String, Object> renderedConfig, RunContext context) {
        return Map.of("ok", true);
    }
}
