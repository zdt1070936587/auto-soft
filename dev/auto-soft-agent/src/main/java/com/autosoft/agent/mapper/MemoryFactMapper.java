package com.autosoft.agent.mapper;

import com.autosoft.agent.assistant.memory.FactSearchHit;
import com.autosoft.agent.entity.AiMemoryFactDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface MemoryFactMapper extends BaseMapper<AiMemoryFactDO> {

    @Insert("""
            INSERT INTO ai_memory_fact
            (user_id, category, fact_key, fact_value, confidence, confirmed, source_episode_id,
             embedding, last_used_at, created_by, updated_by, deleted)
            VALUES
            (#{userId}, #{category}, #{factKey}, #{factValue}, #{confidence}, #{confirmed}, #{sourceEpisodeId},
             CAST(#{embedding} AS vector), NOW(), #{userId}, #{userId}, 0)
            ON CONFLICT (user_id, category, fact_key) DO UPDATE SET
              fact_value = EXCLUDED.fact_value,
              confidence = EXCLUDED.confidence,
              confirmed = CASE WHEN ai_memory_fact.confirmed = 1 THEN ai_memory_fact.confirmed ELSE EXCLUDED.confirmed END,
              embedding = EXCLUDED.embedding,
              last_used_at = NOW(),
              updated_at = NOW(),
              deleted = 0
            """)
    int upsertFact(@Param("userId") Long userId,
                   @Param("category") String category,
                   @Param("factKey") String factKey,
                   @Param("factValue") String factValue,
                   @Param("confidence") float confidence,
                   @Param("confirmed") int confirmed,
                   @Param("sourceEpisodeId") Long sourceEpisodeId,
                   @Param("embedding") String embedding);

    @Select("""
            SELECT id, category, fact_key AS factKey, fact_value AS factValue, confidence, confirmed,
                   last_used_at AS lastUsedAt, NULL AS score
            FROM ai_memory_fact
            WHERE user_id = #{userId} AND deleted = 0
            ORDER BY confirmed DESC, updated_at DESC
            """)
    List<FactSearchHit> listByUser(@Param("userId") Long userId);

    @Select("""
            SELECT id, category, fact_key AS factKey, fact_value AS factValue, confidence, confirmed,
                   last_used_at AS lastUsedAt, NULL AS score
            FROM ai_memory_fact
            WHERE user_id = #{userId} AND deleted = 0 AND category = 'PROFILE'
            ORDER BY confirmed DESC, fact_key ASC
            """)
    List<FactSearchHit> listProfileFacts(@Param("userId") Long userId);

    @Select("""
            SELECT id, category, fact_key AS factKey, fact_value AS factValue, confidence, confirmed,
                   last_used_at AS lastUsedAt,
                   1 - (embedding <=> CAST(#{queryVector} AS vector)) AS score
            FROM ai_memory_fact
            WHERE user_id = #{userId} AND deleted = 0 AND embedding IS NOT NULL
            ORDER BY embedding <=> CAST(#{queryVector} AS vector)
            LIMIT #{topK}
            """)
    List<FactSearchHit> searchSimilar(@Param("userId") Long userId,
                                      @Param("queryVector") String queryVector,
                                      @Param("topK") int topK);

    @Select("""
            SELECT * FROM ai_memory_fact
            WHERE user_id = #{userId} AND category = #{category} AND fact_key = #{factKey} AND deleted = 0
            LIMIT 1
            """)
    AiMemoryFactDO findActive(@Param("userId") Long userId,
                              @Param("category") String category,
                              @Param("factKey") String factKey);

    @Update("""
            UPDATE ai_memory_fact SET deleted = 1, updated_at = NOW()
            WHERE id = #{id} AND user_id = #{userId} AND deleted = 0
            """)
    int softDelete(@Param("userId") Long userId, @Param("id") Long id);

    @Update("""
            UPDATE ai_memory_fact SET confirmed = 1, updated_at = NOW()
            WHERE id = #{id} AND user_id = #{userId} AND deleted = 0
            """)
    int confirm(@Param("userId") Long userId, @Param("id") Long id);
}
