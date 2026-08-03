package cz.humblej.squares.server;

/** Stable keys for messages exposed by the Squares REST API. */
public final class ServerMessageKeys {
    public static final String HANDLE_UNAVAILABLE_TITLE =
            "squares.api.handle-unavailable.title";
    public static final String HANDLE_UNAVAILABLE_DETAIL =
            "squares.api.handle-unavailable.detail";
    public static final String SUBMISSION_CONFLICT_TITLE =
            "squares.api.submission-conflict.title";
    public static final String SUBMISSION_CONFLICT_DETAIL =
            "squares.api.submission-conflict.detail";
    public static final String INVALID_SUBMISSION_TITLE =
            "squares.api.invalid-submission.title";
    public static final String INSTALLATION_NOT_REGISTERED_TITLE =
            "squares.api.installation-not-registered.title";
    public static final String INSTALLATION_NOT_REGISTERED_DETAIL =
            "squares.api.installation-not-registered.detail";
    public static final String SUBMISSION_NOT_FOUND_TITLE =
            "squares.api.submission-not-found.title";
    public static final String SUBMISSION_NOT_FOUND_DETAIL =
            "squares.api.submission-not-found.detail";
    public static final String SUBMISSION_NETWORK_COMPLETE =
            "squares.api.invalid-submission.network-complete";
    public static final String SUBMISSION_SAME_SEAT =
            "squares.api.invalid-submission.same-seat";
    public static final String SUBMISSION_RULES_VERSION =
            "squares.api.invalid-submission.rules-version";
    public static final String SUBMISSION_AUTHENTICATED_PLAYER =
            "squares.api.invalid-submission.authenticated-player";
    public static final String SUBMISSION_PROFILE_PLAYER_ID =
            "squares.api.invalid-submission.profile-player-id";
    public static final String SUBMISSION_SAME_PLAYER =
            "squares.api.invalid-submission.same-player";
    public static final String SUBMISSION_UNIQUE_SEATS =
            "squares.api.invalid-submission.unique-seats";
    public static final String SUBMISSION_BOTH_SEATS =
            "squares.api.invalid-submission.both-seats";
    public static final String SUBMISSION_DOMAIN_RULES =
            "squares.api.invalid-submission.domain-rules";

    private ServerMessageKeys() {
    }
}
