package elite.intel.ai.mouth.edge;

import java.nio.charset.StandardCharsets;
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
        while (escapedBytes(remaining) > MAX_ESCAPED_TEXT_BYTES) {
            int split = largestFittingWhitespace(remaining);
            if (split <= 0) {
                split = largestFittingCodePoint(remaining);
            }
            add(remaining, 0, split, output);
            remaining = remaining.substring(split).strip();
        }
        if (!remaining.isBlank()) {
            output.add(remaining);
        }
    }

    private static int largestFittingWhitespace(String text) {
        int lastWhitespace = -1;
        int index = 0;
        while (index < text.length()) {
            int codePoint = text.codePointAt(index);
            int next = index + Character.charCount(codePoint);
            if (Character.isWhitespace(codePoint) && escapedBytes(text.substring(0, index)) <= MAX_ESCAPED_TEXT_BYTES) {
                lastWhitespace = index;
            }
            if (escapedBytes(text.substring(0, next)) > MAX_ESCAPED_TEXT_BYTES) {
                break;
            }
            index = next;
        }
        return lastWhitespace;
    }

    private static int largestFittingCodePoint(String text) {
        int index = 0;
        int lastFit = 0;
        while (index < text.length()) {
            int next = index + Character.charCount(text.codePointAt(index));
            if (escapedBytes(text.substring(0, next)) > MAX_ESCAPED_TEXT_BYTES) {
                break;
            }
            lastFit = next;
            index = next;
        }
        if (lastFit == 0) {
            throw new IllegalArgumentException("A single character exceeds Edge's SSML text limit");
        }
        return lastFit;
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

    private static int escapedBytes(String text) {
        return EdgeSsml.escape(text).getBytes(StandardCharsets.UTF_8).length;
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
