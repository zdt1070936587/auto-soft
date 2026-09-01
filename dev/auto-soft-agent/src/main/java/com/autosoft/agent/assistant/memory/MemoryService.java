package com.autosoft.agent.assistant.memory;

import com.autosoft.agent.assistant.config.AssistantMemoryProperties;
import com.autosoft.agent.entity.AiMemoryFactDO;
import com.autosoft.agent.mapper.MemoryEpisodeMapper;
import com.autosoft.agent.mapper.MemoryFactMapper;
import com.autosoft.common.core.ResultCode;
import com.autosoft.common.exception.BizException;
import com.autosoft.common.utils.AssertUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Memory业务服务。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
@Service
public class MemoryService {

    private static final Logger log = LoggerFactory.getLogger(MemoryService.class);
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            .withZone(ZoneId.of("Asia/Shanghai"));

    private static final Set<String> ALLOWED_FACT_KEYS = Set.of("name", "role", "team");
    private static final Set<String> PROFILE_KEYWORDS = Set.of("我叫", "我是", "负责", "我的名");

    private final MemoryEpisodeMapper episodeMapper;
    private final MemoryFactMapper factMapper;
    private final EmbeddingService embeddingService;
    private final AssistantMemoryProperties properties;
    private final JsonMapper jsonMapper;

    public MemoryService(MemoryEpisodeMapper episodeMapper,
                         MemoryFactMapper factMapper,
                         EmbeddingService embeddingService,
                         AssistantMemoryProperties properties,
                         JsonMapper jsonMapper) {
        this.episodeMapper = episodeMapper;
        this.factMapper = factMapper;
        this.embeddingService = embeddingService;
        this.properties = properties;
        this.jsonMapper = jsonMapper;
    }

    public boolean isEnabled() {
        return properties.isEnabled();
    }

