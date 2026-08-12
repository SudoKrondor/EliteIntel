package elite.intel.ui.overlay;

import elite.intel.gameapi.gamestate.dtos.GameEvents;
import elite.intel.gameapi.journal.events.dto.shiploadout.ModuleDto;
import elite.intel.gameapi.journal.events.dto.shiploadout.ShipLoadOutDto;
import elite.intel.session.PlayerSession;
import elite.intel.util.json.GsonFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * When the mining card appears at all, and where its numbers come from - the
 * parts that run against real stored state rather than values handed in by a
 * test.
 * <p>
 * Exercises the persisted path deliberately. The card is derived on every poll
 * from the mining-target table, the stored loadout and the stored Cargo event,
 * and the cargo fixtures below are real journal shapes: the hold is keyed by the
 * lower-cased game symbol while a target is stored as its English display name,
 * so a change to either side would break the card silently on screen.
 */
class MiningObjectiveLifecycleTest {

    /**
     * A real {@code Cargo.json} body, verbatim in shape: limpets are inventory
     * like anything else, and {@code Count} includes them.
     */
    private static final String MINING_HOLD = """
            { "timestamp":"2026-08-02T02:10:00Z", "event":"Cargo", "Vessel":"Ship", "Count":226, "Inventory":[
            { "Name":"platinum", "Count":89, "Stolen":0 },
            { "Name":"drones", "Name_Localised":"Limpet", "Count":137, "Stolen":0 }
            ] }""";

    private final PlayerSession session = PlayerSession.getInstance();
    private final MiningObjectiveSource source = new MiningObjectiveSource(PlayerSession.getInstance(), () -> false);

    private ShipLoadOutDto previousLoadout;
    private GameEvents.CargoEvent previousCargo;
    private Set<String> previousTargets;

    @BeforeEach
    void setUp() {
        previousLoadout = session.getShipLoadout();
        previousCargo = session.getShipCargo();
        previousTargets = session.getMiningTargets();

        session.clearMiningTargets();
        session.addMiningTarget("Platinum");
        session.setShipLoadout(miningShip());
        session.setShipCargo(cargo(MINING_HOLD));
    }

    @AfterEach
    void restore() {
        session.clearMiningTargets();
        if (previousTargets != null) previousTargets.forEach(session::addMiningTarget);
        if (previousLoadout != null) session.setShipLoadout(previousLoadout);
        if (previousCargo != null) session.setShipCargo(previousCargo);
    }

    @Test
    void aMiningShipWithTargetsShowsWhatItHasMinedAndWhatItHasLeftToMineWith() {
        HudObjective card = source.currentObjective().orElseThrow();

        assertEquals("MINING", card.title());
        assertEquals(List.of("HOLD", "PLATINUM", "LIMPETS"),
                card.rows().stream().map(HudRow::label).toList());
        assertEquals("89 T", valueOf(card, "PLATINUM"));
        assertEquals("137", valueOf(card, "LIMPETS"));
        assertEquals(226, rowOf(card, "HOLD").current());
        assertEquals(512, rowOf(card, "HOLD").max());
    }

    /**
     * The refinery is what makes it a mining ship. A hauler carrying platinum is
     * not mining, and must not be told that it is.
     */
    @Test
    void aShipWithoutARefineryShowsNoCard() {
        session.setShipLoadout(shipWithoutRefinery());

        assertTrue(source.currentObjective().isEmpty());
    }

    @Test
    void clearingTheTargetsClearsTheCard() {
        session.clearMiningTargets();

        assertTrue(source.currentObjective().isEmpty());
    }

    /**
     * Half the mineable commodities are stored under a display name that looks
     * nothing like the hold's key, so the two are matched through the
     * commodities table rather than by comparing strings.
     */
    @Test
    void aTargetIsMatchedToTheHoldByItsGameSymbol() {
        session.clearMiningTargets();
        session.addMiningTarget("Low Temperature Diamonds");
        session.setShipCargo(cargo("""
                { "timestamp":"2026-08-02T02:10:00Z", "event":"Cargo", "Vessel":"Ship", "Count":54, "Inventory":[
                { "Name":"lowtemperaturediamond", "Count":42, "Stolen":0 },
                { "Name":"drones", "Name_Localised":"Limpet", "Count":12, "Stolen":0 }
                ] }"""));

        HudObjective card = source.currentObjective().orElseThrow();

        assertEquals("42 T", valueOf(card, "LOW TEMPERATURE DIAMONDS"));
    }

    /**
     * An SRV reports its own hold under the same event. Counting it would put
     * someone else's tonnage on the mining ship's card.
     */
    @Test
    void anSrvHoldIsNotTheMiningShipsHold() {
        session.setShipCargo(cargo("""
                { "timestamp":"2026-08-02T02:10:00Z", "event":"Cargo", "Vessel":"SRV", "Count":2, "Inventory":[
                { "Name":"platinum", "Count":2, "Stolen":0 }
                ] }"""));

        assertTrue(source.currentObjective().isEmpty());
    }

    // -- fixtures --------------------------------------------------------------

    private static GameEvents.CargoEvent cargo(String json) {
        return GsonFactory.getGson().fromJson(json, GameEvents.CargoEvent.class);
    }

    private static ShipLoadOutDto miningShip() {
        return loadout(module("Int Refinery Size4 Class5"), module("Hpt Mininglaser Fixed Medium"));
    }

    private static ShipLoadOutDto shipWithoutRefinery() {
        return loadout(module("Int Cargorack Size6 Class1"), module("Hpt Pulselaser Fixed Medium"));
    }

    private static ShipLoadOutDto loadout(ModuleDto... modules) {
        ShipLoadOutDto dto = new ShipLoadOutDto();
        dto.setShipName("Rock Hopper");
        dto.setCargoCapacity(512);
        dto.setModules(List.of(modules));
        return dto;
    }

    /**
     * Module names as {@code LoadoutConverter} stores them: the journal's
     * {@code Int_Refinery_Size4_Class5} made readable.
     */
    private static ModuleDto module(String item) {
        ModuleDto dto = new ModuleDto();
        dto.setItem(item);
        return dto;
    }

    private static HudRow rowOf(HudObjective card, String label) {
        return card.rows().stream()
                .filter(row -> label.equals(row.label()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no " + label + " row on the card"));
    }

    private static String valueOf(HudObjective card, String label) {
        return rowOf(card, label).value();
    }
}
