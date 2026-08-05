package cz.humblej.squares.ui;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

import cz.humblej.squares.app.BuildInfo;
import cz.humblej.squares.model.GameResult;
import cz.humblej.squares.model.PlayerProfile;
import cz.humblej.squares.model.PlayerResult;
import cz.humblej.squares.persistence.SyncSummary;

/** Localized desktop-client messages backed by resource bundles. */
public final class Messages {
    private static final String BUNDLE_NAME = "i18n.messages";
    private static final String LANGUAGE_PREFERENCE = "language";
    private static final Preferences PREFERENCES =
            Preferences.userNodeForPackage(Messages.class);

    public static String APP_TITLE;
    public static String PLAYER_RED;
    public static String PLAYER_BLUE;
    public static String PLAYER_GUEST;
    public static String PLAYER_CPU;
    public static String WINDOW_HOST;
    public static String WINDOW_CLIENT;
    public static String WINDOW_LOCAL;
    public static String WINDOW_COMPUTER;
    public static String GAME_MODE_LOCAL;
    public static String GAME_MODE_COMPUTER;
    public static String GAME_MODE_HOST;
    public static String GAME_MODE_JOIN;
    public static String GAME_MODE_PROMPT;
    public static String HOST_ADDRESS_PROMPT;
    public static String CONNECTING_TO_HOST;
    public static String CHAT_HOST_TITLE;
    public static String CHAT_CLIENT_TITLE;
    public static String CHAT_SEND;
    public static String CHAT_EMOTICONS;
    public static String CHAT_YOU;
    public static String CHAT_HOST;
    public static String CHAT_CLIENT;
    public static String GAME_OPTIONS_TITLE;
    public static String GAME_OPTIONS_BOARD_SIZE;
    public static String GAME_OPTIONS_THINK_TIME;
    public static String GAME_OPTIONS_DIFFICULTY;
    public static String GAME_OPTIONS_RANDOM_EDGES;
    public static String THINK_TIME_NONE;
    public static String DIFFICULTY_EASY;
    public static String DIFFICULTY_MEDIUM;
    public static String DIFFICULTY_HARD;
    public static String BOARD_SIZE_PROMPT;
    public static String BOARD_SIZE_TITLE;
    public static String ADAPTER_PROMPT;
    public static String ADAPTER_TITLE;
    public static String NO_NETWORK_ADAPTER;
    public static String NETWORK_SETTINGS_ADAPTER;
    public static String NETWORK_SETTINGS_PORT;
    public static String NETWORK_SETTINGS_ACTIVE_CLIENT;
    public static String INVALID_PORT;
    public static String CURRENT_NETWORK_ADDRESS;
    public static String MENU_GAME;
    public static String MENU_SETTINGS;
    public static String MENU_SWITCH_PROFILE;
    public static String MENU_STATISTICS;
    public static String MENU_ONLINE_ACCOUNT;
    public static String MENU_SOUNDS;
    public static String MENU_LANGUAGE;
    public static String LANGUAGE_CZECH;
    public static String LANGUAGE_ENGLISH;
    public static String LANGUAGE_CHANGE_TITLE;
    public static String LANGUAGE_CHANGE_RESTART;
    public static String MENU_ABOUT;
    public static String MENU_EXIT;
    public static String PROFILE_TITLE;
    public static String PROFILE_SELECT_PROMPT;
    public static String PROFILE_CONTINUE;
    public static String PROFILE_NEW;
    public static String PROFILE_RENAME;
    public static String PROFILE_ARCHIVE;
    public static String PROFILE_EXIT;
    public static String PROFILE_NAME_PROMPT;
    public static String PROFILE_FIRST_NAME_PROMPT;
    public static String PROFILE_NAME_REQUIRED;
    public static String PROFILE_LAST_CANNOT_ARCHIVE;
    public static String PROFILE_GUEST;
    public static String PROFILE_OPPONENT_PROMPT;
    public static String PROFILE_OPPONENT_TITLE;
    public static String PROFILE_NETWORK_CHANGE_ONLY_AT_START;
    public static String ONLINE_ACCOUNT_TITLE;
    public static String ONLINE_SIGNED_OUT;
    public static String ONLINE_LOGIN;
    public static String ONLINE_LOGOUT;
    public static String ONLINE_RETRY;
    public static String ONLINE_CLOSE;
    public static String ONLINE_LOADING;
    public static String ONLINE_BROWSER_WAIT;
    public static String ONLINE_LOGGING_OUT;
    public static String ONLINE_LOGGED_OUT;
    public static String ONLINE_SESSION_EXPIRED;
    public static String ONLINE_SESSION_RESTORE_FAILED;
    public static String ONLINE_ONBOARDING_REQUIRED;
    public static String ONLINE_HANDLE;
    public static String ONLINE_DISPLAY_NAME;
    public static String ONLINE_CREATE_PROFILE;
    public static String ONLINE_SAVE_PROFILE;
    public static String ONLINE_PROFILE_SAVING;
    public static String ONLINE_LOCAL_PROFILE;
    public static String ONLINE_PLAYER_ID;
    public static String ONLINE_INSTALLATION_ID;
    public static String ONLINE_PROFILE_NOT_LINKED;
    public static String ONLINE_PROFILE_LINKED;
    public static String ONLINE_PROFILE_LINKED_ELSEWHERE;
    public static String ONLINE_LINK_PROFILE;
    public static String ONLINE_UNLINK_PROFILE;
    public static String ONLINE_LINKING_PROFILE;
    public static String ONLINE_UNLINKING_PROFILE;
    public static String ONLINE_UNLINK_CONFIRM;
    public static String ONLINE_SYNC_STATUS;
    public static String ONLINE_SYNC_NOW;
    public static String ONLINE_SYNCING;
    public static String DATABASE_ERROR_TITLE;
    public static String DATABASE_NEWER_SCHEMA;
    public static String DATABASE_INITIALIZATION_FAILED;
    public static String DATABASE_DIRECTORY_CREATE_FAILED;
    public static String DATABASE_SQLITE_DRIVER_MISSING;
    public static String DATABASE_READ_FAILED;
    public static String GAME_RESULT_SAVE_FAILED;
    public static String SYNC_STATE_LOAD_FAILED;
    public static String SYNC_STATE_SAVE_FAILED;
    public static String PROFILE_LIST_LOAD_FAILED;
    public static String PROFILE_SELECTED_LOAD_FAILED;
    public static String PROFILE_NOT_FOUND;
    public static String PROFILE_ARCHIVE_FAILED;
    public static String PROFILE_ARCHIVED_CANNOT_SELECT;
    public static String PROFILE_SELECTION_SAVE_FAILED;
    public static String PROFILE_LOAD_FAILED;
    public static String PROFILE_DUPLICATE_NAME;
    public static String PROFILE_SAVE_FAILED;
    public static String INSTALLATION_ID_FAILED;
    public static String PROFILE_LINK_LOAD_FAILED;
    public static String PROFILE_LINK_SAVE_FAILED;
    public static String PROFILE_UNLINK_FAILED;
    public static String PROFILE_LINK_DIFFERENT_ACCOUNT;
    public static String STATISTICS_TITLE;
    public static String STATISTICS_LOCAL_LEADERBOARD;
    public static String STATISTICS_CURRENT_PROFILE_MISSING;
    public static String STATISTICS_ARCHIVED_PROFILE_SUFFIX;
    public static String STATISTICS_LOAD_FAILED;
    public static String STATISTICS_COLUMN_POSITION;
    public static String STATISTICS_COLUMN_PROFILE;
    public static String STATISTICS_COLUMN_GAMES;
    public static String STATISTICS_COLUMN_WINS;
    public static String STATISTICS_COLUMN_DRAWS;
    public static String STATISTICS_COLUMN_LOSSES;
    public static String STATISTICS_COLUMN_SCORE;
    public static String STATISTICS_COLUMN_WIN_PERCENTAGE;
    public static String STATISTICS_TAB_LOCAL;
    public static String STATISTICS_TAB_GLOBAL;
    public static String STATISTICS_TAB_RANKED;
    public static String STATISTICS_GLOBAL_MY_POSITION;
    public static String STATISTICS_GLOBAL_LOGGED_OUT;
    public static String STATISTICS_GLOBAL_NO_RESULT;
    public static String STATISTICS_GLOBAL_EMPTY;
    public static String STATISTICS_GLOBAL_LOADING;
    public static String STATISTICS_GLOBAL_UNAVAILABLE;
    public static String STATISTICS_GLOBAL_RETRY;
    public static String STATISTICS_GLOBAL_REFRESH;
    public static String STATISTICS_GLOBAL_PREVIOUS;
    public static String STATISTICS_GLOBAL_NEXT;
    public static String STATISTICS_RANKED_UNAVAILABLE;
    public static String ABOUT_TITLE;
    public static String CHANGE_SIZE_TITLE;
    public static String OPTION_YES;
    public static String OPTION_NO;
    public static String OPTION_OK;
    public static String OPTION_CANCEL;
    public static String RESTART_BUTTON;
    public static String RESTART_TITLE;
    public static String RESTART_CONFIRM;
    public static String RESTART_WAITING_FOR_CLIENT;
    public static String RESTART_REQUEST_SENT;
    public static String RESTART_REQUEST_FROM_CLIENT;
    public static String RESTART_REQUEST_FROM_HOST;
    public static String RESTART_DECLINED_BY_CLIENT;
    public static String RESTART_DECLINED_BY_HOST;
    public static String RESTART_HOST_BUSY;
    public static String GAME_OVER_TITLE;
    public static String NEW_GAME_PROMPT;
    public static String NETWORK_GAME_TITLE;
    public static String NETWORK_HOST_ENDED;
    public static String NETWORK_CONNECT_FAILED;
    public static String NETWORK_INCOMPATIBLE_BUILD;
    public static String NETWORK_INCOMPATIBLE_PROTOCOL;
    public static String INVALID_SIZE_MESSAGE;
    public static String BUILD_INFO_PREFIX;
    public static String BUILD_FILE_TIME_PREFIX;
    public static String BUILD_INFO_UNKNOWN;
    public static String HELP_TEXT;

