package cz.humblej.squares.auth;

public final class OnlineAccount {
    private final String accountStatus;
    private final boolean onboardingRequired;
    private final OnlinePlayer player;

    OnlineAccount(String accountStatus, boolean onboardingRequired, OnlinePlayer player) {
        this.accountStatus = accountStatus;
        this.onboardingRequired = onboardingRequired;
        this.player = player;
    }

    public String accountStatus() {
        return accountStatus;
    }

    public boolean onboardingRequired() {
        return onboardingRequired;
    }

    public OnlinePlayer player() {
        return player;
    }
}
