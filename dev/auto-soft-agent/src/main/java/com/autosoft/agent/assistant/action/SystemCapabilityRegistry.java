package com.autosoft.agent.assistant.action;

import com.autosoft.agent.assistant.action.model.CapabilityDefinition;
import com.autosoft.agent.assistant.action.model.CapabilityField;
import com.autosoft.agent.assistant.action.model.CapabilitySearchHit;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 系统内置能力注册表。
 *
 * @author zhaodt
 * @since 2026-09-02
 */
@Component
public class SystemCapabilityRegistry {

    private final Map<String, CapabilityDefinition> capabilities = new LinkedHashMap<>();

    @PostConstruct
    void init() {
        registerUserCreate();
    }

    public List<CapabilitySearchHit> search(String keyword, int limit) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        String kw = keyword.trim().toLowerCase(Locale.ROOT);
        List<ScoredHit> scored = new ArrayList<>();
        for (CapabilityDefinition cap : capabilities.values()) {
            int score = scoreCapability(cap, kw);
            if (score > 0) {
                CapabilitySearchHit hit = new CapabilitySearchHit();
                hit.setCapabilityId(cap.getCapabilityId());
                hit.setLabel(cap.getLabel());
                hit.setPath(cap.getPath());
                hit.setScore(score);
                hit.setSource("system");
                scored.add(new ScoredHit(score, hit));
            }
        }
        scored.sort(Comparator.comparingInt(ScoredHit::score).reversed());
        int capped = limit <= 0 ? 5 : Math.min(limit, 10);
        return scored.stream().limit(capped).map(item -> item.hit).toList();
    }

    public CapabilityDefinition get(String capabilityId) {
        return capabilities.get(capabilityId);
    }

    public List<CapabilityDefinition> all() {
        return List.copyOf(capabilities.values());
    }

    private void registerUserCreate() {
        CapabilityDefinition cap = new CapabilityDefinition();
        cap.setCapabilityId("system.user.create");
        cap.setLabel("新建系统用户");
        cap.setDescription("在系统用户管理中创建账号");
        cap.setPath("/system/users");
        cap.setPermission("system:user:create");
        cap.setTargetType("system_modal");
        cap.setModalKey("userCreate");
        cap.setOperation("create");
        cap.setApiMethod("POST");
        cap.setApiPath("/api/users");
        cap.setKeywords(List.of("用户", "账号", "系统用户", "用户管理", "新建用户", "添加用户"));
        cap.setFields(List.of(
                field("username", "用户名", "string", true,
                        "^[a-zA-Z][a-zA-Z0-9_]{3,31}$", "以字母开头，4-32 位字母数字下划线", false, false, null, List.of()),
                field("password", "密码", "string", true,
                        "^(?=.*[A-Za-z])(?=.*\\d).{8,32}$", "8-32 位且包含字母和数字", true, false, null, List.of()),
                field("nickname", "昵称", "string", true, null, null, false, false, null, List.of()),
                field("roleIds", "角色", "role_ref", true, null, null, false, true, null, List.of()),
                field("status", "状态", "int", false, null, null, false, false, 1, List.of(0, 1))
        ));
        capabilities.put(cap.getCapabilityId(), cap);
    }

    private static CapabilityField field(String key, String label, String type, boolean required,
                                         String pattern, String hint, boolean sensitive, boolean multi,
                                         Object defaultValue, List<Object> enumValues) {
        CapabilityField field = new CapabilityField();
        field.setKey(key);
        field.setLabel(label);
        field.setType(type);
        field.setRequired(required);
        field.setPattern(pattern);
        field.setHint(hint);
        field.setSensitive(sensitive);
        field.setMulti(multi);
        field.setDefaultValue(defaultValue);
        field.setEnumValues(enumValues);
        return field;
    }

    private static int scoreCapability(CapabilityDefinition cap, String kw) {
        int best = 0;
        if (cap.getLabel() != null) {
            String label = cap.getLabel().toLowerCase(Locale.ROOT);
            if (label.contains(kw)) {
                best = Math.max(best, label.equals(kw) ? 100 : 90);
            }
        }
        if (cap.getPath() != null && cap.getPath().toLowerCase(Locale.ROOT).contains(kw.replace(" ", ""))) {
            best = Math.max(best, 40);
        }
        for (String keyword : cap.getKeywords()) {
            if (keyword == null) {
                continue;
            }
            String item = keyword.toLowerCase(Locale.ROOT);
            if (item.contains(kw) || kw.contains(item)) {
                best = Math.max(best, item.equals(kw) ? 95 : 80);
            }
        }
        if (kw.contains("用户") || kw.contains("账号")) {
            if ("system.user.create".equals(cap.getCapabilityId())) {
                best = Math.max(best, 85);
            }
        }
        return best;
    }

    private record ScoredHit(int score, CapabilitySearchHit hit) {
    }
}
