package elite.intel.ai.mouth.supertonic.stress;

/**
 * RUAccent's stress notation turned into what Supertonic reads.
 * <p>
 * RUAccent writes a {@code +} before the stressed vowel ({@code г+олоса}). Supertonic decomposes its input, so a
 * combining acute (U+0301) after the vowel reaches it as a token of its own - the one it learned from é and á -
 * and it moves the stress there ({@code го́лоса}). Measured by ear on 2026-10-29.
 */
final class StressMarks {

    static final char COMBINING_ACUTE = '́';
    private static final String VOWELS = "аеёиоуыэюяАЕЁИОУЫЭЮЯ";

    private StressMarks() {
    }

    /**
     * Each {@code +} before a vowel becomes an acute after it. A {@code +} on ё is dropped, since ё is always
     * stressed and already says so, and a stray {@code +} before anything else is dropped too.
     */
    static String fromPlusNotation(String word) {
        StringBuilder out = new StringBuilder(word.length() + 2);
        for (int i = 0; i < word.length(); i++) {
            char ch = word.charAt(i);
            if (ch != '+') {
                out.append(ch);
                continue;
            }
            if (i + 1 >= word.length()) continue;
            char vowel = word.charAt(i + 1);
            if (VOWELS.indexOf(vowel) < 0) continue;
            out.append(vowel);
            if (vowel != 'ё' && vowel != 'Ё') out.append(COMBINING_ACUTE);
            i++;
        }
        return out.toString();
    }

    static int countVowels(String word) {
        int count = 0;
        for (int i = 0; i < word.length(); i++) {
            if (VOWELS.indexOf(word.charAt(i)) >= 0) count++;
        }
        return count;
    }

    /**
     * {@code marked} written with the capitals {@code source} has, letter for letter, skipping the {@code +}s.
     */
    static String withCaseOf(String source, String marked) {
        StringBuilder out = new StringBuilder(marked.length());
        int letter = 0;
        for (int i = 0; i < marked.length(); i++) {
            char ch = marked.charAt(i);
            if (ch == '+') {
                out.append(ch);
                continue;
            }
            boolean upper = letter < source.length() && Character.isUpperCase(source.charAt(letter));
            out.append(upper ? Character.toUpperCase(ch) : ch);
            letter++;
        }
        return out.toString();
    }
}
