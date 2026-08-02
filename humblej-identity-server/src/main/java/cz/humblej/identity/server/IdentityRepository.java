package cz.humblej.identity.server;

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

    Optional<PlayerIdentityRecord> findPlayerIdentity(UUID accountId) {
        List<PlayerIdentityRecord> identities = jdbc.query("""
                SELECT player_id, account_id, created_at
                FROM player_identities
                WHERE account_id = ?
                """, (result, row) -> new PlayerIdentityRecord(
                        result.getObject("player_id", UUID.class),
                        result.getObject("account_id", UUID.class),
                        result.getTimestamp("created_at").toInstant()),
                accountId);
        return identities.stream().findFirst();
    }

    PlayerIdentityRecord insertPlayerIdentity(UUID playerId, UUID accountId, Instant now) {
        return jdbc.queryForObject("""
                INSERT INTO player_identities (player_id, account_id, created_at, updated_at)
                VALUES (?, ?, ?, ?)
                RETURNING player_id, account_id, created_at
                """, (result, row) -> new PlayerIdentityRecord(
                        result.getObject("player_id", UUID.class),
                        result.getObject("account_id", UUID.class),
                        result.getTimestamp("created_at").toInstant()),
                playerId, accountId, Timestamp.from(now), Timestamp.from(now));
    }

    Optional<InstallationRecord> findInstallation(UUID accountId, UUID installationId) {
        List<InstallationRecord> installations = jdbc.query("""
                SELECT installation_id, platform, app_version, core_version, locale,
                       created_at, last_seen_at
                FROM installations
                WHERE account_id = ? AND installation_id = ?
                """, (result, row) -> new InstallationRecord(
                        result.getObject("installation_id", UUID.class),
                        result.getString("platform"),
                        result.getString("app_version"),
                        result.getString("core_version"),
                        result.getString("locale"),
                        result.getTimestamp("created_at").toInstant(),
                        result.getTimestamp("last_seen_at").toInstant()),
                accountId, installationId);
        return installations.stream().findFirst();
    }

    InstallationRecord insertInstallation(
            UUID accountId, UUID installationId, PutInstallationRequest request, Instant now) {
        return jdbc.queryForObject("""
                INSERT INTO installations (
                    account_id, installation_id, platform, app_version, core_version,
                    locale, created_at, last_seen_at, revoked_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, NULL)
                RETURNING installation_id, platform, app_version, core_version, locale,
                          created_at, last_seen_at
                """, installationRowMapper(),
                accountId, installationId, request.platform(), request.appVersion(),
                request.coreVersion(), request.locale(), Timestamp.from(now), Timestamp.from(now));
    }

    InstallationRecord updateInstallation(
            UUID accountId, UUID installationId, PutInstallationRequest request, Instant now) {
        return jdbc.queryForObject("""
                UPDATE installations
                SET platform = ?, app_version = ?, core_version = ?, locale = ?,
                    last_seen_at = ?, revoked_at = NULL
                WHERE account_id = ? AND installation_id = ?
                RETURNING installation_id, platform, app_version, core_version, locale,
                          created_at, last_seen_at
                """, installationRowMapper(),
                request.platform(), request.appVersion(), request.coreVersion(), request.locale(),
                Timestamp.from(now), accountId, installationId);
    }

    private static org.springframework.jdbc.core.RowMapper<InstallationRecord> installationRowMapper() {
        return (result, row) -> new InstallationRecord(
                result.getObject("installation_id", UUID.class),
                result.getString("platform"),
                result.getString("app_version"),
                result.getString("core_version"),
                result.getString("locale"),
                result.getTimestamp("created_at").toInstant(),
                result.getTimestamp("last_seen_at").toInstant());
    }
}
