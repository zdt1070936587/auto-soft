package com.autosoft.workflow.exec;

/**
 * NodeFailed异常。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
public class NodeFailedException extends RuntimeException {

    public NodeFailedException(String message) {
        super(message);
    }
}
