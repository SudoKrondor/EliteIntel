package elite.intel.gameapi.search.spansh.station.shipyard;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Every hull a shipyard can sell, under the English name Spansh files it under.
 *
 * <p>WHY a fixed list in code and not the {@code ship_make} table: that table is the same set of hulls under
 * the same identifiers, but {@code ShipMakeManager} overwrites its display names with the journal's
 * {@code _Localised} text, so on a German or Russian client it no longer holds the English name - and Spansh's
 * ship filter matches nothing else. The table is still where the client's own name for a hull is read from;
 * this list is only what is sent.
 *
 * <p>Taken from Spansh's own list of ships (49, measured 2026-11-01). A hull released after that is simply not
 * found until it is added here; keep it in step with {@code ShipPadSizes} and the {@code ship_make} table.
 */
public final class ShipyardCatalogue {

    private static final List<ShipyardShip> SHIPS = List.of(
            new ShipyardShip("Adder", "Adder"),
            new ShipyardShip("Anaconda", "Anaconda"),
            new ShipyardShip("AnacondaMkII", "Anaconda Mk II"),
            new ShipyardShip("Asp", "Asp Explorer"),
            new ShipyardShip("Asp_Scout", "Asp Scout"),
            new ShipyardShip("BelugaLiner", "Beluga Liner"),
            new ShipyardShip("CobraMkIII", "Cobra MkIII"),
            new ShipyardShip("CobraMkIV", "Cobra MkIV"),
            new ShipyardShip("CobraMkV", "Cobra MkV"),
            new ShipyardShip("Corsair", "Corsair"),
            new ShipyardShip("Cutter", "Imperial Cutter"),
            new ShipyardShip("DiamondBack", "Diamondback Scout"),
            new ShipyardShip("DiamondBackXL", "Diamondback Explorer"),
            new ShipyardShip("Dolphin", "Dolphin"),
            new ShipyardShip("Eagle", "Eagle"),
            new ShipyardShip("Empire_Courier", "Imperial Courier"),
            new ShipyardShip("Empire_Eagle", "Imperial Eagle"),
            new ShipyardShip("Empire_Trader", "Imperial Clipper"),
            new ShipyardShip("Explorer_NX", "Caspian Explorer"),
            new ShipyardShip("Federation_Corvette", "Federal Corvette"),
            new ShipyardShip("Federation_Dropship", "Federal Dropship"),
            new ShipyardShip("Federation_Dropship_MkII", "Federal Assault Ship"),
            new ShipyardShip("Federation_Gunship", "Federal Gunship"),
            new ShipyardShip("FerDeLance", "Fer-de-Lance"),
            new ShipyardShip("Hauler", "Hauler"),
            new ShipyardShip("Independant_Trader", "Keelback"),
            new ShipyardShip("Krait_Light", "Krait Phantom"),
            new ShipyardShip("Krait_MkII", "Krait MkII"),
            new ShipyardShip("LakonMiner", "Type-11 Prospector"),
            new ShipyardShip("Mamba", "Mamba"),
            new ShipyardShip("Mandalay", "Mandalay"),
            new ShipyardShip("MediumTransport01", "Lynx Highliner"),
            new ShipyardShip("Orca", "Orca"),
            new ShipyardShip("PantherMkII", "Panther Clipper MkII"),
            new ShipyardShip("Python", "Python"),
            new ShipyardShip("Python_NX", "Python MkII"),
            new ShipyardShip("SideWinder", "Sidewinder"),
            new ShipyardShip("SmallCombat01_NX", "Kestrel Mk II"),
            new ShipyardShip("Type6", "Type-6 Transporter"),
            new ShipyardShip("Type7", "Type-7 Transporter"),
            new ShipyardShip("Type8", "Type-8 Transporter"),
            new ShipyardShip("Type9", "Type-9 Heavy"),
            new ShipyardShip("Type9_Military", "Type-10 Defender"),
            new ShipyardShip("TypeX", "Alliance Chieftain"),
            new ShipyardShip("TypeX_2", "Alliance Crusader"),
            new ShipyardShip("TypeX_3", "Alliance Challenger"),
            new ShipyardShip("Viper", "Viper MkIII"),
            new ShipyardShip("Viper_MkIV", "Viper MkIV"),
            new ShipyardShip("Vulture", "Vulture")
    );

    private ShipyardCatalogue() {
    }

    public static List<ShipyardShip> all() {
        return SHIPS;
    }

    /**
     * The hull with this symbol, ignoring case, so the journal's lower-cased identifier finds it too.
     */
    public static Optional<ShipyardShip> bySymbol(String symbol) {
        if (symbol == null) return Optional.empty();
        String wanted = symbol.strip().toLowerCase(Locale.ROOT);
        return SHIPS.stream().filter(ship -> ship.symbol().toLowerCase(Locale.ROOT).equals(wanted)).findFirst();
    }
}
