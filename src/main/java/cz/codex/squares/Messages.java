package cz.codex.squares;

final class Messages {
    static final String APP_TITLE = "Squares";
    static final String WINDOW_HOST = "Squares - hostitel (\u010derven\u00fd)";
    static final String WINDOW_CLIENT = "Squares - klient (modr\u00fd)";
    static final String WINDOW_LOCAL = "Squares - lok\u00e1ln\u00ed hra";
    static final String WINDOW_COMPUTER = "Squares - proti po\u010d\u00edta\u010di";

    static final String GAME_MODE_LOCAL = "\u010clov\u011bk vs. \u010dlov\u011bk";
    static final String GAME_MODE_COMPUTER = "\u010clov\u011bk vs. CPU";
    static final String GAME_MODE_HOST = "S\u00ed\u0165ov\u00e1 hra - server";
    static final String GAME_MODE_JOIN = "S\u00ed\u0165ov\u00e1 hra - klient";
    static final String GAME_MODE_PROMPT = "Vyber re\u017eim hry.";

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
    static final String MENU_SOUNDS = "Zvuky";
    static final String MENU_ABOUT = "O h\u0159e";
    static final String MENU_EXIT = "Ukon\u010dit";
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

    static String localInfo(int rows, int columns) {
        return "IP: lok\u00e1ln\u00ed hra\n"
                + "Plocha: " + boardSize(rows, columns) + "\n"
                + "Status: oba hr\u00e1\u010di na tomto PC";
    }

    static String computerInfo(int rows, int columns, String difficulty) {
        return "IP: lok\u00e1ln\u00ed hra\n"
                + "Plocha: " + boardSize(rows, columns) + "\n"
                + "Status: \u010derven\u00fd hr\u00e1\u010d vs. po\u010d\u00edta\u010d (" + difficulty + ")";
    }

    static String waitingForClient() {
        return "Status: \u010dek\u00e1m na klienta...";
    }

    static String clientConnected(String address) {
        return "Status: klient p\u0159ipojen: " + address;
    }

    static String connected() {
        return "Status: p\u0159ipojeno";
    }

    static String settingsRestartConfirm() {
        return "Zm\u011bna nastaven\u00ed restartuje aktu\u00e1ln\u00ed hru.\n\nChcete pokra\u010dovat?";
    }

    static String buildMismatch(String hostBuild, String clientBuild) {
        return NETWORK_INCOMPATIBLE_BUILD + "\n\n"
                + "Verze hostitele: " + hostBuild + "\n"
                + "Verze klienta: " + clientBuild;
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
