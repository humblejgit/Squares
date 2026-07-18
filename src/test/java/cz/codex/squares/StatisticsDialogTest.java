package cz.codex.squares;

import org.junit.Test;

import javax.swing.table.TableModel;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class StatisticsDialogTest {
    @Test
    public void leaderboardTableShowsRankingAndCannotBeEdited() {
        PlayerProfile active = new PlayerProfile(UUID.randomUUID(), "Jana", Instant.EPOCH, false);
        PlayerProfile archived = new PlayerProfile(UUID.randomUUID(), "Petr", Instant.EPOCH, true);
        TableModel model = StatisticsDialog.createTableModel(Arrays.asList(
                new LocalProfileStatistics(active, 2, 1, 1, 0, 20),
                new LocalProfileStatistics(archived, 1, 0, 0, 1, 5)));

        assertEquals(2, model.getRowCount());
        assertEquals(Messages.STATISTICS_COLUMN_PROFILE, model.getColumnName(1));
        assertEquals(1, model.getValueAt(0, 0));
        assertEquals("Jana", model.getValueAt(0, 1));
        assertEquals("50,0 %", model.getValueAt(0, 7));
        assertEquals("Petr (archivovan\u00fd)", model.getValueAt(1, 1));
        assertFalse(model.isCellEditable(0, 1));
    }
}
