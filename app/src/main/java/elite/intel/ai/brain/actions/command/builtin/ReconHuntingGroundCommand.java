package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.db.dao.PirateHuntingGroundsDao.HuntingGround;
import elite.intel.db.dao.PirateMissionProviderDao.MissionProvider;
import elite.intel.db.managers.HuntingGroundManager;
import elite.intel.db.managers.HuntingGroundManager.PirateMissionTuple;
import elite.intel.db.managers.LocationManager;
import elite.intel.eventbus.GameEventBus;
import elite.intel.gameapi.inputs.RoutePlotter;
import elite.intel.util.StringUtls;

import java.util.List;

/**
 * Self-describing "recon hunting ground" command.
 * Owns its own execution: body migrated 1:1 from the legacy ReconPirateMissionTargetSystemHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class ReconHuntingGroundCommand implements IntelCommand {
    public static final String ID = "recon_hunting_ground";


    @Override
    public String id() {
        return ID;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        HuntingGroundManager manager = HuntingGroundManager.getInstance();
        LocationManager locationManager = LocationManager.getInstance();
        List<PirateMissionTuple<HuntingGround, List<MissionProvider>>> huntingGrounds = manager.findTargetSystemInRangeForRecon(locationManager.getGalacticCoordinates());


        HuntingGround target = huntingGrounds.stream().filter(
                data -> data.getTarget().getTargetFaction() == null && !data.getTarget().isHasResSite()
        ).findFirst().map(PirateMissionTuple::getTarget).orElse(null);

        if (target == null) {
            GameEventBus.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.pirate.noReconSystems")));
            return;
        }

        boolean multipleMissionProviders = huntingGrounds.getFirst().getMissionProvider().size() > 1;
        if (multipleMissionProviders) {
            GameEventBus.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.pirate.multipleProviders")));
        }

        String starSystem = target.getStarSystem();

        GameEventBus.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.pirate.reconSystem", starSystem)));

        RoutePlotter plotter = new RoutePlotter();
        plotter.plotRoute(starSystem);
    }
}
