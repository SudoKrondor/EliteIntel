package elite.intel.junit.gameapi.journal.subscribers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import elite.intel.db.dao.ShipDao;
import elite.intel.db.managers.ShipManager;
import elite.intel.gameapi.journal.events.LoadoutEvent;
import elite.intel.gameapi.journal.subscribers.LoadoutSubscriber;
import elite.intel.session.PlayerSession;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;

class LoadoutSubscriberTest {

    private final LoadoutSubscriber subscriber = new LoadoutSubscriber();
    private final PlayerSession session = PlayerSession.getInstance();
    private final ShipManager shipManager = ShipManager.getInstance();

    @Test
    void newShipIsRegisteredInShipManagerOnFirstLoadout() throws InterruptedException {
        int shipId = 90_001;

        subscriber.onLoadoutEvent(loadoutEvent(shipId, "cobra3", "Iron Cobra", "CO-01", 40));

        awaitTrue(() -> shipManager.getShipById(shipId) != null);

        ShipDao.Ship ship = shipManager.getShipById(shipId);
        assertNotNull(ship);
        assertEquals("Iron Cobra", ship.getShipName());
        assertEquals(40, ship.getCargoCapacity());
    }

    @Test
    void existingShipCargoCapacityIsUpdatedOnSubsequentLoadout() throws InterruptedException {
        int shipId = 90_002;
        // Register the ship first
        subscriber.onLoadoutEvent(loadoutEvent(shipId, "asp", "Deep Space Explorer", "AS-01", 80));
        awaitTrue(() -> shipManager.getShipById(shipId) != null);

        // Fire again with updated cargo capacity
        subscriber.onLoadoutEvent(loadoutEvent(shipId, "asp", "Deep Space Explorer", "AS-01", 120));
        awaitTrue(() -> shipManager.getShipById(shipId) != null && shipManager.getShipById(shipId).getCargoCapacity() == 120);

        assertEquals(120, shipManager.getShipById(shipId).getCargoCapacity());
    }

    @Test
    void shipLoadoutIsStoredInPlayerSession() throws InterruptedException {
        int shipId = 90_003;

        subscriber.onLoadoutEvent(loadoutEvent(shipId, "sidewinder", "My Sidey", "SD-01", 4));

        awaitTrue(() -> session.getShipLoadout() != null
                && "My Sidey".equals(session.getShipLoadout().getShipName()));

        assertNotNull(session.getShipLoadout());
        assertEquals("My Sidey", session.getShipLoadout().getShipName());
    }

    private static LoadoutEvent loadoutEvent(int shipId, String ship, String shipName,
                                             String shipIdent, int cargoCapacity) {
        JsonObject j = new JsonObject();
        j.addProperty("timestamp", Instant.now().toString());
        j.addProperty("event", "Loadout");
        j.addProperty("Ship", ship);
        j.addProperty("ShipID", shipId);
        j.addProperty("ShipName", shipName);
        j.addProperty("ShipIdent", shipIdent);
        j.addProperty("HullHealth", 1.0);
        j.addProperty("UnladenMass", 42.0);
        j.addProperty("CargoCapacity", cargoCapacity);
        j.addProperty("MaxJumpRange", 25.0);
        j.addProperty("Rebuy", 100_000L);
        j.addProperty("ModulesValue", 500_000L);

        JsonObject fuelCapacity = new JsonObject();
        fuelCapacity.addProperty("Main", 8.0);
        fuelCapacity.addProperty("Reserve", 0.49);
        j.add("FuelCapacity", fuelCapacity);

        j.add("Modules", new JsonArray());
        return new LoadoutEvent(j);
    }

    private static void awaitTrue(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 2000;
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() > deadline) fail("Condition not met within 2 seconds");
            Thread.sleep(10);
        }
    }
}
