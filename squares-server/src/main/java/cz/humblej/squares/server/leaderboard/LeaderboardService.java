package cz.humblej.squares.server.leaderboard;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cz.humblej.identity.server.IdentityService;

@Service
class LeaderboardService {
    private static final String CURSOR_VERSION = "v1:";

    private final LeaderboardRepository repository;
    private final IdentityService identities;
    private final Clock clock;

    LeaderboardService(
            LeaderboardRepository repository,
            IdentityService identities,
            Clock clock) {
        this.repository = repository;
        this.identities = identities;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    LeaderboardPageResponse getCasualPage(int limit, String cursor) {
        long afterRank = decodeCursor(cursor);
        List<LeaderboardRow> rows = repository.findCasualPage(afterRank, limit + 1);
        boolean hasNext = rows.size() > limit;
        List<LeaderboardEntryResponse> entries = new ArrayList<>();
        for (int index = 0; index < Math.min(limit, rows.size()); index++) {
            entries.add(rows.get(index).response());
        }
        String nextCursor = hasNext
                ? encodeCursor(rows.get(limit - 1).rank())
                : null;
        return new LeaderboardPageResponse(
                "casual", "TOTAL_SCORE", List.copyOf(entries),
                nextCursor, Instant.now(clock));
    }

    @Transactional(readOnly = true)
    Optional<LeaderboardEntryResponse> getMyCasualEntry(Jwt jwt) {
        return repository.findCasualPlayer(
                identities.resolveIdentity(jwt).playerId()).map(LeaderboardRow::response);
    }

    private static String encodeCursor(long rank) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(
                (CURSOR_VERSION + rank).getBytes(StandardCharsets.US_ASCII));
    }

    private static long decodeCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return 0;
        }
        if (cursor.length() > 512) {
            throw new InvalidLeaderboardCursorException();
        }
        try {
            String decoded = new String(
                    Base64.getUrlDecoder().decode(cursor), StandardCharsets.US_ASCII);
            if (!decoded.startsWith(CURSOR_VERSION)) {
                throw new IllegalArgumentException();
            }
            long rank = Long.parseLong(decoded.substring(CURSOR_VERSION.length()));
            if (rank < 1) {
                throw new IllegalArgumentException();
            }
            return rank;
        } catch (IllegalArgumentException exception) {
            throw new InvalidLeaderboardCursorException();
        }
    }
}
