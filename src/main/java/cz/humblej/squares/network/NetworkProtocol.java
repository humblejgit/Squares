package cz.humblej.squares.network;

import cz.humblej.squares.app.BuildInfo;
import cz.humblej.squares.model.PlayerProfile;
import cz.humblej.squares.ui.Messages;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

final class NetworkProtocol {
    private static final String PROTOCOL_ID = "profiles-and-results-1";

    private NetworkProtocol() {
    }

    static BufferedReader reader(Socket socket) throws IOException {
        return new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
    }

    static PrintWriter writer(Socket socket) throws IOException {
        return new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
    }

    static String buildId() {
        return BuildInfo.buildId() + "/" + PROTOCOL_ID;
    }

    static String encodeValue(String message) {
        return Base64.getEncoder().encodeToString(message.getBytes(StandardCharsets.UTF_8));
    }

    static String decodeValue(String encoded) {
        try {
            return new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exception) {
            return "";
        }
    }

    static String encodeProfile(PlayerProfile profile) {
        return "PROFILE " + profile.id() + " " + profile.createdAt().toEpochMilli() + " "
                + encodeValue(profile.displayName());
    }

    static PlayerProfile decodeProfile(String line) {
        if (line == null || !line.startsWith("PROFILE ")) {
            throw new IllegalArgumentException(Messages.NETWORK_INCOMPATIBLE_PROTOCOL);
        }

        String[] parts = line.split(" ", 4);
        if (parts.length != 4) {
            throw new IllegalArgumentException(Messages.NETWORK_INCOMPATIBLE_PROTOCOL);
        }

        return new PlayerProfile(UUID.fromString(parts[1]), decodeValue(parts[3]),
                Instant.ofEpochMilli(Long.parseLong(parts[2])), false);
    }

    static String edgeType(boolean horizontal) {
        return horizontal ? "H" : "V";
    }
}
