package coop.local.database.job;

public enum JobStatus {
    CREATED(1000),
    RESERVED(2000),
    PENDING(3000),
    WAITING_FOR_ACK(4000),
    WAITING_FOR_COMPLETE(5000),
    COMPLETE(9000),
    FAILED(9000);

    private final int rank;
    JobStatus(int rank) {
        this.rank = rank;
    }

    public int rank() {
        return rank;
    }
}