    private static Language language;
    private static Language preferredLanguage;
    private static ResourceBundle bundle;

    static {
        applyLanguage(loadLanguage());
    }

    private Messages() {
    }

    public static Language language() {
        return language;
    }

    public static Language preferredLanguage() {
        return preferredLanguage;
    }

    /** Stores a language choice that will be applied on the next application start. */
    public static void saveLanguageForNextStart(Language selected) {
        if (selected == null) {
            return;
        }
        try {
            PREFERENCES.put(LANGUAGE_PREFERENCE, selected.code());
            preferredLanguage = selected;
        } catch (SecurityException ignored) {
            // The application remains usable with the current language.
        }
    }

    static void useLanguageForTests(Language selected) {
        applyLanguage(selected);
    }

    public static String text(String key, Object... arguments) {
        String pattern = bundle.getString(key);
        return arguments.length == 0
                ? pattern : new MessageFormat(pattern, language.locale()).format(arguments);
    }

    public static String hostInfo(String address, int port, int rows, int columns) {
        return text("HOST_INFO", address, Integer.valueOf(port), boardSize(rows, columns));
    }

    public static String onlineSignedIn(String displayName, String handle) {
        return text("ONLINE_SIGNED_IN", displayName, handle);
    }

    public static String clientInfo(String host, int port, int rows, int columns) {
        return text("CLIENT_INFO", host, Integer.valueOf(port), boardSize(rows, columns));
    }

