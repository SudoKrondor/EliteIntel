package elite.intel.gameapi.carrier;

import elite.intel.db.managers.FleetCarrierManager;
import elite.intel.db.managers.SquadronCarrierManager;
import elite.intel.db.managers.StationMarketsManager;
import elite.intel.gameapi.gamestate.dtos.GameEvents;
import elite.intel.gameapi.journal.events.CargoTransferEvent;
import elite.intel.gameapi.journal.events.dto.CarrierDataDto;
import elite.intel.session.DockedMarket;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The carrier's hold between market reads.
 * <p>
 * Reproduced from a live run: 640 tonnes of steel loaded off the carrier, hauled to a construction site and
 * delivered - and the next "what does the build need" sent the commander straight back to the carrier for
 * steel that was no longer aboard, because the only account of its cargo was a {@code Market.json} written
 * before the load.
 */
class CarrierHoldLedgerTest {

    private static final String CALLSIGN = "GHY-L8X";
    private static final long PAD = 3712500736L;
    private static final String SYSTEM = "Hyades Sector NR-V b2-2";

    private final StationMarketsManager markets = StationMarketsManager.getInstance();

    @BeforeEach
    @AfterEach
    void clean() {
        markets.clear();
        FleetCarrierManager.getInstance().save(new CarrierDataDto());
        SquadronCarrierManager.getInstance().save(new CarrierDataDto());
        DockedMarket.getInstance().departed();
    }

    /**
     * The reported bug, end to end.
     */
    @Test
    void emptyingTheCarrierStopsItAnsweringForCargoItNoLongerHolds() {
        ourCarrierWith(Map.of("steel", 640));

        onOurPad();
        CarrierHoldLedger.transferred(transfers("steel", 640, "toship"));

        assertTrue(OwnCarrierHold.fleetCarrier().isEmpty(),
                "the carrier is empty; the market snapshot must not go on answering for it");
    }

    @Test
    void loadingTheCarrierIsVisibleBeforeItsMarketIsOpenedAgain() {
        ourCarrierWith(Map.of("steel", 640));

        onOurPad();
        CarrierHoldLedger.transferred(transfers("titanium", 100, "tocarrier"));

        Optional<OwnCarrierHold.Held> held = OwnCarrierHold.fleetCarrier();
        assertTrue(held.isPresent());
        assertEquals(100, held.get().stockOf("titanium"));
        assertEquals(640, held.get().stockOf("steel"), "the rest of the hold is untouched");
    }

    /**
     * A market read is a complete account, so it replaces the ledger rather than adding to it - otherwise
     * opening the same screen twice would double the hold.
     */
    @Test
    void aMarketReadReplacesTheLedgerRatherThanAddingToIt() {
        ourCarrierWith(Map.of("steel", 640));
        GameEvents.MarketEvent market = marketOf(Map.of("steel", 640));

        CarrierHoldLedger.seedFrom(market);
        CarrierHoldLedger.seedFrom(market);

        assertEquals(640, OwnCarrierHold.fleetCarrier().orElseThrow().stockOf("steel"));
    }

    /**
     * The upgrade path, and the case where a commander hauls before the app has ever tracked that carrier:
     * the first transfer adopts the stored snapshot so it is a correction, not the whole account.
     */
    @Test
    void anUntrackedCarrierAdoptsItsSnapshotBeforeTheFirstTransfer() {
        ourCarrierWith(Map.of("steel", 640, "titanium", 200));

        onOurPad();
        CarrierHoldLedger.transferred(transfers("steel", 640, "toship"));

        Optional<OwnCarrierHold.Held> held = OwnCarrierHold.fleetCarrier();
        assertTrue(held.isPresent());
        assertEquals(0, held.get().stockOf("steel"));
        assertEquals(200, held.get().stockOf("titanium"),
                "adopting the snapshot must not lose the goods the transfer said nothing about");
    }

    /**
     * Buying off our own carrier's shelves empties them exactly as a transfer would - and {@code
     * Market.json} was written when the screen opened, so it still shows the goods on sale.
     */
    @Test
    void buyingOffOurOwnCarrierDropsItsHold() {
        ourCarrierWith(Map.of("steel", 640));

        onOurPad();
        CarrierHoldLedger.bought(PAD, "steel", 640);

        assertTrue(OwnCarrierHold.fleetCarrier().isEmpty(),
                "we bought the last of it; the carrier is empty");
    }

