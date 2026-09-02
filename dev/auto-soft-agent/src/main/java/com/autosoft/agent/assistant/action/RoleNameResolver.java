package com.autosoft.agent.assistant.action;

import com.autosoft.common.core.ResultCode;
import com.autosoft.common.exception.BizException;
import com.autosoft.system.entity.RoleDO;
import com.autosoft.system.mapper.RoleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 角色名称/编码解析为 ID 列表。
 *
 * @author zhaodt
 * @since 2026-09-02
 */
@Component
public class RoleNameResolver {

    private final RoleMapper roleMapper;

    public RoleNameResolver(RoleMapper roleMapper) {
        this.roleMapper = roleMapper;
    }

    /**
     * 将角色名称或编码解析为 ID 列表。
     */
    public List<Long> resolve(Object raw) {
        if (raw == null) {
            return List.of();
        }
        List<String> tokens = toTokens(raw);
        if (tokens.isEmpty()) {
            return List.of();
        }
        List<RoleDO> roles = roleMapper.selectList(new LambdaQueryWrapper<RoleDO>()
                .eq(RoleDO::getStatus, 1)
                .orderByAsc(RoleDO::getSort));
        Set<Long> ids = new LinkedHashSet<>();
        for (String token : tokens) {
            RoleDO matched = matchRole(roles, token);
            if (matched == null) {
                String options = roles.stream()
                        .map(RoleDO::getName)
                        .collect(Collectors.joining("、"));
                throw new BizException(ResultCode.BAD_REQUEST,
                        "未找到角色「" + token + "」，可选：" + options);
            }
            ids.add(matched.getId());
        }
        return new ArrayList<>(ids);
    }

    public List<String> displayNameList(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return List.of();
        }
        List<RoleDO> roles = roleMapper.selectList(new LambdaQueryWrapper<RoleDO>()
                .in(RoleDO::getId, roleIds));
        return roles.stream().map(RoleDO::getName).toList();
    }

    private static List<String> toTokens(Object raw) {
        List<String> tokens = new ArrayList<>();
        if (raw instanceof Collection<?> collection) {
            for (Object item : collection) {
                addToken(tokens, item);
            }
            return tokens;
        }
        String text = String.valueOf(raw).trim();
        if (text.isEmpty()) {
            return tokens;
        }
        if (text.contains("、") || text.contains(",") || text.contains("，")) {
            for (String part : text.split("[、,，]")) {
                addToken(tokens, part);
            }
            return tokens;
        }
        addToken(tokens, text);
        return tokens;
    }

    private static void addToken(List<String> tokens, Object value) {
        if (value == null) {
            return;
        }
        String text = String.valueOf(value).trim();
        if (!text.isEmpty()) {
            tokens.add(text);
        }
    }

    private static RoleDO matchRole(List<RoleDO> roles, String token) {
        String normalized = token.trim().toLowerCase(Locale.ROOT);
        for (RoleDO role : roles) {
            if (role.getName() != null && role.getName().equalsIgnoreCase(token)) {
                return role;
            }
            if (role.getCode() != null && role.getCode().equalsIgnoreCase(normalized)) {
                return role;
            }
        }
        for (RoleDO role : roles) {
            if (role.getName() != null && role.getName().contains(token)) {
                return role;
            }
        }
        return null;
    }
}
