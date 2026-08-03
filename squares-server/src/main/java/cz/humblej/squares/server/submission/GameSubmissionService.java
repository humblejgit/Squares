package cz.humblej.squares.server.submission;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cz.humblej.identity.server.IdentityService;
import cz.humblej.identity.server.ResolvedIdentity;
import cz.humblej.squares.model.GameResult;
import cz.humblej.squares.model.PlayerProfile;
import cz.humblej.squares.model.PlayerResult;
import cz.humblej.squares.server.metadata.MetadataProperties;
import cz.humblej.squares.server.ServerMessageKeys;

@Service
class GameSubmissionService {
    private final GameSubmissionRepository repository;
    private final IdentityService identityService;
    private final ObjectMapper mapper;
    private final Clock clock;
    private final MetadataProperties metadata;

    GameSubmissionService(GameSubmissionRepository repository,
                          IdentityService identityService,
                          ObjectMapper mapper,
                          Clock clock,
                          MetadataProperties metadata) {
        this.repository = repository;
        this.identityService = identityService;
        this.mapper = mapper;
        this.clock = clock;
        this.metadata = metadata;
    }

    @Transactional
    GameSubmissionStatusResponse get(Jwt jwt, UUID gameId) {
        ResolvedIdentity identity = identityService.resolveIdentity(jwt);
        return repository.find(gameId, identity.playerId())
                .map(SubmissionRecord::response)
                .orElseThrow(SubmissionNotFoundException::new);
    }

    @Transactional
    SubmissionMutation put(Jwt jwt, UUID gameId, UUID installationId,
                           PutGameSubmissionRequest request) {
        ResolvedIdentity identity = identityService.resolveIdentityForUpdate(jwt);
        if (!repository.installationExists(identity.accountId(), installationId)) {
            throw new InstallationNotRegisteredException();
        }

        validate(gameId, identity.playerId(), request);
        String payload = json(request);
        byte[] payloadHash = hash(payload);
        String canonicalPayload = canonicalJson(request);
        byte[] canonicalHash = hash(canonicalPayload);

        repository.lockGame(gameId);
        SubmissionRecord existing = repository.find(gameId, identity.playerId()).orElse(null);
        if (existing != null) {
            if (!Arrays.equals(existing.payloadHash(), payloadHash)) {
                throw new SubmissionPayloadConflictException();
            }
            return new SubmissionMutation(existing.response(), false);
        }

        Instant now = Instant.now(clock);
        byte[] storedCanonicalHash = repository.findCanonicalHash(gameId).orElse(null);
        if (storedCanonicalHash == null) {
            repository.insertGame(gameId, canonicalPayload, canonicalHash,
                    "UNVERIFIED", request.startedAt(), request.finishedAt(), now);
        }

        boolean network = request.mode() == GameResult.Mode.NETWORK;
        int peerSubmissionCount = repository.countOtherSubmissions(gameId, identity.playerId());
        boolean peerAlreadySubmitted = peerSubmissionCount > 0;
        if (peerAlreadySubmitted) {
            if (peerSubmissionCount > 1) {
                throw new InvalidSubmissionException(
                        ServerMessageKeys.SUBMISSION_NETWORK_COMPLETE);
            }
            String otherSeat = repository.findOtherSubmittedSeat(gameId, identity.playerId())
                    .orElseThrow(IllegalStateException::new);
            if (otherSeat.equals(request.submittedBySeat().name())) {
                throw new InvalidSubmissionException(
                        ServerMessageKeys.SUBMISSION_SAME_SEAT);
            }
        }
        boolean canonicalMatch = storedCanonicalHash == null
                || Arrays.equals(storedCanonicalHash, canonicalHash);
        String initialStatus = network ? "PENDING_PEER" : "ACCEPTED";
        repository.insertSubmission(gameId, identity.accountId(), identity.playerId(),
                installationId, payload, payloadHash, initialStatus, now);
        for (SubmittedPlayerRequest player : request.players()) {
            repository.insertGamePlayer(gameId, player, now);
        }

        if (peerAlreadySubmitted) {
            if (network && canonicalMatch) {
                repository.updateGameAndSubmissions(
                        gameId, "MATCHED", "PEER_CONFIRMED", now);
            } else {
                repository.updateGameAndSubmissions(
                        gameId, "CONFLICTED", "CONFLICTED", now);
            }
        }

        SubmissionRecord created = repository.find(gameId, identity.playerId())
                .orElseThrow(IllegalStateException::new);
        return new SubmissionMutation(created.response(), true);
    }

