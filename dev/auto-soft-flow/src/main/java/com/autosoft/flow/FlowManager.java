package com.autosoft.flow;

import com.autosoft.common.core.ResultCode;
import com.autosoft.common.exception.BizException;
import com.autosoft.common.utils.AssertUtils;
import com.autosoft.flow.entity.FlowDefinitionDO;
import com.autosoft.flow.entity.FlowInstanceDO;
import com.autosoft.flow.entity.FlowTaskDO;
import com.autosoft.flow.entity.MetaEntityFlowDO;
import com.autosoft.flow.mapper.FlowDefinitionMapper;
import com.autosoft.flow.mapper.FlowInstanceMapper;
import com.autosoft.flow.mapper.FlowTaskMapper;
import com.autosoft.flow.mapper.MetaEntityFlowMapper;
import com.autosoft.flow.vo.FlowTaskVO;
import com.autosoft.framework.log.OperLog;
import com.autosoft.framework.security.LoginUser;
import com.autosoft.framework.security.SecurityUtils;
import com.autosoft.meta.app.MetaCatalogService;
import com.autosoft.meta.entity.MetaAppDO;
import com.autosoft.meta.entity.MetaEntityDO;
import com.autosoft.meta.runtime.FlowBinder;
import com.autosoft.meta.runtime.FlowHook;
import com.autosoft.meta.runtime.FlowSubmitHook;
import com.autosoft.meta.runtime.RuntimeService;
import com.autosoft.system.mapper.RoleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 单线审批封装。驳回终止当前实例，再次提交 start 新实例。
 */
@Service
public class FlowManager implements FlowHook, FlowSubmitHook, FlowBinder {

    private final MetaEntityFlowMapper bindMapper;
    private final FlowDefinitionMapper definitionMapper;
    private final FlowInstanceMapper instanceMapper;
    private final FlowTaskMapper taskMapper;
    private final RoleMapper roleMapper;
    private final MetaCatalogService catalogService;
    private final RuntimeService runtimeService;

    public FlowManager(MetaEntityFlowMapper bindMapper, FlowDefinitionMapper definitionMapper,
                       FlowInstanceMapper instanceMapper, FlowTaskMapper taskMapper, RoleMapper roleMapper,
                       MetaCatalogService catalogService, @Lazy RuntimeService runtimeService) {
        this.bindMapper = bindMapper;
        this.definitionMapper = definitionMapper;
        this.instanceMapper = instanceMapper;
        this.taskMapper = taskMapper;
        this.roleMapper = roleMapper;
        this.catalogService = catalogService;
        this.runtimeService = runtimeService;
    }

    @Override
    public boolean bound(String appCode, String entityCode) {
        MetaEntityFlowDO bind = findBind(appCode, entityCode);
        return bind != null && bind.getEnabled() != null && bind.getEnabled() == 1;
    }

    @Override
    public void assertWritable(String appCode, String entityCode, String flowStatus) {
        if (!bound(appCode, entityCode)) {
            return;
        }
        if ("processing".equals(flowStatus) || "approved".equals(flowStatus)) {
            throw new BizException(ResultCode.BAD_REQUEST, "审批中或已通过的记录不可修改");
        }
    }

