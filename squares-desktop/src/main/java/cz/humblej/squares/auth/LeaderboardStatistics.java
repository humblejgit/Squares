package cz.humblej.squares.auth;

public final class LeaderboardStatistics {
    private final long games;
    private final long wins;
    private final long draws;
    private final long losses;
    private final long totalScore;
    private final double winPercentage;

    public LeaderboardStatistics(
            long games, long wins, long draws, long losses,
            long totalScore, double winPercentage) {
        this.games = games;
        this.wins = wins;
        this.draws = draws;
        this.losses = losses;
        this.totalScore = totalScore;
        this.winPercentage = winPercentage;
    }

    public long games() { return games; }
    public long wins() { return wins; }
    public long draws() { return draws; }
    public long losses() { return losses; }
    public long totalScore() { return totalScore; }
    public double winPercentage() { return winPercentage; }
}