    @Test
    void sellingOntoOurOwnCarrierRaisesItsHold() {
        ourCarrierWith(Map.of("steel", 640));

        onOurPad();
        CarrierHoldLedger.sold(PAD, "$Titanium_name;", 200);

        OwnCarrierHold.Held held = OwnCarrierHold.fleetCarrier().orElseThrow();
        assertEquals(200, held.stockOf("titanium"));
        assertEquals(640, held.stockOf("steel"), "the rest of the hold is untouched");
    }

    /**
     * Trading at a station is the ordinary case and says nothing about the carrier, wherever it is parked.
     */
    @Test
    void tradingAtSomeoneElsesMarketLeavesOurLedgerAlone() {
        ourCarrierWith(Map.of("steel", 640));
        DockedMarket.getInstance().arrived(4278665219L, "Fairfax Landing");

        CarrierHoldLedger.bought(4278665219L, "steel", 640);

        assertEquals(640, OwnCarrierHold.fleetCarrier().orElseThrow().stockOf("steel"));
    }

    /**
     * A trade names its market but not its owner, and the pad names the owner but not the market. Neither
     * is enough alone: a marker left over from an earlier pad would otherwise book a station's trade
     * against the carrier.
     */
    @Test
    void aTradeAwayFromThePadWeThinkWeAreOnIsIgnored() {
        ourCarrierWith(Map.of("steel", 640));

        onOurPad();
        CarrierHoldLedger.bought(4278665219L, "steel", 640);

        assertEquals(640, OwnCarrierHold.fleetCarrier().orElseThrow().stockOf("steel"));
    }

    /**
     * Another commander's carrier, or an ordinary station, is not our cargo.
     */
    @Test
    void someoneElsesMarketDoesNotSeedOurLedger() {
        ourCarrierWith(Map.of("steel", 640));
        GameEvents.MarketEvent theirs = marketOf(Map.of("gold", 5));
        theirs.setStationName("XYZ-99Z");

        CarrierHoldLedger.seedFrom(theirs);

        assertFalse(CarrierHoldLedger.isTracking(FleetCarrierManager.getInstance().get()));
    }

    /**
     * A carrier we have never read a market for and never hauled anything on or off holds nothing we know
     * of - which is not the same as holding nothing, and must not silence a snapshot we do have.
     */
    @Test
    void anUntrackedCarrierStillAnswersFromItsSnapshot() {
        ourCarrierWith(Map.of("steel", 640));

        assertEquals(640, OwnCarrierHold.fleetCarrier().orElseThrow().stockOf("steel"));
    }

    // ---- tritium: the one commodity that is also the carrier's fuel ----

    /**
     * The whole point of the enhancement: a commander who lists their spare tritium no longer has to keep
     * telling us how much of it there is.
     */
    @Test
    void tritiumOnOurCarriersMarketBecomesTheFuelReserve() {
        ourCarrierWith(Map.of("steel", 640, "tritium", 14232));

        CarrierHoldLedger.seedFrom(marketOf(Map.of("steel", 640, "tritium", 14232)));

        assertEquals(14232, FleetCarrierManager.getInstance().get().getFuelReserve());
    }

    /**
     * A carrier's market shows a good at zero whether the owner has none or never listed it, and most
     * commanders never list their fuel at all. Reading that zero as an answer would wipe the figure they
     * have been keeping by hand.
     */
    @Test
    void aMarketWithNoTritiumLeavesAManualReserveAlone() {
        ourCarrierWith(Map.of("steel", 640));
        manualReserve(500);

        CarrierHoldLedger.seedFrom(marketOf(Map.of("steel", 640)));

        assertEquals(500, FleetCarrierManager.getInstance().get().getFuelReserve(),
                "not listed is not the same as not aboard");
    }

    @Test
    void aTritiumLineOfZeroIsAlsoLeftAlone() {
        ourCarrierWith(Map.of("steel", 640));
        manualReserve(500);

        CarrierHoldLedger.seedFrom(marketOf(Map.of("steel", 640, "tritium", 0)));

        assertEquals(500, FleetCarrierManager.getInstance().get().getFuelReserve());
    }

    /**
     * A movement we watched is unambiguous, so unlike a market listing it counts in both directions. Only
     * counting the inbound half is how the reserve came to climb for ever.
     */
    @Test
    void tritiumCarriedOffTheCarrierComesOffTheReserve() {
        ourCarrierWith(Map.of("tritium", 1000));
        CarrierHoldLedger.seedFrom(marketOf(Map.of("tritium", 1000)));

        onOurPad();
        CarrierHoldLedger.transferred(transfers("tritium", 400, "toship"));

        assertEquals(600, FleetCarrierManager.getInstance().get().getFuelReserve());
    }

