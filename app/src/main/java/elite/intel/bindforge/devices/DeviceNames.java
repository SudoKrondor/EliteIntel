package elite.intel.bindforge.devices;

import java.text.Normalizer;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * What a device may be called: the name every controller is offered, and the rule a typed name is held to.
 * <p>
 * Both exist because the name is not a label. It becomes an <strong>XML element tag</strong> in every
 * installation's {@code DeviceMappings.xml} and the <strong>filename stem</strong> of its {@code .buttonMap},
 * and SDL-style names such as {@code Virpil Controls 20220720} are neither. A name the game cannot read is
 * worse than no name, because nothing reports it: the entry is simply not there for the game.
 * <p>
 * Pure functions over their inputs. The caller gathers the names already taken - the master's, every
 * installation's entries, Frontier's - so the rule can be tested without a database or a game installation.
 * The rule is "The default-name rule" in {@code docs/02-features/bindforge/alias-designer.md}.
 */
public final class DeviceNames {

    public static final int MIN_LENGTH = 3;
    public static final int MAX_LENGTH = 50;

    /** Prefixed when the reported name does not start with a letter, or is too short to stand alone. */
    static final String SHORT_PREFIX = "Device";

    /** The whole name when nothing usable is left of the reported one. */
    static final String FALLBACK = "Controller";

    /**
     * The first number tried on a clash. {@code T-Rudder} from an unrelated device becomes {@code TRudder2},
     * per the spec's own example - the clashing name is the first, so its twin is the second.
     */
    private static final int FIRST_SUFFIX = 2;

    private DeviceNames() {
    }

    /** Why a typed name was refused. */
    public enum Problem {
        TOO_SHORT,
        TOO_LONG,
        NOT_STARTING_WITH_LETTER,
        INVALID_CHARACTER,
        CLASHES
    }

    /**
     * The answer to a typed name.
     *
     * @param problem     what is wrong, or {@code null} when the name is acceptable
     * @param clashesWith the existing name it collides with, when {@code problem} is {@link Problem#CLASHES};
     *                    otherwise {@code null}. Carried so the user is told <em>which</em> name, since the
     *                    collision ignores case and punctuation and may not be obvious
     */
    public record Verdict(Problem problem, String clashesWith) {

        static final Verdict VALID = new Verdict(null, null);

        public boolean valid() {
            return problem == null;
        }
    }

    /**
     * The name a controller is offered at onboarding, always one {@link #validate} accepts.
     *
     * @param reported the name the device reports through the Device Service; {@code null} is treated as
     *                 empty
     * @param taken    every name already in use - the master's, the installations' entries, Frontier's
     */
    public static String defaultName(String reported, Collection<String> taken) {
        String name = lettersAndDigits(reported == null ? "" : reported);

        if (name.isEmpty()) {
            name = FALLBACK;
        } else if (!startsWithLetter(name) || name.length() < MIN_LENGTH) {
            name = SHORT_PREFIX + name;
        }
        name = cut(name, MAX_LENGTH);

        Set<String> takenKeys = keys(taken);
        if (!takenKeys.contains(key(name))) return name;

        // WHY: the name is shortened by exactly the number's width rather than always by one, so the result
        // stays within MAX_LENGTH at 10 and beyond. The loop ends: each pass yields a distinct key and the
        // taken set is finite.
        for (int n = FIRST_SUFFIX; ; n++) {
            String suffix = Integer.toString(n);
            String candidate = cut(name, MAX_LENGTH - suffix.length()) + suffix;
            if (!takenKeys.contains(key(candidate))) return candidate;
        }
    }

    /**
     * Whether a name the user typed may be used.
     * <p>
     * A typed name may also use {@code -} and {@code _}, which the default never produces. The checks run in
     * the order a user can act on: length first, because a two-letter name has nothing else worth reporting.
     *
     * @param taken every name already in use, <em>excluding the device's own current name</em> - renaming
     *              {@code Lvwap} to {@code LVWAP} is not a clash with itself
     */
    public static Verdict validate(String typed, Collection<String> taken) {
        if (typed == null || typed.length() < MIN_LENGTH) return new Verdict(Problem.TOO_SHORT, null);
        if (typed.length() > MAX_LENGTH) return new Verdict(Problem.TOO_LONG, null);
        if (!startsWithLetter(typed)) return new Verdict(Problem.NOT_STARTING_WITH_LETTER, null);

        for (int i = 0; i < typed.length(); i++) {
            char c = typed.charAt(i);
            if (!isAsciiLetterOrDigit(c) && c != '-' && c != '_') {
                return new Verdict(Problem.INVALID_CHARACTER, null);
            }
        }

        Optional<String> clash = clashIn(typed, taken);
        return clash.map(name -> new Verdict(Problem.CLASHES, name)).orElse(Verdict.VALID);
    }

    /**
     * Whether two names would be confused: equal once case, {@code -} and {@code _} are ignored.
     * <p>
     * Deliberately looser than what the game compares. XML tags are case-sensitive, so {@code TRudder} and
     * {@code T-Rudder} are different elements to the game - and indistinguishable to a person reading a list.
     */
    public static boolean sameName(String a, String b) {
        return key(a).equals(key(b));
    }

    /**
     * The controllers onboarding asks about: attached, not one of Frontier's, and named nowhere - no entry in
     * any installation and no record in the master.
     * <p>
     * A controller that already has a name is never asked, whether Frontier's or the user's: there is no
     * decision to put to them. One that is not attached is not asked either, because a name with nothing
     * plugged in to show it on is a question the user cannot check.
     */
    public static List<MyDevice> needingName(List<MyDevice> devices) {
        return devices.stream()
                .filter(MyDevice::attached)
                .filter(device -> !device.builtIn())
                .filter(device -> device.entryName() == null)
                .filter(device -> device.alias() == null)
                .toList();
    }

    private static Optional<String> clashIn(String name, Collection<String> taken) {
        String key = key(name);
        return taken.stream().filter(existing -> key(existing).equals(key)).findFirst();
    }

    /**
     * Letters and digits only, accents folded first.
     * <p>
     * WHY ASCII and not every Unicode letter (Alan, 2026-10-06): XML would accept {@code Contrôleur} as a tag,
     * but the same text is a filename the game opens and nobody has tested what it does with one. Folding the
     * accent keeps the name readable - {@code Controleur} - rather than dropping the letter outright.
     */
    private static String lettersAndDigits(String reported) {
        String folded = Normalizer.normalize(reported, Normalizer.Form.NFD);
        StringBuilder kept = new StringBuilder(folded.length());
        for (int i = 0; i < folded.length(); i++) {
            char c = folded.charAt(i);
            if (isAsciiLetterOrDigit(c)) kept.append(c);
        }
        return kept.toString();
    }

    private static boolean startsWithLetter(String name) {
        char first = name.charAt(0);
        return (first >= 'A' && first <= 'Z') || (first >= 'a' && first <= 'z');
    }

    private static boolean isAsciiLetterOrDigit(char c) {
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9');
    }

    private static String cut(String name, int length) {
        return name.length() <= length ? name : name.substring(0, length);
    }

    private static Set<String> keys(Collection<String> names) {
        Set<String> keys = new HashSet<>();
        for (String name : names) keys.add(key(name));
        return keys;
    }

    private static String key(String name) {
        return name.replace("-", "").replace("_", "").toLowerCase(Locale.ROOT);
    }
}
