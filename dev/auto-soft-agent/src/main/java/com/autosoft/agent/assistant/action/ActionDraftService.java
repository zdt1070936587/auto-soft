package com.autosoft.agent.assistant.action;

import com.autosoft.agent.assistant.action.model.CapabilityDefinition;
import com.autosoft.agent.assistant.action.model.FieldValidationResult;
import com.autosoft.agent.assistant.action.vo.ActionDraftVO;
import com.autosoft.agent.assistant.config.AssistantActionProperties;
import com.autosoft.agent.entity.ActionDraftDO;
import com.autosoft.agent.mapper.ActionDraftMapper;
import com.autosoft.common.core.ResultCode;
import com.autosoft.common.exception.BizException;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 操作草稿持久化与状态流转。
 *
 * @author zhaodt
 * @since 2026-09-02
 */
@Service
public class ActionDraftService {

    private final ActionDraftMapper draftMapper;
    private final CapabilityDiscoveryService discoveryService;
    private final ActionFieldValidator fieldValidator;
    private final AssistantActionProperties properties;
    private final JsonMapper jsonMapper;

    public ActionDraftService(ActionDraftMapper draftMapper,
                              CapabilityDiscoveryService discoveryService,
                              ActionFieldValidator fieldValidator,
                              AssistantActionProperties properties,
                              JsonMapper jsonMapper) {
        this.draftMapper = draftMapper;
        this.discoveryService = discoveryService;
        this.fieldValidator = fieldValidator;
        this.properties = properties;
        this.jsonMapper = jsonMapper;
    }

    public PrepareResult createOrUpdate(Long sessionId, Long userId, String capabilityId,
                                        Map<String, Object> fieldValues, String draftId) {
        CapabilityDefinition capability = discoveryService.require(capabilityId);
        ActionDraftDO existing = null;
        Map<String, Object> mergedValues = new LinkedHashMap<>();
        if (draftId != null && !draftId.isBlank()) {
            existing = requireOwnedDraft(draftId, userId);
            touchExpiry(existing);
            mergedValues.putAll(readMap(existing.getValuesJson()));
        }
        if (fieldValues != null) {
            mergedValues.putAll(fieldValues);
        }
        FieldValidationResult validation = fieldValidator.validate(capability, mergedValues);
        Instant now = Instant.now();
        ActionDraftDO row = existing == null ? new ActionDraftDO() : existing;
        if (existing == null) {
            row.setId(UUID.randomUUID().toString());
            row.setSessionId(sessionId);
            row.setUserId(userId);
            row.setCreatedAt(now);
        }
        row.setCapabilityId(capabilityId);
        row.setStatus(validation.isReady() ? "ready" : "draft");
        row.setTargetPath(capability.getPath());
        row.setTargetType(capability.getTargetType());
        row.setModalKey(capability.getModalKey());
        row.setValuesJson(jsonMapper.writeValueAsString(validation.getValues()));
        row.setDisplayJson(jsonMapper.writeValueAsString(validation.getDisplayValues()));
        row.setMissingJson(jsonMapper.writeValueAsString(validation.getMissing()));
        row.setUnknownJson(jsonMapper.writeValueAsString(validation.getUnknown()));
        row.setExpiresAt(now.plusSeconds(properties.getActionDraftTtlMinutes() * 60L));
        row.setUpdatedAt(now);
        if (existing == null) {
            draftMapper.insert(row);
        } else {
            draftMapper.updateById(row);
        }
        ActionDraftVO vo = toVo(row, capability);
        String message = buildMessage(validation, capability);
        return new PrepareResult(vo, message);
    }

    public ActionDraftVO get(String draftId, Long userId) {
        ActionDraftDO row = requireOwnedDraft(draftId, userId);
        expireIfNeeded(row);
        CapabilityDefinition capability = discoveryService.load(row.getCapabilityId());
        return toVo(row, capability);
    }

    public void consume(String draftId, Long userId) {
        ActionDraftDO row = requireOwnedDraft(draftId, userId);
        expireIfNeeded(row);
        if ("expired".equals(row.getStatus())) {
            throw new BizException(ResultCode.BAD_REQUEST, "操作计划已过期，请重新描述需求");
        }
        if ("cancelled".equals(row.getStatus())) {
            throw new BizException(ResultCode.BAD_REQUEST, "操作计划已取消");
        }
        if (!"ready".equals(row.getStatus())) {
            throw new BizException(ResultCode.BAD_REQUEST, "还有必填信息未填写");
        }
        row.setStatus("consumed");
        row.setConsumedAt(Instant.now());
        row.setUpdatedAt(Instant.now());
        draftMapper.updateById(row);
    }

