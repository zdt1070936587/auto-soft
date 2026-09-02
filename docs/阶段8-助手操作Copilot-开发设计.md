# 阶段 8：助手操作 Copilot — 详细开发设计

> 文档日期：2026-09-02  
> 阶段计划：[阶段8-助手操作Copilot.md](./阶段8-助手操作Copilot.md)  
> 实现规格：[spec/assistant-action-copilot.spec.md](./spec/assistant-action-copilot.spec.md)  
> 状态：**编码指导（与代码同步）**

本文是阶段 8 的**编码级设计**：类职责、方法签名、文件清单、改造点与编码顺序。协议细节以 spec 为准。

---

## 1. Flyway 版本修正

仓库已占用 `V2.2.0__page_visit.sql`、`V2.3.0__workflow_http_host.sql`。ActionDraft 表使用：

**`dev/auto-soft-boot/src/main/resources/db/migration/V2.4.0__assistant_action_draft.sql`**

---

## 2. 后端包与类清单

```
dev/auto-soft-agent/src/main/java/com/autosoft/agent/
  entity/ActionDraftDO.java
  mapper/ActionDraftMapper.java
  assistant/
    config/AssistantActionProperties.java
    action/
      ActionDraftService.java
      ActionDraftJsonBuilder.java
      ActionFieldValidator.java
      ActionLogSanitizer.java
      CapabilityDiscoveryService.java
      RoleNameResolver.java
      SystemCapabilityRegistry.java
      model/CapabilityDefinition.java, CapabilityField.java, CapabilitySearchHit.java,
            CapabilitySearchResult.java, FieldValidationResult.java
      vo/ActionDraftVO.java
      web/AssistantActionController.java
      tool/impl/SearchCapabilitiesTool.java, GetCapabilitySchemaTool.java,
            PrepareActionDraftTool.java, GetActionDraftTool.java, AssistantAskUserTool.java
```

改造现有类：

| 文件 | 改动 |
| --- | --- |
| `AssistantInfraConfig` | `@EnableConfigurationProperties(AssistantActionProperties.class)` |
| `AssistantToolContext` | `lastDraft`, `askUser`, `askQuestion` |
| `AssistantService` | `action_missing` / `ask_user` SSE；`prepare_action_draft` structured；tool_log 脱敏 |
| `AssistantPromptBuilder` | ACTION 规则 6～12 条 |
| `AssistantIntentHint` | ACTION 关键词与 OPER 分流 |
| `application.yml` | `autosoft.assistant.action-draft-ttl-minutes` 等 |

---

## 3. 核心类方法

### 3.1 `SystemCapabilityRegistry`

- `Optional<CapabilityDefinition> get(String capabilityId)`
- `List<CapabilitySearchHit> search(String keyword, LoginUser user, int limit)`
- P0 内置 `system.user.create`，字段对齐 `UserCreateDTO`

### 3.2 `CapabilityDiscoveryService`

```java
CapabilitySearchResult search(Long userId, LoginUser user, String keyword, String intent, int limit);
Optional<CapabilityDefinition> require(String capabilityId, LoginUser user);
```

Runtime 发现：菜单 path 匹配 `/app/{app}/{entity}` → `RuntimeService.schema` → 过滤权限 `app:*:create`。

### 3.3 `ActionFieldValidator`

```java
FieldValidationResult validate(CapabilityDefinition cap, Map<String, Object> rawFieldValues);
```

输出：`normalizedValues`, `displayValues`, `missing`, `unknown`, `ready`。

### 3.4 `ActionDraftService`

```java
ActionDraftVO createOrUpdate(Long sessionId, Long userId, String capabilityId,
    Map<String, Object> fieldValues, UUID draftId);
ActionDraftVO get(UUID id, Long userId);
void consume(UUID id, Long userId);
void cancel(UUID id, Long userId);
```

### 3.5 Agent 工具

均实现 `AssistantTool`，Spring `@Component` 自动注册到 `AssistantToolRegistry`。

---

## 4. SSE 与 structured

| 触发 | event |
| --- | --- |
| `prepare_action_draft` 且 status=draft | `action_missing` |
| status=ready | `structured` type=`action_plan` |
| `ask_user` 工具 | `ask_user`，本轮结束 |

---

## 5. 前端文件

```
web/src/stores/assistantAction.ts
web/src/components/assistant/ActionPlanCard.vue
web/src/api/assistant.ts          # 扩展类型与 REST
web/src/components/assistant/AssistantPanel.vue
web/src/components/assistant/AssistantMessageItem.vue
web/src/views/system/user/UserView.vue
web/src/components/schema/SchemaRenderer.vue
web/src/views/runtime/RuntimePageView.vue
```

Pinia `consume(capabilityId)` 传递 draft；禁止 URL 带 password。

---

## 6. 编码顺序

1. V2.4.0 迁移 + DO/Mapper + Properties  
2. Capability 模型 + SystemCapabilityRegistry + RoleNameResolver  
3. CapabilityDiscoveryService  
4. ActionFieldValidator + ActionDraftService  
5. 5 个 AssistantTool + Context 扩展  
6. AssistantService + Prompt/IntentHint + Sanitizer  
7. AssistantActionController  
8. 前端 store / ActionPlanCard / Panel / UserView / SchemaRenderer  
9. api.md / database.md / user-guide.md  

---

## 7. 验收

见 [阶段8-助手操作Copilot.md §12](./阶段8-助手操作Copilot.md)；额外检查 tool_log 密码脱敏、consume 后不重复开 Modal、阶段 7 导航/oper log 回归。
