package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.db.managers.MissionManager;
import elite.intel.gameapi.journal.events.CargoDepotEvent;
import elite.intel.gameapi.journal.events.dto.MissionDto;

/**
 * Records how much of a mission's cargo has been handed in, so the mission card can say what is
 * still owed rather than what was asked for at the board.
 * <p>
 * Silent: the game already shows the hand-in on screen.
 */
@SuppressWarnings("unused")
public class CargoDepotSubscriber {

    private final MissionManager missionManager = MissionManager.getInstance();

    @Subscribe
    public void onCargoDepot(CargoDepotEvent event) {
        MissionDto mission = missionManager.getMission(event.getMissionID());
        // A depot update can arrive for a mission accepted before this DB existed. Nothing to update.
        if (mission == null) return;
        long before = mission.getItemsDelivered();
        mission.setItemsDelivered(event.getItemsDelivered());
        if (mission.getItemsDelivered() != before) missionManager.save(mission);
    }
}
