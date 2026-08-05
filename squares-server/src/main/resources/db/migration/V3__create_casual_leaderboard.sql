CREATE VIEW casual_leaderboard AS
WITH statistics AS (
    SELECT p.player_id,
           p.handle,
           p.display_name,
           p.revision,
           p.created_at,
           COUNT(*) AS games,
           COUNT(*) FILTER (WHERE gp.outcome = 'WIN') AS wins,
           COUNT(*) FILTER (WHERE gp.outcome = 'DRAW') AS draws,
           COUNT(*) FILTER (WHERE gp.outcome = 'LOSS') AS losses,
           SUM(gp.score) AS total_score
    FROM game_players gp
    JOIN games g ON g.game_id = gp.game_id
    JOIN players p ON p.player_id = gp.player_id
    WHERE NOT g.ranked
      AND g.verification_status <> 'CONFLICTED'
    GROUP BY p.player_id, p.handle, p.display_name, p.revision, p.created_at
)
SELECT ROW_NUMBER() OVER (
           ORDER BY total_score DESC,
                    wins DESC,
                    games DESC,
                    lower(handle),
                    player_id
       ) AS rank,
       player_id,
       handle,
       display_name,
       revision,
       created_at,
       games,
       wins,
       draws,
       losses,
       total_score
FROM statistics;

COMMENT ON VIEW casual_leaderboard IS
    'Current casual ranking derived from non-conflicted, non-ranked game results.';
