package com.autosoft.workflow.exec;

import com.autosoft.meta.runtime.RuntimeService;
import com.autosoft.workflow.graph.NodeTypes;
import com.autosoft.workflow.graph.WfNode;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * MetaUpsertNode执行器。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
@Component
public class MetaUpsertNodeExecutor implements NodeExecutor {

    private final RuntimeService runtimeService;

    public MetaUpsertNodeExecutor(RuntimeService runtimeService) {
        this.runtimeService = runtimeService;
    }

    @Override
    public String type() {
        return NodeTypes.META_UPSERT;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object execute(WfNode node, Map<String, Object> renderedConfig, RunContext context) {
        String app = str(renderedConfig.get("app"));
        String entity = str(renderedConfig.get("entity"));
        Long id = parseId(renderedConfig.get("id"));
        Map<String, Object> fields = new LinkedHashMap<>();
        Object raw = renderedConfig.get("fields");
        if (raw instanceof Map<?, ?> map) {
            map.forEach((k, v) -> fields.put(String.valueOf(k), v));
        }
        return runtimeService.upsertPublishedRow(app, entity, id, fields);
    }

    private static Long parseId(Object raw) {
        if (raw == null || String.valueOf(raw).isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(String.valueOf(raw).trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("meta.upsert id 不是数字");
        }
    }

    private static String str(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
