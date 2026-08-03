package cz.humblej.squares.ui;

import cz.humblej.identity.client.AuthenticationException;
import cz.humblej.identity.model.InstallationInfo;
import cz.humblej.squares.auth.OnlineAccount;
import cz.humblej.squares.auth.OnlineAccountService;
import cz.humblej.squares.auth.OnlinePlayer;
import cz.humblej.squares.model.PlayerProfile;
import cz.humblej.squares.persistence.PlayerIdentityStore;
import cz.humblej.squares.persistence.ProfileServerLink;
import cz.humblej.squares.persistence.StorageException;
import cz.humblej.squares.persistence.SyncSummary;
import cz.humblej.squares.sync.GameResultSyncService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public final class OnlineAccountDialog {
    private final JDialog dialog;
    private final OnlineAccountService service;
    private final PlayerProfile localProfile;
    private final PlayerIdentityStore identityStore;
    private final InstallationInfo installation;
    private final GameResultSyncService syncService;
    private final JPanel root = new JPanel(new BorderLayout(10, 10));

    private OnlineAccountDialog(JFrame parent, OnlineAccountService service,
                                PlayerProfile localProfile,
                                PlayerIdentityStore identityStore,
                                InstallationInfo installation,
                                GameResultSyncService syncService) {
        this.service = service;
        this.localProfile = localProfile;
        this.identityStore = identityStore;
        this.installation = installation;
        this.syncService = syncService;
        this.dialog = new JDialog(parent, Messages.ONLINE_ACCOUNT_TITLE, true);
        root.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setContentPane(root);
    }

    public static void show(JFrame parent, OnlineAccountService service,
                            PlayerProfile localProfile,
                            PlayerIdentityStore identityStore,
                            InstallationInfo installation,
                            GameResultSyncService syncService) {
        OnlineAccountDialog accountDialog = new OnlineAccountDialog(
                parent, service, localProfile, identityStore, installation, syncService);
        String warning = service.consumeRestorationWarning();
        if (warning != null) {
            JOptionPane.showMessageDialog(parent,
                    Messages.ONLINE_SESSION_RESTORE_FAILED + "\n\n" + warning,
                    Messages.ONLINE_ACCOUNT_TITLE,
                    JOptionPane.WARNING_MESSAGE);
        }

        if (service.hasSession()) {
            accountDialog.loadAccount();
        } else {
            accountDialog.showSignedOut(null);
        }
        accountDialog.dialog.setVisible(true);
    }

    private void loadAccount() {
        runTask(Messages.ONLINE_LOADING, () -> {
            OnlineAccount account = loadRegisteredAccount();
            synchronizeWithoutHidingAccount();
            return account;
        }, this::showAccount);
    }

    private void login() {
        runTask(Messages.ONLINE_BROWSER_WAIT, () -> {
            OnlineAccount account = service.login();
            service.registerInstallation(installation);
            synchronizeWithoutHidingAccount();
            return account;
        }, this::showAccount);
    }

    private void saveProfile(JTextField handleField, JTextField displayNameField) {
        String handle = handleField.getText().trim().toLowerCase(Locale.ROOT);
        String displayName = displayNameField.getText().trim();
        runTask(Messages.ONLINE_PROFILE_SAVING, () -> {
            service.putProfile(handle, displayName);
            return loadRegisteredAccount();
        }, this::showAccount);
    }

    private OnlineAccount loadRegisteredAccount() throws AuthenticationException {
        OnlineAccount account = service.getMe();
        service.registerInstallation(installation);
        return account;
    }

    private void linkProfile(OnlineAccount account) {
        runTask(Messages.ONLINE_LINKING_PROFILE, () -> {
            try {
                identityStore.link(localProfile.id(), account.playerId(), installation.installationId());
            } catch (StorageException exception) {
                throw new AuthenticationException(exception.getMessage(), exception);
            }
            syncService.synchronizeInBackground();
            return account;
        }, this::showAccount);
    }

    private void unlinkProfile(OnlineAccount account) {
        int choice = JOptionPane.showConfirmDialog(dialog,
                Messages.ONLINE_UNLINK_CONFIRM,
                Messages.ONLINE_ACCOUNT_TITLE,
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }
        runTask(Messages.ONLINE_UNLINKING_PROFILE, () -> {
            try {
                identityStore.unlink(localProfile.id());
            } catch (StorageException exception) {
                throw new AuthenticationException(exception.getMessage(), exception);
            }
            return account;
        }, this::showAccount);
    }

    private void logout() {
        runTask(Messages.ONLINE_LOGGING_OUT, () -> {
            service.logout();
            return null;
        }, ignored -> showSignedOut(Messages.ONLINE_LOGGED_OUT));
    }

    private void showSignedOut(String information) {
        root.removeAll();
        String message = information == null
                ? Messages.ONLINE_SIGNED_OUT
                : information + "\n\n" + Messages.ONLINE_SIGNED_OUT;
        message += "\n\n" + Messages.ONLINE_LOCAL_PROFILE + " " + localProfile.displayName()
                + "\n" + Messages.ONLINE_INSTALLATION_ID + " " + installation.installationId();
        root.add(new JLabel(toHtml(message), SwingConstants.CENTER), BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton login = new JButton(Messages.ONLINE_LOGIN);
        JButton close = new JButton(Messages.ONLINE_CLOSE);
        login.addActionListener(event -> login());
        close.addActionListener(event -> dialog.dispose());
        buttons.add(login);
        buttons.add(close);
        root.add(buttons, BorderLayout.SOUTH);
        refreshDialog();
    }

    private void showAccount(OnlineAccount account) {
        root.removeAll();
        OnlinePlayer player = account.player();

        String status = account.onboardingRequired()
                ? Messages.ONLINE_ONBOARDING_REQUIRED
                : Messages.onlineSignedIn(player.displayName(), player.handle());
        root.add(new JLabel(toHtml(status), SwingConstants.CENTER), BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(5, 5, 5, 5);
        constraints.anchor = GridBagConstraints.LINE_START;

        JTextField handle = new JTextField(player == null ? "" : player.handle(), 24);
        JTextField displayName = new JTextField(player == null ? "" : player.displayName(), 24);
        addFormRow(form, constraints, 0, Messages.ONLINE_HANDLE, handle);
        addFormRow(form, constraints, 1, Messages.ONLINE_DISPLAY_NAME, displayName);
        addFormRow(form, constraints, 2, Messages.ONLINE_LOCAL_PROFILE,
                new JLabel(localProfile.displayName()));
        addFormRow(form, constraints, 3, Messages.ONLINE_PLAYER_ID,
                new JLabel(account.playerId().toString()));
        addFormRow(form, constraints, 4, Messages.ONLINE_INSTALLATION_ID,
                new JLabel(installation.installationId().toString()));

        ProfileServerLink link;
        try {
            link = identityStore.findLink(localProfile.id());
        } catch (StorageException exception) {
            JOptionPane.showMessageDialog(dialog, exception.getMessage(),
                    Messages.ONLINE_ACCOUNT_TITLE, JOptionPane.ERROR_MESSAGE);
            link = null;
        }
        boolean linkedHere = link != null && link.playerId().equals(account.playerId());
        String linkStatus = link == null
                ? Messages.ONLINE_PROFILE_NOT_LINKED
                : linkedHere
                ? Messages.ONLINE_PROFILE_LINKED
                : Messages.ONLINE_PROFILE_LINKED_ELSEWHERE;
        addFormRow(form, constraints, 5, "", new JLabel(linkStatus));
        SyncSummary syncSummary;
        try {
            syncSummary = syncService.summary(account);
        } catch (StorageException exception) {
            syncSummary = new SyncSummary(0, 0, 0, 0);
        }
        addFormRow(form, constraints, 6, Messages.ONLINE_SYNC_STATUS,
                new JLabel(Messages.onlineSyncStatus(syncSummary)));
        root.add(form, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton save = new JButton(account.onboardingRequired()
                ? Messages.ONLINE_CREATE_PROFILE : Messages.ONLINE_SAVE_PROFILE);
        JButton logout = new JButton(Messages.ONLINE_LOGOUT);
        JButton close = new JButton(Messages.ONLINE_CLOSE);
        JButton linkButton = new JButton(link == null
                ? Messages.ONLINE_LINK_PROFILE : Messages.ONLINE_UNLINK_PROFILE);
        JButton syncButton = new JButton(Messages.ONLINE_SYNC_NOW);
        save.addActionListener(event -> saveProfile(handle, displayName));
        if (link == null) {
            linkButton.addActionListener(event -> linkProfile(account));
        } else {
            linkButton.addActionListener(event -> unlinkProfile(account));
        }
        logout.addActionListener(event -> logout());
        syncButton.setEnabled(linkedHere);
        syncButton.addActionListener(event -> synchronize(account));
        close.addActionListener(event -> dialog.dispose());
        buttons.add(save);
        buttons.add(linkButton);
        buttons.add(syncButton);
        buttons.add(logout);
        buttons.add(close);
        root.add(buttons, BorderLayout.SOUTH);
        refreshDialog();
    }

    private void synchronize(OnlineAccount account) {
        runTask(Messages.ONLINE_SYNCING, () -> {
            syncService.synchronizeNow(true);
            return account;
        }, this::showAccount);
    }

    private void synchronizeWithoutHidingAccount() throws AuthenticationException {
        try {
            syncService.synchronizeNow(false);
        } catch (StorageException ignored) {
            // The account remains usable and the local status will expose pending work.
        } catch (AuthenticationException exception) {
            if (exception.sessionExpired()) {
                throw exception;
            }
            // A submission failure must not hide an otherwise loaded online account.
        }
    }

    private void showBusy(String message) {
        root.removeAll();
        JLabel label = new JLabel(toHtml(message), SwingConstants.CENTER);
        JProgressBar progress = new JProgressBar();
        progress.setIndeterminate(true);
        JPanel center = new JPanel(new BorderLayout(0, 10));
        center.add(label, BorderLayout.CENTER);
        center.add(progress, BorderLayout.SOUTH);
        root.add(center, BorderLayout.CENTER);
        refreshDialog();
    }

    private <T> void runTask(String busyMessage, AccountTask<T> task, AccountResult<T> result) {
        showBusy(busyMessage);
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return task.run();
            }

            @Override
            protected void done() {
                try {
                    result.accept(get());
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showFailure(new AuthenticationException("Operace byla přerušena.", exception));
                } catch (ExecutionException exception) {
                    Throwable cause = exception.getCause();
                    showFailure(cause instanceof AuthenticationException
                            ? (AuthenticationException) cause
                            : new AuthenticationException("Operace se nezdařila.", cause));
                }
            }
        }.execute();
    }

    private void showFailure(AuthenticationException exception) {
        String message = exception.sessionExpired()
                ? Messages.ONLINE_SESSION_EXPIRED
                : exception.getMessage();
        JOptionPane.showMessageDialog(dialog, message,
                Messages.ONLINE_ACCOUNT_TITLE, JOptionPane.ERROR_MESSAGE);
        if (service.hasSession()) {
            showUnavailable(message);
        } else {
            showSignedOut(exception.sessionExpired() ? Messages.ONLINE_SESSION_EXPIRED : null);
        }
    }

    private void showUnavailable(String message) {
        root.removeAll();
        root.add(new JLabel(toHtml(message), SwingConstants.CENTER), BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton retry = new JButton(Messages.ONLINE_RETRY);
        JButton logout = new JButton(Messages.ONLINE_LOGOUT);
        JButton close = new JButton(Messages.ONLINE_CLOSE);
        retry.addActionListener(event -> loadAccount());
        logout.addActionListener(event -> logout());
        close.addActionListener(event -> dialog.dispose());
        buttons.add(retry);
        buttons.add(logout);
        buttons.add(close);
        root.add(buttons, BorderLayout.SOUTH);
        refreshDialog();
    }

    private static void addFormRow(JPanel form, GridBagConstraints constraints, int row,
                                   String label, JComponent field) {
        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.weightx = 0;
        constraints.fill = GridBagConstraints.NONE;
        form.add(new JLabel(label), constraints);
        constraints.gridx = 1;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        form.add(field, constraints);
    }

    private void refreshDialog() {
        root.revalidate();
        root.repaint();
        dialog.pack();
        dialog.setMinimumSize(dialog.getSize());
        dialog.setLocationRelativeTo(dialog.getParent());
    }

    private static String toHtml(String text) {
        return "<html><div style='text-align:center'>"
                + text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\n", "<br>")
                + "</div></html>";
    }

    private interface AccountTask<T> {
        T run() throws Exception;
    }

    private interface AccountResult<T> {
        void accept(T value);
    }
}
