package cz.codex.squares;

final class Messages {
    static final String APP_TITLE = "Squares";
    static final String WINDOW_HOST = "Squares - hostitel (\u010derven\u00fd)";
    static final String WINDOW_CLIENT = "Squares - klient (modr\u00fd)";
    static final String WINDOW_LOCAL = "Squares - lok\u00e1ln\u00ed hra";

    static final String GAME_MODE_LOCAL = "Na jednom PC";
    static final String GAME_MODE_HOST = "Hostitel";
    static final String GAME_MODE_JOIN = "P\u0159ipojit";
    static final String GAME_MODE_PROMPT = "Vyber re\u017eim hry.";

    static final String HOST_ADDRESS_PROMPT = "IP adresa hostitele:";
    static final String CONNECTING_TO_HOST = "P\u0159ipojov\u00e1n\u00ed k hostiteli...";
    static final String BOARD_SIZE_PROMPT = "Vyber velikost hrac\u00ed plochy:";
    static final String BOARD_SIZE_TITLE = "Squares - velikost";
    static final String PORT_PROMPT = "Port:";
    static final String ADAPTER_PROMPT = "Vyber s\u00ed\u0165ov\u00fd adapt\u00e9r pro IP adresu serveru:";
    static final String ADAPTER_TITLE = "Squares - adapt\u00e9r";
    static final String VIRTUAL_ADAPTER_SUFFIX = " (virtu\u00e1ln\u00ed)";
    static final String MENU_GAME = "Hra";
    static final String MENU_CHANGE_SIZE = "Zm\u011bnit velikost pole...";
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

    static final String GAME_OVER_TITLE = "Konec hry";
    static final String NEW_GAME_PROMPT = "Chcete pokra\u010dovat novou hrou?";
    static final String NETWORK_GAME_TITLE = "S\u00ed\u0165ov\u00e1 hra";
    static final String NETWORK_HOST_ENDED = "Hostitel ukon\u010dil s\u00ed\u0165ovou hru.";
    static final String NETWORK_CONNECT_FAILED = "Nepoda\u0159ilo se p\u0159ipojit nebo spojen\u00ed spadlo.";
    static final String INVALID_SIZE_MESSAGE = "Neplatn\u00e1 zpr\u00e1va velikosti plochy: ";
    static final String BUILD_INFO_PREFIX = "Build: ";
    static final String BUILD_INFO_UNKNOWN = "nezn\u00e1m\u00fd";
    static final String HELP_TEXT = "C\u00edlem hry je uzav\u00edrat \u010dtvere\u010dky.\n"
            + "Hr\u00e1\u010di se st\u0159\u00eddaj\u00ed v ozna\u010dov\u00e1n\u00ed hran.\n"
            + "Kdo uzav\u0159e \u010dtvere\u010dek, z\u00edsk\u00e1 bod a hraje znovu.\n"
            + "Vyhr\u00e1v\u00e1 hr\u00e1\u010d s vy\u0161\u0161\u00edm po\u010dtem bod\u016f.";

    private Messages() {
    }

    static String hostInfo(String address, int port, int rows, int columns) {
        return "Hostitel: " + address + "  Port: " + port + "  Plocha: " + boardSize(rows, columns);
    }

    static String clientInfo(String host, int port, int rows, int columns) {
        return "Klient: p\u0159ipojeno k " + host + "  Port: " + port + "  Plocha: " + boardSize(rows, columns);
    }

    static String localInfo(int rows, int columns) {
        return "Lok\u00e1ln\u00ed hra na jednom PC  Plocha: " + boardSize(rows, columns);
    }

    static String waitingForClient() {
        return "\u010cek\u00e1m na klienta...";
    }

    static String clientConnected(String address) {
        return "Klient p\u0159ipojen: " + address;
    }

    static String connected() {
        return "P\u0159ipojeno";
    }

    static String hostStarted(int port, String address, int rows, int columns) {
        return "Hostitel b\u011b\u017e\u00ed na portu " + port + ".\n"
                + "Druh\u00fd hr\u00e1\u010d se p\u0159ipoj\u00ed na IP adresu: " + address + "\n"
                + "Velikost hrac\u00ed plochy: " + boardSize(rows, columns);
    }

    static String changeSizeConfirm(int rows, int columns) {
        return "Zm\u011bnit velikost pole na " + boardSize(rows, columns) + "?\n"
                + "Aktu\u00e1ln\u00ed hra se restartuje.";
    }

    static String aboutText() {
        return APP_TITLE + "\n"
                + BUILD_INFO_PREFIX + BuildInfo.buildTime() + "\n\n"
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

    private static String boardSize(int rows, int columns) {
        return rows + "x" + columns;
    }
}
