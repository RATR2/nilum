package io.github.r4t2.nilum.common.logging;


public final class NilumConsoleFormat {

    public static final String PREFIX_COLOR_HEX = "5E3191";
    public static final String PREFIX_TEXT = "「Nilum 」";

    private static final String ANSI_ESCAPE = "";
    private static final String ANSI_RESET = ANSI_ESCAPE + "[0m";

    private NilumConsoleFormat() {
    }

    public static String ansi(NilumLogLevel level, String message) {
        return ansiColor(PREFIX_COLOR_HEX) + PREFIX_TEXT + ANSI_RESET
                + ansiColor(messageColorHex(level)) + message + ANSI_RESET;
    }

    public static String plain(NilumLogLevel level, String message) {
        return PREFIX_TEXT + message;
    }

    public static String messageColorHex(NilumLogLevel level) {
        return switch (level) {
            case DEBUG, INFO -> "A8A8A8";
            case WARN -> "FFFF99";
            case ERROR -> "FA795F";
            case MODERATION -> "7FB3FF";
        };
    }

    private static String ansiColor(String hex) {
        int r = Integer.parseInt(hex.substring(0, 2), 16);
        int g = Integer.parseInt(hex.substring(2, 4), 16);
        int b = Integer.parseInt(hex.substring(4, 6), 16);
        return ANSI_ESCAPE + "[38;2;" + r + ";" + g + ";" + b + "m";
    }
}
