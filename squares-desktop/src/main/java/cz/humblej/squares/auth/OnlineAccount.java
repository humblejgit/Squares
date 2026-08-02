package cz.humblej.squares.auth;

import java.util.UUID;

public final class OnlineAccount {
    private final String accountStatus;
    private final UUID playerId;
    private final boolean onboardingRequired;
    private final OnlinePlayer player;

    OnlineAccount(String accountStatus, UUID playerId,
                  boolean onboardingRequired, OnlinePlayer player) {
        this.accountStatus = accountStatus;
        this.playerId = playerId;
        this.onboardingRequired = onboardingRequired;
        this.player = player;
    }

    public String accountStatus() {
        return accountStatus;
    }

    public boolean onboardingRequired() {
        return onboardingRequired;
    }

    public UUID playerId() {
        return playerId;
    }

    public OnlinePlayer player() {
        return player;
    }
}
