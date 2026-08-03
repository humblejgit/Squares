package cz.humblej.squares.persistence;

public final class SyncSummary {
    private final long pending;
    private final long sending;
    private final long sent;
    private final long failed;
    private final long pendingPeer;
    private final long matched;
    private final long conflicted;

    public SyncSummary(long pending, long sending, long sent, long failed) {
        this(pending, sending, sent, failed, 0, 0, 0);
    }

    public SyncSummary(long pending, long sending, long sent, long failed,
                       long pendingPeer, long matched, long conflicted) {
        this.pending = pending;
        this.sending = sending;
        this.sent = sent;
        this.failed = failed;
        this.pendingPeer = pendingPeer;
        this.matched = matched;
        this.conflicted = conflicted;
    }

    public long pending() { return pending; }
    public long sending() { return sending; }
    public long sent() { return sent; }
    public long failed() { return failed; }
    public long pendingPeer() { return pendingPeer; }
    public long matched() { return matched; }
    public long conflicted() { return conflicted; }
}