    public static String localInfo(int rows, int columns, String redPlayerName, String bluePlayerName) {
        return text("LOCAL_INFO", boardSize(rows, columns), redPlayerName, bluePlayerName);
    }

    public static String computerInfo(int rows, int columns, String redPlayerName, String difficulty) {
        return text("COMPUTER_INFO", boardSize(rows, columns), redPlayerName,
                difficulty.toLowerCase(language.locale()));
    }

    public static String waitingForClient() {
        return text("WAITING_FOR_CLIENT");
    }

    public static String networkPlayersStatus(String redPlayerName, String bluePlayerName) {
        return text("NETWORK_PLAYERS_STATUS", redPlayerName, bluePlayerName);
    }

    public static String settingsRestartConfirm() {
        return text("SETTINGS_RESTART_CONFIRM");
    }

    public static String buildMismatch(String hostBuild, String clientBuild) {
        return text("BUILD_MISMATCH", hostBuild, clientBuild);
    }

    public static String profileArchiveConfirm(String displayName) {
        return text("PROFILE_ARCHIVE_CONFIRM", displayName);
    }

    public static String statisticsCurrentProfile(String displayName) {
        return text("STATISTICS_CURRENT_PROFILE", displayName);
    }

    public static String statisticsRecord(long games, long wins, long draws, long losses) {
        return text("STATISTICS_RECORD", Long.valueOf(games), Long.valueOf(wins),
                Long.valueOf(draws), Long.valueOf(losses));
    }

