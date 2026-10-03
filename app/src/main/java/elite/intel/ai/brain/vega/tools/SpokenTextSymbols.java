package elite.intel.ai.brain.vega.tools;

import java.util.regex.Pattern;

/**
 * Removes the typographic characters a speech engine reads aloud instead of reading past.
 *
 * <p>WHY this exists: the {@code speak} argument is handed to the TTS engine verbatim, and none of the
 * engines behind {@code MouthInterface} were trained on characters a commander could not type. An em dash
 * is the worst of them, because it is not merely ignored - next to a figure it is voiced as a minus sign,
 * so "Extreme cold - 273 C" was read out as "extreme cold minus 273 C", inverting the reading. Asterisks
 * and quotation marks are voiced by name: a model that emphasises a landmark as {@code *Jameson Memorial*}
 * has the commander hear "asterisk Jameson Memorial asterisk".
 *
 * <p>Deterministic rather than prompt-only. The prompt asks for plain speech, and that reduces how often
 * this has to act, but a rule in a prompt is a tendency and this is a guarantee - one leaked em dash beside
 * a temperature is a wrong number spoken with confidence. The strip runs inside
 * {@link SpeakFunction#textOf}, so the line spoken, the line shown and the line remembered are the same
 * one, and the characters never reach the chat panel either.
 *
 * <p>Deliberately narrow. Apostrophes stay, because they are inside words the engines pronounce correctly
 * and a commander types them. Underscores and hyphens stay: both appear inside real names the game uses.
 * The minus sign proper ({@code U+2212}) stays, because a model that writes it means it, and "minus" is
 * then the right reading.
 */
public final class SpokenTextSymbols {

    /**
     * The dashes no keyboard offers: em, en and horizontal bar. Any spaces around one go with it, so the
     * replacement lands as a single comma however the model spaced it.
     */
    private static final Pattern UNTYPEABLE_DASH = Pattern.compile("\\s*[\u2013\u2014\u2015]\\s*");

    /**
     * Asterisks, and quotation marks in every shape a model reaches for: straight, curly, low-9, and the
     * guillemets used by several of the commander languages.
     */
    private static final Pattern VOICED_ALOUD = Pattern.compile("[*\"\u201C\u201D\u201E\u00AB\u00BB]");

    /**
     * A comma the dash rule introduced next to punctuation that was already there.
     */
    private static final Pattern DOUBLED_PUNCTUATION = Pattern.compile("\\s*,\\s*(?=[,.;:!?])");

    private static final Pattern REPEATED_SPACE = Pattern.compile("[ \\t]{2,}");

    /**
     * A comma left leading a line, or one stranded before the end, by a dash that opened or closed it.
     */
    private static final Pattern STRANDED_COMMA = Pattern.compile("^\\s*,\\s*|\\s*,\\s*$");

    private SpokenTextSymbols() {
    }

    /**
     * The line with those characters resolved: dashes become a comma, which is the pause the model meant
     * by them, and the rest are dropped. Returns {@code text} unchanged when it holds none of them, and
     * handles null as the empty string so callers need no guard of their own.
     */
    public static String strip(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        String cleaned = UNTYPEABLE_DASH.matcher(text).replaceAll(", ");
        cleaned = VOICED_ALOUD.matcher(cleaned).replaceAll("");
        cleaned = DOUBLED_PUNCTUATION.matcher(cleaned).replaceAll("");
        cleaned = REPEATED_SPACE.matcher(cleaned).replaceAll(" ");
        cleaned = STRANDED_COMMA.matcher(cleaned).replaceAll("");
        return cleaned.trim();
    }
}
