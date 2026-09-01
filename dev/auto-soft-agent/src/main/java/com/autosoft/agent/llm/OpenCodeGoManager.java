package com.autosoft.agent.llm;

import com.autosoft.agent.crypto.AesGcmCipher;
import com.autosoft.agent.entity.SysLlmConfigDO;
import com.autosoft.agent.mapper.SysLlmConfigMapper;
import com.autosoft.common.core.ResultCode;
import com.autosoft.common.exception.BizException;
import com.autosoft.common.utils.AssertUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * OpenCode Go 入口。禁止打印 API Key。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
@Component
public class OpenCodeGoManager {

    private static final Logger log = LoggerFactory.getLogger(OpenCodeGoManager.class);
    public static final String FALLBACK_MODEL = "kimi-k2.7-code";

    private final ModelProtocolRouter router;
    private final ChatCompletionsClient chatCompletionsClient;
    private final SysLlmConfigMapper configMapper;
    private final AesGcmCipher aesGcmCipher;

    public OpenCodeGoManager(ModelProtocolRouter router, ChatCompletionsClient chatCompletionsClient,
                             SysLlmConfigMapper configMapper, AesGcmCipher aesGcmCipher) {
        this.router = router;
        this.chatCompletionsClient = chatCompletionsClient;
        this.configMapper = configMapper;
        this.aesGcmCipher = aesGcmCipher;
    }

    public LlmTurn chat(List<Map<String, Object>> messages, List<Map<String, Object>> tools) {
        SysLlmConfigDO config = requireConfig();
        String apiKey = decryptKey(config);
        String modelId = config.getDefaultModel() == null || config.getDefaultModel().isBlank()
                ? FALLBACK_MODEL : config.getDefaultModel();
        log.info("opencode chat model={}", modelId);
        return router.chat(messages, tools, modelId, apiKey);
    }

    public List<Map<String, Object>> listModels() {
        SysLlmConfigDO config = requireConfig();
        String apiKey = decryptKey(config);
        return chatCompletionsClient.listModels(apiKey);
    }

    public String decryptKey(SysLlmConfigDO config) {
        if (config.getApiKeyCipher() == null || config.getApiKeyCipher().isBlank()) {
            throw new BizException(ResultCode.BAD_REQUEST, "请先在模型设置中配置 API Key");
        }
        return aesGcmCipher.decrypt(config.getApiKeyCipher(), config.getApiKeyIv());
    }

    public SysLlmConfigDO requireConfig() {
        List<SysLlmConfigDO> rows = configMapper.selectList(null);
        AssertUtils.isTrue(rows != null && !rows.isEmpty(), "模型配置不存在");
        return rows.get(0);
    }
}