    public static String statisticsScore(long totalScore, double winPercentage) {
        return text("STATISTICS_SCORE", Long.valueOf(totalScore), formatWinPercentage(winPercentage));
    }

    public static String statisticsGlobalPlayer(String displayName, String handle) {
        return text("STATISTICS_GLOBAL_PLAYER", displayName, handle);
    }

    public static String statisticsGlobalRank(long rank) {
        return text("STATISTICS_GLOBAL_RANK", Long.valueOf(rank));
    }

    public static String statisticsGlobalUpdated(java.time.Instant generatedAt) {
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter
                .ofLocalizedDateTime(java.time.format.FormatStyle.SHORT)
                .withLocale(language.locale())
                .withZone(java.time.ZoneId.systemDefault());
        return text("STATISTICS_GLOBAL_UPDATED", formatter.format(generatedAt));
    }

    public static String statisticsProfileName(PlayerProfile profile) {
        return profile.displayName() + (profile.archived() ? STATISTICS_ARCHIVED_PROFILE_SUFFIX : "");
    }

    public static String formatWinPercentage(double winPercentage) {
        return String.format(language.locale(), "%.1f %%", Double.valueOf(winPercentage));
    }

    public static String databaseInitializationFailed(String detail) {
        return text("DATABASE_INITIALIZATION_FAILED_DETAIL", detail);
    }

    public static String databaseSaveFailed(String detail) {
        return text("DATABASE_SAVE_FAILED_DETAIL", detail);
    }

    public static String onlineSyncStatus(SyncSummary summary) {
        return text("ONLINE_SYNC_STATUS_FORMAT", Long.valueOf(summary.pending()),
                Long.valueOf(summary.sending()), Long.valueOf(summary.sent()),
                Long.valueOf(summary.failed()), Long.valueOf(summary.pendingPeer()),
                Long.valueOf(summary.matched()), Long.valueOf(summary.conflicted()));
    }

    public static String aboutText() {
        return APP_TITLE + "\n" + BuildInfo.displayText() + "\n\n" + HELP_TEXT;
    }

    public static String redWins(int redScore, int blueScore) {
        return text("RED_WINS", Integer.valueOf(redScore), Integer.valueOf(blueScore));
    }

    public static String blueWins(int blueScore, int redScore) {
        return text("BLUE_WINS", Integer.valueOf(blueScore), Integer.valueOf(redScore));
    }

    public static String draw(int redScore, int blueScore) {
        return text("DRAW", Integer.valueOf(redScore), Integer.valueOf(blueScore));
    }

    public static String gameOver(GameResult result) {
        if (result.finishReason() == GameResult.FinishReason.TIME_LIMIT) {
            return result.redPlayer().outcome() == PlayerResult.Outcome.LOSS
                    ? redLostOnTime() : blueLostOnTime();
        }
        int redScore = result.redPlayer().score();
        int blueScore = result.bluePlayer().score();
        if (result.redPlayer().outcome() == PlayerResult.Outcome.WIN) {
            return redWins(redScore, blueScore);
        }
        if (result.bluePlayer().outcome() == PlayerResult.Outcome.WIN) {
            return blueWins(blueScore, redScore);
        }
        return draw(redScore, blueScore);
    }

    public static String redLostOnTime() {
        return text("RED_LOST_ON_TIME");
    }

    public static String blueLostOnTime() {
        return text("BLUE_LOST_ON_TIME");
    }

    private static Language loadLanguage() {
        String stored = null;
        try {
            stored = PREFERENCES.get(LANGUAGE_PREFERENCE, null);
        } catch (SecurityException ignored) {
            // Use the operating-system language below.
        }
        Language selected = Language.fromCode(stored);
        if (selected != null) {
            return selected;
        }
        return "cs".equalsIgnoreCase(Locale.getDefault().getLanguage())
                ? Language.CZECH : Language.ENGLISH;
    }

    private static void applyLanguage(Language selected) {
        language = selected == null ? Language.CZECH : selected;
        preferredLanguage = language;
        Locale.setDefault(language.locale());
        bundle = ResourceBundle.getBundle(
                BUNDLE_NAME, language.locale(), new Utf8ResourceControl());
        reloadConstants();
    }

