package elite.intel.gameapi.journal.subscribers;

import elite.intel.db.managers.ShipManager;
import elite.intel.gameapi.journal.events.StoredShipsEvent;
import elite.intel.gameapi.journal.events.dto.shiploadout.LoadoutConverter;

import java.util.List;

/**
 * Turns a {@link StoredShipsEvent} into fleet rows, shared by the live subscriber and the pre-scan. A ship is
 * named the way its Loadout names it - the commander's name for it, or its make's display name - so a ship added
 * here reads the same after it is flown.
 */
final class StoredShips {

    private StoredShips() {
    }

    /** @return how many ships the fleet did not know and now does */
    static int addMissing(StoredShipsEvent event, String voice, String commanderName) {
        List<ShipManager.StoredShip> ships = event.allShips().stream()
                .map(s -> new ShipManager.StoredShip(s.getShipId(),
                        LoadoutConverter.toDisplayShipName(s.getName(), s.getShipType()),
                        s.getShipType()))
                .toList();
        return ShipManager.getInstance().addMissingShips(ships, voice, commanderName);
    }
}
