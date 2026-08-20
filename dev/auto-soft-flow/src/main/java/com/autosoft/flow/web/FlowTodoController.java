package com.autosoft.flow.web;

import com.autosoft.common.core.R;
import com.autosoft.common.utils.AssertUtils;
import com.autosoft.flow.FlowManager;
import com.autosoft.flow.dto.FlowCommentDTO;
import com.autosoft.flow.vo.FlowTaskVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 待办入口。无引擎类型。
 */
@RestController
@RequestMapping("/api/flow")
public class FlowTodoController {

    private final FlowManager flowManager;

    public FlowTodoController(FlowManager flowManager) {
        this.flowManager = flowManager;
    }

    @GetMapping("/todo")
    public R<List<FlowTaskVO>> todo() {
        return R.ok(flowManager.myTodo());
    }

    @GetMapping("/done")
    public R<List<FlowTaskVO>> done() {
        return R.ok(flowManager.myDone());
    }

    @PostMapping("/todo/{taskId}/complete")
    public R<Void> complete(@PathVariable Long taskId, @RequestBody(required = false) FlowCommentDTO body) {
        flowManager.completeApproved(taskId, body == null ? null : body.getComment());
        return R.ok();
    }

    @PostMapping("/todo/{taskId}/reject")
    public R<Void> reject(@PathVariable Long taskId, @RequestBody FlowCommentDTO body) {
        String comment = body == null ? null : body.getComment();
        AssertUtils.notBlank(comment, "驳回必须填写意见");
        flowManager.reject(taskId, comment);
        return R.ok();
    }
}
