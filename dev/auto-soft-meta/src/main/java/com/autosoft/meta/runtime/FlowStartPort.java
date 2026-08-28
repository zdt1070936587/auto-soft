package com.autosoft.meta.runtime;

import java.util.List;

/**
 * 由 flow 模块实现。workflow 只依赖本接口，避免与 flow 循环依赖。
 */
public interface FlowStartPort {

    Long startSimple(String app, String entity, Long bizId, List<String> roleCodes);
}
