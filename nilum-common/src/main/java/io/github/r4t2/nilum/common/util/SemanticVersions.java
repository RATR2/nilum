package io.github.r4t2.nilum.common.util;

/** Minimal semver-ish comparison for "major.minor.patch[-suffix]" version */
public final class SemanticVersions {

    private SemanticVersions() {
    }

    /**
     * @return negative if {@code a < b}, zero if equal, positive if {@code a > b}
     */
    public static int compare(String a, String b) {
        int[] partsA = numericParts(a);
        int[] partsB = numericParts(b);

        for (int i = 0; i < 3; i++) {
            int diff = Integer.compare(partsA[i], partsB[i]);
            if (diff != 0) {
                return diff;
            }
        }

        String suffixA = suffix(a);
        String suffixB = suffix(b);
        if (suffixA.isEmpty() != suffixB.isEmpty()) {
            return suffixA.isEmpty() ? 1 : -1;
        }
        return suffixA.compareTo(suffixB);
    }

    public static boolean isOlder(String candidate, String than) {
        return compare(candidate, than) < 0;
    }

    public static boolean isNewer(String candidate, String than) {
        return compare(candidate, than) > 0;
    }

    private static int[] numericParts(String version) {
        String core = version.contains("-") ? version.substring(0, version.indexOf('-')) : version;
        String[] segments = core.split("\\.", 3);
        int[] parts = new int[3];
        for (int i = 0; i < 3 && i < segments.length; i++) {
            parts[i] = parseIntOrZero(segments[i]);
        }
        return parts;
    }

    private static String suffix(String version) {
        int dash = version.indexOf('-');
        return dash < 0 ? "" : version.substring(dash + 1);
    }

    private static int parseIntOrZero(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
