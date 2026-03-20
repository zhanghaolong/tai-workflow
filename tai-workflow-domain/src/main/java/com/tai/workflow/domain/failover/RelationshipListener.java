package com.tai.workflow.domain.failover;

/**
 * @author zhanghaolong1989@163.com
 */
public interface RelationshipListener {
    void becomeLeader();

    void lostLeader();
}
