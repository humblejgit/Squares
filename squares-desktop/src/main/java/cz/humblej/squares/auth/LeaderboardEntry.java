package cz.humblej.squares.auth;

public final class LeaderboardEntry {
    private final long rank;
    private final OnlinePlayer player;
    private final LeaderboardStatistics statistics;

    public LeaderboardEntry(
            long rank, OnlinePlayer player, LeaderboardStatistics statistics) {
        this.rank = rank;
        this.player = player;
        this.statistics = statistics;
    }

    public long rank() { return rank; }
    public OnlinePlayer player() { return player; }
    public LeaderboardStatistics statistics() { return statistics; }
}
