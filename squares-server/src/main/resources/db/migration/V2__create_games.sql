CREATE TABLE game_submissions (
    submission_id uuid PRIMARY KEY,
    game_id uuid NOT NULL,
    submitted_by_account_id uuid NOT NULL REFERENCES accounts (account_id),
    submitted_by_player_id uuid NOT NULL REFERENCES players (player_id),
    installation_id uuid NOT NULL,
    payload jsonb NOT NULL,
    payload_hash bytea NOT NULL,
    status varchar(32) NOT NULL,
    received_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    UNIQUE (game_id, submitted_by_player_id),
    FOREIGN KEY (submitted_by_account_id, installation_id)
        REFERENCES installations (account_id, installation_id),
    CONSTRAINT game_submissions_status_check
        CHECK (status IN ('ACCEPTED', 'PENDING_PEER', 'MATCHED', 'CONFLICTED')),
    CONSTRAINT game_submissions_payload_hash_check
        CHECK (octet_length(payload_hash) = 32)
);

CREATE INDEX game_submissions_game_idx
    ON game_submissions (game_id);

CREATE INDEX game_submissions_player_received_idx
    ON game_submissions (submitted_by_player_id, received_at DESC);

CREATE TABLE games (
    game_id uuid PRIMARY KEY,
    canonical_payload jsonb NOT NULL,
    canonical_payload_hash bytea NOT NULL,
    verification_status varchar(32) NOT NULL,
    ranked boolean NOT NULL DEFAULT false,
    started_at timestamptz NOT NULL,
    finished_at timestamptz NOT NULL,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    CONSTRAINT games_verification_status_check
        CHECK (verification_status IN ('UNVERIFIED', 'PEER_CONFIRMED', 'SERVER_VERIFIED', 'CONFLICTED')),
    CONSTRAINT games_payload_hash_check
        CHECK (octet_length(canonical_payload_hash) = 32),
    CONSTRAINT games_time_order_check
        CHECK (finished_at >= started_at),
    CONSTRAINT games_ranked_verification_check
        CHECK (NOT ranked OR verification_status = 'SERVER_VERIFIED')
);

CREATE TABLE game_players (
    game_id uuid NOT NULL REFERENCES games (game_id) ON DELETE CASCADE,
    seat varchar(8) NOT NULL,
    player_id uuid REFERENCES players (player_id),
    player_type varchar(16) NOT NULL,
    display_name_snapshot varchar(40) NOT NULL,
    score integer NOT NULL,
    thinking_seconds integer NOT NULL,
    outcome varchar(8) NOT NULL,
    PRIMARY KEY (game_id, seat),
    CONSTRAINT game_players_seat_check
        CHECK (seat IN ('RED', 'BLUE')),
    CONSTRAINT game_players_type_check
        CHECK (player_type IN ('PROFILE', 'GUEST', 'CPU')),
    CONSTRAINT game_players_profile_check
        CHECK (player_type = 'PROFILE' OR player_id IS NULL),
    CONSTRAINT game_players_display_name_check
        CHECK (char_length(display_name_snapshot) BETWEEN 1 AND 40),
    CONSTRAINT game_players_score_check
        CHECK (score >= 0),
    CONSTRAINT game_players_thinking_seconds_check
        CHECK (thinking_seconds >= 0),
    CONSTRAINT game_players_outcome_check
        CHECK (outcome IN ('WIN', 'LOSS', 'DRAW'))
);

CREATE INDEX game_players_player_idx
    ON game_players (player_id)
    WHERE player_id IS NOT NULL;
