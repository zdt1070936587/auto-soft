package com.autosoft.workflow.def;

import com.autosoft.common.core.ResultCode;
import com.autosoft.common.exception.BizException;
import com.autosoft.common.utils.AssertUtils;
import com.autosoft.framework.log.OperLog;
import com.autosoft.framework.security.LoginUser;
import com.autosoft.framework.security.SecurityUtils;
import com.autosoft.meta.app.AppKind;
import com.autosoft.meta.app.MetaCatalogService;
import com.autosoft.meta.ddl.Identifiers;
import com.autosoft.meta.dto.MetaAppSaveDTO;
import com.autosoft.meta.entity.MetaAppDO;
import com.autosoft.meta.entity.MetaAppMenuDO;
import com.autosoft.meta.mapper.MetaAppMapper;
import com.autosoft.meta.mapper.MetaAppMenuMapper;
import com.autosoft.system.entity.MenuDO;
import com.autosoft.system.entity.RoleDO;
import com.autosoft.system.entity.RoleMenuDO;
import com.autosoft.system.mapper.MenuMapper;
import com.autosoft.system.mapper.RoleMapper;
import com.autosoft.system.mapper.RoleMenuMapper;
import com.autosoft.workflow.dto.WorkflowCreateDTO;
import com.autosoft.workflow.dto.WorkflowPublishDTO;
import com.autosoft.workflow.entity.WfDefinitionDO;
import com.autosoft.workflow.entity.WfDefinitionVersionDO;
import com.autosoft.workflow.graph.GraphCodec;
import com.autosoft.workflow.graph.WorkflowGraph;
import com.autosoft.workflow.graph.WorkflowGraphEditor;
import com.autosoft.workflow.graph.WorkflowGraphValidator;
import com.autosoft.workflow.mapper.WfDefinitionMapper;
import com.autosoft.workflow.mapper.WfDefinitionVersionMapper;
import com.autosoft.workflow.schedule.WorkflowScheduleService;
import com.autosoft.workflow.vo.WorkflowDefinitionVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class WorkflowDefinitionService {

    private final WfDefinitionMapper definitionMapper;
    private final WfDefinitionVersionMapper versionMapper;
    private final MetaCatalogService catalogService;
    private final MetaAppMapper appMapper;
    private final MetaAppMenuMapper appMenuMapper;
    private final MenuMapper menuMapper;
    private final RoleMapper roleMapper;
    private final RoleMenuMapper roleMenuMapper;
    private final WorkflowGraphValidator validator;
    private final JsonMapper jsonMapper;
    private final WorkflowScheduleService scheduleService;

    public WorkflowDefinitionService(WfDefinitionMapper definitionMapper, WfDefinitionVersionMapper versionMapper,
                                     MetaCatalogService catalogService, MetaAppMapper appMapper,
                                     MetaAppMenuMapper appMenuMapper, MenuMapper menuMapper, RoleMapper roleMapper,
                                     RoleMenuMapper roleMenuMapper, WorkflowGraphValidator validator,
                                     JsonMapper jsonMapper, @Lazy WorkflowScheduleService scheduleService) {
        this.definitionMapper = definitionMapper;
        this.versionMapper = versionMapper;
        this.catalogService = catalogService;
        this.appMapper = appMapper;
        this.appMenuMapper = appMenuMapper;
        this.menuMapper = menuMapper;
        this.roleMapper = roleMapper;
        this.roleMenuMapper = roleMenuMapper;
        this.validator = validator;
        this.jsonMapper = jsonMapper;
        this.scheduleService = scheduleService;
    }

    @OperLog(module = "WORKFLOW", action = "CREATE")
    @Transactional(rollbackFor = Exception.class)
    public Long create(WorkflowCreateDTO dto) {
        Identifiers.assertCode(dto.getCode(), "code");
        if (definitionMapper.selectOne(new LambdaQueryWrapper<WfDefinitionDO>()
                .eq(WfDefinitionDO::getCode, dto.getCode())) != null) {
            throw new BizException(ResultCode.BAD_REQUEST, "工作流编码已存在");
        }
        MetaAppSaveDTO appDto = new MetaAppSaveDTO();
        appDto.setCode(dto.getCode());
        appDto.setName(dto.getName());
        appDto.setAppKind(AppKind.WORKFLOW.code());
        appDto.setGrantRoles(blankToUser(dto.getGrantRoles()));
        Long appId = catalogService.createApp(appDto);

        WorkflowGraph graph = GraphCodec.empty(dto.getName());
        WfDefinitionDO def = new WfDefinitionDO();
        def.setAppId(appId);
        def.setCode(dto.getCode());
        def.setName(dto.getName());
        def.setStatus(WfDefinitionDO.DRAFT);
        def.setGraphJson(GraphCodec.toJson(graph, jsonMapper));
        def.setVersion(0);
        def.setGrantRoles(blankToUser(dto.getGrantRoles()));
        def.setVisibility("private");
        definitionMapper.insert(def);
        return def.getId();
    }

    public WorkflowDefinitionVO get(Long id) {
        WfDefinitionDO def = requireStudio(id);
        return toVo(def);
    }

    public WorkflowDefinitionVO getByAppId(Long appId) {
        WfDefinitionDO def = requireByAppId(appId);
        assertStudioAccess(def);
        return toVo(def);
    }

    public WorkflowDefinitionVO getPublishedByCode(String code) {
        Identifiers.assertCode(code, "code");
        WfDefinitionDO def = definitionMapper.selectOne(new LambdaQueryWrapper<WfDefinitionDO>()
                .eq(WfDefinitionDO::getCode, code));
        if (def == null) {
            throw new BizException(ResultCode.NOT_FOUND, "工作流不存在");
        }
        AssertUtils.isTrue(WfDefinitionDO.PUBLISHED.equals(def.getStatus()), "工作流未发布");
        LoginUser user = SecurityUtils.requireUser();
        String perm = "wf:" + def.getCode() + ":run";
        if (!user.hasPermission(perm) && !user.isDeveloper()) {
            throw new BizException(ResultCode.FORBIDDEN, "无权限运行该工作流");
        }
        return toVo(def);
    }

    @OperLog(module = "WORKFLOW", action = "UPDATE")
    @Transactional(rollbackFor = Exception.class)
    public void saveGraph(Long id, Map<String, Object> graphMap) {
        WfDefinitionDO def = requireStudio(id);
        WorkflowGraph graph = GraphCodec.parse(jsonMapper.writeValueAsString(graphMap), jsonMapper);
        persistGraph(def, graph);
    }

    public void validate(Long id) {
        WfDefinitionDO def = requireStudio(id);
        validator.validate(loadGraph(def));
    }

    public WorkflowGraph loadGraph(WfDefinitionDO def) {
        return GraphCodec.parse(def.getGraphJson(), jsonMapper);
    }

    public WfDefinitionDO requireById(Long id) {
        WfDefinitionDO def = definitionMapper.selectById(id);
        if (def == null) {
            throw new BizException(ResultCode.NOT_FOUND, "工作流不存在");
        }
        return def;
    }

    public WfDefinitionDO requireStudio(Long id) {
        WfDefinitionDO def = requireById(id);
        assertStudioAccess(def);
        return def;
    }

    public WfDefinitionDO requireByAppId(Long appId) {
        WfDefinitionDO def = definitionMapper.selectOne(new LambdaQueryWrapper<WfDefinitionDO>()
                .eq(WfDefinitionDO::getAppId, appId));
        if (def == null) {
            throw new BizException(ResultCode.NOT_FOUND, "当前应用未绑定工作流");
        }
        return def;
    }

    @Transactional(rollbackFor = Exception.class)
    public WorkflowDefinitionVO mutate(Long appId, GraphMutator mutator) {
        WfDefinitionDO def = requireByAppId(appId);
        assertStudioAccess(def);
        WorkflowGraph graph = loadGraph(def);
        mutator.apply(graph);
        persistGraph(def, graph);
        return toVo(def);
    }

    @OperLog(module = "WORKFLOW", action = "PUBLISH")
    @Transactional(rollbackFor = Exception.class)
    public void publish(Long id, WorkflowPublishDTO dto) {
        AssertUtils.isTrue(dto != null && dto.isConfirm(), "发布必须 confirm=true");
        WfDefinitionDO def = requireStudio(id);
        WorkflowGraph graph = loadGraph(def);
        validator.validate(graph);
        if (dto.getGrantRoles() != null && !dto.getGrantRoles().isBlank()) {
            def.setGrantRoles(dto.getGrantRoles());
        }
        int next = def.getVersion() == null ? 1 : def.getVersion() + 1;
        WfDefinitionVersionDO snap = new WfDefinitionVersionDO();
        snap.setDefinitionId(def.getId());
        snap.setVersion(next);
        snap.setGraphJson(def.getGraphJson());
        versionMapper.insert(snap);
        def.setVersion(next);
        def.setStatus(WfDefinitionDO.PUBLISHED);
        definitionMapper.updateById(def);

        MetaAppDO app = catalogService.requireApp(def.getAppId());
        app.setGrantRoles(def.getGrantRoles());
        app.setStatus(MetaAppDO.PUBLISHED);
        app.setVersion(next);
        appMapper.updateById(app);
        upsertRunMenu(def, app);
        scheduleService.syncFromGraph(def, graph);
    }

    public String publishedGraphJson(WfDefinitionDO def) {
        AssertUtils.isTrue(def.getVersion() != null && def.getVersion() > 0, "尚未发布");
        WfDefinitionVersionDO snap = versionMapper.selectOne(new LambdaQueryWrapper<WfDefinitionVersionDO>()
                .eq(WfDefinitionVersionDO::getDefinitionId, def.getId())
                .eq(WfDefinitionVersionDO::getVersion, def.getVersion()));
        if (snap == null) {
            throw new BizException(ResultCode.NOT_FOUND, "发布快照不存在");
        }
        return snap.getGraphJson();
    }

    public WfDefinitionDO requirePublishedByCode(String code) {
        Identifiers.assertCode(code, "code");
        WfDefinitionDO def = definitionMapper.selectOne(new LambdaQueryWrapper<WfDefinitionDO>()
                .eq(WfDefinitionDO::getCode, code));
        if (def == null) {
            throw new BizException(ResultCode.NOT_FOUND, "工作流不存在");
        }
        AssertUtils.isTrue(WfDefinitionDO.PUBLISHED.equals(def.getStatus()), "工作流未发布");
        return def;
    }

    public void setTrigger(Long appId, String type, Map<String, String> inputSchema,
                           String app, String entity, String cron, Boolean enabled) {
        mutate(appId, graph -> WorkflowGraphEditor.setTrigger(graph, type, inputSchema, app, entity, cron, enabled));
    }

    public void addNode(Long appId, String id, String type, String title, Map<String, Object> config) {
        mutate(appId, graph -> WorkflowGraphEditor.addNode(graph, id, type, title, config));
    }

    public void updateNode(Long appId, String id, String title, Map<String, Object> config) {
        mutate(appId, graph -> WorkflowGraphEditor.updateNode(graph, id, title, config));
    }

    public void removeNode(Long appId, String id) {
        mutate(appId, graph -> WorkflowGraphEditor.removeNode(graph, id));
    }

    public void connect(Long appId, String from, String to, String when) {
        mutate(appId, graph -> WorkflowGraphEditor.connect(graph, from, to, when));
    }

    public WorkflowDefinitionVO toVo(WfDefinitionDO def) {
        WorkflowDefinitionVO vo = new WorkflowDefinitionVO();
        vo.setId(def.getId());
        vo.setAppId(def.getAppId());
        vo.setCode(def.getCode());
        vo.setName(def.getName());
        vo.setStatus(def.getStatus());
        vo.setVersion(def.getVersion());
        vo.setGrantRoles(def.getGrantRoles());
        vo.setAppKind(AppKind.WORKFLOW.code());
        vo.setPublished(WfDefinitionDO.PUBLISHED.equals(def.getStatus()));
        vo.setGraph(GraphCodec.toMap(loadGraph(def)));
        return vo;
    }

    private void persistGraph(WfDefinitionDO def, WorkflowGraph graph) {
        if (graph.getName() == null || graph.getName().isBlank()) {
            graph.setName(def.getName());
        }
        def.setGraphJson(GraphCodec.toJson(graph, jsonMapper));
        def.setName(graph.getName() == null ? def.getName() : graph.getName());
        definitionMapper.updateById(def);
    }

    private void assertStudioAccess(WfDefinitionDO def) {
        LoginUser user = SecurityUtils.requireUser();
        if (user.isSuperAdmin()) {
            return;
        }
        if (!user.isDeveloper()) {
            throw new BizException(ResultCode.FORBIDDEN, "无权限编辑工作流草稿");
        }
        if (def.getCreatedBy() != null && def.getCreatedBy() != 0L && !def.getCreatedBy().equals(user.getUserId())) {
            throw new BizException(ResultCode.FORBIDDEN, "不能编辑他人工作流草稿");
        }
    }

    private void upsertRunMenu(WfDefinitionDO def, MetaAppDO app) {
        String path = "/wf/" + def.getCode();
        String perm = "wf:" + def.getCode() + ":run";
        MenuDO menu = menuMapper.selectOne(new LambdaQueryWrapper<MenuDO>().eq(MenuDO::getPath, path));
        if (menu == null) {
            menu = new MenuDO();
            menu.setParentId(0L);
            menu.setName(def.getName());
            menu.setPath(path);
            menu.setComponent("WorkflowRunView");
            menu.setMenuType(MenuDO.TYPE_MENU);
            menu.setPermission(perm);
            menu.setSort(85);
            menu.setVisible(1);
            menu.setStatus(1);
            menuMapper.insert(menu);
            MetaAppMenuDO link = new MetaAppMenuDO();
            link.setAppId(app.getId());
            link.setMenuId(menu.getId());
            appMenuMapper.insert(link);
        } else {
            menu.setName(def.getName());
            menu.setVisible(1);
            menu.setStatus(1);
            menu.setPermission(perm);
            menuMapper.updateById(menu);
        }
        List<String> codes = Arrays.stream(def.getGrantRoles().split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).toList();
        for (String code : codes) {
            RoleDO role = roleMapper.selectOne(new LambdaQueryWrapper<RoleDO>().eq(RoleDO::getCode, code));
            if (role == null) {
                throw new BizException(ResultCode.BAD_REQUEST, "角色不存在: " + code);
            }
            Long count = roleMenuMapper.selectCount(new LambdaQueryWrapper<RoleMenuDO>()
                    .eq(RoleMenuDO::getRoleId, role.getId()).eq(RoleMenuDO::getMenuId, menu.getId()));
            if (count == 0) {
                RoleMenuDO rm = new RoleMenuDO();
                rm.setRoleId(role.getId());
                rm.setMenuId(menu.getId());
                roleMenuMapper.insert(rm);
            }
        }
    }

    private static String blankToUser(String grantRoles) {
        return grantRoles == null || grantRoles.isBlank() ? "USER" : grantRoles;
    }

    @FunctionalInterface
    public interface GraphMutator {
        void apply(WorkflowGraph graph);
    }
}
