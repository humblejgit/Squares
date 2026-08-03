package cz.humblej.squares.persistence;

import java.util.UUID;

import cz.humblej.squares.model.PlayerResult;

public final class OutboxEvent {
    private final UUID eventId;
    private final String payload;
    private final PlayerResult.Seat submittedBySeat;
    private final int attempts;

    OutboxEvent(UUID eventId, String payload,
                PlayerResult.Seat submittedBySeat, int attempts) {
        this.eventId = eventId;
        this.payload = payload;
        this.submittedBySeat = submittedBySeat;
        this.attempts = attempts;
    }

    public UUID eventId() { return eventId; }
    public String payload() { return payload; }
    public PlayerResult.Seat submittedBySeat() { return submittedBySeat; }
    public int attempts() { return attempts; }
}
