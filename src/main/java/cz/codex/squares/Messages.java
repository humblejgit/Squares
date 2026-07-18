package cz.codex.squares;

final class Messages {
    static final String APP_TITLE = "Squares";
    static final String PLAYER_RED = "\u010cerven\u00fd hr\u00e1\u010d";
    static final String PLAYER_BLUE = "Modr\u00fd hr\u00e1\u010d";
    static final String PLAYER_GUEST = "Host";
    static final String PLAYER_CPU = "CPU";
    static final String WINDOW_HOST = "Squares - hostitel (\u010derven\u00fd)";
    static final String WINDOW_CLIENT = "Squares - klient (modr\u00fd)";
    static final String WINDOW_LOCAL = "Squares - lok\u00e1ln\u00ed hra";
    static final String WINDOW_COMPUTER = "Squares - proti po\u010d\u00edta\u010di";

    static final String GAME_MODE_LOCAL = "\u010clov\u011bk vs. \u010dlov\u011bk";
    static final String GAME_MODE_COMPUTER = "\u010clov\u011bk vs. CPU";
    static final String GAME_MODE_HOST = "S\u00ed\u0165ov\u00e1 hra - server";
    static final String GAME_MODE_JOIN = "S\u00ed\u0165ov\u00e1 hra - klient";
    static final String GAME_MODE_PROMPT = "RE\u017dIM HRY";

