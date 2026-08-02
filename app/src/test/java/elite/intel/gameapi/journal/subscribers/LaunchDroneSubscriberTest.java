package elite.intel.gameapi.journal.subscribers;

import com.google.gson.JsonObject;
import elite.intel.gameapi.SubscriberRegistration;
import elite.intel.gameapi.gamestate.dtos.GameEvents;
import elite.intel.gameapi.journal.EventRegistry;
import elite.intel.gameapi.journal.events.BaseEvent;
import elite.intel.gameapi.journal.events.LaunchDroneEvent;
import elite.intel.session.PlayerSession;
import elite.intel.util.json.GsonFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Limpets leave the hold one at a time and the game never rewrites {@code Cargo.json} to say so,
 * so the stored count is only as current as this subscriber keeps it. Without it a miner reads a
 * full limpet rack off the HUD while actually running dry.
 * <p>
 * Runs against the real stored hold rather than a stub, because the count survives a round trip
 * through JSON in the database between the launch and the next thing that reads it.
 */
class LaunchDroneSubscriberTest {

    private final PlayerSession session = PlayerSession.getInstance();
    private final LaunchDroneSubscriber subscriber = new LaunchDroneSubscriber();

    private GameEvents.CargoEvent previousCargo;

    @BeforeEach
    void setUp() {
        previousCargo = session.getShipCargo();
        session.setShipCargo(hold(226, 89, 137));
    }

    @AfterEach
    void restore() {
        if (previousCargo != null) session.setShipCargo(previousCargo);
    }

    @Test
    void launchingALimpetTakesOneOutOfTheHold() {
        subscriber.onLaunchDrone(launchDrone("Collection"));

        GameEvents.CargoEvent cargo = session.getShipCargo();
        assertEquals(136, cargo.getDroneCount());
        assertEquals(225, cargo.getCount(), "the limpet leaves the hold, so the hold is one lighter");
        assertEquals(89, cargo.getTradeableCount(), "mined cargo is untouched by a launch");
    }

    @Test
    void everyLaunchCounts() {
        subscriber.onLaunchDrone(launchDrone("Prospector"));
        subscriber.onLaunchDrone(launchDrone("Collection"));
        subscriber.onLaunchDrone(launchDrone("Collection"));

        assertEquals(134, session.getShipCargo().getDroneCount());
    }

    /**
     * The launch that fails for want of a limpet still writes a journal line.
     */
    @Test
    void anEmptyLimpetRackDoesNotGoNegative() {
        session.setShipCargo(hold(89, 89, 0));

        subscriber.onLaunchDrone(launchDrone("Collection"));

        assertEquals(0, session.getShipCargo().getDroneCount());
        assertEquals(89, session.getShipCargo().getCount());
    }

    /**
     * This is a correction to a snapshot, never a tally of its own - the game's own count has to
     * win the moment it produces one, or a missed launch would be wrong forever.
     */
    @Test
    void aFreshCargoSnapshotOverrulesTheCorrection() {
        subscriber.onLaunchDrone(launchDrone("Collection"));
        subscriber.onLaunchDrone(launchDrone("Collection"));

        session.setShipCargo(hold(226, 89, 137));

        assertEquals(137, session.getShipCargo().getDroneCount());
    }

    /**
     * The subscriber is found by a package scan, so nothing in the source refers to it - a rename
     * or a move would take it silently off the bus.
     */
    @Test
    void theSubscriberIsOnTheLiveBus() {
        assertTrue(SubscriberRegistration.liveSubscriberClasses().contains(LaunchDroneSubscriber.class));
    }

    // -- fixtures --------------------------------------------------------------

    /**
     * Built through the registry, so the journal's {@code LaunchDrone} line is what is being
     * tested rather than a hand-made event object.
     */
    private static LaunchDroneEvent launchDrone(String type) {
        JsonObject json = GsonFactory.getGson().fromJson("""
                { "timestamp":"%s", "event":"LaunchDrone", "Type":"%s" }
                """.formatted(Instant.now().toString(), type), JsonObject.class);
        BaseEvent event = EventRegistry.createEvent("LaunchDrone", json);
        return assertInstanceOf(LaunchDroneEvent.class, event, "LaunchDrone must be a registered event");
    }

    private static GameEvents.CargoEvent hold(int total, int platinum, int limpets) {
        return GsonFactory.getGson().fromJson("""
                        { "timestamp":"%s", "event":"Cargo", "Vessel":"Ship", "Count":%d, "Inventory":[
                        { "Name":"platinum", "Count":%d, "Stolen":0 },
                        { "Name":"drones", "Name_Localised":"Limpet", "Count":%d, "Stolen":0 }
                        ] }""".formatted(Instant.now().toString(), total, platinum, limpets),
                GameEvents.CargoEvent.class);
    }
}
