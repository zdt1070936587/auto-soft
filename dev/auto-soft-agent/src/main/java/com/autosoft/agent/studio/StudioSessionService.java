package com.autosoft.agent.studio;

import com.autosoft.agent.dto.UpdateModeDTO;
import com.autosoft.agent.entity.AiAttachmentDO;
import com.autosoft.agent.entity.AiMessageDO;
import com.autosoft.agent.entity.AiSessionDO;
import com.autosoft.agent.entity.AiToolLogDO;
import com.autosoft.agent.mapper.AiMessageMapper;
import com.autosoft.agent.mapper.AiSessionMapper;
import com.autosoft.agent.mapper.AiToolLogMapper;
import com.autosoft.agent.vo.AiAttachmentVO;
import com.autosoft.agent.vo.AiMessageVO;
import com.autosoft.agent.vo.AiSessionVO;
import com.autosoft.common.core.ResultCode;
import com.autosoft.common.exception.BizException;
import com.autosoft.common.utils.AssertUtils;
import com.autosoft.framework.log.OperLog;
import com.autosoft.framework.security.LoginUser;
import com.autosoft.framework.security.SecurityUtils;
import com.autosoft.meta.app.MetaCatalogService;
import com.autosoft.meta.entity.MetaAppDO;
import com.autosoft.meta.runtime.RuntimeService;
import com.autosoft.meta.vo.PageViewVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 工作室会话。仅会话所属开发者可访问。
 */
@Service
public class StudioSessionService {

    private final AiSessionMapper sessionMapper;
    private final AiMessageMapper messageMapper;
    private final AiToolLogMapper toolLogMapper;
    private final MetaCatalogService catalogService;
    private final RuntimeService runtimeService;
    private final StudioAttachmentService attachmentService;

    public StudioSessionService(AiSessionMapper sessionMapper, AiMessageMapper messageMapper,
                                AiToolLogMapper toolLogMapper,
                                MetaCatalogService catalogService, RuntimeService runtimeService,
                                StudioAttachmentService attachmentService) {
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
        this.toolLogMapper = toolLogMapper;
        this.catalogService = catalogService;
        this.runtimeService = runtimeService;
        this.attachmentService = attachmentService;
    }

    public List<AiSessionVO> listMine() {
        LoginUser user = SecurityUtils.requireUser();
        return sessionMapper.selectList(new LambdaQueryWrapper<AiSessionDO>()
                        .eq(AiSessionDO::getUserId, user.getUserId())
                        .orderByDesc(AiSessionDO::getId))
                .stream().map(this::toSessionVo).toList();
    }

    @OperLog(module = "STUDIO", action = "CREATE")
    @Transactional(rollbackFor = Exception.class)
    public Long create() {
        LoginUser user = SecurityUtils.requireUser();
        AiSessionDO session = new AiSessionDO();
        session.setUserId(user.getUserId());
        session.setTitle("新会话");
        session.setStatus("ACTIVE");
        session.setAgentMode(AgentMode.DEVELOP.code());
        session.setTokenInput(0L);
        session.setTokenOutput(0L);
        sessionMapper.insert(session);
        return session.getId();
    }

    public List<AiMessageVO> messages(Long sessionId) {
        requireOwned(sessionId);
        List<AiMessageDO> rows = messageMapper.selectList(new LambdaQueryWrapper<AiMessageDO>()
                .eq(AiMessageDO::getSessionId, sessionId)
                .orderByAsc(AiMessageDO::getId));
        List<Long> messageIds = rows.stream().map(AiMessageDO::getId).toList();
        List<AiAttachmentDO> attachments = attachmentService.listByMessageIds(messageIds);
        Map<Long, List<AiAttachmentDO>> grouped = attachments.stream()
                .collect(Collectors.groupingBy(AiAttachmentDO::getMessageId));
        List<AiMessageVO> result = new ArrayList<>();
        for (AiMessageDO row : rows) {
            if ("__tool_calls".equals(row.getToolName())) {
                continue;
            }
            AiMessageVO vo = toMessageVo(row);
            List<AiAttachmentDO> linked = grouped.getOrDefault(row.getId(), List.of());
            vo.setAttachments(attachmentService.toVoList(linked));
            result.add(vo);
        }
        return result;
    }

    public PageViewVO schema(Long sessionId) {
        AiSessionDO session = requireOwned(sessionId);
        if (session.getAppId() == null) {
            return null;
        }
        MetaAppDO app = catalogService.requireApp(session.getAppId());
        return runtimeService.resolveAppView(app.getCode(), true);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateMode(Long sessionId, UpdateModeDTO dto) {
        AiSessionDO session = requireOwned(sessionId);
        AgentMode mode = AgentMode.from(dto.getAgentMode());
        session.setAgentMode(mode.code());
        sessionMapper.updateById(session);
    }

    @OperLog(module = "STUDIO", action = "DELETE")
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long sessionId) {
        requireOwned(sessionId);
        messageMapper.delete(new LambdaQueryWrapper<AiMessageDO>().eq(AiMessageDO::getSessionId, sessionId));
        toolLogMapper.delete(new LambdaQueryWrapper<AiToolLogDO>().eq(AiToolLogDO::getSessionId, sessionId));
        attachmentService.purgeSession(sessionId);
        sessionMapper.deleteById(sessionId);
    }

    public AiSessionDO requireOwned(Long sessionId) {
        LoginUser user = SecurityUtils.requireUser();
        AiSessionDO session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BizException(ResultCode.NOT_FOUND, "会话不存在");
        }
        if (!user.isSuperAdmin() && !user.getUserId().equals(session.getUserId())) {
            throw new BizException(ResultCode.FORBIDDEN, "不能查看他人会话");
        }
        return session;
    }

    private AiSessionVO toSessionVo(AiSessionDO source) {
        AiSessionVO vo = new AiSessionVO();
        vo.setId(source.getId());
        vo.setTitle(source.getTitle());
        vo.setAppId(source.getAppId());
        vo.setStatus(source.getStatus());
        vo.setTokenInput(source.getTokenInput());
        vo.setTokenOutput(source.getTokenOutput());
        vo.setAgentMode(source.getAgentMode() == null ? AgentMode.DEVELOP.code() : source.getAgentMode());
        vo.setCreatedAt(source.getCreatedAt());
        return vo;
    }

    private AiMessageVO toMessageVo(AiMessageDO source) {
        AiMessageVO vo = new AiMessageVO();
        vo.setId(source.getId());
        vo.setRole(source.getRole());
        vo.setContent(source.getContent());
        vo.setToolName(source.getToolName());
        vo.setCreatedAt(source.getCreatedAt());
        return vo;
    }
}
