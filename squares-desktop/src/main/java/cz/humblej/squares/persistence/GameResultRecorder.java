package cz.humblej.squares.persistence;

import cz.humblej.squares.model.GameResult;
import cz.humblej.squares.ui.Messages;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public final class GameResultRecorder {
    private final JFrame parent;
    private final GameStore store;
    private final Runnable savedCallback;

    public GameResultRecorder(JFrame parent, GameStore store) {
        this(parent, store, null);
    }

    public GameResultRecorder(JFrame parent, GameStore store, Runnable savedCallback) {
        this.parent = parent;
        this.store = store;
        this.savedCallback = savedCallback;
    }

    public void record(GameResult result) {
        try {
            if (store.save(result) && savedCallback != null) {
                savedCallback.run();
            }
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
