package elite.intel.session;

import elite.intel.gameapi.gamestate.dtos.GameEvents;

import java.util.ArrayList;
import java.util.List;

/**
 * The Odyssey micro-resources the commander is holding, and the rule for which of the game's two
 * snapshots of them is telling the truth.
 * <p>
 * The game splits micro-resources across {@code Backpack.json} and {@code ShipLocker.json} depending
 * on where the commander is standing, and leaves the other file stale, so a reader that simply adds
 * them up counts the same item twice. That rule lives here rather than at any call site, because
 * getting it wrong is silent: the count is merely too high.
 * <p>
 * WHY this holds state in memory where {@link PlayerSession}'s other state goes through a database
 * manager: both files are full snapshots that {@code AuxiliaryFilesMonitor} republishes the moment it
 * starts, so a table would only ever hold a copy of something already re-read by the time anything
 * asks for it.
 */
public final class SuitInventory {

    private static volatile SuitInventory instance;

    private volatile GameEvents.BackpackEvent backpack;
    private volatile GameEvents.ShipLockerEvent shipLocker;

    private SuitInventory() {
    }

    public static SuitInventory getInstance() {
        SuitInventory result = instance;
        if (result == null) {
            synchronized (SuitInventory.class) {
                result = instance;
                if (result == null) {
                    instance = result = new SuitInventory();
                }
            }
        }
        return result;
    }

    /**
     * Everything the commander is actually carrying.
     * <p>
     * The ship's locker always counts. The backpack counts only while the commander is on foot:
     * boarding folds its contents into the locker <em>without</em> rewriting {@code Backpack.json},
     * so once aboard that file still lists what was carried and adding it in would double it.
     */
    public List<GameEvents.MicroResource> items() {
        List<GameEvents.MicroResource> items = new ArrayList<>();
        if (shipLocker != null && shipLocker.getItems() != null) items.addAll(shipLocker.getItems());
        if (backpack == null || backpack.getItems() == null) return items;
        if (Status.getInstance().isOnFoot()) items.addAll(backpack.getItems());
        return items;
    }

    public void setBackpack(GameEvents.BackpackEvent event) {
        this.backpack = event;
    }

    public void setShipLocker(GameEvents.ShipLockerEvent event) {
        this.shipLocker = event;
    }
}
