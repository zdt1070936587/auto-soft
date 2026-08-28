package com.autosoft.agent.assistant;

import com.autosoft.agent.assistant.vo.AiAssistantMessageVO;
import com.autosoft.agent.assistant.vo.AiAssistantSessionVO;
import com.autosoft.agent.entity.AiAssistantMessageDO;
import com.autosoft.agent.entity.AiAssistantSessionDO;
import com.autosoft.agent.entity.AiAssistantToolLogDO;
import com.autosoft.agent.mapper.AiAssistantMessageMapper;
import com.autosoft.agent.mapper.AiAssistantSessionMapper;
import com.autosoft.agent.mapper.AiAssistantToolLogMapper;
import com.autosoft.common.core.ResultCode;
import com.autosoft.common.exception.BizException;
import com.autosoft.framework.security.LoginUser;
import com.autosoft.framework.security.SecurityUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 全局助手会话 CRUD。
 */
@Service
public class AssistantSessionService {

    private final AiAssistantSessionMapper sessionMapper;
    private final AiAssistantMessageMapper messageMapper;
    private final AiAssistantToolLogMapper toolLogMapper;

    public AssistantSessionService(AiAssistantSessionMapper sessionMapper,
                                   AiAssistantMessageMapper messageMapper,
                                   AiAssistantToolLogMapper toolLogMapper) {
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
        this.toolLogMapper = toolLogMapper;
    }

    public List<AiAssistantSessionVO> listMine() {
        LoginUser user = SecurityUtils.requireUser();
        return sessionMapper.selectList(new LambdaQueryWrapper<AiAssistantSessionDO>()
                        .eq(AiAssistantSessionDO::getUserId, user.getUserId())
                        .orderByDesc(AiAssistantSessionDO::getId))
                .stream().map(this::toSessionVo).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create() {
        LoginUser user = SecurityUtils.requireUser();
        AiAssistantSessionDO session = new AiAssistantSessionDO();
        session.setUserId(user.getUserId());
        session.setTitle("新对话");
        session.setStatus("ACTIVE");
        session.setTokenInput(0L);
        session.setTokenOutput(0L);
        sessionMapper.insert(session);
        return session.getId();
    }

    public List<AiAssistantMessageVO> messages(Long sessionId) {
        requireOwned(sessionId);
        return messageMapper.selectList(new LambdaQueryWrapper<AiAssistantMessageDO>()
                        .eq(AiAssistantMessageDO::getSessionId, sessionId)
                        .orderByAsc(AiAssistantMessageDO::getId))
                .stream()
                .filter(row -> !"__tool_calls".equals(row.getToolName()))
                .map(this::toMessageVo)
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long sessionId) {
        requireOwned(sessionId);
        messageMapper.delete(new LambdaQueryWrapper<AiAssistantMessageDO>()
                .eq(AiAssistantMessageDO::getSessionId, sessionId));
        toolLogMapper.delete(new LambdaQueryWrapper<AiAssistantToolLogDO>()
                .eq(AiAssistantToolLogDO::getSessionId, sessionId));
        sessionMapper.deleteById(sessionId);
    }

    public AiAssistantSessionDO requireOwned(Long sessionId) {
        LoginUser user = SecurityUtils.requireUser();
        AiAssistantSessionDO session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BizException(ResultCode.NOT_FOUND, "会话不存在");
        }
        if (!user.isSuperAdmin() && !user.getUserId().equals(session.getUserId())) {
            throw new BizException(ResultCode.FORBIDDEN, "不能查看他人会话");
        }
        return session;
    }

    private AiAssistantSessionVO toSessionVo(AiAssistantSessionDO source) {
        AiAssistantSessionVO vo = new AiAssistantSessionVO();
        vo.setId(source.getId());
        vo.setTitle(source.getTitle());
        vo.setStatus(source.getStatus());
        vo.setTokenInput(source.getTokenInput());
        vo.setTokenOutput(source.getTokenOutput());
        vo.setCreatedAt(source.getCreatedAt());
        return vo;
    }

    private AiAssistantMessageVO toMessageVo(AiAssistantMessageDO source) {
        AiAssistantMessageVO vo = new AiAssistantMessageVO();
        vo.setId(source.getId());
        vo.setRole(source.getRole());
        vo.setContent(source.getContent());
        vo.setPayloadJson(source.getPayloadJson());
        vo.setToolName(source.getToolName());
        vo.setCreatedAt(source.getCreatedAt());
        return vo;
    }
}