    private static void reloadConstants() {
        APP_TITLE = text("APP_TITLE"); PLAYER_RED = text("PLAYER_RED");
        PLAYER_BLUE = text("PLAYER_BLUE"); PLAYER_GUEST = text("PLAYER_GUEST");
        PLAYER_CPU = text("PLAYER_CPU"); WINDOW_HOST = text("WINDOW_HOST");
        WINDOW_CLIENT = text("WINDOW_CLIENT"); WINDOW_LOCAL = text("WINDOW_LOCAL");
        WINDOW_COMPUTER = text("WINDOW_COMPUTER"); GAME_MODE_LOCAL = text("GAME_MODE_LOCAL");
        GAME_MODE_COMPUTER = text("GAME_MODE_COMPUTER"); GAME_MODE_HOST = text("GAME_MODE_HOST");
        GAME_MODE_JOIN = text("GAME_MODE_JOIN"); GAME_MODE_PROMPT = text("GAME_MODE_PROMPT");
        HOST_ADDRESS_PROMPT = text("HOST_ADDRESS_PROMPT"); CONNECTING_TO_HOST = text("CONNECTING_TO_HOST");
        CHAT_HOST_TITLE = text("CHAT_HOST_TITLE"); CHAT_CLIENT_TITLE = text("CHAT_CLIENT_TITLE");
        CHAT_SEND = text("CHAT_SEND"); CHAT_EMOTICONS = text("CHAT_EMOTICONS");
        CHAT_YOU = text("CHAT_YOU"); CHAT_HOST = text("CHAT_HOST"); CHAT_CLIENT = text("CHAT_CLIENT");
        GAME_OPTIONS_TITLE = text("GAME_OPTIONS_TITLE"); GAME_OPTIONS_BOARD_SIZE = text("GAME_OPTIONS_BOARD_SIZE");
        GAME_OPTIONS_THINK_TIME = text("GAME_OPTIONS_THINK_TIME"); GAME_OPTIONS_DIFFICULTY = text("GAME_OPTIONS_DIFFICULTY");
        GAME_OPTIONS_RANDOM_EDGES = text("GAME_OPTIONS_RANDOM_EDGES"); THINK_TIME_NONE = text("THINK_TIME_NONE");
        DIFFICULTY_EASY = text("DIFFICULTY_EASY"); DIFFICULTY_MEDIUM = text("DIFFICULTY_MEDIUM");
        DIFFICULTY_HARD = text("DIFFICULTY_HARD"); BOARD_SIZE_PROMPT = text("BOARD_SIZE_PROMPT");
        BOARD_SIZE_TITLE = text("BOARD_SIZE_TITLE"); ADAPTER_PROMPT = text("ADAPTER_PROMPT");
        ADAPTER_TITLE = text("ADAPTER_TITLE"); NO_NETWORK_ADAPTER = text("NO_NETWORK_ADAPTER");
        NETWORK_SETTINGS_ADAPTER = text("NETWORK_SETTINGS_ADAPTER"); NETWORK_SETTINGS_PORT = text("NETWORK_SETTINGS_PORT");
        NETWORK_SETTINGS_ACTIVE_CLIENT = text("NETWORK_SETTINGS_ACTIVE_CLIENT"); INVALID_PORT = text("INVALID_PORT");
        CURRENT_NETWORK_ADDRESS = text("CURRENT_NETWORK_ADDRESS"); MENU_GAME = text("MENU_GAME");
        MENU_SETTINGS = text("MENU_SETTINGS"); MENU_SWITCH_PROFILE = text("MENU_SWITCH_PROFILE");
        MENU_STATISTICS = text("MENU_STATISTICS"); MENU_ONLINE_ACCOUNT = text("MENU_ONLINE_ACCOUNT");
        MENU_SOUNDS = text("MENU_SOUNDS"); MENU_LANGUAGE = text("MENU_LANGUAGE");
        LANGUAGE_CZECH = text("LANGUAGE_CZECH"); LANGUAGE_ENGLISH = text("LANGUAGE_ENGLISH");
        LANGUAGE_CHANGE_TITLE = text("LANGUAGE_CHANGE_TITLE"); LANGUAGE_CHANGE_RESTART = text("LANGUAGE_CHANGE_RESTART");
        MENU_ABOUT = text("MENU_ABOUT"); MENU_EXIT = text("MENU_EXIT"); PROFILE_TITLE = text("PROFILE_TITLE");
        PROFILE_SELECT_PROMPT = text("PROFILE_SELECT_PROMPT"); PROFILE_CONTINUE = text("PROFILE_CONTINUE");
        PROFILE_NEW = text("PROFILE_NEW"); PROFILE_RENAME = text("PROFILE_RENAME");
        PROFILE_ARCHIVE = text("PROFILE_ARCHIVE"); PROFILE_EXIT = text("PROFILE_EXIT");
        PROFILE_NAME_PROMPT = text("PROFILE_NAME_PROMPT"); PROFILE_FIRST_NAME_PROMPT = text("PROFILE_FIRST_NAME_PROMPT");
        PROFILE_NAME_REQUIRED = text("PROFILE_NAME_REQUIRED"); PROFILE_LAST_CANNOT_ARCHIVE = text("PROFILE_LAST_CANNOT_ARCHIVE");
        PROFILE_GUEST = text("PROFILE_GUEST"); PROFILE_OPPONENT_PROMPT = text("PROFILE_OPPONENT_PROMPT");
        PROFILE_OPPONENT_TITLE = text("PROFILE_OPPONENT_TITLE"); PROFILE_NETWORK_CHANGE_ONLY_AT_START = text("PROFILE_NETWORK_CHANGE_ONLY_AT_START");
        ONLINE_ACCOUNT_TITLE = text("ONLINE_ACCOUNT_TITLE"); ONLINE_SIGNED_OUT = text("ONLINE_SIGNED_OUT");
        ONLINE_LOGIN = text("ONLINE_LOGIN"); ONLINE_LOGOUT = text("ONLINE_LOGOUT"); ONLINE_RETRY = text("ONLINE_RETRY");
        ONLINE_CLOSE = text("ONLINE_CLOSE"); ONLINE_LOADING = text("ONLINE_LOADING"); ONLINE_BROWSER_WAIT = text("ONLINE_BROWSER_WAIT");
        ONLINE_LOGGING_OUT = text("ONLINE_LOGGING_OUT"); ONLINE_LOGGED_OUT = text("ONLINE_LOGGED_OUT");
        ONLINE_SESSION_EXPIRED = text("ONLINE_SESSION_EXPIRED"); ONLINE_SESSION_RESTORE_FAILED = text("ONLINE_SESSION_RESTORE_FAILED");
        ONLINE_ONBOARDING_REQUIRED = text("ONLINE_ONBOARDING_REQUIRED"); ONLINE_HANDLE = text("ONLINE_HANDLE");
        ONLINE_DISPLAY_NAME = text("ONLINE_DISPLAY_NAME"); ONLINE_CREATE_PROFILE = text("ONLINE_CREATE_PROFILE");
        ONLINE_SAVE_PROFILE = text("ONLINE_SAVE_PROFILE"); ONLINE_PROFILE_SAVING = text("ONLINE_PROFILE_SAVING");
        ONLINE_LOCAL_PROFILE = text("ONLINE_LOCAL_PROFILE"); ONLINE_PLAYER_ID = text("ONLINE_PLAYER_ID");
        ONLINE_INSTALLATION_ID = text("ONLINE_INSTALLATION_ID"); ONLINE_PROFILE_NOT_LINKED = text("ONLINE_PROFILE_NOT_LINKED");
        ONLINE_PROFILE_LINKED = text("ONLINE_PROFILE_LINKED"); ONLINE_PROFILE_LINKED_ELSEWHERE = text("ONLINE_PROFILE_LINKED_ELSEWHERE");
        ONLINE_LINK_PROFILE = text("ONLINE_LINK_PROFILE"); ONLINE_UNLINK_PROFILE = text("ONLINE_UNLINK_PROFILE");
        ONLINE_LINKING_PROFILE = text("ONLINE_LINKING_PROFILE"); ONLINE_UNLINKING_PROFILE = text("ONLINE_UNLINKING_PROFILE");
        ONLINE_UNLINK_CONFIRM = text("ONLINE_UNLINK_CONFIRM"); ONLINE_SYNC_STATUS = text("ONLINE_SYNC_STATUS");
        ONLINE_SYNC_NOW = text("ONLINE_SYNC_NOW"); ONLINE_SYNCING = text("ONLINE_SYNCING");
        DATABASE_ERROR_TITLE = text("DATABASE_ERROR_TITLE"); DATABASE_NEWER_SCHEMA = text("DATABASE_NEWER_SCHEMA");
        DATABASE_INITIALIZATION_FAILED = text("DATABASE_INITIALIZATION_FAILED"); DATABASE_DIRECTORY_CREATE_FAILED = text("DATABASE_DIRECTORY_CREATE_FAILED");
        DATABASE_SQLITE_DRIVER_MISSING = text("DATABASE_SQLITE_DRIVER_MISSING"); DATABASE_READ_FAILED = text("DATABASE_READ_FAILED");
        GAME_RESULT_SAVE_FAILED = text("GAME_RESULT_SAVE_FAILED"); SYNC_STATE_LOAD_FAILED = text("SYNC_STATE_LOAD_FAILED");
        SYNC_STATE_SAVE_FAILED = text("SYNC_STATE_SAVE_FAILED"); PROFILE_LIST_LOAD_FAILED = text("PROFILE_LIST_LOAD_FAILED");
        PROFILE_SELECTED_LOAD_FAILED = text("PROFILE_SELECTED_LOAD_FAILED"); PROFILE_NOT_FOUND = text("PROFILE_NOT_FOUND");
        PROFILE_ARCHIVE_FAILED = text("PROFILE_ARCHIVE_FAILED"); PROFILE_ARCHIVED_CANNOT_SELECT = text("PROFILE_ARCHIVED_CANNOT_SELECT");
        PROFILE_SELECTION_SAVE_FAILED = text("PROFILE_SELECTION_SAVE_FAILED"); PROFILE_LOAD_FAILED = text("PROFILE_LOAD_FAILED");
        PROFILE_DUPLICATE_NAME = text("PROFILE_DUPLICATE_NAME"); PROFILE_SAVE_FAILED = text("PROFILE_SAVE_FAILED");
        INSTALLATION_ID_FAILED = text("INSTALLATION_ID_FAILED"); PROFILE_LINK_LOAD_FAILED = text("PROFILE_LINK_LOAD_FAILED");
        PROFILE_LINK_SAVE_FAILED = text("PROFILE_LINK_SAVE_FAILED"); PROFILE_UNLINK_FAILED = text("PROFILE_UNLINK_FAILED");
        PROFILE_LINK_DIFFERENT_ACCOUNT = text("PROFILE_LINK_DIFFERENT_ACCOUNT"); STATISTICS_TITLE = text("STATISTICS_TITLE");
        STATISTICS_LOCAL_LEADERBOARD = text("STATISTICS_LOCAL_LEADERBOARD"); STATISTICS_CURRENT_PROFILE_MISSING = text("STATISTICS_CURRENT_PROFILE_MISSING");
        STATISTICS_ARCHIVED_PROFILE_SUFFIX = text("STATISTICS_ARCHIVED_PROFILE_SUFFIX"); STATISTICS_LOAD_FAILED = text("STATISTICS_LOAD_FAILED");
        STATISTICS_COLUMN_POSITION = text("STATISTICS_COLUMN_POSITION"); STATISTICS_COLUMN_PROFILE = text("STATISTICS_COLUMN_PROFILE");
        STATISTICS_COLUMN_GAMES = text("STATISTICS_COLUMN_GAMES"); STATISTICS_COLUMN_WINS = text("STATISTICS_COLUMN_WINS");
        STATISTICS_COLUMN_DRAWS = text("STATISTICS_COLUMN_DRAWS"); STATISTICS_COLUMN_LOSSES = text("STATISTICS_COLUMN_LOSSES");
        STATISTICS_COLUMN_SCORE = text("STATISTICS_COLUMN_SCORE"); STATISTICS_COLUMN_WIN_PERCENTAGE = text("STATISTICS_COLUMN_WIN_PERCENTAGE");
        STATISTICS_TAB_LOCAL = text("STATISTICS_TAB_LOCAL"); STATISTICS_TAB_GLOBAL = text("STATISTICS_TAB_GLOBAL");
        STATISTICS_TAB_RANKED = text("STATISTICS_TAB_RANKED"); STATISTICS_GLOBAL_MY_POSITION = text("STATISTICS_GLOBAL_MY_POSITION");
        STATISTICS_GLOBAL_LOGGED_OUT = text("STATISTICS_GLOBAL_LOGGED_OUT"); STATISTICS_GLOBAL_NO_RESULT = text("STATISTICS_GLOBAL_NO_RESULT");
        STATISTICS_GLOBAL_EMPTY = text("STATISTICS_GLOBAL_EMPTY"); STATISTICS_GLOBAL_LOADING = text("STATISTICS_GLOBAL_LOADING");
        STATISTICS_GLOBAL_UNAVAILABLE = text("STATISTICS_GLOBAL_UNAVAILABLE"); STATISTICS_GLOBAL_RETRY = text("STATISTICS_GLOBAL_RETRY");
        STATISTICS_GLOBAL_REFRESH = text("STATISTICS_GLOBAL_REFRESH"); STATISTICS_GLOBAL_PREVIOUS = text("STATISTICS_GLOBAL_PREVIOUS");
        STATISTICS_GLOBAL_NEXT = text("STATISTICS_GLOBAL_NEXT"); STATISTICS_RANKED_UNAVAILABLE = text("STATISTICS_RANKED_UNAVAILABLE");
        ABOUT_TITLE = text("ABOUT_TITLE"); CHANGE_SIZE_TITLE = text("CHANGE_SIZE_TITLE"); OPTION_YES = text("OPTION_YES");
        OPTION_NO = text("OPTION_NO"); OPTION_OK = text("OPTION_OK"); OPTION_CANCEL = text("OPTION_CANCEL");
        RESTART_BUTTON = text("RESTART_BUTTON"); RESTART_TITLE = text("RESTART_TITLE"); RESTART_CONFIRM = text("RESTART_CONFIRM");
        RESTART_WAITING_FOR_CLIENT = text("RESTART_WAITING_FOR_CLIENT"); RESTART_REQUEST_SENT = text("RESTART_REQUEST_SENT");
        RESTART_REQUEST_FROM_CLIENT = text("RESTART_REQUEST_FROM_CLIENT"); RESTART_REQUEST_FROM_HOST = text("RESTART_REQUEST_FROM_HOST");
        RESTART_DECLINED_BY_CLIENT = text("RESTART_DECLINED_BY_CLIENT"); RESTART_DECLINED_BY_HOST = text("RESTART_DECLINED_BY_HOST");
        RESTART_HOST_BUSY = text("RESTART_HOST_BUSY"); GAME_OVER_TITLE = text("GAME_OVER_TITLE"); NEW_GAME_PROMPT = text("NEW_GAME_PROMPT");
        NETWORK_GAME_TITLE = text("NETWORK_GAME_TITLE"); NETWORK_HOST_ENDED = text("NETWORK_HOST_ENDED");
        NETWORK_CONNECT_FAILED = text("NETWORK_CONNECT_FAILED"); NETWORK_INCOMPATIBLE_BUILD = text("NETWORK_INCOMPATIBLE_BUILD");
        NETWORK_INCOMPATIBLE_PROTOCOL = text("NETWORK_INCOMPATIBLE_PROTOCOL"); INVALID_SIZE_MESSAGE = text("INVALID_SIZE_MESSAGE");
        BUILD_INFO_PREFIX = text("BUILD_INFO_PREFIX"); BUILD_FILE_TIME_PREFIX = text("BUILD_FILE_TIME_PREFIX");
        BUILD_INFO_UNKNOWN = text("BUILD_INFO_UNKNOWN"); HELP_TEXT = text("HELP_TEXT");
    }

