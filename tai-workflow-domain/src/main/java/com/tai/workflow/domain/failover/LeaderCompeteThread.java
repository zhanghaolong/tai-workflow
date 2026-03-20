package com.tai.workflow.domain.failover;

import com.tai.workflow.repository.entity.WorkflowNodeLeaderEntity;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

/**
 * @author zhanghaolong1989@163.com
 */
@Slf4j
public class LeaderCompeteThread implements Runnable {
    private final RelationshipListener relationshipListener;
    private final FailoverManagerBaseOnDatabase failoverManagerBaseOnDatabase;
    private final String selfNodeId;
    private volatile String currentLeaderId;
    private AbstractState noLeaderState = new NoLeaderState();

    private AbstractState leaderState = new LeaderState();
    private AbstractState workerState = new WorkerState();

    private volatile AbstractState state;

    public LeaderCompeteThread(String selfNodeId, RelationshipListener relationshipListener,
            FailoverManagerBaseOnDatabase failoverManagerBaseOnDatabase) {
        this.selfNodeId = selfNodeId;
        this.relationshipListener = relationshipListener;
        this.failoverManagerBaseOnDatabase = failoverManagerBaseOnDatabase;
    }

    public void run() {
        try {
            if (leaderExpired()) {
                state = noLeaderState;
            } else {
                WorkflowNodeLeaderEntity leader = failoverManagerBaseOnDatabase.queryLeader();
                if (Objects.nonNull(leader)) {
                    this.currentLeaderId = leader.getLeaderNode();
                    if (StringUtils.equals(currentLeaderId, selfNodeId)) {
                        this.state = leaderState;
                    } else {
                        this.state = workerState;
                    }
                } else {
                    state = noLeaderState;
                }
            }

            this.state.handle();
        } catch (Throwable throwable) {
            log.error("it occurs error during leader compete", throwable);
        }
    }

    private boolean doLeaderCompeteByInsertWithSameUniqueKey() {
        return failoverManagerBaseOnDatabase.tryInsertLeader(selfNodeId);
    }

    private boolean leaderExpired() {
        return failoverManagerBaseOnDatabase.deleteExpiredLeader(failoverManagerBaseOnDatabase.getWorkflowConfiguration().getNamespace());
    }

    abstract static class AbstractState {
        protected abstract void handle();
    }

    /**
     * worker 状态处理
     */
    final class WorkerState extends AbstractState {
        protected void handle() {
            if (leaderExpired()) {
                log.info("has found leader {} expired", currentLeaderId);
                state = new NoLeaderState();
            }
        }
    }

    /**
     * leader 状态处理
     */
    final class LeaderState extends AbstractState {
        protected void handle() {
            boolean refreshSuccess = BooleanUtils.toBoolean(failoverManagerBaseOnDatabase.refreshLeader(selfNodeId));
            if (!refreshSuccess) {
                log.info("{} has lost leader", selfNodeId);
                state = new WorkerState();
                relationshipListener.lostLeader();
            }
        }
    }

    /**
     * 刚开始没有 leader 时的状态处理
     */
    final class NoLeaderState extends AbstractState {
        protected void handle() {
            boolean competeSuccess = doLeaderCompeteByInsertWithSameUniqueKey();
            if (competeSuccess) {
                log.info("{} has become leader", selfNodeId);
                relationshipListener.becomeLeader();
                state = new LeaderState();
            } else {
                state = new WorkerState();
            }
        }
    }
}
