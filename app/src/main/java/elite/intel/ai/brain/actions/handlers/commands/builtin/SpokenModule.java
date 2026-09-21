package elite.intel.ai.brain.actions.handlers.commands.builtin;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * What the commander said they want to buy, taken apart: the module's name, and the size, class and mount
 * they put around it - "fuel scoop size 6 class B", "6b fuel scoop", "gimballed beam laser".
 * <p>
 * Parsed here rather than asked of the LLM as separate arguments, so the reflex and the model hand the
 * command the same thing - the words as spoken - and a weak local model that would have dropped the
 * designation on the floor while filling three arguments only has to keep one string intact.
 * <p>
 * Rating letters stop at E on purpose: the classes a pilot names are A to E, and letting F through I
 * match would turn "a" in "a fuel scoop" into a class.
 *
 * @param name   the words left once the designation is removed; what the catalogue is asked to match
 * @param size   the slot size 1 to 8, or null when none was said
 * @param rating the class letter A to E in upper case, or null when none was said
 * @param mount  a weapon mount in Spansh's vocabulary ({@code Fixed}, {@code Gimbal}, {@code Turret}), or
 *               null when none was said
 */
record SpokenModule(String name, Integer size, String rating, String mount) {

    /**
     * "class B", "rating B", "grade B", and "B rated" / "B class" / "B grade".
     */
    private static final Pattern RATING = Pattern.compile(
            "\\b(?:(?:class|rating|grade)\\s*([a-e])|([a-e])[\\s-]?(?:rated|class|grade))\\b");

    /**
     * "size 6" and "class 6" - a pilot says either for the slot size.
     */
    private static final Pattern SIZE = Pattern.compile("\\b(?:size|class)\\s*([1-8])\\b");

    /**
     * The screen designation, "6B" or "6 B".
     */
    private static final Pattern DESIGNATION = Pattern.compile("\\b([1-8])\\s?([a-e])\\b");

    private static final Pattern MOUNT = Pattern.compile("\\b(fixed|gimballed|gimbaled|gimbal|turreted|turret)\\b");

    private static final Map<String, String> MOUNT_WORDS = Map.of(
            "fixed", "Fixed",
            "gimballed", "Gimbal", "gimbaled", "Gimbal", "gimbal", "Gimbal",
            "turreted", "Turret", "turret", "Turret");

    /**
     * The word "module" itself, which names nothing in the catalogue and only spoils the match.
     */
    private static final Pattern MODULE_WORD = Pattern.compile("\\bmodules?\\b");

    /**
     * Takes the spoken request apart. Never null; a request with no designation at all comes back with the
     * words untouched and every other field null.
     */
    static SpokenModule parse(String spoken) {
        String rest = spoken == null ? "" : spoken.toLowerCase(Locale.ROOT);
        String rating = null;
        Integer size = null;
        String mount = null;

        Matcher ratingWords = RATING.matcher(rest);
        if (ratingWords.find()) {
            rating = letter(ratingWords.group(1) != null ? ratingWords.group(1) : ratingWords.group(2));
            rest = cut(rest, ratingWords);
        }
        Matcher sizeWords = SIZE.matcher(rest);
        if (sizeWords.find()) {
            size = Integer.valueOf(sizeWords.group(1));
            rest = cut(rest, sizeWords);
        }
        Matcher designation = DESIGNATION.matcher(rest);
        if (designation.find()) {
            if (size == null) size = Integer.valueOf(designation.group(1));
            if (rating == null) rating = letter(designation.group(2));
            rest = cut(rest, designation);
        }
        Matcher mountWord = MOUNT.matcher(rest);
        if (mountWord.find()) {
            mount = MOUNT_WORDS.get(mountWord.group(1));
            rest = cut(rest, mountWord);
        }
        rest = MODULE_WORD.matcher(rest).replaceAll(" ");
        return new SpokenModule(rest.trim().replaceAll("\\s+", " "), size, rating, mount);
    }

    /**
     * Whether any part of a designation was said. A request carrying one is a module request beyond doubt,
     * because no commodity has a size or a class.
     */
    boolean isSpecific() {
        return size != null || rating != null || mount != null;
    }

    private static String letter(String group) {
        return group.toUpperCase(Locale.ROOT);
    }

    private static String cut(String text, Matcher match) {
        return text.substring(0, match.start()) + " " + text.substring(match.end());
    }
}