    static final String HOST_ADDRESS_PROMPT = "IP adresa hostitele:";
    static final String CONNECTING_TO_HOST = "P\u0159ipojov\u00e1n\u00ed k hostiteli...";
    static final String CHAT_HOST_TITLE = "Chat";
    static final String CHAT_CLIENT_TITLE = "Chat";
    static final String CHAT_SEND = "Odeslat";
    static final String CHAT_EMOTICONS = "Emotikony";
    static final String CHAT_YOU = "J\u00e1";
    static final String CHAT_HOST = "Hostitel";
    static final String CHAT_CLIENT = "Klient";
    static final String GAME_OPTIONS_TITLE = "Squares - nastaven\u00ed hry";
    static final String GAME_OPTIONS_BOARD_SIZE = "Velikost pole:";
    static final String GAME_OPTIONS_THINK_TIME = "Max. \u010das na p\u0159em\u00fd\u0161len\u00ed:";
    static final String GAME_OPTIONS_DIFFICULTY = "Obt\u00ed\u017enost po\u010d\u00edta\u010de:";
    static final String GAME_OPTIONS_RANDOM_EDGES = "N\u00e1hodn\u00e9 vygenerov\u00e1n\u00ed hran";
    static final String THINK_TIME_NONE = "Bez limitu";
    static final String DIFFICULTY_EASY = "Lehk\u00e1";
    static final String DIFFICULTY_MEDIUM = "St\u0159edn\u00ed";
    static final String DIFFICULTY_HARD = "T\u011b\u017ek\u00e1";
    static final String BOARD_SIZE_PROMPT = "Vyber velikost hrac\u00ed plochy:";
    static final String BOARD_SIZE_TITLE = "Squares - velikost";
    static final String ADAPTER_PROMPT = "Vyber s\u00ed\u0165ov\u00fd adapt\u00e9r pro IP adresu serveru:";
    static final String ADAPTER_TITLE = "Squares - adapt\u00e9r";
    static final String NO_NETWORK_ADAPTER = "Nebyl nalezen \u017e\u00e1dn\u00fd vhodn\u00fd s\u00ed\u0165ov\u00fd adapt\u00e9r.\n\nHostitelskou hru nelze spustit.";
    static final String NETWORK_SETTINGS_ADAPTER = "S\u00ed\u0165ov\u00fd adapt\u00e9r:";
    static final String NETWORK_SETTINGS_PORT = "Port:";
    static final String NETWORK_SETTINGS_ACTIVE_CLIENT = "S\u00ed\u0165ov\u00fd adapt\u00e9r a port nelze m\u011bnit po p\u0159ipojen\u00ed klienta.";
    static final String INVALID_PORT = "Port mus\u00ed b\u00fdt \u010d\u00edslo od 1 do 65535.";
    static final String CURRENT_NETWORK_ADDRESS = "aktu\u00e1ln\u00ed adresa";
    static final String MENU_GAME = "Hra";
    static final String MENU_SETTINGS = "Nastaven\u00ed";
    static final String MENU_SWITCH_PROFILE = "P\u0159epnout profil";
    static final String MENU_STATISTICS = "Statistiky";
    static final String MENU_SOUNDS = "Zvuky";
    static final String MENU_ABOUT = "O h\u0159e";
    static final String MENU_EXIT = "Ukon\u010dit";
    static final String PROFILE_TITLE = "U\u017eivatelsk\u00fd profil";
    static final String PROFILE_SELECT_PROMPT = "Vyberte profil pro tuto hru:";
    static final String PROFILE_CONTINUE = "Pokra\u010dovat";
    static final String PROFILE_NEW = "Nov\u00fd";
    static final String PROFILE_RENAME = "P\u0159ejmenovat";
    static final String PROFILE_ARCHIVE = "Archivovat";
    static final String PROFILE_EXIT = "Konec";
    static final String PROFILE_NAME_PROMPT = "Jm\u00e9no profilu:";
    static final String PROFILE_FIRST_NAME_PROMPT = "Vytvo\u0159te prvn\u00ed profil hr\u00e1\u010de:";
    static final String PROFILE_NAME_REQUIRED = "Jm\u00e9no profilu nesm\u00ed b\u00fdt pr\u00e1zdn\u00e9.";
    static final String PROFILE_LAST_CANNOT_ARCHIVE = "Posledn\u00ed aktivn\u00ed profil nelze archivovat.";
    static final String PROFILE_GUEST = "Host (bez profilu)";
    static final String PROFILE_OPPONENT_PROMPT = "Vyberte profil modr\u00e9ho hr\u00e1\u010de:";
    static final String PROFILE_OPPONENT_TITLE = "Druh\u00fd hr\u00e1\u010d";
    static final String PROFILE_NETWORK_CHANGE_ONLY_AT_START =
            "Profil v s\u00ed\u0165ov\u00e9 h\u0159e lze m\u011bnit jen p\u0159i spust\u011bn\u00ed aplikace.";
    static final String DATABASE_ERROR_TITLE = "Lok\u00e1ln\u00ed datab\u00e1ze";
    static final String DATABASE_NEWER_SCHEMA = "Datab\u00e1ze poch\u00e1z\u00ed z nov\u011bj\u0161\u00ed verze aplikace.";
    static final String DATABASE_INITIALIZATION_FAILED = "Inicializace lok\u00e1ln\u00ed datab\u00e1ze selhala.";
    static final String DATABASE_DIRECTORY_CREATE_FAILED = "Nelze vytvo\u0159it slo\u017eku lok\u00e1ln\u00ed datab\u00e1ze.";
    static final String DATABASE_SQLITE_DRIVER_MISSING = "V aplikaci chyb\u00ed ovlada\u010d SQLite.";
    static final String DATABASE_READ_FAILED = "Na\u010dten\u00ed lok\u00e1ln\u00ed datab\u00e1ze selhalo.";
    static final String GAME_RESULT_SAVE_FAILED = "Ulo\u017een\u00ed v\u00fdsledku hry selhalo.";
    static final String PROFILE_LIST_LOAD_FAILED = "Na\u010dten\u00ed profil\u016f selhalo.";
    static final String PROFILE_SELECTED_LOAD_FAILED = "Na\u010dten\u00ed vybran\u00e9ho profilu selhalo.";
    static final String PROFILE_NOT_FOUND = "Profil nebyl nalezen.";
    static final String PROFILE_ARCHIVE_FAILED = "Archivace profilu selhala.";
    static final String PROFILE_ARCHIVED_CANNOT_SELECT = "Archivovan\u00fd profil nelze vybrat.";
    static final String PROFILE_SELECTION_SAVE_FAILED = "V\u00fdb\u011br profilu se nepoda\u0159ilo ulo\u017eit.";
    static final String PROFILE_LOAD_FAILED = "Na\u010dten\u00ed profilu selhalo.";
    static final String PROFILE_DUPLICATE_NAME = "Profil s t\u00edmto jm\u00e9nem ji\u017e existuje.";
    static final String PROFILE_SAVE_FAILED = "Ulo\u017een\u00ed profilu selhalo.";
    static final String STATISTICS_TITLE = "Statistiky a m\u00edstn\u00ed \u017eeb\u0159\u00ed\u010dek";
    static final String STATISTICS_LOCAL_LEADERBOARD = "M\u00edstn\u00ed \u017eeb\u0159\u00ed\u010dek";
    static final String STATISTICS_CURRENT_PROFILE_MISSING =
            "Pro aktu\u00e1ln\u00ed profil nejsou dostupn\u00e1 data.";
    static final String STATISTICS_ARCHIVED_PROFILE_SUFFIX = " (archivovan\u00fd)";
    static final String STATISTICS_LOAD_FAILED = "Na\u010dten\u00ed statistik selhalo.";
    static final String STATISTICS_COLUMN_POSITION = "Po\u0159ad\u00ed";
    static final String STATISTICS_COLUMN_PROFILE = "Profil";
    static final String STATISTICS_COLUMN_GAMES = "Hry";
    static final String STATISTICS_COLUMN_WINS = "V\u00fdhry";
    static final String STATISTICS_COLUMN_DRAWS = "Rem\u00edzy";
    static final String STATISTICS_COLUMN_LOSSES = "Prohry";
    static final String STATISTICS_COLUMN_SCORE = "Sk\u00f3re";
    static final String STATISTICS_COLUMN_WIN_PERCENTAGE = "% v\u00fdher";
    static final String ABOUT_TITLE = "O h\u0159e";
    static final String CHANGE_SIZE_TITLE = "Zm\u011bna velikosti";
    static final String OPTION_YES = "Ano";
    static final String OPTION_NO = "Ne";
    static final String OPTION_OK = "OK";
    static final String OPTION_CANCEL = "Storno";

