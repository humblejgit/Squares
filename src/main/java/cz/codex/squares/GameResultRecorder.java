package cz.codex.squares;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

final class GameResultRecorder {
    private final JFrame parent;
    private final GameStore store;

    GameResultRecorder(JFrame parent, GameStore store) {
        this.parent = parent;
        this.store = store;
    }

    void record(GameResult result) {
        try {
            store.save(result);
        } catch (StorageException exception) {
            Runnable warning = () -> JOptionPane.showMessageDialog(parent,
                    Messages.databaseSaveFailed(exception.getMessage()),
                    Messages.DATABASE_ERROR_TITLE,
                    JOptionPane.WARNING_MESSAGE);

            if (SwingUtilities.isEventDispatchThread()) {
                warning.run();
            } else {
                SwingUtilities.invokeLater(warning);
            }
        }
    }
}