    @Override
    @OperLog(module = "FLOW", action = "SUBMIT")
    @Transactional(rollbackFor = Exception.class)
    public void submit(String appCode, String entityCode, Long bizId) {
        MetaEntityFlowDO bind = findBind(appCode, entityCode);
        if (bind == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "未绑定审批流程");
        }
        Map<String, Object> row = runtimeService.getRow(appCode, entityCode, bizId);
        String status = String.valueOf(row.get("flow_status"));
        AssertUtils.isTrue("draft".equals(status) || "rejected".equals(status), "当前状态不可提交");
        FlowDefinitionDO def = definitionMapper.selectById(bind.getDefinitionId());
        AssertUtils.notNull(def, "流程定义不存在");
        List<String> roles = splitRoles(def.getApproveRoleCodes());
        LoginUser user = SecurityUtils.requireUser();
        FlowInstanceDO ins = new FlowInstanceDO();
        ins.setDefinitionId(def.getId());
        ins.setAppCode(appCode);
        ins.setEntityCode(entityCode);
        ins.setBizId(bizId);
        ins.setStatus("processing");
        ins.setStartUserId(user.getUserId());
        ins.setCurrentLevel(0);
        instanceMapper.insert(ins);
        FlowTaskDO task = new FlowTaskDO();
        task.setInstanceId(ins.getId());
        task.setLevelNo(0);
        task.setRoleCode(roles.get(0));
        task.setStatus("pending");
        taskMapper.insert(task);
        runtimeService.updateStatus(appCode, entityCode, bizId, "processing");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSimpleFlow(Long entityId, List<String> roleCodes) {
        AssertUtils.isTrue(roleCodes != null && !roleCodes.isEmpty() && roleCodes.size() <= 3, "审批角色 1-3 级");
        for (String code : roleCodes) {
            AssertUtils.notNull(roleMapper.selectOne(new LambdaQueryWrapper<com.autosoft.system.entity.RoleDO>()
                    .eq(com.autosoft.system.entity.RoleDO::getCode, code)), "角色不存在: " + code);
        }
        MetaEntityDO entity = catalogService.requireEntity(entityId);
        MetaAppDO app = catalogService.requireApp(entity.getAppId());
        String flowCode = "flow_" + app.getCode() + "_" + entity.getCode();
        FlowDefinitionDO def = definitionMapper.selectOne(new LambdaQueryWrapper<FlowDefinitionDO>()
                .eq(FlowDefinitionDO::getFlowCode, flowCode));
        if (def == null) {
            def = new FlowDefinitionDO();
            def.setFlowCode(flowCode);
            def.setName(entity.getName() + "审批");
            def.setApproveRoleCodes(String.join(",", roleCodes));
            def.setEnabled(1);
            definitionMapper.insert(def);
        } else {
            def.setApproveRoleCodes(String.join(",", roleCodes));
            definitionMapper.updateById(def);
        }
        bindFlow(entityId, def.getId());
        return def.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindFlow(Long entityId, Long definitionId) {
        catalogService.requireEntity(entityId);
        FlowDefinitionDO def = definitionMapper.selectById(definitionId);
        AssertUtils.notNull(def, "流程定义不存在");
        MetaEntityFlowDO bind = bindMapper.selectOne(new LambdaQueryWrapper<MetaEntityFlowDO>()
                .eq(MetaEntityFlowDO::getEntityId, entityId));
        if (bind == null) {
            bind = new MetaEntityFlowDO();
            bind.setEntityId(entityId);
            bind.setFlowCode(def.getFlowCode());
            bind.setDefinitionId(def.getId());
            bind.setApproveRoleCodes(def.getApproveRoleCodes());
            bind.setEnabled(1);
            bindMapper.insert(bind);
            return;
        }
        bind.setFlowCode(def.getFlowCode());
        bind.setDefinitionId(def.getId());
        bind.setApproveRoleCodes(def.getApproveRoleCodes());
        bind.setEnabled(1);
        bindMapper.updateById(bind);
    }

    @OperLog(module = "FLOW", action = "COMPLETE")
    @Transactional(rollbackFor = Exception.class)
    public void completeApproved(Long taskId, String comment) {
        complete(taskId, comment, false);
    }

    @OperLog(module = "FLOW", action = "REJECT")
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long taskId, String comment) {
        complete(taskId, comment, true);
    }

