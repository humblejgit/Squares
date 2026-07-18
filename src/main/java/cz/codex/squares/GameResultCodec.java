package cz.codex.squares;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

final class GameResultCodec {
    private static final int FORMAT_VERSION = 1;

    private GameResultCodec() {
    }

    static String encode(GameResult result) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            output.writeInt(FORMAT_VERSION);
            writeUuid(output, result.gameId());
            output.writeLong(result.startedAt().toEpochMilli());
            output.writeLong(result.finishedAt().toEpochMilli());
            output.writeUTF(result.mode().name());
            output.writeUTF(result.finishReason().name());
            output.writeInt(result.rows());
            output.writeInt(result.columns());
            output.writeInt(result.thinkingTimeLimitSeconds());
            output.writeInt(result.totalSeconds());
            output.writeBoolean(result.randomInitialEdges());
            output.writeBoolean(result.cpuDifficulty() != null);
            if (result.cpuDifficulty() != null) {
                output.writeUTF(result.cpuDifficulty().name());
            }
            writePlayer(output, result.redPlayer());
            writePlayer(output, result.bluePlayer());
        }
        return Base64.getEncoder().encodeToString(bytes.toByteArray());
    }

    static GameResult decode(String encoded) throws IOException {
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(encoded);
        } catch (IllegalArgumentException exception) {
            throw new IOException("Neplatné kódování výsledku hry.", exception);
        }

        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes))) {
            int version = input.readInt();
            if (version != FORMAT_VERSION) {
                throw new IOException("Nepodporovaná verze výsledku hry: " + version);
            }

            UUID gameId = readUuid(input);
            Instant startedAt = Instant.ofEpochMilli(input.readLong());
            Instant finishedAt = Instant.ofEpochMilli(input.readLong());
            GameResult.Mode mode = GameResult.Mode.valueOf(input.readUTF());
            GameResult.FinishReason finishReason = GameResult.FinishReason.valueOf(input.readUTF());
            int rows = input.readInt();
            int columns = input.readInt();
            int thinkingTimeLimitSeconds = input.readInt();
            int totalSeconds = input.readInt();
            boolean randomInitialEdges = input.readBoolean();
            GameResult.CpuDifficulty cpuDifficulty = input.readBoolean()
                    ? GameResult.CpuDifficulty.valueOf(input.readUTF())
                    : null;
            PlayerResult red = readPlayer(input, PlayerResult.Seat.RED);
            PlayerResult blue = readPlayer(input, PlayerResult.Seat.BLUE);

            if (input.available() != 0) {
                throw new IOException("Výsledek hry obsahuje neočekávaná data.");
            }

            return new GameResult(gameId, startedAt, finishedAt, mode, finishReason,
                    rows, columns, thinkingTimeLimitSeconds, totalSeconds,
                    randomInitialEdges, cpuDifficulty, red, blue);
        } catch (IllegalArgumentException exception) {
            throw new IOException("Neplatný strukturovaný výsledek hry.", exception);
        }
    }

    private static void writePlayer(DataOutputStream output, PlayerResult player) throws IOException {
        output.writeUTF(player.playerType().name());
        output.writeBoolean(player.hasProfile());
        if (player.hasProfile()) {
            writeUuid(output, player.profile().id());
            output.writeLong(player.profile().createdAt().toEpochMilli());
        }
        output.writeUTF(player.displayName());
        output.writeInt(player.score());
        output.writeInt(player.thinkingSeconds());
        output.writeUTF(player.outcome().name());
    }

    private static PlayerResult readPlayer(DataInputStream input, PlayerResult.Seat seat) throws IOException {
        PlayerResult.PlayerType type = PlayerResult.PlayerType.valueOf(input.readUTF());
        boolean hasProfile = input.readBoolean();
        UUID profileId = null;
        Instant profileCreatedAt = null;

        if (hasProfile) {
            profileId = readUuid(input);
            profileCreatedAt = Instant.ofEpochMilli(input.readLong());
        }

        String displayName = input.readUTF();
        int score = input.readInt();
        int thinkingSeconds = input.readInt();
        PlayerResult.Outcome outcome = PlayerResult.Outcome.valueOf(input.readUTF());

        if (type == PlayerResult.PlayerType.PROFILE && hasProfile) {
            PlayerProfile profile = new PlayerProfile(profileId, displayName, profileCreatedAt, false);
            return PlayerResult.forProfile(seat, profile, score, thinkingSeconds, outcome);
        }

        if (type == PlayerResult.PlayerType.GUEST && !hasProfile) {
            return PlayerResult.guest(seat, displayName, score, thinkingSeconds, outcome);
        }

        if (type == PlayerResult.PlayerType.CPU && !hasProfile) {
            return PlayerResult.computer(seat, displayName, score, thinkingSeconds, outcome);
        }

        throw new IOException("Neplatná identita hráče ve výsledku hry.");
    }

    private static void writeUuid(DataOutputStream output, UUID value) throws IOException {
        output.writeLong(value.getMostSignificantBits());
        output.writeLong(value.getLeastSignificantBits());
    }

    private static UUID readUuid(DataInputStream input) throws IOException {
        return new UUID(input.readLong(), input.readLong());
    }
}
