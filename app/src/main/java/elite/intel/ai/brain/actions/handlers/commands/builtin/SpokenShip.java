package elite.intel.ai.brain.actions.handlers.commands.builtin;

import elite.intel.db.FuzzySearch;
import elite.intel.db.managers.ShipMakeManager;
import elite.intel.gameapi.search.spansh.station.shipyard.ShipyardCatalogue;
import elite.intel.gameapi.search.spansh.station.shipyard.ShipyardShip;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Which hull the commander named, out of the shipyard catalogue.
 *
 * <p>Ship names are a closed list of a few dozen, and the ways they get said are few and predictable, so they are
 * compared as KEYS rather than as text: lower case, punctuation and spacing gone, the mark spoken any way at all
 * ("mark three", "mk 3", "MkIII") reduced to the same Roman numeral. "cobra mark 3", "Cobra Mk III" and
 * "CobraMkIII" are then one key, and "type 9" is the Type-9 Heavy.
 *
 * <p>A bare family name ("cobra", "krait") is several hulls, and buying a ship is not something to guess at: it
 * comes back as {@link Several} so the commander is asked which one, rather than flown to whichever was nearest.
 */
final class SpokenShip {

    /**
     * What the spoken name turned out to be.
     */
    sealed interface Match permits One, Several, None {
    }

    record One(ShipyardShip ship) implements Match {
    }

    record Several(List<ShipyardShip> ships) implements Match {
        Several {
            ships = List.copyOf(ships);
        }
    }

    record None() implements Match {
    }

    /**
     * More hulls than this answering a fragment is no answer at all - "a" opens half the catalogue - so it is
     * treated as no match rather than read out as a list. Six is the Type-6 to Type-11 family, the largest real one.
     */
    static final int MAX_CHOICES = 6;

    /**
     * Shortest fragment matched as the opening of a name; shorter ones open too many.
     */
    private static final int MIN_PREFIX = 3;

    private static final Pattern NON_ALNUM = Pattern.compile("[^\\p{L}\\p{N}]+");
    /**
     * "mark" as a word, or "mk" opening one - "MkIII" is a single word until this splits it.
     */
    private static final Pattern MARK = Pattern.compile("\\bmark\\b|\\bmk\\.?");

    /**
     * Spoken numbers, as the hull names write them: a mark as its Roman numeral, a Type as its digits. The two
     * never meet - marks run 2 to 5 and Types 6 to 11 - so one table serves both. No 1: no hull has a Mk I, and
     * "i" is a word.
     */
    private static final Map<String, String> NUMERALS = Map.ofEntries(
            Map.entry("2", "ii"), Map.entry("two", "ii"),
            Map.entry("3", "iii"), Map.entry("three", "iii"),
            Map.entry("4", "iv"), Map.entry("four", "iv"),
            Map.entry("5", "v"), Map.entry("five", "v"),
            Map.entry("six", "6"), Map.entry("seven", "7"), Map.entry("eight", "8"),
            Map.entry("nine", "9"), Map.entry("ten", "10"), Map.entry("eleven", "11"));

    /**
     * Words that never tell one hull from another, dropped from both sides. "ship" goes too, so "Federal Assault
     * Ship" is found when the commander leaves it off - and a commander who says "a python ship" is still heard.
     */
    private static final Set<String> NOISE = Set.of("mk", "a", "an", "the", "ship");

    /**
     * The superpower word a hull's name opens with, which commanders drop: "cutter", "corvette", "courier".
     */
    private static final Set<String> MAKERS = Set.of("imperial", "federal", "alliance");

    private SpokenShip() {
    }

    /**
     * The hull named, against the full catalogue, also answering to the name the game client uses for it.
     */
    static Match resolve(String spoken) {
        return resolve(spoken, ShipyardCatalogue.all(),
                ship -> ShipMakeManager.getInstance().getDisplayName(ship.symbol()));
    }

