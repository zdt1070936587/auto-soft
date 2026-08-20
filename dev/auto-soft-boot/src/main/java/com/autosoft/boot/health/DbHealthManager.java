package com.autosoft.boot.health;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 数据库探活。只负责 SELECT 1，不向调用方抛出连接异常。
 */
@Component
public class DbHealthManager {

    private static final Logger log = LoggerFactory.getLogger(DbHealthManager.class);
    private static final String PING_SQL = "SELECT 1";

    private final JdbcTemplate jdbcTemplate;

    public DbHealthManager(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * @return 库可达为 true，否则 false
     */
    public boolean ping() {
        try {
            jdbcTemplate.queryForObject(PING_SQL, Integer.class);
            return true;
        } catch (Exception ex) {
            log.warn("database ping failed: {}", ex.getMessage());
            return false;
        }
    }
}
