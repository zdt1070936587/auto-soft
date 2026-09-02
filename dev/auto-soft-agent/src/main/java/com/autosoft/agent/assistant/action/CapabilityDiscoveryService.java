package com.autosoft.agent.assistant.action;

import com.autosoft.agent.assistant.action.model.CapabilityDefinition;
import com.autosoft.agent.assistant.action.model.CapabilityField;
import com.autosoft.agent.assistant.action.model.CapabilitySearchHit;
import com.autosoft.agent.assistant.action.model.CapabilitySearchResult;
import com.autosoft.agent.assistant.config.AssistantActionProperties;
import com.autosoft.common.core.ResultCode;
import com.autosoft.common.exception.BizException;
import com.autosoft.framework.security.LoginUser;
import com.autosoft.framework.security.SecurityUtils;
import com.autosoft.meta.ddl.Identifiers;
import com.autosoft.meta.runtime.RuntimeService;
import com.autosoft.meta.vo.MetaFieldVO;
import com.autosoft.meta.vo.RuntimeSchemaVO;
import com.autosoft.system.menu.MenuService;
import com.autosoft.system.vo.MenuSearchHit;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 能力发现：系统注册 + 运行时动态 CRUD。
 *
 * @author zhaodt
 * @since 2026-09-02
 */
@Service
public class CapabilityDiscoveryService {

    private static final Pattern APP_ENTITY_PATH = Pattern.compile("^/app/([^/]+)/([^/]+)$");

    private final SystemCapabilityRegistry systemRegistry;
    private final MenuService menuService;
    private final RuntimeService runtimeService;
    private final AssistantActionProperties properties;
    private final JsonMapper jsonMapper;

    public CapabilityDiscoveryService(SystemCapabilityRegistry systemRegistry,
                                      MenuService menuService,
                                      RuntimeService runtimeService,
                                      AssistantActionProperties properties,
                                      JsonMapper jsonMapper) {
        this.systemRegistry = systemRegistry;
        this.menuService = menuService;
        this.runtimeService = runtimeService;
        this.properties = properties;
        this.jsonMapper = jsonMapper;
    }

    public CapabilitySearchResult search(String keyword, String intent, int limit, Long userId) {
        if (intent != null && !intent.isBlank() && !"create".equalsIgnoreCase(intent)) {
            return emptyResult();
        }
        LoginUser user = SecurityUtils.requireUser();
        List<CapabilitySearchHit> merged = new ArrayList<>();
        merged.addAll(systemRegistry.search(keyword, limit));
        merged.addAll(searchRuntime(keyword, limit, userId, user));
        merged.sort(Comparator.comparingInt(CapabilitySearchHit::getScore).reversed());
        int capped = limit <= 0 ? 5 : Math.min(limit, 10);
        List<CapabilitySearchHit> items = dedupe(merged).stream().limit(capped).toList();

        CapabilitySearchResult result = new CapabilitySearchResult();
        result.setItems(items);
        result.setAmbiguous(isAmbiguous(items));
        return result;
    }

    public CapabilityDefinition require(String capabilityId) {
        LoginUser user = SecurityUtils.requireUser();
        CapabilityDefinition cap = load(capabilityId);
        if (cap == null) {
            throw new BizException(ResultCode.NOT_FOUND, "没有找到可执行的功能，请换个说法或去菜单手动操作");
        }
        if (!user.hasPermission(cap.getPermission()) && !user.isDeveloper()) {
            throw new BizException(ResultCode.FORBIDDEN, "你没有执行该操作的权限");
        }
        return cap;
    }

    public CapabilityDefinition load(String capabilityId) {
        CapabilityDefinition system = systemRegistry.get(capabilityId);
        if (system != null) {
            return copyDefinition(system);
        }
        if (capabilityId == null || !capabilityId.startsWith("runtime.")) {
            return null;
        }
        String[] parts = capabilityId.split("\\.");
        if (parts.length != 4 || !"create".equals(parts[3])) {
            return null;
        }
        return buildRuntimeCapability(parts[1], parts[2]);
    }

