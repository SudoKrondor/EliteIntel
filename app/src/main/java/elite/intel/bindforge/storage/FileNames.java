package elite.intel.bindforge.storage;

/**
 * The one test for "a file name, not a path" (Krondor's review, 2026-10-27): a name BindForge writes into a game
 * folder or the working copy - from a backup's zip, or a set of files - must not lead out of that folder, and must be
 * one Windows accepts. Before, the working copy and RESTORE each had a check of their own, and they differed.
 */
final class FileNames {

    /** Characters Windows refuses in a name - the path separators among them. */
    private static final String NOT_IN_A_NAME = "<>:\"|?*\\/";

    private FileNames() {
    }

    /**
     * Whether {@code name} is a plain file name. Refused: blank; a path - a separator, a drive's colon, {@code .} or
     * {@code ..}; a character below a space; one Windows refuses; and a space or dot at the end, which Windows drops,
     * so the file written would not be the one named.
     */
    static boolean plain(String name) {
        if (name == null || name.isBlank() || name.endsWith(" ") || name.endsWith(".")) return false;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c < ' ' || NOT_IN_A_NAME.indexOf(c) >= 0) return false;
        }
        return true;
    }
}
