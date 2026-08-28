package com.autosoft.agent.mapper;

import com.autosoft.agent.assistant.memory.EpisodeSearchHit;
import com.autosoft.agent.entity.AiMemoryEpisodeDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.Instant;
import java.util.List;

public interface MemoryEpisodeMapper extends BaseMapper<AiMemoryEpisodeDO> {

    @Insert("""
            INSERT INTO ai_memory_episode
            (user_id, session_id, episode_type, content_full, content_summary, importance,
             embedding, occurred_at, decay_stage, created_by, updated_by, deleted)
            VALUES
            (#{userId}, #{sessionId}, #{episodeType}, #{contentFull}, #{contentSummary}, #{importance},
             CAST(#{embedding} AS vector), #{occurredAt}, #{decayStage}, #{userId}, #{userId}, 0)
            """)
    int insertEpisode(@Param("userId") Long userId,
                      @Param("sessionId") Long sessionId,
                      @Param("episodeType") String episodeType,
                      @Param("contentFull") String contentFull,
                      @Param("contentSummary") String contentSummary,
                      @Param("importance") int importance,
                      @Param("embedding") String embedding,
                      @Param("occurredAt") Instant occurredAt,
                      @Param("decayStage") int decayStage);

    @Select("""
            SELECT id, content_summary AS contentSummary, occurred_at AS occurredAt, importance,
                   1 - (embedding <=> CAST(#{queryVector} AS vector)) AS score
            FROM ai_memory_episode
            WHERE user_id = #{userId} AND deleted = 0 AND decay_stage < 2 AND embedding IS NOT NULL
            ORDER BY embedding <=> CAST(#{queryVector} AS vector)
            LIMIT #{topK}
            """)
    List<EpisodeSearchHit> searchSimilar(@Param("userId") Long userId,
                                         @Param("queryVector") String queryVector,
                                         @Param("topK") int topK);

    @Select("""
            SELECT id, content_summary AS contentSummary, occurred_at AS occurredAt, importance, NULL AS score
            FROM ai_memory_episode
            WHERE user_id = #{userId} AND deleted = 0 AND decay_stage < 2
            ORDER BY occurred_at DESC
            LIMIT #{topK}
            """)
    List<EpisodeSearchHit> listRecent(@Param("userId") Long userId, @Param("topK") int topK);

    @Select("""
            SELECT * FROM ai_memory_episode
            WHERE deleted = 0 AND decay_stage = 0 AND occurred_at < #{before}
            ORDER BY occurred_at ASC
            LIMIT #{limit}
            """)
    List<AiMemoryEpisodeDO> listForDecayFull(@Param("before") Instant before, @Param("limit") int limit);

    @Select("""
            SELECT * FROM ai_memory_episode
            WHERE deleted = 0 AND decay_stage = 1 AND occurred_at < #{before} AND importance < #{maxImportance}
            ORDER BY occurred_at ASC
            LIMIT #{limit}
            """)
    List<AiMemoryEpisodeDO> listForDecayArchive(@Param("before") Instant before,
                                                 @Param("maxImportance") int maxImportance,
                                                 @Param("limit") int limit);

    @Update("""
            UPDATE ai_memory_episode
            SET content_full = NULL, content_summary = #{summary}, decay_stage = 1,
                embedding = CAST(#{embedding} AS vector), updated_at = NOW()
            WHERE id = #{id} AND decay_stage = 0 AND deleted = 0
            """)
    int updateDecayFull(@Param("id") Long id,
                        @Param("summary") String summary,
                        @Param("embedding") String embedding);

    @Update("""
            UPDATE ai_memory_episode
            SET decay_stage = 2, updated_at = NOW()
            WHERE id = #{id} AND decay_stage = 1 AND deleted = 0
            """)
    int updateDecayArchive(@Param("id") Long id);

    @Select("""
            SELECT id, content_summary AS contentSummary, occurred_at AS occurredAt, importance, NULL AS score
            FROM ai_memory_episode
            WHERE user_id = #{userId} AND deleted = 0
            ORDER BY occurred_at DESC
            LIMIT #{limit}
            """)
    List<EpisodeSearchHit> listByUser(@Param("userId") Long userId, @Param("limit") int limit);
}
