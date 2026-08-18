package elite.intel.ai.mouth.edge;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EdgeSentenceSplitterTest {
    @Test
    void recognizesAsciiUnicodeQuotesAndNewlinesButNotCommasOrDecimals() {
        assertEquals(List.of("First sentence.", "Second?", "Third!"),
                EdgeSentenceSplitter.split("First sentence. Second? Third!"));
        assertEquals(List.of("Вітаю!", "Як справи？", "Добре…。", "Гаразд！"),
                EdgeSentenceSplitter.split("Вітаю! Як справи？ Добре…。 Гаразд！"));
        assertEquals(List.of("He said \"go.\"", "Then left."),
                EdgeSentenceSplitter.split("He said \"go.\" Then left."));
        assertEquals(List.of("Value 3.14, still one sentence."),
                EdgeSentenceSplitter.split("Value 3.14, still one sentence."));
        assertEquals(List.of("line one", "line two"), EdgeSentenceSplitter.split("line one\nline two"));
    }

    @Test
    void boundsEscapedUtf8WithoutBreakingCodePoints() {
        String token = "😀<& Привіт ";
        String text = token.repeat(600).strip();
        List<String> parts = EdgeSentenceSplitter.split(text);

        assertTrue(parts.size() > 1);
        assertTrue(parts.stream().allMatch(part ->
                EdgeSsml.escape(part).getBytes(StandardCharsets.UTF_8).length
                        <= EdgeSentenceSplitter.MAX_ESCAPED_TEXT_BYTES));
        assertFalse(parts.stream().anyMatch(part -> Character.isLowSurrogate(part.charAt(0))));
        assertEquals(text.replaceAll("\\s+", " "), String.join(" ", parts).replaceAll("\\s+", " "));
    }

    @Test
    void usesTheFull4096ByteLimitAndKeepsEscapedEntitiesWhole() {
        String ascii = "a".repeat(4_097);
        List<String> asciiParts = EdgeSentenceSplitter.split(ascii);
        assertEquals(4_096, asciiParts.getFirst().getBytes(StandardCharsets.UTF_8).length);
        assertEquals(ascii, String.join("", asciiParts));

        String entitiesAndEmoji = "<&😀>".repeat(1_000);
        List<String> escapedParts = EdgeSentenceSplitter.split(entitiesAndEmoji);
        assertEquals(entitiesAndEmoji, String.join("", escapedParts));
        assertTrue(escapedParts.stream().allMatch(part ->
                EdgeSsml.escape(part).getBytes(StandardCharsets.UTF_8).length <= 4_096));
        assertTrue(escapedParts.stream().map(EdgeSsml::escape)
                .noneMatch(part -> part.matches("(?s).*&(?:amp|lt|gt|quot|apos)?$")));
    }
}
