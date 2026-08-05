package cz.humblej.squares.sync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import cz.humblej.identity.client.AuthenticationException;
import cz.humblej.identity.model.InstallationInfo;
import cz.humblej.squares.auth.GameSubmissionStatus;
import cz.humblej.squares.codec.GameResultCodec;
import cz.humblej.squares.model.GameResult;
import cz.humblej.squares.model.PlayerProfile;
import cz.humblej.squares.model.PlayerResult;
import cz.humblej.squares.persistence.GameSyncStore;
import cz.humblej.squares.persistence.OutboxEvent;
import cz.humblej.squares.persistence.StorageException;
import cz.humblej.squares.persistence.SyncSummary;

public class GameResultSyncServiceTest {
    private static final UUID PLAYER_ID = UUID.fromString(
            "00000000-0000-0000-0000-000000000001");
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-08-05T18:00:00Z"), ZoneOffset.UTC);

    @Test
    public void backgroundTriggerDuringSynchronizationRunsAnotherPass() throws Exception {
        RecordingAccountGateway account = new RecordingAccountGateway(true);
        RecordingStore store = new RecordingStore();
        GameResultSyncService service = new GameResultSyncService(
                account, store, installation(), CLOCK);
        store.firstSummaryHook = service::synchronizeInBackground;

        service.synchronizeInBackground();

        assertTrue("The follow-up synchronization did not finish in time.",
                store.secondPass.await(5, TimeUnit.SECONDS));
        assertEquals(2, account.playerRequests.get());
        assertEquals(2, account.installationRegistrations.get());
        assertEquals(2, store.recoveries.get());
        assertEquals(2, store.summaries.get());
    }

    @Test
    public void backgroundSynchronizationDoesNotStartWithoutSession() {
        RecordingAccountGateway account = new RecordingAccountGateway(false);
        RecordingStore store = new RecordingStore();
        GameResultSyncService service = new GameResultSyncService(
                account, store, installation(), CLOCK);

        service.synchronizeInBackground();

        assertEquals(0, account.playerRequests.get());
        assertEquals(0, store.recoveries.get());
    }

    @Test
    public void transientFailureMarksResultForRetryAfterOneMinute() throws Exception {
        AuthenticationException failure = new AuthenticationException(
                "server offline", null, false, 503, "service-unavailable");
        RecordingAccountGateway account = new RecordingAccountGateway(true);
        account.submissionFailure = failure;
        RecordingStore store = new RecordingStore();
        GameResult game = gameResult();
        OutboxEvent event = new OutboxEvent(
                game.gameId(), GameResultCodec.encode(game), PlayerResult.Seat.RED, 1);
        store.nextEvent = event;
        GameResultSyncService service = new GameResultSyncService(
                account, store, installation(), CLOCK);

        try {
            service.synchronizeNow(false);
            throw new AssertionError("A transient submission failure was expected.");
        } catch (AuthenticationException exception) {
            assertSame(failure, exception);
        }

        assertEquals(event.eventId(), store.retriedEventId);
        assertEquals("server offline", store.retryError);
        assertEquals(Instant.now(CLOCK).plusSeconds(60), store.retryAt);
    }

    private static InstallationInfo installation() {
        return new InstallationInfo(
                UUID.fromString("00000000-0000-0000-0000-000000000002"),
                "WINDOWS", "4.5.0", "4.5.0", "cs-CZ");
    }

    private static GameResult gameResult() {
        PlayerProfile redProfile = new PlayerProfile(
                UUID.fromString("00000000-0000-0000-0000-000000000003"),
                "Jana", Instant.parse("2026-01-01T00:00:00Z"), false);
        return new GameResult(
                UUID.fromString("00000000-0000-0000-0000-000000000004"),
                Instant.parse("2026-08-05T17:58:00Z"),
                Instant.parse("2026-08-05T18:00:00Z"),
                GameResult.Mode.LOCAL, GameResult.FinishReason.BOARD_FULL,
                5, 5, 120, 120, false, null,
                PlayerResult.forProfile(PlayerResult.Seat.RED, redProfile,
                        8, 50, PlayerResult.Outcome.WIN),
                PlayerResult.guest(PlayerResult.Seat.BLUE, "Petr",
                        4, 60, PlayerResult.Outcome.LOSS));
    }

    private static final class RecordingAccountGateway
            implements GameResultSyncService.AccountGateway {
        private final boolean session;
        private final AtomicInteger playerRequests = new AtomicInteger();
        private final AtomicInteger installationRegistrations = new AtomicInteger();
        private volatile AuthenticationException submissionFailure;

        private RecordingAccountGateway(boolean session) {
            this.session = session;
        }

        @Override
        public boolean hasSession() {
            return session;
        }

        @Override
        public UUID getPlayerId() {
            playerRequests.incrementAndGet();
            return PLAYER_ID;
        }

        @Override
        public void registerInstallation(InstallationInfo installation) {
            installationRegistrations.incrementAndGet();
        }

        @Override
        public GameSubmissionStatus submitGame(
                GameResult game, PlayerResult.Seat submittedBySeat,
                UUID playerId, UUID installationId) throws AuthenticationException {
            if (submissionFailure != null) {
                throw submissionFailure;
            }
            throw new AssertionError("No result should be submitted in this test.");
        }

        @Override
        public GameSubmissionStatus getGameSubmission(UUID gameId)
                throws AuthenticationException {
            throw new AssertionError("No peer status should be requested in this test.");
        }
    }

    private static final class RecordingStore implements GameSyncStore {
        private final AtomicInteger recoveries = new AtomicInteger();
        private final AtomicInteger summaries = new AtomicInteger();
        private final CountDownLatch secondPass = new CountDownLatch(1);
        private volatile Runnable firstSummaryHook;
        private volatile OutboxEvent nextEvent;
        private volatile UUID retriedEventId;
        private volatile String retryError;
        private volatile Instant retryAt;

        @Override
        public void recoverInterruptedSync() {
            recoveries.incrementAndGet();
        }

        @Override
        public OutboxEvent claimNext(UUID playerId, Instant now, boolean includeDeferred) {
            OutboxEvent event = nextEvent;
            nextEvent = null;
            return event;
        }

        @Override
        public void markSent(UUID eventId, GameSubmissionStatus status, Instant syncedAt) {
            throw new AssertionError("No result should be marked as sent in this test.");
        }

        @Override
        public void markRetry(UUID eventId, String error, Instant nextAttemptAt) {
            retriedEventId = eventId;
            retryError = error;
            retryAt = nextAttemptAt;
        }

        @Override
        public void markDead(UUID eventId, String error) {
            throw new AssertionError("No result should be marked dead in this test.");
        }

        @Override
        public List<UUID> findPendingPeerSubmissions(UUID playerId, int limit) {
            return Collections.emptyList();
        }

        @Override
        public SyncSummary summary(UUID playerId) throws StorageException {
            int pass = summaries.incrementAndGet();
            if (pass == 1 && firstSummaryHook != null) {
                firstSummaryHook.run();
            } else if (pass == 2) {
                secondPass.countDown();
            }
            return new SyncSummary(0, 0, 0, 0);
        }
    }
}
