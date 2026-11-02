package elite.intel.gameapi.search.spansh.station.shipyard;

/**
 * A ship hull as Spansh sells it at a shipyard.
 *
 * @param symbol the game's own internal name, as Spansh reports it ({@code CobraMkIII}, {@code Empire_Trader});
 *               lower-cased it is the journal's ship identifier and the {@code ship_make} key
 * @param name   Spansh's English name ({@code Cobra MkIII}, {@code Imperial Clipper}), which is the only thing its
 *               ship filter matches - exactly, so a name Spansh does not know matches no station
 */
public record ShipyardShip(String symbol, String name) {

    public ShipyardShip {
        if (symbol == null || symbol.isBlank() || name == null || name.isBlank()) {
            throw new IllegalArgumentException("A shipyard ship needs both its symbol and its name");
        }
    }
}
