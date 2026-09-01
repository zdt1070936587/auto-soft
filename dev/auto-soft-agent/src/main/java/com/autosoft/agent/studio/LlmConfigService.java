package com.autosoft.agent.studio;

import com.autosoft.agent.crypto.AesGcmCipher;
import com.autosoft.agent.dto.LlmConfigSaveDTO;
import com.autosoft.agent.entity.SysLlmConfigDO;
import com.autosoft.agent.llm.OpenCodeGoManager;
import com.autosoft.agent.mapper.SysLlmConfigMapper;
import com.autosoft.agent.vo.LlmConfigVO;
import com.autosoft.common.utils.AssertUtils;
import com.autosoft.framework.log.OperLog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统级 LLM 配置。Key 只写不回显。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
@Service
public class LlmConfigService {

    private final OpenCodeGoManager openCodeGoManager;
    private final SysLlmConfigMapper configMapper;
    private final AesGcmCipher aesGcmCipher;
    private volatile List<Map<String, Object>> modelCache = List.of();
    private volatile long modelCacheAt;

    public LlmConfigService(OpenCodeGoManager openCodeGoManager, SysLlmConfigMapper configMapper,
                            AesGcmCipher aesGcmCipher) {
        this.openCodeGoManager = openCodeGoManager;
        this.configMapper = configMapper;
        this.aesGcmCipher = aesGcmCipher;
    }

    public LlmConfigVO get() {
        SysLlmConfigDO row = openCodeGoManager.requireConfig();
        LlmConfigVO vo = new LlmConfigVO();
        vo.setDefaultModel(row.getDefaultModel());
        vo.setAllowedModelsJson(row.getAllowedModelsJson());
        boolean configured = row.getApiKeyCipher() != null && !row.getApiKeyCipher().isBlank();
        vo.setKeyConfigured(configured);
        vo.setKeyMask(configured ? "********" : "");
        return vo;
    }

    @OperLog(module = "STUDIO", action = "UPDATE")
    @Transactional(rollbackFor = Exception.class)
    public void save(LlmConfigSaveDTO dto) {
        SysLlmConfigDO row = openCodeGoManager.requireConfig();
        if (dto.getApiKey() != null && !dto.getApiKey().isBlank()) {
            AesGcmCipher.Encrypted encrypted = aesGcmCipher.encrypt(dto.getApiKey().trim());
            row.setApiKeyCipher(encrypted.cipherBase64());
            row.setApiKeyIv(encrypted.ivHex());
        }
        if (dto.getDefaultModel() != null && !dto.getDefaultModel().isBlank()) {
            row.setDefaultModel(dto.getDefaultModel().trim());
        }
        if (dto.getAllowedModelsJson() != null) {
            row.setAllowedModelsJson(dto.getAllowedModelsJson());
        }
        configMapper.updateById(row);
        modelCache = List.of();
        modelCacheAt = 0L;
    }

    public List<Map<String, Object>> listModels() {
        if (System.currentTimeMillis() - modelCacheAt < 3600_000L && !modelCache.isEmpty()) {
            return modelCache;
        }
        SysLlmConfigDO row = openCodeGoManager.requireConfig();
        AssertUtils.isTrue(row.getApiKeyCipher() != null && !row.getApiKeyCipher().isBlank(), "请先配置 API Key");
        List<Map<String, Object>> models = openCodeGoManager.listModels();
        List<String> allowed = parseAllowed(row.getAllowedModelsJson());
        List<Map<String, Object>> filtered = new ArrayList<>();
        for (Map<String, Object> model : models) {
            Object id = model.get("id");
            if (id == null) {
                continue;
            }
            if (!allowed.isEmpty() && !allowed.contains(String.valueOf(id))) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", id);
            item.put("object", model.get("object"));
            filtered.add(item);
        }
        modelCache = List.copyOf(filtered);
        modelCacheAt = System.currentTimeMillis();
        return modelCache;
    }

    public List<com.autosoft.agent.vo.LlmModelVO> listModelVos() {
        return listModels().stream()
                .map(item -> new com.autosoft.agent.vo.LlmModelVO(String.valueOf(item.get("id"))))
                .toList();
    }

    private List<String> parseAllowed(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (String part : json.replace("[", "").replace("]", "").replace("\"", "").split(",")) {
            if (!part.isBlank()) {
                result.add(part.trim());
            }
        }
        return result;
    }
}
