package com.autosoft.agent.assistant.memory;

import com.autosoft.agent.entity.AiMemoryEpisodeDO;
import com.autosoft.agent.llm.LlmTurn;
import com.autosoft.agent.llm.OpenCodeGoManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class FactExtractionService {

    private static final Logger log = LoggerFactory.getLogger(FactExtractionService.class);

    private final OpenCodeGoManager openCodeGoManager;
    private final JsonMapper jsonMapper;

    public FactExtractionService(OpenCodeGoManager openCodeGoManager, JsonMapper jsonMapper) {
        this.openCodeGoManager = openCodeGoManager;
        this.jsonMapper = jsonMapper;
    }

    public List<ExtractedFact> extractFromEpisode(AiMemoryEpisodeDO episode) {
        String content = episode.getContentSummary();
        if (content == null || content.isBlank()) {
            return List.of();
        }
        String prompt = """
                从以下对话/操作摘要中提取用户画像 fact。只输出 JSON 数组，不要 markdown。
                每项格式：{"category":"PROFILE|PREFERENCE|PROJECT","fact_key":"name|role|team|preference_*","fact_value":"...","confidence":0.0~1.0}
                没有可提取项则输出 []。
                摘要：
                """ + content;
        try {
            LlmTurn turn = openCodeGoManager.chat(
                    List.of(Map.of("role", "user", "content", prompt)),
                    List.of());
            String text = turn.getContent() == null ? "[]" : turn.getContent().trim();
            int start = text.indexOf('[');
            int end = text.lastIndexOf(']');
            if (start >= 0 && end > start) {
                text = text.substring(start, end + 1);
            }
            List<Map<String, Object>> rows = jsonMapper.readValue(text, new TypeReference<List<Map<String, Object>>>() {
            });
            List<ExtractedFact> facts = new ArrayList<>();
            for (Map<String, Object> row : rows) {
                ExtractedFact fact = toFact(row);
                if (fact != null && fact.confidence() >= 0.7f) {
                    facts.add(fact);
                }
            }
            return facts;
        } catch (RuntimeException ex) {
            log.warn("fact extraction failed, episodeId={}", episode.getId());
            return List.of();
        }
    }

    public String summarizeForDecay(String contentFull) {
        if (contentFull == null || contentFull.isBlank()) {
            return "";
        }
        String prompt = "将以下对话压缩为不超过300字的中文摘要，保留关键信息：\n" + contentFull;
        try {
            LlmTurn turn = openCodeGoManager.chat(
                    List.of(Map.of("role", "user", "content", prompt)),
                    List.of());
            String text = turn.getContent();
            if (text == null || text.isBlank()) {
                return contentFull.length() > 300 ? contentFull.substring(0, 300) : contentFull;
            }
            return text.length() > 500 ? text.substring(0, 500) : text.trim();
        } catch (RuntimeException ex) {
            log.warn("episode summarize failed");
            return contentFull.length() > 300 ? contentFull.substring(0, 300) : contentFull;
        }
    }

    private ExtractedFact toFact(Map<String, Object> row) {
        Object category = row.get("category");
        Object key = row.get("fact_key");
        Object value = row.get("fact_value");
        if (category == null || key == null || value == null) {
            return null;
        }
        float confidence = 0.8f;
        Object conf = row.get("confidence");
        if (conf instanceof Number number) {
            confidence = number.floatValue();
        }
        try {
            MemoryService.validateFactKey(String.valueOf(key));
        } catch (RuntimeException ex) {
            return null;
        }
        return new ExtractedFact(String.valueOf(category).toUpperCase(),
                String.valueOf(key), String.valueOf(value), confidence);
    }

    public record ExtractedFact(String category, String factKey, String factValue, float confidence) {
    }
}
