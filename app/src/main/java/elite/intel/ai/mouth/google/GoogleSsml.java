package elite.intel.ai.mouth.google;

/**
 * Turns sanitized plain text into Google TTS SSML for the Chirp3-HD (and Standard fallback) voices, which honor
 * SSML on the synchronous synthesis path. This is Google-only: the Kokoro/espeak-ng path keeps plain text.
 * <p>
 * The neural voices phrase flatly on bare punctuation, so this escapes the text for XML, normalizes "!" to "."
 * (its exclamatory intonation sounds unnatural), and inserts explicit {@code <break>} pauses after sentence ends,
 * ellipses, and commas to improve rhythm. Sentence punctuation is kept for its intonation cue, while a clause comma
 * is replaced by its break: keeping both makes Chirp add its automatic comma pause to the explicit pause. Pause
 * durations are tuning constants, adjusted by ear.
 */
final class GoogleSsml {

    /** Pause after a sentence end (". ! ?"). */
    private static final String SENTENCE_BREAK = "300ms";
    /** Pause for an ellipsis ("..."). */
    private static final String ELLIPSIS_BREAK = "300ms";
    /** Short clause pause after a comma. */
    private static final String COMMA_BREAK = "120ms";

    private GoogleSsml() {
    }

    /**
     * Wraps sanitized (punctuation-preserving) plain text as an SSML document. Safe for blank input and for text
     * without punctuation, which is simply wrapped in {@code <speak>}.
     *
     * @param text sanitized plain text (must be XML-unescaped; this method escapes it)
     * @return a well-formed SSML {@code <speak>} document ready for {@code SynthesisInput.setSsml}
     */
    static String wrap(String text) {
        if (text == null || text.isBlank()) {
            return "<speak></speak>";
        }
        // Escape first so any '<', '>', '&' in the source text cannot break the SSML, then flatten "!" and add
        // our own break tags.
        return "<speak>" + insertBreaks(normalizeExclamations(escapeXml(text))) + "</speak>";
    }

    private static String escapeXml(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    /**
     * Speaks "!" as a normal sentence end. Chirp3-HD reads an exclamation with an exaggerated, excited intonation
     * that sounds unnatural for the calm crew persona, and SSML cannot tame that prosody, so a run of "!" is
     * replaced with a single "." (the sentence pause is unaffected). "?" is left intact - question intonation
     * reads fine.
     */
    private static String normalizeExclamations(String s) {
        return s.replaceAll("!+", ".");
    }

    /**
     * Inserts a {@code <break>} after punctuation. Ellipsis is handled before the single-dot rule so "..." yields
     * one ellipsis pause rather than three sentence pauses; the sentence and clause rules only fire when the mark
     * is followed by whitespace or end of text, so decimals ("3.5") and abbreviations are left intact.
     */
    private static String insertBreaks(String s) {
        return s
                .replaceAll("\\.{3,}", "..." + breakTag(ELLIPSIS_BREAK))
                .replaceAll("(?<=[.!?])(?=\\s|$)", breakTag(SENTENCE_BREAK))
                .replaceAll(",(?=\\s|$)", breakTag(COMMA_BREAK));
    }

    private static String breakTag(String time) {
        return "<break time=\"" + time + "\"/>";
    }
}
