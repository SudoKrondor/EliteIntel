package elite.intel.ai.brain.vega.memory.facts.sources;

import java.util.List;

/**
 * Shared presentation helper for the location fact sources: cleans an attribute value and assembles a head plus a list
 * of ready parts into one compact line, appending parts (highest-value first) only while they fit under the shared
 * length cap so a single verbose field can never blow up the lean facts block. Keeps every location source's form
 * identical.
 */
public final class FactLine {

    /** Hard cap on a single fact line, so one verbose field can never blow up the lean facts block. */
    public static final int MAX_CHARS = 160;

    private static final String ELLIPSIS = "...";

    private FactLine() {
    }

    /** A displayable attribute value, or null when empty or the journal's "unknown" placeholder. */
    static String value(String raw) {
        if (raw == null) {
            return null;
        }
        String v = raw.strip();
        return v.isEmpty() || "unknown".equalsIgnoreCase(v) ? null : v;
    }

    /**
     * {@code head}, then the parts joined with ", ", each appended only while it fits under {@link #MAX_CHARS}.
     * <p>
     * The head is shortened first when it alone is over the cap. It used to be trusted to be short, which held
     * while every head was a literal plus a station or system name; a head built from a journal-supplied string
     * (a mission's own description, which is unbounded and localized) could carry the whole line past the cap
     * this class exists to enforce. A caller with a long name in its head should still shorten that name itself
     * with {@link #shortened}, so the parts it appends survive rather than being cut off with it.
     */
    static String capped(String head, List<String> parts) {
        StringBuilder sb = new StringBuilder(shortened(head, MAX_CHARS));
        boolean first = true;
        for (String part : parts) {
            String prefix = first ? ": " : ", ";
            if (sb.length() + prefix.length() + part.length() > MAX_CHARS) {
                break;
            }
            sb.append(prefix).append(part);
            first = false;
        }
        return sb.toString();
    }

    /**
     * {@code value} within {@code maxChars}, cut at the last word boundary that fits and marked with an ellipsis
     * so the reader can tell it was cut. Text already within the limit is returned untouched.
     */
    static String shortened(String value, int maxChars) {
        if (value == null || value.length() <= maxChars) {
            return value;
        }
        int room = maxChars - ELLIPSIS.length();
        int lastSpace = value.lastIndexOf(' ', room);
        // A single word longer than the room available has no boundary to cut at, so cut it mid-word.
        int end = lastSpace > 0 ? lastSpace : room;
        return value.substring(0, end).stripTrailing() + ELLIPSIS;
    }
}
