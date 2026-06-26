package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.CommandOutcome;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.db.dao.PirateHuntingGroundsDao.HuntingGround;
import elite.intel.db.dao.PirateMissionProviderDao.MissionProvider;
import elite.intel.db.managers.HuntingGroundManager;
import elite.intel.db.managers.HuntingGroundManager.PirateMissionTuple;
import elite.intel.db.managers.LocationManager;
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

    @Override public String llmDescription() { return "Scout the current location as a hunting ground."; }


    @Override
    public String id() {
        return ID;
    }

    @Override
    public JsonObject execute(JsonObject params, String responseText) {
        HuntingGroundManager manager = HuntingGroundManager.getInstance();
        LocationManager locationManager = LocationManager.getInstance();
        List<PirateMissionTuple<HuntingGround, List<MissionProvider>>> huntingGrounds = manager.findTargetSystemInRangeForRecon(locationManager.getGalacticCoordinates());


        HuntingGround target = huntingGrounds.stream().filter(
                data -> data.getTarget().getTargetFaction() == null && !data.getTarget().isHasResSite()
        ).findFirst().map(PirateMissionTuple::getTarget).orElse(null);

        if (target == null) {
            return CommandOutcome.critical(StringUtls.localizedLlm("handler.pirate.noReconSystems"));
        }

        boolean multipleMissionProviders = huntingGrounds.getFirst().getMissionProvider().size() > 1;
        String starSystem = target.getStarSystem();

        // Single outcome: when several providers exist, fold that note into the recon announcement.
        String announcement = StringUtls.localizedLlm("handler.pirate.reconSystem", starSystem);
        if (multipleMissionProviders) {
            announcement = StringUtls.localizedLlm("handler.pirate.multipleProviders") + " " + announcement;
        }

        RoutePlotter plotter = new RoutePlotter();
        plotter.plotRoute(starSystem);
        return CommandOutcome.critical(announcement);
    }
}
