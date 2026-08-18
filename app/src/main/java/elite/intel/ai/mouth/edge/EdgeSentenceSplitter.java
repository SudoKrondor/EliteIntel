package elite.intel.ai.mouth.edge;

import java.util.ArrayList;
import java.util.List;

/** Punctuation-aware sentence splitting with an escaped UTF-8 limit for Edge's SSML messages. */
final class EdgeSentenceSplitter {
    static final int MAX_ESCAPED_TEXT_BYTES = 4_096;

    private EdgeSentenceSplitter() {
    }

    static List<String> split(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        List<String> sentences = sentenceBoundaries(text.strip());
        List<String> bounded = new ArrayList<>();
        for (String sentence : sentences) {
            splitOversized(sentence, bounded);
        }
        return List.copyOf(bounded);
    }

    private static List<String> sentenceBoundaries(String text) {
        List<String> result = new ArrayList<>();
        int start = 0;
        int index = 0;
        while (index < text.length()) {
            int codePoint = text.codePointAt(index);
            int next = index + Character.charCount(codePoint);
            if (codePoint == '\r' || codePoint == '\n') {
                add(text, start, index, result);
                start = skipWhitespace(text, next);
                index = start;
                continue;
            }
            if (!isTerminal(codePoint)) {
                index = next;
                continue;
            }
            int boundary = consumeSentenceEnd(text, next);
            int following = skipWhitespace(text, boundary);
            boolean asciiPeriodNeedsSpace = codePoint == '.' && following == boundary && boundary < text.length();
            if (!asciiPeriodNeedsSpace) {
                add(text, start, boundary, result);
                start = following;
                index = following;
            } else {
                index = next;
            }
        }
        add(text, start, text.length(), result);
        return result;
    }

    private static void splitOversized(String text, List<String> output) {
        String remaining = text.strip();
        while (!remaining.isBlank()) {
            int split = oversizedBoundary(remaining);
            if (split < 0) {
                output.add(remaining);
                return;
            }
            add(remaining, 0, split, output);
            remaining = remaining.substring(split).strip();
        }
    }

    /**
     * Index to cut at so the first piece fits Edge's escaped-byte cap, or -1 when all of {@code text} fits.
     * Prefers the last whitespace inside the cap so a chunk breaks between words, and falls back to the last
     * code point that fits when one unbroken run fills the whole budget.
     * <p>
     * The scan accumulates {@link EdgeSsml#escapedByteLength(int)} per code point rather than re-escaping a
     * growing prefix, which is what keeps a long narration linear instead of quadratic in its length.
     */
    private static int oversizedBoundary(String text) {
        int escaped = 0;
        int lastWhitespace = -1;
        int lastFit = 0;
        int index = 0;
        while (index < text.length()) {
            int codePoint = text.codePointAt(index);
            int cost = EdgeSsml.escapedByteLength(codePoint);
            // Recorded before the cap check, not after: a cut here excludes the whitespace itself, so this is
            // still a usable boundary when the whitespace is the very character that would cross the cap.
            if (Character.isWhitespace(codePoint)) {
                lastWhitespace = index;
            }
            if (escaped + cost > MAX_ESCAPED_TEXT_BYTES) {
                if (lastWhitespace > 0) {
                    return lastWhitespace;
                }
                if (lastFit == 0) {
                    throw new IllegalArgumentException("A single character exceeds Edge's SSML text limit");
                }
                return lastFit;
            }
            escaped += cost;
            index += Character.charCount(codePoint);
            lastFit = index;
        }
        return -1;
    }

    private static int consumeSentenceEnd(String text, int index) {
        int cursor = index;
        while (cursor < text.length()) {
            int codePoint = text.codePointAt(cursor);
            if (!isTerminal(codePoint) && !isClosingPunctuation(codePoint)) {
                break;
            }
            cursor += Character.charCount(codePoint);
        }
        return cursor;
    }

    private static int skipWhitespace(String text, int index) {
        int cursor = index;
        while (cursor < text.length()) {
            int codePoint = text.codePointAt(cursor);
            if (!Character.isWhitespace(codePoint)) {
                break;
            }
            cursor += Character.charCount(codePoint);
        }
        return cursor;
    }

    private static boolean isTerminal(int codePoint) {
        return codePoint == '.' || codePoint == '!' || codePoint == '?'
                || codePoint == 0x2026 || codePoint == 0x3002
                || codePoint == 0xFF01 || codePoint == 0xFF1F;
    }

    private static boolean isClosingPunctuation(int codePoint) {
        return codePoint == '\"' || codePoint == '\'' || codePoint == 0x2019 || codePoint == 0x201D
                || codePoint == 0x00BB || codePoint == ')' || codePoint == ']' || codePoint == 0xFF09;
    }

    private static void add(String text, int start, int end, List<String> result) {
        if (end <= start) {
            return;
        }
        String sentence = text.substring(start, end).strip();
        if (!sentence.isBlank()) {
            result.add(sentence);
        }
    }
}