    private static String boardSize(int rows, int columns) {
        return rows + "x" + columns;
    }

    public enum Language {
        CZECH("cs", Locale.forLanguageTag("cs-CZ")),
        ENGLISH("en", Locale.ENGLISH);

        private final String code;
        private final Locale locale;

        Language(String code, Locale locale) {
            this.code = code;
            this.locale = locale;
        }

        public String code() {
            return code;
        }

        public Locale locale() {
            return locale;
        }

        static Language fromCode(String code) {
            for (Language candidate : values()) {
                if (candidate.code.equalsIgnoreCase(code == null ? "" : code)) {
                    return candidate;
                }
            }
            return null;
        }
    }

    private static final class Utf8ResourceControl extends ResourceBundle.Control {
        @Override
        public ResourceBundle newBundle(String baseName, Locale locale, String format,
                                        ClassLoader loader, boolean reload)
                throws IOException {
            if (!"java.properties".equals(format)) {
                return null;
            }
            String bundleName = toBundleName(baseName, locale);
            String resourceName = toResourceName(bundleName, "properties");
            URL resource = loader.getResource(resourceName);
            if (resource == null) {
                return null;
            }
            URLConnection connection = resource.openConnection();
            if (reload) {
                connection.setUseCaches(false);
            }
            try (InputStream input = connection.getInputStream();
                 InputStreamReader reader = new InputStreamReader(input, "UTF-8")) {
                return new PropertyResourceBundle(reader);
            }
        }
    }
}
