package elite.intel.ai.mouth.supertonic.stress;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * One sentence cut into the words RUAccent works on, each remembered by where it sits in the original text so
 * the marks can be written back without touching anything between the words.
 * <p>
 * RUAccent's own pipeline deletes every character it does not know ({@code %}, {@code /}, {@code ’}, ...) and
 * reflows the spacing around punctuation. We hand its models the same view of the sentence, but the commander
 * hears our text, so here those characters become spaces in the {@link #context()} copy and stay in the original.
 * Every replacement keeps the length, so a word's offsets are the same in both.
 */
final class SentenceWords {

    // RUAccent's word pattern: a run of word characters (with its own stress '+' notation inside), or a run of
    // punctuation, which counts as a word too and so shows up in the context the models read.
    // WHY rewritten: RUAccent's \w*(?:\+\w+)*|[^\w\s]+ can match empty, and where it does Python retries the
    // same spot for a non-empty match - the punctuation - while Java steps past it. This is the same set of
    // non-empty matches.
    private static final Pattern WORD = Pattern.compile("\\w+(?:\\+\\w+)*|(?:\\+\\w+)+|[^\\w\\s]+",
            Pattern.UNICODE_CHARACTER_CLASS);
    // RUAccent's normalise step keeps only these characters (its source concatenates two string literals, which
    // is why the double quote is missing and the apostrophe is in).
    private static final Pattern NOT_KEPT = Pattern.compile("[^a-zA-Z0-9\\sа-яА-ЯёЁ—.,!?:;'(){}\\[\\]«»„“”-]");
    private static final String PUNCTUATION_BEFORE_WHICH_SPACE_GOES = "!\"#$%&'()*,./:;<=>?@[\\]^_`{|}-";

    private static final String FREE_STANDING_DASH = "~";

    private final String original;
    private final String context;
    private final String splittable;
    private final List<Span> spans;

    record Span(int start, int end) {
    }

    private SentenceWords(String original, String context, String splittable, List<Span> spans) {
        this.original = original;
        this.context = context;
        this.splittable = splittable;
        this.spans = spans;
    }

    static SentenceWords of(String sentence) {
        String context = NOT_KEPT.matcher(sentence).replaceAll(" ");
        // WHY: RUAccent turns a free-standing dash into '~' so its spacing rules leave the spaces around it,
        // where a hyphen inside a word ("что-то") is joined back up. Same length.
        String splittable = lowerCaseKeepingLength(context.replace(" - ", " " + FREE_STANDING_DASH + " "));
        List<Span> spans = new ArrayList<>();
        Matcher matcher = WORD.matcher(splittable);
        while (matcher.find()) spans.add(new Span(matcher.start(), matcher.end()));
        return new SentenceWords(sentence, context, splittable, List.copyOf(spans));
    }

    /**
     * The words as the original text spells them.
     */
    List<String> words() {
        return spans.stream().map(span -> original.substring(span.start(), span.end())).toList();
    }

    /**
     * The words as RUAccent's homograph context spells them: a free-standing dash is its '~'.
     */
    List<String> contextWords(List<String> words) {
        List<String> context = new ArrayList<>(words);
        for (int i = 0; i < spans.size(); i++) {
            Span span = spans.get(i);
            if (splittable.substring(span.start(), span.end()).equals(FREE_STANDING_DASH))
                context.set(i, FREE_STANDING_DASH);
        }
        return context;
    }

    /**
     * The sentence as RUAccent's models read it, lower-cased for the ё model, offsets unchanged.
     */
    String lowerCaseContext() {
        return lowerCaseKeepingLength(context);
    }

    List<Span> spans() {
        return spans;
    }

    /**
     * The original sentence with each word replaced by its processed form; everything between words untouched.
     */
    String rebuild(List<String> processed) {
        StringBuilder out = new StringBuilder(original.length() + 8);
        int at = 0;
        for (int i = 0; i < spans.size(); i++) {
            Span span = spans.get(i);
            out.append(original, at, span.start()).append(processed.get(i));
            at = span.end();
        }
        return out.append(original, at, original.length()).toString();
    }

    /**
     * RUAccent's context for one homograph: the words joined by spaces, the homograph wrapped in {@code <w>}
     * tags, then its spacing rules applied (no space before punctuation, none around a hyphen, '~' back to '-').
     *
     * @param words the words from {@link #contextWords}
     */
    static String homographContext(List<String> words, int position) {
        List<String> tagged = new ArrayList<>(words);
        tagged.set(position, " <w>" + words.get(position) + "</w> ");
        String text = String.join(" ", tagged);
        for (char punctuation : PUNCTUATION_BEFORE_WHICH_SPACE_GOES.toCharArray()) {
            if (punctuation == '-') text = text.replace(" -", "-").replace("- ", "-");
            text = text.replace(" " + punctuation, String.valueOf(punctuation));
        }
        return text.replace('~', '-');
    }

    /**
     * Lower case one char at a time: {@link String#toLowerCase} can change the length, and offsets must hold.
     */
    static String lowerCaseKeepingLength(String text) {
        char[] chars = text.toCharArray();
        for (int i = 0; i < chars.length; i++) chars[i] = Character.toLowerCase(chars[i]);
        return new String(chars);
    }
}
