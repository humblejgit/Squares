package cz.codex.squares;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.Scrollable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.function.Consumer;

final class ChatPanel extends JPanel {
    private static final int CHAT_WIDTH = 320;
    private static final SimpleDateFormat MESSAGE_TIME_FORMAT = new SimpleDateFormat("HH:mm");
    static final Color RED_MESSAGE_BACKGROUND = new Color(255, 232, 228);
    static final Color BLUE_MESSAGE_BACKGROUND = new Color(226, 239, 255);
    private static final String[] EMOTICONS = {
            "\uD83D\uDE00", "\uD83D\uDE04", "\uD83D\uDE02", "\uD83D\uDE09", "\uD83D\uDE0A",
            "\uD83D\uDC4D", "\uD83D\uDC4F", "\u2764", "\uD83C\uDF89", "\uD83E\uDD14"
    };
    private static final String[] QUICK_TEXTS = {
            "d\u011blej", "po\u010dkej", "WTF", "k\u00e1mo", "D'oh!"
    };

    private final JPanel messagesPanel;
    private final JScrollPane messagesScrollPane;
    private final JTextField inputField;
    private final Color ownMessageBackground;
    private final Color remoteMessageBackground;
    private final Consumer<String> sender;

    ChatPanel(String title, Color ownMessageBackground, Color remoteMessageBackground, Consumer<String> sender) {
        super(new BorderLayout(0, 6));
        this.ownMessageBackground = ownMessageBackground;
        this.remoteMessageBackground = remoteMessageBackground;
        this.sender = sender;
        this.messagesPanel = new MessagesPanel();
        this.messagesScrollPane = new JScrollPane(messagesPanel);
        this.inputField = new JTextField();

        JButton sendButton = new JButton(Messages.CHAT_SEND);
        JPanel bottomPanel = new JPanel(new BorderLayout(0, 6));
        JPanel emoticonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        JPanel quickTextsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        JPanel shortcutsPanel = new JPanel(new BorderLayout(0, 3));
        JPanel inputPanel = new JPanel(new BorderLayout(6, 0));

        setBorder(BorderFactory.createTitledBorder(title));
        setPreferredSize(new Dimension(CHAT_WIDTH, 1));
        emoticonsPanel.setBorder(BorderFactory.createTitledBorder(Messages.CHAT_EMOTICONS));

        messagesPanel.setLayout(new BoxLayout(messagesPanel, BoxLayout.Y_AXIS));
        messagesPanel.setBackground(Color.WHITE);
        messagesScrollPane.setBorder(BorderFactory.createEmptyBorder());
        messagesScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        for (String emoticon : EMOTICONS) {
            JButton emoticonButton = new JButton(emoticon);
            emoticonButton.setFocusable(false);
            emoticonButton.setMargin(new Insets(1, 4, 1, 4));
            emoticonButton.addActionListener(event -> insertEmoticon(emoticon));
            emoticonsPanel.add(emoticonButton);
        }

        for (String quickText : QUICK_TEXTS) {
            JButton quickTextButton = new JButton(quickText);
            quickTextButton.setFocusable(false);
            quickTextButton.setMargin(new Insets(1, 6, 1, 6));
            quickTextButton.addActionListener(event -> insertQuickText(quickText));
            quickTextsPanel.add(quickTextButton);
        }

        sendButton.addActionListener(event -> sendCurrentText());
        inputField.addActionListener(event -> sendCurrentText());
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        shortcutsPanel.add(emoticonsPanel, BorderLayout.NORTH);
        shortcutsPanel.add(quickTextsPanel, BorderLayout.SOUTH);
        bottomPanel.add(shortcutsPanel, BorderLayout.NORTH);
        bottomPanel.add(inputPanel, BorderLayout.SOUTH);

        add(messagesScrollPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    void receive(String label, String message) {
        append(false, message);
    }

    private void sendCurrentText() {
        String message = inputField.getText().trim();

        if (message.isEmpty()) {
            return;
        }

        inputField.setText("");
        append(true, message);
        sender.accept(message);
    }

    private void insertEmoticon(String emoticon) {
        inputField.replaceSelection(emoticon);
        inputField.requestFocusInWindow();
    }

    private void insertQuickText(String quickText) {
        String text = inputField.getText();
        int caretPosition = inputField.getCaretPosition();
        String prefix = caretPosition > 0 && !Character.isWhitespace(text.charAt(caretPosition - 1)) ? " " : "";
        String suffix = caretPosition < text.length() && !Character.isWhitespace(text.charAt(caretPosition)) ? " " : "";

        inputField.replaceSelection(prefix + quickText + suffix);
        inputField.requestFocusInWindow();
    }

    private void append(boolean ownMessage, String message) {
        JPanel row = new MessageRow();
        JLabel timeLabel = new JLabel(MESSAGE_TIME_FORMAT.format(new Date()));
        JTextArea messageArea = new JTextArea(message);

        row.setBackground(ownMessage ? ownMessageBackground : remoteMessageBackground);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Color.WHITE),
                BorderFactory.createEmptyBorder(5, 7, 5, 7)));
        timeLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 7));
        messageArea.setEditable(false);
        messageArea.setFocusable(false);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        messageArea.setOpaque(false);
        messageArea.setBorder(BorderFactory.createEmptyBorder());

        row.add(timeLabel, BorderLayout.WEST);
        row.add(messageArea, BorderLayout.CENTER);
        messagesPanel.add(row);
        messagesPanel.revalidate();
        messagesPanel.repaint();
        scrollToBottom();
    }

    private void scrollToBottom() {
        SwingUtilities.invokeLater(() ->
                messagesScrollPane.getVerticalScrollBar().setValue(
                        messagesScrollPane.getVerticalScrollBar().getMaximum()));
    }

    private static final class MessageRow extends JPanel {
        private MessageRow() {
            super(new BorderLayout());
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        }
    }

    private static final class MessagesPanel extends JPanel implements Scrollable {
        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return Math.max(16, visibleRect.height - 16);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }
}
