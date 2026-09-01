package com.autosoft.workflow.exec;

import com.autosoft.common.utils.AssertUtils;
import com.autosoft.workflow.graph.ConditionDsl;
import com.autosoft.workflow.graph.NodeTypes;
import com.autosoft.workflow.graph.WfNode;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * ConditionNode执行器。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
@Component
public class ConditionNodeExecutor implements NodeExecutor {

    @Override
    public String type() {
        return NodeTypes.CONDITION;
    }

    @Override
    public Object execute(WfNode node, Map<String, Object> renderedConfig, RunContext context) {
        String expr = str(renderedConfig.get("expr"));
        AssertUtils.notBlank(expr, "condition 缺少 expr");
        boolean result = ConditionDsl.evaluate(expr, context);
        return Map.of("result", result);
    }

    private static String str(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
