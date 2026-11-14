package az.baku.managesystemback.entity;

/**
 * @author Fatima Sultanova
 */
public enum DeploymentStatus {
    DRAFT,
    QUEUED,
    BUILDING,
    LIVE,
    FAILED,
    STOPPED,
    SUPERSEDED;

    public boolean isInProgress() {
        return this == QUEUED || this == BUILDING;
    }

    public boolean isFinished() {
        return this == LIVE || this == FAILED || this == STOPPED || this == SUPERSEDED;
    }
}
