package com.autosoft.agent.studio;

import com.autosoft.agent.entity.AiMessageDO;
import com.autosoft.agent.entity.AiSessionDO;
import com.autosoft.agent.mapper.AiMessageMapper;
import com.autosoft.agent.mapper.AiSessionMapper;
import com.autosoft.agent.vo.AiMessageVO;
import com.autosoft.agent.vo.AiSessionVO;
import com.autosoft.common.core.ResultCode;
import com.autosoft.common.exception.BizException;
import com.autosoft.framework.log.OperLog;
import com.autosoft.framework.security.LoginUser;
import com.autosoft.framework.security.SecurityUtils;
import com.autosoft.meta.app.MetaCatalogService;
import com.autosoft.meta.entity.MetaAppDO;
import com.autosoft.meta.entity.MetaEntityDO;
import com.autosoft.meta.runtime.RuntimeService;
import com.autosoft.meta.vo.RuntimeSchemaVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 工作室会话。仅会话所属开发者可访问。
 */
@Service
public class StudioSessionService {

    private final AiSessionMapper sessionMapper;
    private final AiMessageMapper messageMapper;
    private final MetaCatalogService catalogService;
    private final RuntimeService runtimeService;

    public StudioSessionService(AiSessionMapper sessionMapper, AiMessageMapper messageMapper,
                                MetaCatalogService catalogService, RuntimeService runtimeService) {
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
        this.catalogService = catalogService;
        this.runtimeService = runtimeService;
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
        session.setTokenInput(0L);
        session.setTokenOutput(0L);
        sessionMapper.insert(session);
        return session.getId();
    }

    public List<AiMessageVO> messages(Long sessionId) {
        requireOwned(sessionId);
        return messageMapper.selectList(new LambdaQueryWrapper<AiMessageDO>()
                        .eq(AiMessageDO::getSessionId, sessionId)
                        .orderByAsc(AiMessageDO::getId))
                .stream()
                .filter(msg -> !"__tool_calls".equals(msg.getToolName()))
                .map(this::toMessageVo)
                .toList();
    }

    public RuntimeSchemaVO schema(Long sessionId) {
        AiSessionDO session = requireOwned(sessionId);
        if (session.getAppId() == null) {
            return null;
        }
        MetaAppDO app = catalogService.requireApp(session.getAppId());
        List<MetaEntityDO> entities = catalogService.listEntities(app.getId());
        if (entities.isEmpty()) {
            return null;
        }
        return runtimeService.schema(app.getCode(), entities.get(0).getCode(), true);
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
