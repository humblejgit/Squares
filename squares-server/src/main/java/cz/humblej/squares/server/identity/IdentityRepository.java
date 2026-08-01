package cz.humblej.squares.server.identity;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class IdentityRepository {
    private final JdbcTemplate jdbc;

    IdentityRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    Optional<AccountRecord> findAccount(String issuer, String subject) {
        List<AccountRecord> accounts = jdbc.query("""
                SELECT a.account_id, a.status, a.created_at
                FROM accounts a
                JOIN account_identities i ON i.account_id = a.account_id
                WHERE i.issuer = ? AND i.subject = ?
                """, (result, row) -> new AccountRecord(
                        result.getObject("account_id", UUID.class),
                        result.getString("status"),
                        result.getTimestamp("created_at").toInstant()),
                issuer, subject);
        return accounts.stream().findFirst();
    }

    void insertAccount(UUID accountId, Instant now) {
        jdbc.update("""
                INSERT INTO accounts (account_id, status, created_at, updated_at)
                VALUES (?, 'ACTIVE', ?, ?)
                """, accountId, Timestamp.from(now), Timestamp.from(now));
    }

    int insertIdentity(String issuer, String subject, UUID accountId, Instant now) {
        return jdbc.update("""
                INSERT INTO account_identities (issuer, subject, account_id, created_at)
                VALUES (?, ?, ?, ?)
                ON CONFLICT (issuer, subject) DO NOTHING
                """, issuer, subject, accountId, Timestamp.from(now));
    }

    void deleteUnlinkedAccount(UUID accountId) {
        jdbc.update("DELETE FROM accounts WHERE account_id = ?", accountId);
    }

    void lockAccount(UUID accountId) {
        jdbc.queryForObject(
                "SELECT account_id FROM accounts WHERE account_id = ? FOR UPDATE",
                UUID.class,
                accountId);
    }

    Optional<PlayerRecord> findPlayer(UUID accountId) {
        List<PlayerRecord> players = jdbc.query("""
                SELECT player_id, account_id, handle, display_name, revision, created_at
                FROM players
                WHERE account_id = ?
                """, (result, row) -> new PlayerRecord(
                        result.getObject("player_id", UUID.class),
                        result.getObject("account_id", UUID.class),
                        result.getString("handle"),
                        result.getString("display_name"),
                        result.getLong("revision"),
                        result.getTimestamp("created_at").toInstant()),
                accountId);
        return players.stream().findFirst();
    }

    PlayerRecord insertPlayer(
            UUID playerId,
            UUID accountId,
            String handle,
            String normalizedHandle,
            String displayName,
            Instant now) {
        return jdbc.queryForObject("""
                INSERT INTO players (
                    player_id, account_id, handle, normalized_handle, display_name,
                    revision, created_at, updated_at
                )
                VALUES (?, ?, ?, ?, ?, 1, ?, ?)
                RETURNING player_id, account_id, handle, display_name, revision, created_at
                """, (result, row) -> new PlayerRecord(
                        result.getObject("player_id", UUID.class),
                        result.getObject("account_id", UUID.class),
                        result.getString("handle"),
                        result.getString("display_name"),
                        result.getLong("revision"),
                        result.getTimestamp("created_at").toInstant()),
                playerId,
                accountId,
                handle,
                normalizedHandle,
                displayName,
                Timestamp.from(now),
                Timestamp.from(now));
    }

    PlayerRecord updatePlayer(
            UUID accountId,
            String handle,
            String normalizedHandle,
            String displayName,
            Instant now) {
        return jdbc.queryForObject("""
                UPDATE players
                SET handle = ?,
                    normalized_handle = ?,
                    display_name = ?,
                    revision = revision + 1,
                    updated_at = ?
                WHERE account_id = ?
                RETURNING player_id, account_id, handle, display_name, revision, created_at
                """, (result, row) -> new PlayerRecord(
                        result.getObject("player_id", UUID.class),
                        result.getObject("account_id", UUID.class),
                        result.getString("handle"),
                        result.getString("display_name"),
                        result.getLong("revision"),
                        result.getTimestamp("created_at").toInstant()),
                handle,
                normalizedHandle,
                displayName,
                Timestamp.from(now),
                accountId);
    }
}
