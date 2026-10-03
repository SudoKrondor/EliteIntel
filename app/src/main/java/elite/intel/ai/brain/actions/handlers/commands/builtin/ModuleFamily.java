package elite.intel.ai.brain.actions.handlers.commands.builtin;

import elite.intel.db.FuzzySearch;

import java.util.*;
import java.util.regex.Pattern;

/**
 * A broad module request - "lasers", "a gimbal laser", "missile racks" - taken as every module in the
 * catalogue it can mean.
 * <p>
 * WHY: the catalogue holds full names only ("Pulse Laser", "Beam Laser"), and a commander shopping for a
 * size 3 gimbal laser often does not care which one - he wants whatever fits the hardpoint. Matching the
 * word against whole names finds nothing, and neither the fuzzy budget nor the LLM can guess which laser
 * was meant.
 * <p>
 * The family is derived from the catalogue, never listed by hand, so a module added to it later joins its
 * family with no change here. A family is every name ending in the spoken words, cut down to its
 * <em>roots</em>: a name that ends in another member's whole name is a variant of that member and is left
 * out, so "laser" means Pulse, Burst, Beam, Mining and Pulse Disruptor Laser and not Retributor Beam Laser
 * or Long Range Mining Laser - and "missile rack" means the plain Missile Rack alone, not every seeker and
 * pack-hound built on it.
 *
 * @param label   what the commander hears the request echoed as: "Laser", "Missile Rack"
 * @param members the catalogue names searched for, in catalogue order
 */
record ModuleFamily(String label, List<String> members) {

    /**
     * Words that open a request without naming anything: "a laser", "any lasers".
     */
    private static final Pattern LEADING_ARTICLE = Pattern.compile("^(?:a|an|the|any|some)\\s+");

    ModuleFamily {
        members = List.copyOf(members);
    }

    /**
     * The family the spoken words name exactly, singular or plural, spaces, hyphens and case aside.
     *
     * @param catalogue every module name the catalogue holds, in its own case
     */
    static Optional<ModuleFamily> exact(String spoken, List<String> catalogue) {
        Map<String, List<String>> families = byEnding(catalogue);
        for (String form : forms(spoken)) {
            List<String> members = families.get(form);
            if (members != null) return Optional.of(of(form, members));
        }
        return Optional.empty();
    }

    /**
     * The family whose ending the spoken words come nearest to, for a name the transcript bent: "lazers",
     * "missale racks".
     * <p>
     * The budget is a quarter of the length, tighter than the whole-name fuzzy match allows, because a
     * family ending is often one short word and a loose match on one would send a search for the wrong
     * kind of module altogether.
     */
    static Optional<ModuleFamily> nearest(String spoken, List<String> catalogue) {
        Map<String, List<String>> families = byEnding(catalogue);
        String best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (String form : forms(spoken)) {
            int budget = Math.max(1, form.length() / 4);
            for (String ending : families.keySet()) {
                int distance = FuzzySearch.levenshteinDistance(form, ending);
                if (distance <= budget && distance < bestDistance) {
                    bestDistance = distance;
                    best = ending;
                }
            }
        }
        return best == null ? Optional.empty() : Optional.of(of(best, families.get(best)));
    }

    /**
     * Every word-ending of every catalogue name, with the names that end in it: "Beam Laser" files itself
     * under "beam laser" and "laser".
     */
    private static Map<String, List<String>> byEnding(List<String> catalogue) {
        Map<String, List<String>> families = new LinkedHashMap<>();
        for (String name : catalogue) {
            String[] words = normalized(name).split(" ");
            for (int first = 0; first < words.length; first++) {
                String ending = String.join(" ", List.of(words).subList(first, words.length));
                families.computeIfAbsent(ending, key -> new ArrayList<>()).add(name);
            }
        }
        return families;
    }

    private static ModuleFamily of(String ending, List<String> members) {
        List<String> roots = roots(members);
        // A family of one is that module under its own name: "missile racks" echoes as "Missile Rack".
        String label = roots.size() == 1 ? roots.getFirst() : titleCase(ending);
        return new ModuleFamily(label, roots);
    }

    /**
     * The members that are no other member's variant: a name ending in another member's whole name is
     * dropped.
     */
    private static List<String> roots(List<String> members) {
        List<String> roots = new ArrayList<>();
        for (String member : members) {
            String name = normalized(member);
            boolean variant = members.stream()
                    .map(ModuleFamily::normalized)
                    .anyMatch(other -> !other.equals(name) && name.endsWith(" " + other));
            if (!variant) roots.add(member);
        }
        return roots;
    }

    /**
     * The spoken words as they are, then with the last word made singular, since the catalogue names one
     * module and a commander shopping says "lasers".
     */
    private static List<String> forms(String spoken) {
        if (spoken == null) return List.of();
        String form = LEADING_ARTICLE.matcher(normalized(spoken)).replaceFirst("");
        if (form.isEmpty()) return List.of();
        if (form.length() > 3 && form.endsWith("s") && !form.endsWith("ss")) {
            return List.of(form, form.substring(0, form.length() - 1));
        }
        return List.of(form);
    }

    /**
     * Lower case, with a hyphen read as the space a transcript puts there: "Multi-Cannon" is "multi cannon".
     */
    private static String normalized(String text) {
        return text.toLowerCase(Locale.ROOT).replace('-', ' ').trim().replaceAll("\\s+", " ");
    }

    private static String titleCase(String words) {
        StringBuilder title = new StringBuilder();
        for (String word : words.split(" ")) {
            if (!title.isEmpty()) title.append(' ');
            title.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return title.toString();
    }
}