    static final String RESTART_BUTTON = "Restart";
    static final String RESTART_TITLE = "Restart hry";
    static final String RESTART_CONFIRM = "Chcete restartovat hru?";
    static final String RESTART_WAITING_FOR_CLIENT = "Restart bude dostupn\u00fd po p\u0159ipojen\u00ed klienta.";
    static final String RESTART_REQUEST_SENT = "\u017d\u00e1dost o restart byla odesl\u00e1na hostiteli.";
    static final String RESTART_REQUEST_FROM_CLIENT = "P\u0159ipojen\u00fd hr\u00e1\u010d si p\u0159eje restartovat hru.\n\nPovolit restart?";
    static final String RESTART_REQUEST_FROM_HOST = "Hostitel si p\u0159eje restartovat hru.\n\nPovolit restart?";
    static final String RESTART_DECLINED_BY_CLIENT = "Klient restart hry nepotvrdil.";
    static final String RESTART_DECLINED_BY_HOST = "Hostitel restart hry nepotvrdil.";
    static final String RESTART_HOST_BUSY = "Hostitel m\u00e1 pr\u00e1v\u011b otev\u0159en\u00e9 nastaven\u00ed hry.\n\nZkuste restart pozd\u011bji.";

    static final String GAME_OVER_TITLE = "Konec hry";
    static final String NEW_GAME_PROMPT = "Chcete pokra\u010dovat novou hrou?";
    static final String NETWORK_GAME_TITLE = "S\u00ed\u0165ov\u00e1 hra";
    static final String NETWORK_HOST_ENDED = "Hostitel ukon\u010dil s\u00ed\u0165ovou hru.";
    static final String NETWORK_CONNECT_FAILED = "Nepoda\u0159ilo se p\u0159ipojit nebo spojen\u00ed spadlo.";
    static final String NETWORK_INCOMPATIBLE_BUILD = "Klient a hostitel mus\u00ed m\u00edt stejn\u00fd build aplikace.";
    static final String NETWORK_INCOMPATIBLE_PROTOCOL = "Hostitel nepou\u017e\u00edv\u00e1 kompatibiln\u00ed verzi s\u00ed\u0165ov\u00e9 hry.";
    static final String INVALID_SIZE_MESSAGE = "Neplatn\u00e1 zpr\u00e1va velikosti plochy: ";
    static final String BUILD_INFO_PREFIX = "Build: ";
    static final String BUILD_FILE_TIME_PREFIX = "soubor: ";
    static final String BUILD_INFO_UNKNOWN = "nezn\u00e1m\u00fd";
    static final String HELP_TEXT = "C\u00edlem hry je uzav\u00edrat \u010dtvere\u010dky.\n"
            + "Hr\u00e1\u010di se st\u0159\u00eddaj\u00ed v ozna\u010dov\u00e1n\u00ed hran.\n"
            + "Kdo uzav\u0159e \u010dtvere\u010dek, z\u00edsk\u00e1 bod a hraje znovu.\n"
            + "Vyhr\u00e1v\u00e1 hr\u00e1\u010d s vy\u0161\u0161\u00edm po\u010dtem bod\u016f.";

    private Messages() {
    }

    static String hostInfo(String address, int port, int rows, int columns) {
        return "IP: " + address + ":" + port + "\n"
                + "Plocha: " + boardSize(rows, columns);
    }

    static String clientInfo(String host, int port, int rows, int columns) {
        return "IP: " + host + ":" + port + "\n"
                + "Plocha: " + boardSize(rows, columns);
    }

    static String localInfo(int rows, int columns, String redPlayerName, String bluePlayerName) {
        return "IP: lok\u00e1ln\u00ed hra\n"
                + "Plocha: " + boardSize(rows, columns) + "\n"
                + "Status: " + redPlayerName + " (\u010derven\u00fd hr\u00e1\u010d) vs. "
                + bluePlayerName + " (modr\u00fd hr\u00e1\u010d)";
    }

