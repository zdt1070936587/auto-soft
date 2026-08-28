package com.autosoft.system.mapper;

import com.autosoft.system.entity.PageVisitDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.Instant;

@Mapper
public interface PageVisitMapper extends BaseMapper<PageVisitDO> {

    @Select("""
            SELECT COUNT(1) > 0 FROM sys_page_visit
            WHERE user_id = #{userId} AND path = #{path} AND visited_at >= #{since}
            """)
    boolean existsRecent(@Param("userId") Long userId,
                         @Param("path") String path,
                         @Param("since") Instant since);

    @Delete("""
            DELETE FROM sys_page_visit WHERE visited_at < #{before}
            """)
    int deleteOlderThan(@Param("before") Instant before);
}
