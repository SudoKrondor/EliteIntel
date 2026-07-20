package elite.intel.ai.brain.vega.memory;

/** Shared deterministic text bounds for ordinary prompt-visible memory. */
final class MemoryTextBounds {

    private static final String ELLIPSIS = "...";

    private MemoryTextBounds() {
    }

    /** Keeps short text verbatim and shortens oversized text at the nearest preceding word boundary. */
    static String entry(String content) {
        return bound(content, CompanionMemoryPolicy.entryMaxChars());
    }

    private static String bound(String content, int maxChars) {
        if (content.length() <= maxChars) {
            return content;
        }
        if (maxChars <= ELLIPSIS.length()) {
            return content.substring(0, Math.max(0, maxChars));
        }

        int hardEnd = maxChars - ELLIPSIS.length();
        int end = precedingWhitespace(content, hardEnd);
        if (end < hardEnd / 2) {
            end = hardEnd;
        }
        String head = content.substring(0, end).stripTrailing();
        if (head.isEmpty()) {
            head = content.substring(0, hardEnd).stripTrailing();
        }
        String bounded = head + ELLIPSIS;
        return bounded.length() <= maxChars ? bounded : bounded.substring(0, maxChars);
    }

    private static int precedingWhitespace(String content, int fromExclusive) {
        for (int i = Math.min(fromExclusive, content.length()) - 1; i >= 0; i--) {
            if (Character.isWhitespace(content.charAt(i))) {
                return i;
            }
        }
        return -1;
    }
}
