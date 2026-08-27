package elite.intel.session;

import elite.intel.gameapi.gamestate.dtos.GameEvents;
import elite.intel.util.json.GsonFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Which of the two Odyssey inventory files is telling the truth.
 * <p>
 * The fixtures are lifted from a commander's own journal: the sample is picked up into the backpack,
 * the commander embarks, and the locker absorbs it while {@code Backpack.json} is never rewritten.
 */
class SuitInventoryTest {

    private static final String SAMPLE =
            "{\"Name\":\"chemicalsample\",\"Name_Localised\":\"Chemical Sample\",\"OwnerID\":0,\"MissionID\":1064450663,\"Count\":1}";

    private final SuitInventory inventory = SuitInventory.getInstance();

    @BeforeEach
    void clean() {
        inventory.setBackpack(null);
        inventory.setShipLocker(null);
        aboard();
    }

    @Test
    @DisplayName("the ship's locker counts whether or not the commander is in the ship")
    void lockerAlwaysCounts() {
        inventory.setShipLocker(locker(SAMPLE));

        assertEquals(List.of("chemicalsample"), names(inventory.items()));
    }

    @Test
    @DisplayName("an item still in the backpack counts while the commander is on foot")
    void backpackCountsOnFoot() {
        onFoot();
        inventory.setBackpack(backpack(SAMPLE));
        inventory.setShipLocker(locker(""));

        assertEquals(List.of("chemicalsample"), names(inventory.items()));
    }

    @Test
    @DisplayName("the backpack left stale by boarding does not count the same item twice")
    void staleBackpackIsNotCountedTwice() {
        // Embarking folds the backpack into the locker without rewriting Backpack.json, so both files
        // list the sample and only the locker is telling the truth.
        inventory.setBackpack(backpack(SAMPLE));
        inventory.setShipLocker(locker(SAMPLE));

        assertEquals(List.of("chemicalsample"), names(inventory.items()),
                "aboard, so the locker alone says what is held");
    }

    @Test
    @DisplayName("nothing seen yet reads as nothing carried, not as a failure")
    void noSnapshotsYet() {
        assertTrue(inventory.items().isEmpty());
    }

    @Test
    @DisplayName("the session reads through to the same inventory")
    void sessionDelegates() {
        PlayerSession.getInstance().setShipLocker(locker(SAMPLE));

        assertEquals(List.of("chemicalsample"), names(PlayerSession.getInstance().getSuitInventory()));
    }

    // -- fixtures --------------------------------------------------------------

    private static List<String> names(List<GameEvents.MicroResource> items) {
        return items.stream().map(GameEvents.MicroResource::getName).toList();
    }

    private static GameEvents.BackpackEvent backpack(String itemsJson) {
        return GsonFactory.getGson().fromJson(
                "{\"event\":\"Backpack\",\"Items\":[" + itemsJson + "]}", GameEvents.BackpackEvent.class);
    }

    private static GameEvents.ShipLockerEvent locker(String itemsJson) {
        return GsonFactory.getGson().fromJson(
                "{\"event\":\"ShipLocker\",\"Items\":[" + itemsJson + "]}", GameEvents.ShipLockerEvent.class);
    }

    private static void onFoot() {
        setFlags2(1L);
    }

    private static void aboard() {
        setFlags2(0L);
    }

    private static void setFlags2(long flags2) {
        GameEvents.StatusEvent event = new GameEvents.StatusEvent();
        event.setFlags2(flags2);
        Status.getInstance().setStatus(event);
    }
}
