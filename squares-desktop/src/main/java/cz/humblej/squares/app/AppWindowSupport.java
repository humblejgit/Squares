package cz.humblej.squares.app;

import cz.humblej.squares.ui.ChatPanel;
import cz.humblej.squares.ui.SquaresPanel;

import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;

public final class AppWindowSupport {
    private static final String CLOCK_PAUSE_LISTENER_PROPERTY = "squares.clockPauseListener";

    private AppWindowSupport() {
    }

    public static void fitToContent(JFrame frame) {
        frame.setMinimumSize(null);
        frame.pack();
        frame.setMinimumSize(frame.getSize());
        frame.setLocationRelativeTo(null);
    }

    public static void showNetworkContent(JFrame frame, SquaresPanel panel, ChatPanel chatPanel) {
        JPanel content = new JPanel(new BorderLayout(10, 0));
        content.add(panel, BorderLayout.CENTER);
        content.add(chatPanel, BorderLayout.EAST);
        frame.setContentPane(content);
        fitToContent(frame);
    }

    public static void installPauseHandling(JFrame frame, SquaresPanel panel) {
        Object existing = frame.getRootPane().getClientProperty(CLOCK_PAUSE_LISTENER_PROPERTY);
        if (existing instanceof WindowFocusListener) {
            frame.removeWindowFocusListener((WindowFocusListener) existing);
        }

        WindowFocusListener listener = new WindowAdapter() {
            @Override
            public void windowGainedFocus(WindowEvent event) {
                panel.setWindowActive(true);
            }

            @Override
            public void windowLostFocus(WindowEvent event) {
                panel.setWindowActive(false);
            }
        };

        frame.addWindowFocusListener(listener);
        frame.getRootPane().putClientProperty(CLOCK_PAUSE_LISTENER_PROPERTY, listener);
        panel.setWindowActive(frame.isActive());
    }
}
