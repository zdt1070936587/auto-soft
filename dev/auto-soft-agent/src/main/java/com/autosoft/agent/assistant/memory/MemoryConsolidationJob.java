package com.autosoft.agent.assistant.memory;

import com.autosoft.agent.assistant.config.AssistantMemoryProperties;
import com.autosoft.agent.entity.AiMemoryEpisodeDO;
import com.autosoft.agent.mapper.MemoryEpisodeMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Component
public class MemoryConsolidationJob {

    private static final Logger log = LoggerFactory.getLogger(MemoryConsolidationJob.class);

    private final AssistantMemoryProperties properties;
    private final MemoryEpisodeMapper episodeMapper;
    private final MemoryService memoryService;
    private final FactExtractionService factExtractionService;
    private final EmbeddingService embeddingService;
    private final OperLogClusterService operLogClusterService;

    public MemoryConsolidationJob(AssistantMemoryProperties properties,
                                  MemoryEpisodeMapper episodeMapper,
                                  MemoryService memoryService,
                                  FactExtractionService factExtractionService,
                                  EmbeddingService embeddingService,
                                  OperLogClusterService operLogClusterService) {
        this.properties = properties;
        this.episodeMapper = episodeMapper;
        this.memoryService = memoryService;
        this.factExtractionService = factExtractionService;
        this.embeddingService = embeddingService;
        this.operLogClusterService = operLogClusterService;
    }

    @Scheduled(cron = "${autosoft.assistant.memory.consolidation.cron:0 0 2 * * ?}")
    public void run() {
        if (!properties.isEnabled() || !properties.getConsolidation().isEnabled()) {
            return;
        }
        log.info("assistant memory consolidation started");
        try {
            decayFullEpisodes();
            decayArchiveEpisodes();
            operLogClusterService.clusterYesterday(properties.getConsolidation().getOperClusterPaddingMinutes());
        } catch (RuntimeException ex) {
            log.error("assistant memory consolidation failed", ex);
        }
        log.info("assistant memory consolidation finished");
    }

    private void decayFullEpisodes() {
        Instant before = Instant.now().minus(properties.getConsolidation().getDecayFullDays(), ChronoUnit.DAYS);
        int limit = properties.getConsolidation().getBatchSize();
        List<AiMemoryEpisodeDO> rows = episodeMapper.listForDecayFull(before, limit);
        for (AiMemoryEpisodeDO row : rows) {
            try {
                String source = row.getContentFull() != null ? row.getContentFull() : row.getContentSummary();
                String summary = factExtractionService.summarizeForDecay(source);
                Optional<String> vector = embeddingService.embedToPgVector(summary);
                if (vector.isEmpty()) {
                    continue;
                }
                episodeMapper.updateDecayFull(row.getId(), summary, vector.get());
            } catch (RuntimeException ex) {
                log.warn("decay full failed, episodeId={}", row.getId());
            }
        }
    }

    private void decayArchiveEpisodes() {
        Instant before = Instant.now().minus(properties.getConsolidation().getDecaySummaryDays(), ChronoUnit.DAYS);
        int limit = properties.getConsolidation().getBatchSize();
        List<AiMemoryEpisodeDO> rows = episodeMapper.listForDecayArchive(before, 7, limit);
        for (AiMemoryEpisodeDO row : rows) {
            try {
                for (FactExtractionService.ExtractedFact fact : factExtractionService.extractFromEpisode(row)) {
                    memoryService.upsertFact(row.getUserId(), fact.category(), fact.factKey(), fact.factValue(),
                            fact.confidence(), false, row.getId());
                }
                episodeMapper.updateDecayArchive(row.getId());
            } catch (RuntimeException ex) {
                log.warn("decay archive failed, episodeId={}", row.getId());
            }
        }
    }
}
