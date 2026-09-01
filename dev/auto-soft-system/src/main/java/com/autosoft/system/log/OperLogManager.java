package com.autosoft.system.log;

import com.autosoft.framework.security.LoginUser;
import com.autosoft.framework.security.SecurityUtils;
import com.autosoft.system.entity.OperLogDO;
import com.autosoft.system.mapper.OperLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 写入操作日志。详情脱敏。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
@Component
public class OperLogManager {

    private static final Logger log = LoggerFactory.getLogger(OperLogManager.class);
    private static final Set<String> SECRET_KEYS = Set.of(
            "password", "newpassword", "oldpassword", "apikey", "token", "secret", "cipher", "authorization");

    private final OperLogMapper operLogMapper;
    private final JsonMapper jsonMapper;

    public OperLogManager(OperLogMapper operLogMapper, JsonMapper jsonMapper) {
        this.operLogMapper = operLogMapper;
        this.jsonMapper = jsonMapper;
    }

    public void write(String module, String action, String bizId, boolean success, int costMs, Object detail) {
        try {
            LoginUser user = SecurityUtils.currentUser();
            OperLogDO row = new OperLogDO();
            if (user != null) {
                row.setUserId(user.getUserId());
                row.setUsername(user.getUsername());
            }
            row.setModule(module);
            row.setAction(action);
            row.setBizId(bizId);
            row.setSuccess(success ? 1 : 0);
            row.setIp(clientIp());
            row.setCostMs(costMs);
            row.setDetailJson(toDetail(detail));
            row.setCreatedAt(Instant.now());
            operLogMapper.insert(row);
        } catch (Exception ex) {
            log.warn("oper log write failed, module={}, action={}", module, action);
        }
    }

    private String toDetail(Object detail) {
        if (detail == null) {
            return null;
        }
        try {
            Object sanitized = sanitize(detail);
            String json = jsonMapper.writeValueAsString(sanitized);
            if (json.length() > 2000) {
                return json.substring(0, 2000);
            }
            return json;
        } catch (Exception ex) {
            return detail.getClass().getSimpleName();
        }
    }

    @SuppressWarnings("unchecked")
    private Object sanitize(Object value) {
        if (value == null || value instanceof Number || value instanceof Boolean) {
            return value;
        }
        if (value instanceof CharSequence text) {
            return text.toString();
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> copy = new LinkedHashMap<>();
            map.forEach((k, v) -> {
                String key = String.valueOf(k);
                if (isSecret(key)) {
                    copy.put(key, "******");
                } else {
                    copy.put(key, sanitize(v));
                }
            });
            return copy;
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream().map(this::sanitize).toList();
        }
        try {
            Map<String, Object> asMap = jsonMapper.convertValue(value, Map.class);
            return sanitize(asMap);
        } catch (Exception ex) {
            return value.getClass().getSimpleName();
        }
    }

    private boolean isSecret(String key) {
        String n = key.toLowerCase(Locale.ROOT).replace("_", "").replace("-", "");
        return SECRET_KEYS.contains(n) || n.contains("password") || n.contains("token") || n.contains("secret")
                || n.contains("apikey");
    }

    private String clientIp() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        HttpServletRequest request = attrs.getRequest();
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
