package elite.intel.ai.brain.inference.lmstudio.install;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Turns what LM Studio's script and {@code lms} print for a terminal into lines worth showing in a log.
 * <p>
 * WHY: both draw for a live terminal - colour codes, braille spinners redrawn in place with carriage returns, and
 * progress bars of {@code #} characters. Shown raw in a log, one spinner turns a second into dozens of garbled
 * lines. Each redraw reaches here as a line of its own ({@code readLine} splits on carriage returns too), so this
 * strips the decoration and {@link ProcessRunner} drops the redraws that then say the same thing again.
 */
final class ConsoleText {

    /**
     * ANSI escape sequences: colours, cursor moves, line erases, cursor show and hide.
     */
    private static final Pattern ANSI = Pattern.compile("\u001B\\[[0-9;?]*[A-Za-z]");

    /**
     * The braille dots LM Studio's spinners are drawn with.
     */
    private static final Pattern SPINNER = Pattern.compile("[⠀-⣿]");

    /**
     * A curl-style progress bar: only {@code #}, {@code =}, {@code O}, {@code -} and spaces, with or without a
     * percentage after them.
     */
    private static final Pattern PROGRESS_BAR = Pattern.compile("^[#=O\\-\\s]*(\\d{1,3}(\\.\\d)?%)?$");

    private ConsoleText() {
    }

    /**
     * The line to log for one line of output, or empty when nothing worth reading is left.
     */
    static Optional<String> clean(String rawLine) {
        String text = SPINNER.matcher(ANSI.matcher(rawLine).replaceAll("")).replaceAll("").strip();
        if (text.isEmpty() || PROGRESS_BAR.matcher(text).matches()) {
            return Optional.empty();
        }
        return Optional.of(text);
    }
}
