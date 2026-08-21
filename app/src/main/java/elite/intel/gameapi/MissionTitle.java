package elite.intel.gameapi;

import java.util.Locale;
import java.util.Map;

/**
 * Turns Frontier's internal mission keys into something a commander can be told.
 * <p>
 * The journal normally ships a {@code LocalisedName} alongside the key ("Source 45 Units of
 * H.E. Suits for the Imperial Navy"), and that is always the better answer. Only when it is
 * missing does the raw key ({@code Mission_Collect_RankEmp}) have to be turned into words -
 * speaking or displaying the key verbatim is exactly what this class exists to prevent.
 */
public final class MissionTitle {

    private static final String UNSPECIFIED = "unspecified mission";

    /**
     * Whole-token expansions for the abbreviations Frontier uses inside mission keys. Anything
     * not listed is split on its camel-case humps and lower-cased, which reads well enough for
     * plain words ("Onslaught", "Sightseeing") and badly only for abbreviations - so an
     * abbreviation that shows up in a new mission key belongs here.
     */
    private static final Map<String, String> TOKENS = Map.ofEntries(
            Map.entry("rankemp", "Imperial Navy rank"),
            Map.entry("rankfed", "Federal Navy rank"),
            Map.entry("onfoot", "on foot"),
            Map.entry("blops", "covert operations"),
            Map.entry("poi", "point of interest"),
            Map.entry("civilwar", "civil war"),
            Map.entry("vip", "VIP"),
            Map.entry("passengervip", "VIP passenger"),
            Map.entry("passengerbulk", "bulk passenger"),
            Map.entry("prisonerofwar", "prisoner of war"),
            Map.entry("aidworker", "aid worker"),
            Map.entry("altruismcredits", "credit donation"),
            Map.entry("altruism", "donation"),
            Map.entry("longdistanceexpedition", "long distance expedition"),
            Map.entry("salvageillegal", "illegal salvage"),
            Map.entry("onslaughtillegal", "illegal onslaught"),
            Map.entry("massacrewing", "wing massacre"),
            // An internal marker on Odyssey keys; it names nothing the commander cares about.
            Map.entry("mb", "")
    );

    private MissionTitle() {
    }

    /**
     * The name to put in front of a human: the game's own localised name when the journal gave
     * one, otherwise the key spelled out in words.
     */
    public static String of(String rawName, String localisedName) {
        if (localisedName != null && !localisedName.isBlank()) {
            return localisedName.trim();
        }
        return fromKey(rawName);
    }

    /**
     * Spells out a raw journal mission key: {@code Mission_Collect_RankEmp_name} becomes
     * "Collect Imperial Navy rank". Never returns the key itself.
     */
    public static String fromKey(String rawName) {
        if (rawName == null || rawName.isBlank()) return UNSPECIFIED;

        String stripped = rawName.trim()
                .replaceAll("(?i)^\\$?mission_?", "")   // the "Mission_" every key starts with
                .replaceAll("(?i)_name;?$", "")         // the "_name" suffix on completion events
                .replaceAll("_\\d+$", "")               // the "_002" variant counter
                .replace(";", "");

        StringBuilder title = new StringBuilder();
        for (String token : stripped.split("_")) {
            if (token.isBlank()) continue;
            String word = TOKENS.getOrDefault(token.toLowerCase(Locale.ROOT), splitCamelCase(token));
            if (word.isBlank()) continue;
            if (!title.isEmpty()) title.append(' ');
            title.append(word);
        }
        if (title.isEmpty()) return UNSPECIFIED;

        title.setCharAt(0, Character.toUpperCase(title.charAt(0)));
        return title.toString();
    }

    private static String splitCamelCase(String token) {
        return token
                .replaceAll("(?<=[a-z0-9])(?=[A-Z])", " ")
                .replaceAll("(?<=[A-Z])(?=[A-Z][a-z])", " ")
                .toLowerCase(Locale.ROOT);
    }
}
