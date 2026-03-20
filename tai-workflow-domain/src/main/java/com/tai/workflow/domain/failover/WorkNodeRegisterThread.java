package com.tai.workflow.domain.failover;

import lombok.extern.slf4j.Slf4j;

/**
 * @author zhanghaolong1989@163.com
 */
@Slf4j
public class WorkNodeRegisterThread implements Runnable {
    private final FailoverManagerBaseOnDatabase failoverManagerBaseOnDatabase;
    private final String selfNodeId;

    public WorkNodeRegisterThread(String selfNodeId, FailoverManagerBaseOnDatabase failoverManagerBaseOnDatabase) {
        this.selfNodeId = selfNodeId;
        this.failoverManagerBaseOnDatabase = failoverManagerBaseOnDatabase;
    }

    public void run() {
        failoverManagerBaseOnDatabase.registerHeartbeat(selfNodeId);
        failoverManagerBaseOnDatabase.doOfflineWorkerNodeProcess();
    }
}