    public String buildContext(Long userId, String userMessage) {
        if (!properties.isEnabled()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        appendProfileFacts(sb, userId);
        appendRelatedEpisodes(sb, userId, userMessage);
        String text = sb.toString().trim();
        if (text.length() > properties.getInjectMaxChars()) {
            return text.substring(0, properties.getInjectMaxChars()) + "…";
        }
        return text;
    }

    public void captureChatEpisodeAsync(Long userId, Long sessionId, String userText, String assistantText) {
        if (!properties.isEnabled()) {
            return;
        }
        Thread.startVirtualThread(() -> {
            try {
                captureChatEpisode(userId, sessionId, userText, assistantText);
            } catch (RuntimeException ex) {
                log.warn("capture episode failed, userId={}", userId, ex);
            }
        });
    }

    public void captureChatEpisode(Long userId, Long sessionId, String userText, String assistantText) {
        if (!properties.isEnabled()) {
            return;
        }
        String full = ("用户: " + nz(userText) + "\n助手: " + nz(assistantText)).trim();
        if (full.length() < 4) {
            return;
        }
        int importance = looksProfileRelated(userText) ? 8 : properties.getEpisodeImportanceDefault();
        String embedText = full.length() > 2000 ? full.substring(0, 2000) : full;
        Optional<String> vector = embeddingService.embedToPgVector(embedText);
        episodeMapper.insertEpisode(userId, sessionId, "CHAT", full, full, importance,
                vector.orElse(null), Instant.now(), 0);
    }

    public void captureOperClusterEpisode(Long userId, String summary) {
        if (!properties.isEnabled() || summary == null || summary.isBlank()) {
            return;
        }
        Optional<String> vector = embeddingService.embedToPgVector(summary);
        episodeMapper.insertEpisode(userId, null, "OPER_CLUSTER", null, summary, 6,
                vector.orElse(null), Instant.now(), 0);
    }

    public Map<String, Object> recall(Long userId, String query, int topK) {
        int k = topK <= 0 ? properties.getRecallTopK() : Math.min(topK, 10);
        List<FactSearchHit> facts;
        List<EpisodeSearchHit> episodes;
        Optional<String> vector = embeddingService.embedToPgVector(query);
        if (vector.isPresent()) {
            facts = factMapper.searchSimilar(userId, vector.get(), Math.min(k, 5));
            episodes = episodeMapper.searchSimilar(userId, vector.get(), k);
        } else {
            facts = factMapper.listProfileFacts(userId);
            episodes = episodeMapper.listRecent(userId, k);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("facts", facts);
        result.put("episodes", episodes);
        return result;
    }

    public void upsertFact(Long userId, String category, String factKey, String factValue,
                           float confidence, boolean confirmed, Long sourceEpisodeId) {
        validateFactKey(factKey);
        AssertUtils.notBlank(category, "category 不能为空");
        AssertUtils.notBlank(factValue, "fact_value 不能为空");
        AiMemoryFactDO existing = factMapper.findActive(userId, category, factKey);
        if (existing != null && existing.getConfirmed() != null && existing.getConfirmed() == 1
                && confidence < existing.getConfidence()) {
            return;
        }
        int confirmedFlag = confirmed ? 1 : 0;
        Optional<String> vector = embeddingService.embedToPgVector(factValue);
        factMapper.upsertFact(userId, category, factKey, factValue, confidence, confirmedFlag,
                sourceEpisodeId, vector.orElse(null));
    }

    public List<FactSearchHit> listFacts(Long userId) {
        return factMapper.listByUser(userId);
    }

    public List<EpisodeSearchHit> listEpisodes(Long userId, int limit) {
        return episodeMapper.listByUser(userId, Math.min(limit, 50));
    }

    public void deleteFact(Long userId, Long factId) {
        int rows = factMapper.softDelete(userId, factId);
        if (rows == 0) {
            throw new BizException(ResultCode.NOT_FOUND, "记忆不存在");
        }
    }

    public void confirmFact(Long userId, Long factId) {
        int rows = factMapper.confirm(userId, factId);
        if (rows == 0) {
            throw new BizException(ResultCode.NOT_FOUND, "记忆不存在");
        }
    }

    public String recallJson(Long userId, String query, int topK) {
        return jsonMapper.writeValueAsString(recall(userId, query, topK));
    }

    private void appendProfileFacts(StringBuilder sb, Long userId) {
        List<FactSearchHit> facts = factMapper.listProfileFacts(userId);
        if (facts.isEmpty()) {
            return;
        }
        sb.append("[用户画像]\n");
        for (FactSearchHit fact : facts) {
            String label = factKeyLabel(fact.getFactKey());
            String suffix = fact.getConfirmed() != null && fact.getConfirmed() == 1 ? " (已确认)" : " (待确认)";
            sb.append("- ").append(label).append(": ").append(fact.getFactValue()).append(suffix).append('\n');
        }
    }

    private void appendRelatedEpisodes(StringBuilder sb, Long userId, String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return;
        }
        List<EpisodeSearchHit> episodes;
        Optional<String> vector = embeddingService.embedToPgVector(userMessage);
        if (vector.isPresent()) {
            episodes = episodeMapper.searchSimilar(userId, vector.get(), properties.getRecallTopK());
        } else {
            episodes = episodeMapper.listRecent(userId, properties.getRecallTopK());
        }
        if (episodes.isEmpty()) {
            return;
        }
        sb.append("[相关记忆]\n");
        for (EpisodeSearchHit ep : episodes) {
            String day = ep.getOccurredAt() == null ? "" : DAY_FMT.format(ep.getOccurredAt());
            sb.append("- ").append(day).append(": ").append(ep.getContentSummary()).append('\n');
        }
    }

    private static boolean looksProfileRelated(String text) {
        if (text == null) {
            return false;
        }
        for (String kw : PROFILE_KEYWORDS) {
            if (text.contains(kw)) {
                return true;
            }
        }
        return false;
    }

    public static void validateFactKey(String factKey) {
        AssertUtils.notBlank(factKey, "fact_key 不能为空");
        if (ALLOWED_FACT_KEYS.contains(factKey) || factKey.startsWith("preference_")) {
            return;
        }
        throw new BizException(ResultCode.BAD_REQUEST, "不支持的 fact_key: " + factKey);
    }

    private static String factKeyLabel(String key) {
        return switch (key) {
            case "name" -> "姓名";
            case "role" -> "职责";
            case "team" -> "团队";
            default -> key;
        };
    }

    private static String nz(String value) {
        return value == null ? "" : value;
    }
}
