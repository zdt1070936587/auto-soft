package com.autosoft.meta.runtime;

public interface FlowBinder {
    Long createSimpleFlow(Long entityId, java.util.List<String> roleCodes);

    void bindFlow(Long entityId, Long definitionId);
}