    static String computerInfo(int rows, int columns, String redPlayerName, String difficulty) {
        return "IP: lok\u00e1ln\u00ed hra\n"
                + "Plocha: " + boardSize(rows, columns) + "\n"
                + "Status: " + redPlayerName + " (\u010derven\u00fd hr\u00e1\u010d) vs. CPU - "
                + difficulty.toLowerCase(java.util.Locale.ROOT)
                + " obt\u00ed\u017enost (modr\u00fd hr\u00e1\u010d)";
    }

    static String waitingForClient() {
        return "Status: \u010dek\u00e1m na klienta...";
    }

    static String networkPlayersStatus(String redPlayerName, String bluePlayerName) {
        return "Status: " + redPlayerName + " (\u010derven\u00fd hr\u00e1\u010d) vs. "
                + bluePlayerName + " (modr\u00fd hr\u00e1\u010d)";
    }

    static String settingsRestartConfirm() {
        return "Zm\u011bna nastaven\u00ed restartuje aktu\u00e1ln\u00ed hru.\n\nChcete pokra\u010dovat?";
    }

    static String buildMismatch(String hostBuild, String clientBuild) {
        return NETWORK_INCOMPATIBLE_BUILD + "\n\n"
                + "Verze hostitele: " + hostBuild + "\n"
                + "Verze klienta: " + clientBuild;
    }

    static String profileArchiveConfirm(String displayName) {
        return "Archivovat profil \u201e" + displayName + "\u201c?\n\n"
                + "Dosavadn\u00ed v\u00fdsledky z\u016fstanou zachov\u00e1ny.";
    }

    static String statisticsCurrentProfile(String displayName) {
        return "Aktu\u00e1ln\u00ed profil: " + displayName;
    }

    static String statisticsRecord(long games, long wins, long draws, long losses) {
        return "Hry: " + games + "   V\u00fdhry: " + wins + "   Rem\u00edzy: " + draws
                + "   Prohry: " + losses;
    }

    static String statisticsScore(long totalScore, double winPercentage) {
        return "Celkov\u00e9 sk\u00f3re: " + totalScore + "   \u00dasp\u011b\u0161nost v\u00fdher: "
                + formatWinPercentage(winPercentage);
    }

    static String statisticsProfileName(PlayerProfile profile) {
        return profile.displayName()
                + (profile.archived() ? STATISTICS_ARCHIVED_PROFILE_SUFFIX : "");
    }

    static String formatWinPercentage(double winPercentage) {
        return String.format(java.util.Locale.forLanguageTag("cs-CZ"), "%.1f %%", winPercentage);
    }

    static String databaseInitializationFailed(String detail) {
        return "Lok\u00e1ln\u00ed datab\u00e1zi se nepoda\u0159ilo otev\u0159\u00edt.\n\n" + detail;
    }

    static String databaseSaveFailed(String detail) {
        return "V\u00fdsledek hry se nepoda\u0159ilo ulo\u017eit. Hru lze d\u00e1le pou\u017e\u00edvat.\n\n" + detail;
    }

    static String aboutText() {
        return APP_TITLE + "\n"
                + BuildInfo.displayText() + "\n\n"
                + HELP_TEXT;
    }

    static String redWins(int redScore, int blueScore) {
        return "\u010cerven\u00fd hr\u00e1\u010d v\u00edt\u011bz\u00ed " + redScore + ":" + blueScore + ".";
    }

    static String blueWins(int blueScore, int redScore) {
        return "Modr\u00fd hr\u00e1\u010d v\u00edt\u011bz\u00ed " + blueScore + ":" + redScore + ".";
    }

    static String draw(int redScore, int blueScore) {
        return "Rem\u00edza " + redScore + ":" + blueScore + ".";
    }

    static String gameOver(GameResult result) {
        if (result.finishReason() == GameResult.FinishReason.TIME_LIMIT) {
            return result.redPlayer().outcome() == PlayerResult.Outcome.LOSS
                    ? redLostOnTime()
                    : blueLostOnTime();
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

    static String redLostOnTime() {
        return "\u010cerven\u00e9mu hr\u00e1\u010di vypr\u0161el \u010das. Modr\u00fd hr\u00e1\u010d v\u00edt\u011bz\u00ed p\u00e1dem na \u010das.";
    }

    static String blueLostOnTime() {
        return "Modr\u00e9mu hr\u00e1\u010di vypr\u0161el \u010das. \u010cerven\u00fd hr\u00e1\u010d v\u00edt\u011bz\u00ed p\u00e1dem na \u010das.";
    }

    private static String boardSize(int rows, int columns) {
        return rows + "x" + columns;
    }
}
