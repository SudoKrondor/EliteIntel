package elite.intel.ai.brain.inference.lmstudio.install;

import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the percentage out of the progress lines {@code lms load} prints, such as {@code Loading google/gemma-4-e4b 42%}.
 */
final class LoadProgressLine {

    private static final Pattern LOADING = Pattern.compile("^Loading\\s+\\S.*?\\s(\\d{1,3})(?:\\.\\d+)?%");

    private LoadProgressLine() {
    }

    /**
     * The whole percent a line reports, or empty for any other line.
     */
    static OptionalInt percent(String line) {
        if (line == null) return OptionalInt.empty();
        Matcher m = LOADING.matcher(line.strip());
        if (!m.find()) return OptionalInt.empty();
        int percent = Integer.parseInt(m.group(1));
        return percent <= 100 ? OptionalInt.of(percent) : OptionalInt.empty();
    }
}
