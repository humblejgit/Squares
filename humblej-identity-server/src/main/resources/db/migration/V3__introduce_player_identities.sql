CREATE TABLE player_identities (
    player_id uuid PRIMARY KEY,
    account_id uuid NOT NULL UNIQUE REFERENCES accounts (account_id),
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL
);

INSERT INTO player_identities (player_id, account_id, created_at, updated_at)
SELECT player_id, account_id, created_at, updated_at
FROM players;

ALTER TABLE players DROP CONSTRAINT players_account_id_key;
ALTER TABLE players DROP CONSTRAINT players_account_id_fkey;
ALTER TABLE players DROP COLUMN account_id;
ALTER TABLE players
    ADD CONSTRAINT players_player_identity_fkey
    FOREIGN KEY (player_id) REFERENCES player_identities (player_id);

COMMENT ON TABLE player_identities IS
    'Game-independent public player identities owned by authenticated accounts.';
COMMENT ON TABLE players IS
    'Squares-specific public profiles sharing the central player identity UUID.';
