package elite.intel.gameapi.search.spansh.commodity;

/**
 * One line of a shopping list handed to {@link SpanshCommoditySearch#searchBasket}.
 * <p>
 * Both handles are carried because they answer different questions: {@code commodity} is the English name
 * Spansh matches a market entry on, and {@code symbol} is the bare journal symbol our own
 * {@code Market.json} sightings are keyed by. Deriving either from the other means a table lookup per
 * station per commodity, inside the loop that weighs fifty markets.
 *
 * @param symbol      bare lower-case journal symbol, or null for a good the commodities table has none for
 * @param commodity   the English name in the commodities table's own spelling
 * @param unitsWanted tonnes the caller still needs; never more is bought at one market
 */
public record WantedCommodity(String symbol, String commodity, int unitsWanted) {
}
