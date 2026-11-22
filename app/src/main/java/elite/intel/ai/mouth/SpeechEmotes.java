package elite.intel.ai.mouth;

import java.util.Collections;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The non-verbal sounds Supertonic voices from inline tags - a sigh, a breath, a laugh - and the rules that keep
 * them sounding like a person rather than a glitch.
 * <p>
 * The engine is fussy about repetition, found by ear: a sigh works only alone (a second or third is read out as the
 * word), a breath works as a pair, and a laugh only lands as a triplet. A local model cannot be trusted to count, so
 * the LLM is asked for one bare tag and {@link #voice} writes the multiplicity the engine needs. It also keeps only
 * the first emote of a reply: the prompt asks for restraint, this enforces it, because a reply littered with them is
 * exactly what makes the voice sound broken.
 * <p>
 * Everywhere else a tag is noise - Google reads it as a word, the AI log shows it, and a reply remembered with its
 * emote teaches the model to repeat it next turn - so {@link #strip} removes it there.
 */
public final class SpeechEmotes {

    /**
     * One sound the engine can make, and how many tags in a row it takes to make it.
     */
    enum Emote {
        SIGH(1), BREATH(2), LAUGH(3);

        private final String tag;
        private final int repeat;

        Emote(int repeat) {
            this.tag = "<" + name().toLowerCase(Locale.ROOT) + ">";
            this.repeat = repeat;
        }

        String spoken() {
            return String.join(" ", Collections.nCopies(repeat, tag));
        }

        static Emote named(String name) {
            for (Emote emote : values()) {
                if (emote.name().equalsIgnoreCase(name)) return emote;
            }
            return null;
        }
    }

    /**
     * A run of angle-bracket word tags with only whitespace between them: "&lt;laugh&gt; &lt;laugh&gt;" is one run.
     * Any word in brackets counts, so a tag the model invented ("&lt;giggle&gt;", "&lt;/sigh&gt;") is caught and
     * dropped instead of read aloud.
     */
    private static final String OPEN = "(?:<|&lt;)";
    private static final String CLOSE = "(?:>|&gt;)";
    // WHY the entities: the prompts are XML-shaped, and a model sometimes escapes the brackets in its reply
    // ("Your Grace.&lt;laugh&gt;"). Unmatched, that reached the engine and the AI log as text.
    private static final Pattern TAG_RUN = Pattern.compile(
            "(?i)(?:" + OPEN + "\\s*/?\\s*\\p{L}+\\s*/?\\s*" + CLOSE + "\\s*)+");
    private static final Pattern ONE_TAG = Pattern.compile(
            "(?i)" + OPEN + "\\s*/?\\s*(\\p{L}+)\\s*/?\\s*" + CLOSE);

    private SpeechEmotes() {
    }

    /**
     * The text as Supertonic should hear it: the reply's first emote written in the form the engine voices,
     * every other tag removed.
     */
    public static String voice(String text) {
        if (text == null || text.isEmpty()) return "";
        Matcher run = TAG_RUN.matcher(text);
        if (!run.find()) return text;
        run.reset();
        StringBuilder out = new StringBuilder();
        boolean spent = false;
        while (run.find()) {
            Emote emote = spent ? null : firstEmoteIn(run.group());
            String replacement = emote == null ? " " : " " + emote.spoken() + " ";
            spent |= emote != null;
            run.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        run.appendTail(out);
        return tidy(out.toString());
    }

    /**
     * The text with every tag removed: what is shown, remembered, or sent to an engine without emotes.
     */
    public static String strip(String text) {
        if (text == null || text.isEmpty()) return "";
        Matcher run = TAG_RUN.matcher(text);
        // WHY untouched without a tag: this runs over every line shown or remembered, and tidying is only for
        // the gaps a removed tag leaves.
        return run.find() ? tidy(run.replaceAll(" ")) : text;
    }

    /**
     * Whether a fragment carries nothing to say but its emote - what a sentence split leaves behind when the
     * laugh follows the full stop. Supertonic should hear that tag with the sentence it belongs to.
     */
    public static boolean isEmoteOnly(String fragment) {
        if (fragment == null || !ONE_TAG.matcher(fragment).find()) return false;
        return strip(fragment).codePoints().noneMatch(Character::isLetterOrDigit);
    }

    private static Emote firstEmoteIn(String run) {
        Matcher tag = ONE_TAG.matcher(run);
        while (tag.find()) {
            if (tag.group().contains("/")) continue;
            Emote emote = Emote.named(tag.group(1));
            if (emote != null) return emote;
        }
        return null;
    }

    private static String tidy(String text) {
        return text.replaceAll("\\s{2,}", " ")
                .replaceAll("\\s+([,.!?;:])", "$1")
                .trim();
    }
}
