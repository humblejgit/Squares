package cz.humblej.squares.network;

import cz.humblej.squares.ui.ChatPanel;
import cz.humblej.squares.ui.Messages;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.lang.reflect.InvocationTargetException;

final class NetworkSwing {
    private NetworkSwing() {
    }

    static void run(Runnable runnable) {
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
            return;
        }

        try {
            SwingUtilities.invokeAndWait(runnable);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } catch (InvocationTargetException exception) {
            throw new IllegalStateException(exception.getCause());
        }
    }

    static void receiveChat(ChatPanel chat, String sender, String encodedMessage) {
        String message = NetworkProtocol.decodeValue(encodedMessage);
        run(() -> {
            if (chat != null) {
                chat.receive(sender, message);
            }
        });
    }

    static void showInfo(Component parent, String message, String title) {
        run(() -> JOptionPane.showMessageDialog(parent, message, title, JOptionPane.INFORMATION_MESSAGE));
    }

    static void showError(String message, Exception exception) {
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
                message + "\n\n" + exception.getMessage(), Messages.NETWORK_GAME_TITLE,
                JOptionPane.WARNING_MESSAGE));
    }

    static void showWarning(JFrame frame, String message) {
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(frame, message,
                Messages.NETWORK_GAME_TITLE, JOptionPane.WARNING_MESSAGE));
    }

    static void close(JFrame frame) {
        SwingUtilities.invokeLater(frame::dispose);
    }
}
