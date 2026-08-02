CREATE TABLE accounts (
    account_id uuid PRIMARY KEY,
    status varchar(16) NOT NULL,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    CONSTRAINT accounts_status_check
        CHECK (status IN ('ACTIVE', 'BLOCKED', 'DELETED'))
);

CREATE TABLE account_identities (
    issuer varchar(512) NOT NULL,
    subject varchar(255) NOT NULL,
    account_id uuid NOT NULL REFERENCES accounts (account_id),
    created_at timestamptz NOT NULL,
    PRIMARY KEY (issuer, subject)
);

CREATE INDEX account_identities_account_idx
    ON account_identities (account_id);

CREATE TABLE players (
    player_id uuid PRIMARY KEY,
    account_id uuid NOT NULL UNIQUE REFERENCES accounts (account_id),
    handle varchar(24) NOT NULL,
    normalized_handle varchar(24) NOT NULL UNIQUE,
    display_name varchar(40) NOT NULL,
    revision bigint NOT NULL DEFAULT 1,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    CONSTRAINT players_handle_check
        CHECK (handle ~ '^[a-z0-9][a-z0-9_-]{2,23}$'),
    CONSTRAINT players_normalized_handle_check
        CHECK (normalized_handle = lower(handle)),
    CONSTRAINT players_revision_check
        CHECK (revision >= 1),
    CONSTRAINT players_display_name_check
        CHECK (char_length(display_name) BETWEEN 1 AND 40)
);

CREATE TABLE installations (
    account_id uuid NOT NULL REFERENCES accounts (account_id),
    installation_id uuid NOT NULL,
    platform varchar(16) NOT NULL,
    app_version varchar(64) NOT NULL,
    core_version varchar(64) NOT NULL,
    locale varchar(35) NOT NULL,
    created_at timestamptz NOT NULL,
    last_seen_at timestamptz NOT NULL,
    revoked_at timestamptz,
    PRIMARY KEY (account_id, installation_id),
    CONSTRAINT installations_platform_check
        CHECK (platform IN ('WINDOWS', 'ANDROID')),
    CONSTRAINT installations_locale_check
        CHECK (char_length(locale) BETWEEN 2 AND 35)
);
