package cz.codex.squares;

import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;

final class ProfileDialog {
    private ProfileDialog() {
    }

    static PlayerProfile chooseActive(JFrame parent, ProfileStore store) {
        while (true) {
            try {
                List<PlayerProfile> profiles = store.findActive();

                if (profiles.isEmpty()) {
                    PlayerProfile created = createProfile(parent, store, true);
                    if (created == null) {
                        return null;
                    }
                    store.select(created.id());
                    return created;
                }

                PlayerProfile selected = store.findSelected();
                JComboBox<PlayerProfile> profileBox = new JComboBox<>(profiles.toArray(new PlayerProfile[0]));
                if (selected != null) {
                    profileBox.setSelectedItem(selected);
                }

                JPanel panel = new JPanel(new BorderLayout(0, 8));
                panel.add(new JLabel(Messages.PROFILE_SELECT_PROMPT), BorderLayout.NORTH);
                panel.add(profileBox, BorderLayout.CENTER);
                Object[] options = {
                        Messages.PROFILE_CONTINUE,
                        Messages.PROFILE_NEW,
                        Messages.PROFILE_RENAME,
                        Messages.PROFILE_ARCHIVE,
                        Messages.PROFILE_EXIT
                };
                int choice = JOptionPane.showOptionDialog(parent, panel, Messages.PROFILE_TITLE,
                        JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
                PlayerProfile chosen = (PlayerProfile) profileBox.getSelectedItem();

                if (choice == 0 && chosen != null) {
                    store.select(chosen.id());
                    return chosen;
                }
                if (choice == 1) {
                    createProfile(parent, store, false);
                } else if (choice == 2 && chosen != null) {
                    renameProfile(parent, store, chosen);
                } else if (choice == 3 && chosen != null) {
                    archiveProfile(parent, store, chosen, profiles.size());
                } else if (choice == 4 || choice == JOptionPane.CLOSED_OPTION) {
                    return null;
                }
            } catch (StorageException | IllegalArgumentException exception) {
                showError(parent, exception.getMessage());
            }
        }
    }

    static PlayerProfile chooseOpponent(JFrame parent, ProfileStore store, PlayerProfile activeProfile) {
        try {
            List<Object> choices = new ArrayList<>();
            choices.add(Messages.PROFILE_GUEST);
            for (PlayerProfile profile : store.findActive()) {
                if (!profile.equals(activeProfile)) {
                    choices.add(profile);
                }
            }

            Object selected = JOptionPane.showInputDialog(parent,
                    Messages.PROFILE_OPPONENT_PROMPT,
                    Messages.PROFILE_OPPONENT_TITLE,
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    choices.toArray(),
                    choices.get(0));
            return selected instanceof PlayerProfile ? (PlayerProfile) selected : null;
        } catch (StorageException exception) {
            showError(parent, exception.getMessage());
            return null;
        }
    }

    private static PlayerProfile createProfile(JFrame parent, ProfileStore store, boolean required)
            throws StorageException {
        while (true) {
            String name = JOptionPane.showInputDialog(parent,
                    required ? Messages.PROFILE_FIRST_NAME_PROMPT : Messages.PROFILE_NAME_PROMPT,
                    Messages.PROFILE_TITLE,
                    JOptionPane.PLAIN_MESSAGE);
            if (name == null) {
                return null;
            }
            try {
                return store.create(name);
            } catch (IllegalArgumentException exception) {
                showError(parent, Messages.PROFILE_NAME_REQUIRED);
            } catch (StorageException exception) {
                showError(parent, exception.getMessage());
            }
        }
    }

    private static void renameProfile(JFrame parent, ProfileStore store, PlayerProfile profile)
            throws StorageException {
        String name = (String) JOptionPane.showInputDialog(parent,
                Messages.PROFILE_NAME_PROMPT,
                Messages.PROFILE_RENAME,
                JOptionPane.PLAIN_MESSAGE,
                null,
                null,
                profile.displayName());
        if (name != null) {
            try {
                store.rename(profile.id(), name);
            } catch (IllegalArgumentException exception) {
                showError(parent, Messages.PROFILE_NAME_REQUIRED);
            }
        }
    }

    private static void archiveProfile(JFrame parent, ProfileStore store, PlayerProfile profile, int activeCount)
            throws StorageException {
        if (activeCount <= 1) {
            showError(parent, Messages.PROFILE_LAST_CANNOT_ARCHIVE);
            return;
        }

        int choice = JOptionPane.showConfirmDialog(parent,
                Messages.profileArchiveConfirm(profile.displayName()),
                Messages.PROFILE_ARCHIVE,
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (choice == JOptionPane.YES_OPTION) {
            store.archive(profile.id());
        }
    }

    private static void showError(JFrame parent, String message) {
        JOptionPane.showMessageDialog(parent, message, Messages.PROFILE_TITLE, JOptionPane.WARNING_MESSAGE);
    }
}
