package com.autosoft.workflow.exec;

import com.autosoft.common.core.ResultCode;
import com.autosoft.common.exception.BizException;
import com.autosoft.system.entity.RoleDO;
import com.autosoft.system.mapper.RoleMapper;
import com.autosoft.workflow.entity.WfNoticeDO;
import com.autosoft.workflow.mapper.WfNoticeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Component;

@Component
public class WfNoticeNotifyAdapter implements WorkflowNotifyPort {

    private final RoleMapper roleMapper;
    private final WfNoticeMapper noticeMapper;

    public WfNoticeNotifyAdapter(RoleMapper roleMapper, WfNoticeMapper noticeMapper) {
        this.roleMapper = roleMapper;
        this.noticeMapper = noticeMapper;
    }

    @Override
    public void send(String toRole, String title, String body, Long runId) {
        RoleDO role = roleMapper.selectOne(new LambdaQueryWrapper<RoleDO>().eq(RoleDO::getCode, toRole));
        if (role == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "通知角色不存在: " + toRole);
        }
        WfNoticeDO notice = new WfNoticeDO();
        notice.setRunId(runId);
        notice.setToRole(toRole);
        notice.setTitle(title == null || title.isBlank() ? "工作流通知" : title);
        notice.setBody(body);
        noticeMapper.insert(notice);
    }
}
