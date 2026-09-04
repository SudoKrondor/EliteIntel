package elite.intel.gameapi;

/**
 * The name of a scanned signal as a human reads it.
 * <p>
 * A surface scan reports a signal type two ways, and code that speaks one has to handle both. A body's
 * signals are decorated symbols with the game's own translation beside them - {@code
 * $SAA_SignalType_Geological;} / "Geological". A ring's are bare commodity names carrying a translation only
 * where it differs: {@code Alexandrite} arrives alone, {@code LowTemperatureDiamond} with "Low Temp.
 * Diamonds". Twelve of the eighteen types seen across two months of journals have no {@code Type_Localised}
 * at all, so a reader that trusts it is as broken as one that trusts the symbol.
 * <p>
 * Reading the symbol is what put "$SAA_SignalType_Geological" and "$PlanetaryMiningLocation_Name" into the
 * commander's ear once the mining update started reporting the latter.
 * <p>
 * Sibling of {@link StationName}, which does this for a station's name, and the opposite of
 * {@link JournalSymbol}, which normalises the other way to produce identifiers to join on.
 */
public final class SignalName {

    /**
     * Frontier's category prefix on a body signal; it names the schema, not the thing.
     */
    private static final String SIGNAL_TYPE_PREFIX = "(?i)^SAA_SignalType_";

    private SignalName() {
    }

    /**
     * The game's own wording when the event carried one, otherwise the symbol turned back into words.
     * Null only when the event carried neither.
     */
    public static String display(String localised, String symbol) {
        if (localised != null && !localised.isBlank()) return localised;
        return spellOut(symbol);
    }

    /**
     * {@code $SAA_SignalType_Geological;} as "Geological" and {@code $PlanetaryMiningLocation_Name;} as
     * "Planetary Mining Location". A bare name Frontier saw no need to translate is already the answer,
     * apart from its camel case: {@code LowTemperatureDiamond} becomes "Low Temperature Diamond".
     */
    private static String spellOut(String symbol) {
        if (symbol == null) return null;
        String name = symbol.trim();
        if (name.isEmpty()) return null;
        if (name.startsWith("$")) name = name.substring(1);
        if (name.endsWith(";")) name = name.substring(0, name.length() - 1);
        name = name.replaceAll(SIGNAL_TYPE_PREFIX, "");
        name = name.replaceAll("(?i)_Name$", "");
        name = name.replace('_', ' ');
        name = name.replaceAll("(?<=[a-z0-9])(?=[A-Z])", " ").trim();
        return name.isEmpty() ? null : name;
    }
}