    public void cancel(String draftId, Long userId) {
        ActionDraftDO row = requireOwnedDraft(draftId, userId);
        if ("consumed".equals(row.getStatus())) {
            throw new BizException(ResultCode.BAD_REQUEST, "操作计划已消费");
        }
        row.setStatus("cancelled");
        row.setUpdatedAt(Instant.now());
        draftMapper.updateById(row);
    }

    private ActionDraftDO requireOwnedDraft(String draftId, Long userId) {
        ActionDraftDO row = draftMapper.selectById(draftId);
        if (row == null) {
            throw new BizException(ResultCode.NOT_FOUND, "操作计划不存在");
        }
        if (!userId.equals(row.getUserId())) {
            throw new BizException(ResultCode.FORBIDDEN, "不能访问他人操作计划");
        }
        return row;
    }

    private void expireIfNeeded(ActionDraftDO row) {
        if (row.getExpiresAt() != null && Instant.now().isAfter(row.getExpiresAt())) {
            if ("draft".equals(row.getStatus()) || "ready".equals(row.getStatus())) {
                row.setStatus("expired");
                row.setUpdatedAt(Instant.now());
                draftMapper.updateById(row);
            }
        }
    }

    private void touchExpiry(ActionDraftDO row) {
        if ("expired".equals(row.getStatus())) {
            throw new BizException(ResultCode.BAD_REQUEST, "操作计划已过期，请重新描述需求");
        }
        if ("cancelled".equals(row.getStatus()) || "consumed".equals(row.getStatus())) {
            throw new BizException(ResultCode.BAD_REQUEST, "操作计划不可用");
        }
    }

    private ActionDraftVO toVo(ActionDraftDO row, CapabilityDefinition capability) {
        ActionDraftVO vo = new ActionDraftVO();
        vo.setDraftId(row.getId());
        vo.setSessionId(row.getSessionId());
        vo.setCapabilityId(row.getCapabilityId());
        vo.setStatus(row.getStatus());
        vo.setLabel(capability == null ? row.getCapabilityId() : capability.getLabel());
        vo.setTargetPath(row.getTargetPath());
        vo.setTargetType(row.getTargetType());
        vo.setModalKey(row.getModalKey());
        vo.setPermission(capability == null ? null : capability.getPermission());
        vo.setValues(readMap(row.getValuesJson()));
        vo.setDisplayValues(readMap(row.getDisplayJson()));
        vo.setMissing(readList(row.getMissingJson()));
        vo.setUnknown(readList(row.getUnknownJson()));
        vo.setExpiresAt(row.getExpiresAt());
        return vo;
    }

    private Map<String, Object> readMap(String json) {
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        return jsonMapper.readValue(json, new TypeReference<Map<String, Object>>() {
        });
    }

    private List<String> readList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        return jsonMapper.readValue(json, new TypeReference<List<String>>() {
        });
    }

    private String buildMessage(FieldValidationResult validation, CapabilityDefinition capability) {
        if (validation.isReady()) {
            return "操作计划已就绪，请确认并前往目标页面。";
        }
        StringBuilder sb = new StringBuilder();
        if (!validation.getMissing().isEmpty()) {
            sb.append("还缺少：");
            sb.append(String.join("、", labels(capability, validation.getMissing())));
            sb.append("。");
        }
        if (!validation.getUnknown().isEmpty()) {
            if (!sb.isEmpty()) {
                sb.append(" ");
            }
            sb.append(capability.getLabel()).append("不支持字段：");
            sb.append(String.join("、", validation.getUnknown()));
            sb.append("。");
        }
        if (!validation.getFieldErrors().isEmpty()) {
            if (!sb.isEmpty()) {
                sb.append(" ");
            }
            sb.append(String.join("；", validation.getFieldErrors().values()));
        }
        return sb.isEmpty() ? "请补充必填信息。" : sb.toString();
    }

    private static List<String> labels(CapabilityDefinition capability, List<String> keys) {
        return keys.stream().map(key -> {
            for (var field : capability.getFields()) {
                if (key.equals(field.getKey())) {
                    return field.getLabel();
                }
            }
            return key;
        }).toList();
    }

    public record PrepareResult(ActionDraftVO draft, String message) {
    }
}
