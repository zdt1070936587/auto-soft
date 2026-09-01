package com.autosoft.agent.assistant;

import com.autosoft.agent.assistant.memory.EpisodeSearchHit;
import com.autosoft.agent.assistant.memory.MemoryService;
import com.autosoft.agent.assistant.vo.AiMemoryFactVO;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AssistantMemory业务服务。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
@Service
public class AssistantMemoryService {

    private final MemoryService memoryService;

    public AssistantMemoryService(MemoryService memoryService) {
        this.memoryService = memoryService;
    }

    public List<AiMemoryFactVO> listFacts(Long userId) {
        return memoryService.listFacts(userId).stream().map(this::toVo).toList();
    }

    public List<EpisodeSearchHit> listEpisodes(Long userId, int limit) {
        return memoryService.listEpisodes(userId, limit);
    }

    public void deleteFact(Long userId, Long factId) {
        memoryService.deleteFact(userId, factId);
    }

    public void confirmFact(Long userId, Long factId) {
        memoryService.confirmFact(userId, factId);
    }

    private AiMemoryFactVO toVo(com.autosoft.agent.assistant.memory.FactSearchHit hit) {
        AiMemoryFactVO vo = new AiMemoryFactVO();
        vo.setId(hit.getId());
        vo.setCategory(hit.getCategory());
        vo.setFactKey(hit.getFactKey());
        vo.setFactValue(hit.getFactValue());
        vo.setConfidence(hit.getConfidence());
        vo.setConfirmed(hit.getConfirmed());
        vo.setLastUsedAt(hit.getLastUsedAt());
        return vo;
    }
}
