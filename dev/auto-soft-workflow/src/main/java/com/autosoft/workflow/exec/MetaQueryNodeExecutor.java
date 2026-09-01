package com.autosoft.workflow.exec;

import com.autosoft.common.utils.AssertUtils;
import com.autosoft.meta.runtime.RuntimeService;
import com.autosoft.workflow.graph.NodeTypes;
import com.autosoft.workflow.graph.WfNode;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * MetaQueryNode执行器。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
@Component
public class MetaQueryNodeExecutor implements NodeExecutor {

    private final RuntimeService runtimeService;

    public MetaQueryNodeExecutor(RuntimeService runtimeService) {
        this.runtimeService = runtimeService;
    }

    @Override
    public String type() {
        return NodeTypes.META_QUERY;
    }

    @Override
    public Object execute(WfNode node, Map<String, Object> renderedConfig, RunContext context) {
        String app = str(renderedConfig.get("app"));
        String entity = str(renderedConfig.get("entity"));
        String idRaw = str(renderedConfig.get("id"));
        AssertUtils.notBlank(idRaw, "meta.query 缺少 id");
        Long id;
        try {
            id = Long.parseLong(idRaw.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("meta.query id 不是数字");
        }
        return runtimeService.getPublishedRow(app, entity, id);
    }

    private static String str(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
