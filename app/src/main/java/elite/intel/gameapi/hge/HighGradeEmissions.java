package elite.intel.gameapi.hge;

import java.util.*;

/**
 * Which Very Rare manufactured materials a High Grade Emissions signal can yield, given the state of
 * the system it appeared in.
 *
 * <p>A High Grade Emissions USS drops different materials depending on the allegiance of the
 * controlling faction and on the states any faction in the system is running. The rules are Frontier's
 * and are documented by the community at
 * <a href="https://elite-dangerous.fandom.com/wiki/High_Grade_Emissions">the wiki</a>; they are
 * reproduced here rather than looked up, because they change only when Frontier changes the game.
 *
 * <p>The rules stack: a Federal system in Boom can yield the Federal pair <em>and</em> the Boom trio,
 * so this returns everything that qualifies rather than picking one bucket.
 */
public final class HighGradeEmissions {

    /**
     * Journal symbols, which is what the material tables are keyed on.
     */
    static final String CORE_DYNAMICS_COMPOSITES = "fedcorecomposites";
    static final String PROPRIETARY_COMPOSITES = "fedproprietarycomposites";
    static final String IMPERIAL_SHIELDING = "imperialshielding";
    static final String IMPROVISED_COMPONENTS = "improvisedcomponents";
    static final String MILITARY_GRADE_ALLOYS = "militarygradealloys";
    static final String MILITARY_SUPERCAPACITORS = "militarysupercapacitors";
    static final String PROTO_HEAT_RADIATORS = "protoheatradiators";
    static final String PROTO_LIGHT_ALLOYS = "protolightalloys";
    static final String PROTO_RADIOLIC_ALLOYS = "protoradiolicalloys";
    static final String PHARMACEUTICAL_ISOLATORS = "pharmaceuticalisolators";

    /**
     * Pharmaceutical Isolators need an Outbreak in a system big enough to have a population to infect.
     */
    static final long OUTBREAK_MINIMUM_POPULATION = 1_000_000L;

    private HighGradeEmissions() {
    }

    /**
     * The material symbols a High Grade Emissions signal in this system can yield, in a stable order
     * and without duplicates. Empty means the system qualifies for nothing, which is the common case
     * and must be treated as "say nothing" rather than "say none".
     *
     * @param allegiance    the controlling faction's allegiance, as the journal spells it
     *                      ({@code Federation}, {@code Empire}, ...); may be null
     * @param factionStates every state running on any faction in the system, as the journal spells
     *                      them ({@code CivilWar}, {@code Boom}, ...); may be null
     * @param population    the system population, which only the Outbreak rule looks at
     */
    public static List<String> materialSymbols(String allegiance, Collection<String> factionStates, long population) {
        Set<String> symbols = new LinkedHashSet<>();
        Set<String> states = normalize(factionStates);

        if (matches(allegiance, "federation")) {
            symbols.add(CORE_DYNAMICS_COMPOSITES);
            symbols.add(PROPRIETARY_COMPOSITES);
        }
        if (matches(allegiance, "empire")) {
            symbols.add(IMPERIAL_SHIELDING);
        }
        if (states.contains("civilunrest")) {
            symbols.add(IMPROVISED_COMPONENTS);
        }
        if (states.contains("war") || states.contains("civilwar")) {
            symbols.add(MILITARY_GRADE_ALLOYS);
            symbols.add(MILITARY_SUPERCAPACITORS);
        }
        if (states.contains("boom") || states.contains("expansion")) {
            symbols.add(PROTO_HEAT_RADIATORS);
            symbols.add(PROTO_LIGHT_ALLOYS);
            symbols.add(PROTO_RADIOLIC_ALLOYS);
        }
        if (states.contains("outbreak") && population > OUTBREAK_MINIMUM_POPULATION) {
            symbols.add(PHARMACEUTICAL_ISOLATORS);
        }

        return List.copyOf(symbols);
    }

    /**
     * Folds the journal's state spellings onto one key. {@code CivilWar} arrives as one word from
     * {@code FactionState} but a hand-written or future variant may carry a space or underscore, and
     * dropping everything that is not a letter makes all of them the same key.
     */
    private static Set<String> normalize(Collection<String> factionStates) {
        if (factionStates == null) return Set.of();
        Set<String> normalized = new LinkedHashSet<>();
        for (String state : factionStates) {
            if (state == null || state.isBlank()) continue;
            normalized.add(state.toLowerCase(Locale.ROOT).replaceAll("[^a-z]", ""));
        }
        return normalized;
    }

    private static boolean matches(String value, String expected) {
        return value != null && expected.equals(value.strip().toLowerCase(Locale.ROOT));
    }
}
