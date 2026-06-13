package elite.intel.ai.brain.actions.handlers.commands;

import com.google.gson.JsonObject;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.db.dao.PirateHuntingGroundsDao.HuntingGround;
import elite.intel.db.dao.PirateMissionProviderDao.MissionProvider;
import elite.intel.db.managers.HuntingGroundManager;
import elite.intel.db.managers.HuntingGroundManager.PirateMissionTuple;
import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.EventBusManager;
import elite.intel.util.StringUtls;
import elite.intel.util.json.GsonFactory;
import elite.intel.util.json.ToJsonConvertible;

import java.util.List;

public class ReconPirateMissionTargetSystemHandler implements CommandHandler {



    @Override public void handle(String action, JsonObject params, String responseText) {
        HuntingGroundManager manager = HuntingGroundManager.getInstance();
        LocationManager locationManager = LocationManager.getInstance();
        List<PirateMissionTuple<HuntingGround, List<MissionProvider>>> huntingGrounds = manager.findTargetSystemInRangeForRecon(locationManager.getGalacticCoordinates());


        HuntingGround target = huntingGrounds.stream().filter(
                data -> data.getTarget().getTargetFaction() == null && !data.getTarget().isHasResSite()
        ).findFirst().map(PirateMissionTuple::getTarget).orElse(null);

        if (target == null) {
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.pirate.noReconSystems")));
            return;
        }

        boolean multipleMissionProviders = huntingGrounds.getFirst().getMissionProvider().size() > 1;
        if (multipleMissionProviders) {
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.pirate.multipleProviders")));
        }

        String starSystem = target.getStarSystem();

        EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.pirate.reconSystem", starSystem)));

        RoutePlotter plotter = new RoutePlotter();
        plotter.plotRoute(starSystem);
    }

    record DataDto(String starSystem, String targetFaction) implements ToJsonConvertible {
        @Override public String toJson() {
            return GsonFactory.getGson().toJson(this);
        }
    }
}