    public CapabilityDefinition schemaForResponse(String capabilityId) {
        CapabilityDefinition cap = require(capabilityId);
        CapabilityDefinition response = copyDefinition(cap);
        for (CapabilityField field : response.getFields()) {
            if (field.isSensitive()) {
                field.setDefaultValue(null);
            }
        }
        return response;
    }

    private List<CapabilitySearchHit> searchRuntime(String keyword, int limit, Long userId, LoginUser user) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        List<MenuSearchHit> menus = menuService.searchMine(userId, keyword, limit <= 0 ? 10 : limit * 2);
        List<CapabilitySearchHit> hits = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (MenuSearchHit menu : menus) {
            if (menu.getPath() == null) {
                continue;
            }
            Matcher matcher = APP_ENTITY_PATH.matcher(menu.getPath());
            if (!matcher.matches()) {
                continue;
            }
            String appCode = matcher.group(1);
            String entityCode = matcher.group(2);
            String capabilityId = "runtime." + appCode + "." + entityCode + ".create";
            if (!seen.add(capabilityId)) {
                continue;
            }
            CapabilityDefinition cap = buildRuntimeCapability(appCode, entityCode);
            if (cap == null) {
                continue;
            }
            if (!user.hasPermission(cap.getPermission()) && !user.isDeveloper()) {
                continue;
            }
            CapabilitySearchHit hit = new CapabilitySearchHit();
            hit.setCapabilityId(capabilityId);
            hit.setLabel("新建" + cap.getLabel().replace("新建", ""));
            hit.setPath(cap.getPath());
            hit.setScore(scoreRuntime(cap, menu, keyword));
            hit.setSource("runtime");
            hits.add(hit);
        }
        return hits;
    }

    private CapabilityDefinition buildRuntimeCapability(String appCode, String entityCode) {
        RuntimeSchemaVO schema;
        try {
            schema = runtimeService.schema(appCode, entityCode, false);
        } catch (BizException ex) {
            return null;
        }
        if (schema == null || !schema.isPublished()) {
            return null;
        }
        CapabilityDefinition cap = new CapabilityDefinition();
        cap.setCapabilityId("runtime." + appCode + "." + entityCode + ".create");
        cap.setLabel("新建" + (schema.getEntityName() == null ? entityCode : schema.getEntityName()));
        cap.setDescription("在「" + (schema.getAppName() == null ? appCode : schema.getAppName()) + "」中新建记录");
        cap.setPath("/app/" + appCode + "/" + entityCode);
        cap.setPermission("app:" + appCode + ":" + entityCode + ":create");
        cap.setTargetType("runtime_form");
        cap.setOperation("create");
        cap.setApiMethod("POST");
        cap.setApiPath("/api/runtime/" + appCode + "/" + entityCode);
        cap.setKeywords(List.of(
                schema.getEntityName() == null ? entityCode : schema.getEntityName(),
                schema.getAppName() == null ? appCode : schema.getAppName(),
                entityCode, appCode));
        List<CapabilityField> fields = new ArrayList<>();
        for (MetaFieldVO metaField : schema.getFields()) {
            if (metaField.getCode() == null || Identifiers.SYSTEM_COLUMNS.contains(metaField.getCode())) {
                continue;
            }
            fields.add(mapField(metaField));
        }
        cap.setFields(fields);
        return cap;
    }

    private CapabilityField mapField(MetaFieldVO metaField) {
        CapabilityField field = new CapabilityField();
        field.setKey(metaField.getCode());
        field.setLabel(metaField.getName() == null ? metaField.getCode() : metaField.getName());
        field.setType(mapFieldType(metaField.getFieldType()));
        boolean required = (metaField.getRequiredFlag() != null && metaField.getRequiredFlag() == 1)
                || (metaField.getNullableFlag() != null && metaField.getNullableFlag() == 0);
        field.setRequired(required);
        if (metaField.getDefaultValue() != null && !metaField.getDefaultValue().isBlank()) {
            field.setDefaultValue(metaField.getDefaultValue());
        }
        parseEnumOptions(field, metaField.getOptionsJson());
        return field;
    }

    private void parseEnumOptions(CapabilityField field, String optionsJson) {
        if (optionsJson == null || optionsJson.isBlank()) {
            return;
        }
        try {
            List<Map<String, Object>> options = jsonMapper.readValue(optionsJson,
                    new TypeReference<List<Map<String, Object>>>() {
                    });
            List<Object> values = new ArrayList<>();
            for (Map<String, Object> option : options) {
                Object value = option.get("value");
                if (value != null) {
                    values.add(value);
                }
            }
            if (!values.isEmpty()) {
                field.setEnumValues(values);
            }
        } catch (RuntimeException ignored) {
            // ignore malformed options
        }
    }

    private static String mapFieldType(String fieldType) {
        if (fieldType == null) {
            return "string";
        }
        return switch (fieldType.toLowerCase(Locale.ROOT)) {
            case "text", "textarea" -> "string";
            case "int" -> "int";
            case "decimal" -> "decimal";
            case "date", "datetime" -> "datetime";
            case "select", "radio" -> "enum";
            case "switch" -> "bool";
            default -> "string";
        };
    }

    private static int scoreRuntime(CapabilityDefinition cap, MenuSearchHit menu, String keyword) {
        int score = menu.getSort() == null ? 60 : 60;
        if (menu.getName() != null && menu.getName().toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT))) {
            score = 75;
        }
        if (cap.getLabel() != null && cap.getLabel().toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT))) {
            score = Math.max(score, 70);
        }
        return score;
    }

    private static List<CapabilitySearchHit> dedupe(List<CapabilitySearchHit> items) {
        Map<String, CapabilitySearchHit> index = new LinkedHashMap<>();
        for (CapabilitySearchHit item : items) {
            CapabilitySearchHit existing = index.get(item.getCapabilityId());
            if (existing == null || item.getScore() > existing.getScore()) {
                index.put(item.getCapabilityId(), item);
            }
        }
        return new ArrayList<>(index.values());
    }

    private boolean isAmbiguous(List<CapabilitySearchHit> items) {
        if (items.size() < 2) {
            return false;
        }
        int gap = items.get(0).getScore() - items.get(1).getScore();
        return gap < properties.getCapabilityAmbiguousScoreGap();
    }

    private static CapabilitySearchResult emptyResult() {
        CapabilitySearchResult result = new CapabilitySearchResult();
        result.setItems(List.of());
        result.setAmbiguous(false);
        return result;
    }

    private static CapabilityDefinition copyDefinition(CapabilityDefinition source) {
        CapabilityDefinition copy = new CapabilityDefinition();
        copy.setCapabilityId(source.getCapabilityId());
        copy.setLabel(source.getLabel());
        copy.setDescription(source.getDescription());
        copy.setPath(source.getPath());
        copy.setPermission(source.getPermission());
        copy.setTargetType(source.getTargetType());
        copy.setModalKey(source.getModalKey());
        copy.setOperation(source.getOperation());
        copy.setApiMethod(source.getApiMethod());
        copy.setApiPath(source.getApiPath());
        copy.setKeywords(new ArrayList<>(source.getKeywords()));
        List<CapabilityField> fields = new ArrayList<>();
        for (CapabilityField field : source.getFields()) {
            CapabilityField item = new CapabilityField();
            item.setKey(field.getKey());
            item.setLabel(field.getLabel());
            item.setType(field.getType());
            item.setRequired(field.isRequired());
            item.setPattern(field.getPattern());
            item.setHint(field.getHint());
            item.setSensitive(field.isSensitive());
            item.setMulti(field.isMulti());
            item.setDefaultValue(field.getDefaultValue());
            item.setEnumValues(new ArrayList<>(field.getEnumValues()));
            fields.add(item);
        }
        copy.setFields(fields);
        return copy;
    }
}
