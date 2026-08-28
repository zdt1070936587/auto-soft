package com.autosoft.workflow.share;

import com.autosoft.common.core.ResultCode;
import com.autosoft.common.exception.BizException;
import com.autosoft.common.utils.AssertUtils;
import com.autosoft.framework.log.OperLog;
import com.autosoft.workflow.def.WorkflowDefinitionService;
import com.autosoft.workflow.dto.WorkflowCreateDTO;
import com.autosoft.workflow.dto.WorkflowShareDTO;
import com.autosoft.workflow.entity.WfDefinitionDO;
import com.autosoft.workflow.entity.WfShareDO;
import com.autosoft.workflow.graph.GraphCodec;
import com.autosoft.workflow.graph.GraphSecrets;
import com.autosoft.workflow.graph.WorkflowGraph;
import com.autosoft.workflow.mapper.WfShareMapper;
import com.autosoft.workflow.vo.WorkflowShareVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

@Service
public class WorkflowShareService {

    private final WfShareMapper shareMapper;
    private final WorkflowDefinitionService definitionService;
    private final JsonMapper jsonMapper;

    public WorkflowShareService(WfShareMapper shareMapper, WorkflowDefinitionService definitionService,
                                 JsonMapper jsonMapper) {
        this.shareMapper = shareMapper;
        this.definitionService = definitionService;
        this.jsonMapper = jsonMapper;
    }

    @OperLog(module = "WORKFLOW", action = "SHARE")
    @Transactional(rollbackFor = Exception.class)
    public WorkflowShareVO create(Long definitionId, WorkflowShareDTO dto) {
        WfDefinitionDO def = definitionService.requireStudio(definitionId);
        String permission = dto.getPermission() == null ? "preview" : dto.getPermission();
        AssertUtils.isTrue("preview".equals(permission) || "copy".equals(permission), "permission 仅 preview 或 copy");
        int days = dto.getExpireDays() == null ? 7 : dto.getExpireDays();
        AssertUtils.isTrue(days >= 1 && days <= 90, "expireDays 需在 1-90");
        WfShareDO share = new WfShareDO();
        share.setDefinitionId(def.getId());
        share.setToken(UUID.randomUUID().toString().replace("-", ""));
        share.setPermission(permission);
        share.setExpireAt(Instant.now().plus(days, ChronoUnit.DAYS));
        shareMapper.insert(share);
        return toVo(share, def, false);
    }

    public WorkflowShareVO preview(String token) {
        WfShareDO share = requireShare(token);
        WfDefinitionDO def = definitionService.requireById(share.getDefinitionId());
        return toVo(share, def, true);
    }

    @OperLog(module = "WORKFLOW", action = "SHARE")
    @Transactional(rollbackFor = Exception.class)
    public Long copy(String token) {
        WfShareDO share = requireShare(token);
        AssertUtils.isTrue("copy".equals(share.getPermission()), "该分享仅允许预览");
        WfDefinitionDO src = definitionService.requireById(share.getDefinitionId());
        WorkflowGraph graph = GraphCodec.parse(src.getGraphJson(), jsonMapper);
        Map<String, Object> stripped = GraphSecrets.stripMap(GraphCodec.toMap(graph));
        WorkflowCreateDTO dto = new WorkflowCreateDTO();
        dto.setCode("cp" + UUID.randomUUID().toString().replace("-", "").substring(0, 10));
        dto.setName(src.getName() + " 副本");
        Long id = definitionService.create(dto);
        definitionService.saveGraph(id, stripped);
        return id;
    }

    private WfShareDO requireShare(String token) {
        AssertUtils.notBlank(token, "token 不能为空");
        WfShareDO share = shareMapper.selectOne(new LambdaQueryWrapper<WfShareDO>().eq(WfShareDO::getToken, token));
        if (share == null) {
            throw new BizException(ResultCode.NOT_FOUND, "分享不存在");
        }
        if (share.getExpireAt() != null && share.getExpireAt().isBefore(Instant.now())) {
            throw new BizException(ResultCode.NOT_FOUND, "分享已过期");
        }
        return share;
    }

    private WorkflowShareVO toVo(WfShareDO share, WfDefinitionDO def, boolean includeGraph) {
        WorkflowShareVO vo = new WorkflowShareVO();
        vo.setToken(share.getToken());
        vo.setPermission(share.getPermission());
        vo.setExpireAt(share.getExpireAt());
        vo.setName(def.getName());
        vo.setCode(def.getCode());
        if (includeGraph) {
            WorkflowGraph graph = GraphCodec.parse(def.getGraphJson(), jsonMapper);
            vo.setGraph(GraphSecrets.stripMap(GraphCodec.toMap(graph)));
        }
        return vo;
    }
}
