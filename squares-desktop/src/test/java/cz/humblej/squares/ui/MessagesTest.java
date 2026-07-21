package cz.humblej.squares.ui;

import cz.humblej.squares.model.GameResult;
import cz.humblej.squares.model.PlayerResult;
import org.junit.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class MessagesTest {
    @Test
    public void networkProfileChangeExplainsStartupRestriction() {
        assertEquals("Profil v s\u00ed\u0165ov\u00e9 h\u0159e lze m\u011bnit jen p\u0159i spust\u011bn\u00ed aplikace.",
                Messages.PROFILE_NETWORK_CHANGE_ONLY_AT_START);
    }

    @Test
    public void networkStatusIncludesBothProfileNamesAndRoles() {
        assertEquals("Status: ban\u00e1n (\u010derven\u00fd hr\u00e1\u010d) vs. "
                        + "jablko (modr\u00fd hr\u00e1\u010d)",
                Messages.networkPlayersStatus("ban\u00e1n", "jablko"));
    }

    @Test
    public void localInfoIncludesBothProfileNamesAndRoles() {
        assertEquals("IP: lok\u00e1ln\u00ed hra\n"
                        + "Plocha: 8x8\n"
                        + "Status: ban\u00e1n (\u010derven\u00fd hr\u00e1\u010d) vs. "
                        + "jablko (modr\u00fd hr\u00e1\u010d)",
                Messages.localInfo(8, 8, "ban\u00e1n", "jablko"));
    }

    @Test
    public void computerInfoIncludesProfileRolesAndLowercaseDifficulty() {
        assertEquals("IP: lok\u00e1ln\u00ed hra\n"
                        + "Plocha: 8x8\n"
                        + "Status: ban\u00e1n (\u010derven\u00fd hr\u00e1\u010d) vs. CPU - "
                        + "st\u0159edn\u00ed obt\u00ed\u017enost (modr\u00fd hr\u00e1\u010d)",
                Messages.computerInfo(8, 8, "ban\u00e1n", Messages.DIFFICULTY_MEDIUM));
    }

    @Test
    public void statisticsTextIsLocalized() {
        assertEquals("Statistiky", Messages.MENU_STATISTICS);
        assertEquals("50,0 %", Messages.formatWinPercentage(50.0));
        assertEquals("Hry: 4   V\u00fdhry: 2   Rem\u00edzy: 1   Prohry: 1",
                Messages.statisticsRecord(4, 2, 1, 1));
    }

    @Test
    public void gameOverTextFormatsStructuredResults() {
        assertEquals("\u010cerven\u00fd hr\u00e1\u010d v\u00edt\u011bz\u00ed 18:12.", Messages.gameOver(result(
                GameResult.FinishReason.BOARD_FULL, 18, 12,
                PlayerResult.Outcome.WIN, PlayerResult.Outcome.LOSS)));
        assertEquals(Messages.blueLostOnTime(), Messages.gameOver(result(
                GameResult.FinishReason.TIME_LIMIT, 3, 8,
                PlayerResult.Outcome.WIN, PlayerResult.Outcome.LOSS)));
        assertEquals("Rem\u00edza 8:8.", Messages.gameOver(result(
                GameResult.FinishReason.BOARD_FULL, 8, 8,
                PlayerResult.Outcome.DRAW, PlayerResult.Outcome.DRAW)));
    }

    private static GameResult result(GameResult.FinishReason reason, int redScore, int blueScore,
                                     PlayerResult.Outcome redOutcome, PlayerResult.Outcome blueOutcome) {
        Instant started = Instant.parse("2026-07-16T12:00:00Z");
        PlayerResult red = PlayerResult.guest(PlayerResult.Seat.RED, Messages.PLAYER_RED,
                redScore, 20, redOutcome);
        PlayerResult blue = PlayerResult.guest(PlayerResult.Seat.BLUE, Messages.PLAYER_BLUE,
                blueScore, 30, blueOutcome);
        return new GameResult(UUID.randomUUID(), started, started.plusSeconds(50),
                GameResult.Mode.LOCAL, reason, 5, 5, 30, 50, false, null, red, blue);
    }
}
