package cz.humblej.squares.sync;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.UUID;

import cz.humblej.identity.client.AuthenticationException;
import cz.humblej.identity.model.InstallationInfo;
import cz.humblej.squares.auth.GameSubmissionStatus;
import cz.humblej.squares.auth.OnlineAccount;
import cz.humblej.squares.auth.OnlineAccountService;
import cz.humblej.squares.codec.GameResultCodec;
import cz.humblej.squares.model.GameResult;
import cz.humblej.squares.model.PlayerResult;
import cz.humblej.squares.persistence.GameSyncStore;
import cz.humblej.squares.persistence.OutboxEvent;
import cz.humblej.squares.persistence.StorageException;
import cz.humblej.squares.persistence.SyncSummary;

public final class GameResultSyncService {
    private static final long MAX_RETRY_SECONDS = 3600L;

    private final AccountGateway accountService;
    private final GameSyncStore store;
    private final InstallationInfo installation;
    private final Clock clock;
    private final ExecutorService executor;
    private final AtomicBoolean backgroundRunning = new AtomicBoolean();
    private final AtomicBoolean backgroundRequested = new AtomicBoolean();

    public GameResultSyncService(OnlineAccountService accountService,
                                 GameSyncStore store,
                                 InstallationInfo installation) {
        this(new OnlineAccountGateway(accountService), store, installation, Clock.systemUTC());
    }

    GameResultSyncService(AccountGateway accountService,
                          GameSyncStore store,
                          InstallationInfo installation,
                          Clock clock) {
        this.accountService = accountService;
        this.store = store;
        this.installation = installation;
        this.clock = clock;
        this.executor = Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable, "squares-result-sync");
                thread.setDaemon(true);
                return thread;
            }
        });
    }

    public void synchronizeInBackground() {
        if (!accountService.hasSession()) {
            return;
        }
        backgroundRequested.set(true);
        startBackgroundWorker();
    }

    private void startBackgroundWorker() {
        if (!backgroundRunning.compareAndSet(false, true)) {
            return;
        }
        executor.execute(() -> {
            try {
                while (backgroundRequested.getAndSet(false)) {
                    try {
                        synchronizeNow(false);
                    } catch (AuthenticationException | StorageException ignored) {
                        // The outbox retains the error and retries on the next trigger.
                    }
                }
            } finally {
                backgroundRunning.set(false);
                if (backgroundRequested.get()) {
                    startBackgroundWorker();
                }
            }
        });
    }

    public synchronized SyncSummary synchronizeNow(boolean includeDeferred)
            throws AuthenticationException, StorageException {
        UUID playerId = accountService.getPlayerId();
        accountService.registerInstallation(installation);
        store.recoverInterruptedSync();

        while (true) {
            OutboxEvent event = store.claimNext(
                    playerId, Instant.now(clock), includeDeferred);
            if (event == null) {
                break;
            }

            GameResult game;
            try {
                game = GameResultCodec.decode(event.payload());
            } catch (IOException exception) {
                store.markDead(event.eventId(), exception.getMessage());
                continue;
            }

            try {
                GameSubmissionStatus status = accountService.submitGame(
                        game, event.submittedBySeat(), playerId,
                        installation.installationId());
                store.markSent(event.eventId(), status, Instant.now(clock));
            } catch (AuthenticationException exception) {
                if (isPermanent(exception)) {
                    store.markDead(event.eventId(), exception.getMessage());
                    continue;
                }
                store.markRetry(event.eventId(), exception.getMessage(),
                        nextAttempt(event.attempts()));
                throw exception;
            }
        }
        for (UUID gameId : store.findPendingPeerSubmissions(playerId, 25)) {
            GameSubmissionStatus status = accountService.getGameSubmission(gameId);
            store.markSent(gameId, status, Instant.now(clock));
        }
        return store.summary(playerId);
    }

    public SyncSummary summary(OnlineAccount account) throws StorageException {
        return store.summary(account.playerId());
    }

    private Instant nextAttempt(int attempts) {
        int exponent = Math.max(0, Math.min(attempts - 1, 6));
        long seconds = Math.min(MAX_RETRY_SECONDS, 60L << exponent);
        return Instant.now(clock).plusSeconds(seconds);
    }

    private static boolean isPermanent(AuthenticationException exception) {
        int status = exception.httpStatus();
        return status >= 400 && status < 500 && status != 408 && status != 429
                && status != 401;
    }

    interface AccountGateway {
        boolean hasSession();

        UUID getPlayerId() throws AuthenticationException;

        void registerInstallation(InstallationInfo installation)
                throws AuthenticationException;

        GameSubmissionStatus submitGame(GameResult game,
                                        PlayerResult.Seat submittedBySeat,
                                        UUID playerId, UUID installationId)
                throws AuthenticationException;

        GameSubmissionStatus getGameSubmission(UUID gameId)
                throws AuthenticationException;
    }

    private static final class OnlineAccountGateway implements AccountGateway {
        private final OnlineAccountService service;

        private OnlineAccountGateway(OnlineAccountService service) {
            this.service = service;
        }

        @Override
        public boolean hasSession() {
            return service.hasSession();
        }

        @Override
        public UUID getPlayerId() throws AuthenticationException {
            return service.getMe().playerId();
        }

        @Override
        public void registerInstallation(InstallationInfo installation)
                throws AuthenticationException {
            service.registerInstallation(installation);
        }

        @Override
        public GameSubmissionStatus submitGame(
                GameResult game,
                PlayerResult.Seat submittedBySeat,
                UUID playerId, UUID installationId) throws AuthenticationException {
            return service.submitGame(game, submittedBySeat, playerId, installationId);
        }

        @Override
        public GameSubmissionStatus getGameSubmission(UUID gameId)
                throws AuthenticationException {
            return service.getGameSubmission(gameId);
        }
    }
}