    private void validate(UUID gameId, UUID authenticatedPlayerId,
                          PutGameSubmissionRequest request) {
        List<SubmittedPlayerRequest> players = request.players();
        if (request.rulesVersion() != metadata.currentRulesVersion()) {
            throw new InvalidSubmissionException(ServerMessageKeys.SUBMISSION_RULES_VERSION);
        }
        SubmittedPlayerRequest red = player(players, PlayerResult.Seat.RED);
        SubmittedPlayerRequest blue = player(players, PlayerResult.Seat.BLUE);
        SubmittedPlayerRequest submitter = request.submittedBySeat() == PlayerResult.Seat.RED
                ? red : blue;

        if (submitter.playerType() != PlayerResult.PlayerType.PROFILE
                || !authenticatedPlayerId.equals(submitter.playerId())) {
            throw new InvalidSubmissionException(
                    ServerMessageKeys.SUBMISSION_AUTHENTICATED_PLAYER);
        }
        for (SubmittedPlayerRequest player : players) {
            if (player.playerType() != PlayerResult.PlayerType.PROFILE && player.playerId() != null) {
                throw new InvalidSubmissionException(
                        ServerMessageKeys.SUBMISSION_PROFILE_PLAYER_ID);
            }
        }
        if (red.playerId() != null && red.playerId().equals(blue.playerId())) {
            throw new InvalidSubmissionException(ServerMessageKeys.SUBMISSION_SAME_PLAYER);
        }

        try {
            new GameResult(gameId, request.startedAt(), request.finishedAt(),
                    request.mode(), request.finishReason(), request.rows(), request.columns(),
                    request.thinkingTimeLimitSeconds(), request.totalSeconds(),
                    request.randomInitialEdges(), request.cpuDifficulty(),
                    domainPlayer(red), domainPlayer(blue));
        } catch (IllegalArgumentException exception) {
            throw new InvalidSubmissionException(ServerMessageKeys.SUBMISSION_DOMAIN_RULES);
        }
    }

    private static SubmittedPlayerRequest player(
            List<SubmittedPlayerRequest> players, PlayerResult.Seat seat) {
        SubmittedPlayerRequest match = null;
        for (SubmittedPlayerRequest player : players) {
            if (player.seat() == seat) {
                if (match != null) {
                    throw new InvalidSubmissionException(ServerMessageKeys.SUBMISSION_UNIQUE_SEATS);
                }
                match = player;
            }
        }
        if (match == null) {
            throw new InvalidSubmissionException(ServerMessageKeys.SUBMISSION_BOTH_SEATS);
        }
        return match;
    }

    private static PlayerResult domainPlayer(SubmittedPlayerRequest player) {
        if (player.playerType() == PlayerResult.PlayerType.PROFILE) {
            PlayerProfile profile = new PlayerProfile(
                    player.playerId() == null ? UUID.randomUUID() : player.playerId(),
                    player.displayNameSnapshot(), Instant.EPOCH, false);
            return PlayerResult.forProfile(player.seat(), profile, player.score(),
                    player.thinkingSeconds(), player.outcome());
        }
        if (player.playerType() == PlayerResult.PlayerType.CPU) {
            return PlayerResult.computer(player.seat(), player.displayNameSnapshot(),
                    player.score(), player.thinkingSeconds(), player.outcome());
        }
        return PlayerResult.guest(player.seat(), player.displayNameSnapshot(),
                player.score(), player.thinkingSeconds(), player.outcome());
    }

    private String json(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Submission could not be serialized.", exception);
        }
    }

    private String canonicalJson(PutGameSubmissionRequest request) {
        JsonNode tree = mapper.valueToTree(request);
        ObjectNode canonical = (ObjectNode) tree;
        canonical.remove("submittedBySeat");
        ArrayNode players = (ArrayNode) canonical.get("players");
        List<ObjectNode> ordered = new ArrayList<>();
        players.forEach(node -> {
            ObjectNode player = (ObjectNode) node;
            player.remove("playerId");
            ordered.add(player);
        });
        ordered.sort(Comparator.comparing(node -> node.get("seat").asText()));
        players.removeAll();
        ordered.forEach(players::add);
        return json(canonical);
    }

    private static byte[] hash(String value) {
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }
}
