package cz.humblej.squares.server.leaderboard;

public record StatisticsResponse(
        long games,
        long wins,
        long draws,
        long losses,
        long totalScore,
        double winPercentage) {
    static StatisticsResponse of(
            long games, long wins, long draws, long losses, long totalScore) {
        double winPercentage = games == 0 ? 0.0 : wins * 100.0 / games;
        return new StatisticsResponse(
                games, wins, draws, losses, totalScore, winPercentage);
    }
}