    /**
     * Most literal reading first, each tier tried only when the one before found nothing: the whole name, symbol
     * or client name word for word; the name without its maker; the opening of a name; and last a name the
     * transcript bent by a letter or two.
     *
     * @param clientName the hull's name on the commander's game client, which may be localized; null when unknown
     */
    static Match resolve(String spoken, List<ShipyardShip> catalogue, Function<ShipyardShip, String> clientName) {
        if (spoken == null) return new None();
        String wanted = key(spoken);
        if (wanted.isEmpty()) return new None();

        List<ShipyardShip> found = new ArrayList<>();
        for (ShipyardShip ship : catalogue) {
            if (fullKeys(ship, clientName).contains(wanted)) found.add(ship);
        }
        if (found.isEmpty()) {
            for (ShipyardShip ship : catalogue) {
                if (wanted.equals(shortKey(ship.name()))) found.add(ship);
            }
        }
        if (found.isEmpty() && wanted.length() >= MIN_PREFIX) {
            for (ShipyardShip ship : catalogue) {
                if (allKeys(ship, clientName).stream().anyMatch(candidate -> candidate.startsWith(wanted))) {
                    found.add(ship);
                }
            }
        }
        if (found.isEmpty()) {
            found = nearest(wanted, catalogue, clientName);
        }
        if (found.isEmpty() || found.size() > MAX_CHOICES) return new None();
        return found.size() == 1 ? new One(found.getFirst()) : new Several(found);
    }

    /**
     * The hulls whose name is closest to the bent one, within a budget that grows with its length: a letter for
     * "pyton", two for "ferdelans". Every hull at the closest distance is returned, so a tie is asked about.
     */
    private static List<ShipyardShip> nearest(String wanted, List<ShipyardShip> catalogue,
                                              Function<ShipyardShip, String> clientName) {
        int budget = Math.min(3, Math.max(1, wanted.length() / 4));
        int best = Integer.MAX_VALUE;
        List<ShipyardShip> closest = new ArrayList<>();
        for (ShipyardShip ship : catalogue) {
            int distance = allKeys(ship, clientName).stream()
                    .mapToInt(candidate -> FuzzySearch.levenshteinDistance(wanted, candidate))
                    .min().orElse(Integer.MAX_VALUE);
            if (distance > budget || distance > best) continue;
            if (distance < best) {
                best = distance;
                closest.clear();
            }
            closest.add(ship);
        }
        return closest;
    }

    private static Set<String> fullKeys(ShipyardShip ship, Function<ShipyardShip, String> clientName) {
        Set<String> keys = new LinkedHashSet<>();
        keys.add(key(ship.name()));
        keys.add(key(ship.symbol()));
        String client = clientName.apply(ship);
        if (client != null && !client.isBlank()) keys.add(key(client));
        keys.remove("");
        return keys;
    }

    /**
     * The names a commander actually says, for the loose tiers. The symbol is left out: it is only ever matched
     * word for word, because as a fragment it reads wrong - "type" would open the Alliance ships, filed as
     * {@code TypeX}.
     */
    private static Set<String> allKeys(ShipyardShip ship, Function<ShipyardShip, String> clientName) {
        Set<String> keys = new LinkedHashSet<>();
        keys.add(key(ship.name()));
        keys.add(shortKey(ship.name()));
        String client = clientName.apply(ship);
        if (client != null && !client.isBlank()) keys.add(key(client));
        keys.remove("");
        return keys;
    }

    /**
     * The name without the superpower word it opens with, or empty when it opens with none.
     */
    private static String shortKey(String name) {
        List<String> words = words(name);
        if (words.size() < 2 || !MAKERS.contains(words.getFirst())) return "";
        return String.join("", words.subList(1, words.size()));
    }

    static String key(String text) {
        return String.join("", words(text));
    }

    private static List<String> words(String text) {
        String lower = MARK.matcher(text.toLowerCase(Locale.ROOT)).replaceAll(" mk ");
        List<String> words = new ArrayList<>();
        for (String word : NON_ALNUM.split(lower)) {
            if (word.isEmpty() || NOISE.contains(word)) continue;
            words.add(NUMERALS.getOrDefault(word, word));
        }
        return words;
    }
}