    private void complete(Long taskId, String comment, boolean reject) {
        LoginUser user = SecurityUtils.requireUser();
        FlowTaskDO task = taskMapper.selectById(taskId);
        if (task == null || !"pending".equals(task.getStatus())) {
            throw new BizException(ResultCode.NOT_FOUND, "待办不存在");
        }
        if (!user.isSuperAdmin() && !user.getRoleCodes().contains(task.getRoleCode())) {
            throw new BizException(ResultCode.FORBIDDEN, "不是当前办理角色");
        }
        if (reject) {
            AssertUtils.notBlank(comment, "驳回必须填写意见");
        }
        task.setStatus(reject ? "rejected" : "done");
        task.setAssigneeId(user.getUserId());
        task.setCommentText(comment);
        taskMapper.updateById(task);
        FlowInstanceDO ins = instanceMapper.selectById(task.getInstanceId());
        FlowDefinitionDO def = definitionMapper.selectById(ins.getDefinitionId());
        List<String> roles = splitRoles(def.getApproveRoleCodes());
        if (reject) {
            ins.setStatus("rejected");
            instanceMapper.updateById(ins);
            runtimeService.updateStatus(ins.getAppCode(), ins.getEntityCode(), ins.getBizId(), "rejected");
            return;
        }
        int next = task.getLevelNo() + 1;
        if (next >= roles.size()) {
            ins.setStatus("approved");
            instanceMapper.updateById(ins);
            runtimeService.updateStatus(ins.getAppCode(), ins.getEntityCode(), ins.getBizId(), "approved");
            return;
        }
        ins.setCurrentLevel(next);
        instanceMapper.updateById(ins);
        FlowTaskDO nextTask = new FlowTaskDO();
        nextTask.setInstanceId(ins.getId());
        nextTask.setLevelNo(next);
        nextTask.setRoleCode(roles.get(next));
        nextTask.setStatus("pending");
        taskMapper.insert(nextTask);
    }

    public List<FlowTaskVO> myTodo() {
        LoginUser user = SecurityUtils.requireUser();
        List<FlowTaskDO> tasks = taskMapper.selectList(new LambdaQueryWrapper<FlowTaskDO>()
                .eq(FlowTaskDO::getStatus, "pending"));
        List<FlowTaskVO> result = new ArrayList<>();
        for (FlowTaskDO task : tasks) {
            if (!user.isSuperAdmin() && !user.getRoleCodes().contains(task.getRoleCode())) {
                continue;
            }
            FlowInstanceDO ins = instanceMapper.selectById(task.getInstanceId());
            if (ins == null) {
                continue;
            }
            FlowTaskVO vo = new FlowTaskVO();
            vo.setTaskId(task.getId());
            vo.setAppCode(ins.getAppCode());
            vo.setEntityCode(ins.getEntityCode());
            vo.setBizId(ins.getBizId());
            vo.setRoleCode(task.getRoleCode());
            vo.setCreatedAt(task.getCreatedAt());
            vo.setStartUserId(ins.getStartUserId());
            vo.setStatus(task.getStatus());
            result.add(vo);
        }
        return result;
    }

    public List<FlowTaskVO> myDone() {
        LoginUser user = SecurityUtils.requireUser();
        List<FlowTaskDO> tasks = taskMapper.selectList(new LambdaQueryWrapper<FlowTaskDO>()
                .eq(FlowTaskDO::getAssigneeId, user.getUserId())
                .in(FlowTaskDO::getStatus, List.of("done", "rejected")));
        List<FlowTaskVO> result = new ArrayList<>();
        for (FlowTaskDO task : tasks) {
            FlowInstanceDO ins = instanceMapper.selectById(task.getInstanceId());
            if (ins == null) {
                continue;
            }
            FlowTaskVO vo = new FlowTaskVO();
            vo.setTaskId(task.getId());
            vo.setAppCode(ins.getAppCode());
            vo.setEntityCode(ins.getEntityCode());
            vo.setBizId(ins.getBizId());
            vo.setStatus(task.getStatus());
            vo.setComment(task.getCommentText() == null ? "" : task.getCommentText());
            result.add(vo);
        }
        return result;
    }

    private MetaEntityFlowDO findBind(String appCode, String entityCode) {
        try {
            MetaEntityDO entity = catalogService.requireEntity(appCode, entityCode);
            return bindMapper.selectOne(new LambdaQueryWrapper<MetaEntityFlowDO>()
                    .eq(MetaEntityFlowDO::getEntityId, entity.getId()).eq(MetaEntityFlowDO::getEnabled, 1));
        } catch (BizException ex) {
            return null;
        }
    }

    private List<String> splitRoles(String raw) {
        return Arrays.stream(raw.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }
}
