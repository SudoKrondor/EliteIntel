package elite.intel.gameapi.journal.subscribers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static elite.intel.util.StringUtls.localizedEvent;

/**
 * The services an approach announcement may mention, decided before the model sees any of them.
 *
 * <p>WHY: the journal lists everything a port runs, most of it present everywhere ({@code dock},
 * {@code contacts}, {@code missions}, {@code crewlounge}) and some of it not even a word
 * ({@code missionsgenerated}, {@code stationMenu}). Handed all 28 at Beaufoy Horizons and told "do not list
 * every service", a small local model read out fifteen of them in one breath, symbols and all. An
 * instruction cannot stop a model reciting a list it was given, so it is never given one: only the services
 * a commander docks <em>for</em> are named, at most {@link #MAX_NAMED} of them, and a port with more is
 * described as having many.
 */
final class SettlementServices {

    /** The most services the announcement names. */
    static final int MAX_NAMED = 3;

    /**
     * The services worth naming, by journal symbol (lower-cased), most useful first: the order decides which
     * {@link #MAX_NAMED} are named when a port has more. Everything else goes unsaid.
     */
    private static final Map<String, String> NOTABLE = orderedMap(
            "shipyard", "shipyard",
            "outfitting", "outfitting",
            "materialtrader", "materialTrader",
            "techbroker", "techBroker",
            "facilitator", "interstellarFactors",
            "blackmarket", "blackMarket",
            "exploration", "cartographics",
            "vistagenomics", "vistaGenomics",
            "searchrescue", "searchRescue",
            "frontlinesolutions", "frontlineSolutions",
            "apexinterstellar", "apexInterstellar",
            "pioneersupplies", "pioneerSupplies");

    private SettlementServices() {
    }

    /**
     * The services sentence for the announcement, or empty when the port runs nothing worth naming - a
     * dock and a refuel pad are not news.
     */
    static String describe(List<String> services) {
        List<String> named = notable(services);
        if (named.isEmpty()) {
            return "";
        }
        List<String> spoken = named.stream().limit(MAX_NAMED)
                .map(key -> localizedEvent("event.approach.settlement.service." + key))
                .toList();
        String key = named.size() > MAX_NAMED
                ? "event.approach.settlement.manyServices"
                : "event.approach.settlement.someServices";
        return localizedEvent(key, String.join(", ", spoken));
    }

    /** The notable services the port runs, as bundle key suffixes in {@link #NOTABLE} order. */
    static List<String> notable(List<String> services) {
        if (services == null || services.isEmpty()) {
            return List.of();
        }
        List<String> present = services.stream().map(s -> s.toLowerCase(Locale.ROOT)).toList();
        List<String> named = new ArrayList<>();
        NOTABLE.forEach((symbol, key) -> {
            if (present.contains(symbol)) {
                named.add(key);
            }
        });
        return named;
    }

    private static Map<String, String> orderedMap(String... pairs) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put(pairs[i], pairs[i + 1]);
        }
        return Collections.unmodifiableMap(map);
    }
}