    @Test
    void tritiumCarriedAboardJoinsTheReserve() {
        ourCarrierWith(Map.of("tritium", 1000));
        CarrierHoldLedger.seedFrom(marketOf(Map.of("tritium", 1000)));

        onOurPad();
        CarrierHoldLedger.transferred(transfers("tritium", 400, "tocarrier"));

        assertEquals(1400, FleetCarrierManager.getInstance().get().getFuelReserve());
    }

    /**
     * Buying our own fuel off our own shelves is the same movement as carrying it across.
     */
    @Test
    void buyingTritiumOffOurCarrierComesOffTheReserve() {
        ourCarrierWith(Map.of("tritium", 1000));
        CarrierHoldLedger.seedFrom(marketOf(Map.of("tritium", 1000)));

        onOurPad();
        CarrierHoldLedger.bought(PAD, "tritium", 400);

        assertEquals(600, FleetCarrierManager.getInstance().get().getFuelReserve());
    }

    /**
     * The reserve is tonnes aboard, and there is no such thing as less than none.
     */
    @Test
    void theReserveNeverGoesNegative() {
        ourCarrierWith(Map.of("tritium", 100));
        CarrierHoldLedger.seedFrom(marketOf(Map.of("tritium", 100)));

        onOurPad();
        CarrierHoldLedger.transferred(transfers("tritium", 500, "toship"));

        assertEquals(0, FleetCarrierManager.getInstance().get().getFuelReserve());
    }

    /**
     * Reading the same market screen twice must not double the fuel - which is exactly what seeding through
     * the movement verb would have done.
     */
    @Test
    void reReadingTheMarketDoesNotDoubleTheReserve() {
        ourCarrierWith(Map.of("tritium", 1000));

        CarrierHoldLedger.seedFrom(marketOf(Map.of("tritium", 1000)));
        CarrierHoldLedger.seedFrom(marketOf(Map.of("tritium", 1000)));

        assertEquals(1000, FleetCarrierManager.getInstance().get().getFuelReserve());
    }

    private static void manualReserve(int tons) {
        CarrierDataDto carrier = FleetCarrierManager.getInstance().get();
        carrier.setFuelReserve(tons);
        FleetCarrierManager.getInstance().save(carrier);
    }

    /**
     * Sets up the fleet carrier as the commander's own, parked in this system, with a stored market read -
     * the state a commander is in after docking at it and opening the commodity screen.
     */
    private void ourCarrierWith(Map<String, Integer> stock) {
        CarrierDataDto carrier = new CarrierDataDto();
        carrier.setCallSign(CALLSIGN);
        carrier.setStarName(SYSTEM);
        FleetCarrierManager.getInstance().save(carrier);
        elite.intel.session.PlayerSession.getInstance().setLastKnownCarrierLocation(SYSTEM);
        markets.save(marketOf(stock));
    }

    private static void onOurPad() {
        DockedMarket.getInstance().arrived(PAD, CALLSIGN);
    }

    private static GameEvents.MarketEvent marketOf(Map<String, Integer> stock) {
        GameEvents.MarketEvent market = new GameEvents.MarketEvent();
        market.setEvent("Market");
        market.setTimestamp(Instant.now().toString());
        market.setMarketID(PAD);
        market.setStationName(CALLSIGN);
        market.setStationType("FleetCarrier");
        market.setStarSystem(SYSTEM);
        List<GameEvents.MarketEvent.MarketItem> items = new ArrayList<>();
        stock.forEach((symbol, units) -> {
            GameEvents.MarketEvent.MarketItem item = new GameEvents.MarketEvent.MarketItem();
            item.setName("$" + symbol + "_name;");
            item.setStock(units);
            items.add(item);
        });
        market.setItems(items);
        return market;
    }

    private static List<CargoTransferEvent.Transfer> transfers(String symbol, int count, String direction) {
        com.google.gson.JsonObject transfer = new com.google.gson.JsonObject();
        transfer.addProperty("Type", symbol);
        transfer.addProperty("Count", count);
        transfer.addProperty("Direction", direction);
        com.google.gson.JsonArray array = new com.google.gson.JsonArray();
        array.add(transfer);
        com.google.gson.JsonObject event = new com.google.gson.JsonObject();
        event.addProperty("timestamp", Instant.now().toString());
        event.addProperty("event", "CargoTransfer");
        event.add("Transfers", array);
        return new CargoTransferEvent(event).getTransfers();
    }
}
