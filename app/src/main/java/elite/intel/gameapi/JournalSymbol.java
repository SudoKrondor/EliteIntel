package elite.intel.gameapi;

import java.util.Locale;

/**
 * Frontier writes the same identifier three ways, and code that joins journal data across events has
 * to reconcile them: a mission's {@code Commodity} is {@code $HazardousEnvironmentSuits_Name;}, a
 * {@code MarketBuy} calls the same good {@code haematite}, and the cargo hold lists it as
 * {@code advancedmedicines}. Normalising to the bare lower-case symbol makes all three comparable,
 * and makes them comparable with the {@code symbol} column of the commodities table.
 * <p>
 * The decorated form is an identifier, never something to show or speak - see {@code MissionTitle}
 * for why that distinction matters.
 */
public final class JournalSymbol {

    private JournalSymbol() {
    }

    /**
     * The bare lower-case symbol behind any of Frontier's spellings, or null when there is nothing
     * to normalise. Already-bare symbols pass through unchanged apart from case.
     */
    public static String normalize(String raw) {
        if (raw == null) return null;
        String symbol = raw.trim();
        if (symbol.startsWith("$")) symbol = symbol.substring(1);
        if (symbol.endsWith(";")) symbol = symbol.substring(0, symbol.length() - 1);
        symbol = symbol.replaceAll("(?i)_name$", "");
        symbol = symbol.toLowerCase(Locale.ROOT);
        return symbol.isBlank() ? null : symbol;
    }
}
