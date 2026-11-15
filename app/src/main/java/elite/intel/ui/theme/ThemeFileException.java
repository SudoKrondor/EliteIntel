package elite.intel.ui.theme;

/**
 * A file that cannot be read as a colour theme: not a theme at all, nameless, or made by a newer version.
 * The message says which, in words a commander can be shown.
 */
public class ThemeFileException extends Exception {

    public ThemeFileException(String message) {
        super(message);
    }

    public ThemeFileException(String message, Throwable cause) {
        super(message, cause);
    }
}
