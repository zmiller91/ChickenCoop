package coop.local.database;

public enum JobStatus {
    CREATED(1000),
    PENDING(2000),
    WAITING_FOR_ACK(3000),
    WAITING_FOR_COMPLETE(4000),
    COMPLETE(5000),
    FAILED(5000);

    private final int rank;
    JobStatus(int rank) {
        this.rank = rank;
    }

    public int rank() {
        return rank;
    }
}
