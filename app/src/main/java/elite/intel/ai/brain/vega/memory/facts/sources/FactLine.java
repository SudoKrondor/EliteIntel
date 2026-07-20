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

    /** {@code head}, then the parts joined with ", ", each appended only while it fits under {@link #MAX_CHARS}. */
    static String capped(String head, List<String> parts) {
        StringBuilder sb = new StringBuilder(head);
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
}
