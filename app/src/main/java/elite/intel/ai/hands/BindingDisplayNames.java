package elite.intel.ai.hands;

import elite.intel.util.StringUtls;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Translates a .binds XML tag into the control as the game's own OPTIONS &gt; CONTROLS screen
 * shows it: which of the four sections it sits under, which collapsible group inside that
 * section, and the row's label.
 * <p>
 * WHY: the tag is Frontier's internal name and appears nowhere in the game, so a commander
 * comparing our table against their control screen had nothing to match on -
 * {@code ExplorationSAANextGenus} is the row the game calls "Next Filter".
 * <p>
 * The table lives in {@code /bindings/ed_control_names.properties} and is read in file order,
 * which is the game's own row order, so the tables list controls in the sequence the commander
 * scrolls past on screen rather than alphabetically. A tag missing from the table keeps working:
 * it falls back to its humanized XML name under {@link BindingSection#OTHER}, which is honest
 * about the gap instead of inventing a label that cannot be found in game.
 */
public final class BindingDisplayNames {

    private static final Logger log = LogManager.getLogger(BindingDisplayNames.class);
    private static final String RESOURCE = "/bindings/ed_control_names.properties";
    /**
     * Separates the group from the control name in a row label.
     */
    private static final String PATH_SEPARATOR = " / ";
    /**
     * Sorts every unmapped control after every mapped one.
     */
    private static final int UNMAPPED_ORDER = Integer.MAX_VALUE;

    private static final Map<String, ControlName> NAMES = load();

    private BindingDisplayNames() {
    }

    /**
     * One control as the game screen presents it.
     *
     * @param group the collapsible subsection heading, or {@code null} for an unmapped control
     * @param order position in the game's own listing; drives table row order
     */
    public record ControlName(BindingSection section, String group, String name, int order) {

        /**
         * The row label: "Flight Rotation / Yaw Left". The group is included because control
         * names repeat across groups - the game has four different rows called "Move Forward".
         */
        public String label() {
            return group == null ? name : group + PATH_SEPARATOR + name;
        }

        /**
         * Lower-cased haystack for the search field: everything a commander might type.
         */
        String searchText(String bindingId, String sectionLabel) {
            return (sectionLabel + " " + label() + " " + bindingId).toLowerCase();
        }
    }

    /**
     * The in-game identity of {@code bindingId}, never {@code null}: an unknown tag comes back
     * as its humanized self under {@link BindingSection#OTHER}.
     */
    public static ControlName lookup(String bindingId) {
        if (bindingId == null || bindingId.isBlank()) {
            return new ControlName(BindingSection.OTHER, null, "", UNMAPPED_ORDER);
        }
        ControlName known = NAMES.get(bindingId);
        return known != null
                ? known
                : new ControlName(BindingSection.OTHER, null, StringUtls.humanizeBindingName(bindingId), UNMAPPED_ORDER);
    }

    /**
     * The row label for {@code bindingId} - what the Binding Profile table and callouts show.
     */
    public static String label(String bindingId) {
        return lookup(bindingId).label();
    }

    /**
     * Lower-cased text the search field matches against, including the raw XML tag.
     */
    public static String searchText(String bindingId, String sectionLabel) {
        return lookup(bindingId).searchText(bindingId, sectionLabel);
    }

    /**
     * Every tag the table names, in game order. Exposed for the coverage test.
     */
    public static Map<String, ControlName> all() {
        return NAMES;
    }

    /**
     * Reads the table in file order. Hand-parsed rather than via {@link java.util.Properties}
     * because the file order carries meaning here - it is the order the game lists the controls -
     * and Properties discards it.
     */
    private static Map<String, ControlName> load() {
        Map<String, ControlName> names = new LinkedHashMap<>();
        try (InputStream in = BindingDisplayNames.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                log.error("Control name table {} is missing; every binding will show its raw XML tag", RESOURCE);
                return names;
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            int order = 0;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                ControlName parsed = parse(trimmed, order);
                if (parsed == null) {
                    continue;
                }
                names.put(trimmed.substring(0, trimmed.indexOf('=')).trim(), parsed);
                order++;
            }
        } catch (Exception e) {
            log.error("Failed to read control name table {}", RESOURCE, e);
        }
        log.info("Loaded {} in-game control names", names.size());
        return names;
    }

    /**
     * Parses one {@code Tag=SECTION|Group|Name} line, or {@code null} if it is malformed.
     */
    private static ControlName parse(String line, int order) {
        int equals = line.indexOf('=');
        if (equals <= 0) {
            log.warn("Ignoring control name line without '=': {}", line);
            return null;
        }
        String[] parts = line.substring(equals + 1).split("\\|", -1);
        if (parts.length != 3) {
            log.warn("Ignoring control name line without SECTION|Group|Name: {}", line);
            return null;
        }
        BindingSection section;
        try {
            section = BindingSection.valueOf(parts[0].trim());
        } catch (IllegalArgumentException e) {
            log.warn("Ignoring control name line with unknown section '{}': {}", parts[0], line);
            return null;
        }
        return new ControlName(section, parts[1].trim(), parts[2].trim(), order);
    }
}
