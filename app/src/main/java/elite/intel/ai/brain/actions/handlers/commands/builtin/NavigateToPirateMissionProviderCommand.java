package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.commands.IntelCommand;
import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.ai.brain.vega.VegaRuntime;
import elite.intel.db.dao.LocationDao.Coordinates;
import elite.intel.db.managers.HuntingGroundManager;
import elite.intel.db.managers.LocationManager;
import elite.intel.db.managers.MissionManager;
import elite.intel.db.managers.ReminderManager;
import elite.intel.gameapi.inputs.RoutePlotter;
import elite.intel.gameapi.journal.events.dto.MissionDto;
import elite.intel.gameapi.missions.MassacrePair;
import elite.intel.session.Status;
import elite.intel.util.StringUtls;

import java.util.List;
import java.util.Objects;

/**
 * Plots a route to a system where pirate massacre contracts can be collected.
 * <p>
 * One command for a question the commander asks in three situations, resolved in the order that
 * respects what they are already doing:
 * <ol>
 *   <li>holding open massacre contracts - go back to where that work is issued, so the stack grows
 *       rather than starting over somewhere else;</li>
 *   <li>standing in a hunting ground with none - find who issues against this system;</li>
 *   <li>neither - go to the best pairing on file.</li>
 * </ol>
 * It replaced two near-identical commands that differed only in which of these they tried first,
 * and which the semantic reducer could not tell apart.
 */
@RegisterCommand
public final class NavigateToPirateMissionProviderCommand implements IntelCommand {

    public static final String ID = "navigate_to_pirate_mission_provider";

    /**
     * How far to look when the commander has given no other clue about where they want to work.
     */
    private static final int DEFAULT_RANGE_LY = 200;

    private final HuntingGroundManager huntingGrounds = HuntingGroundManager.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();
    private final MissionManager missionManager = MissionManager.getInstance();

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String llmDescription() {
        return "Plot a route to a pirate-massacre mission provider system - where the kill contracts are "
                + "collected - for the missions already held, for the hunting ground the ship is in, or for the "
                + "best pairing on record.";
    }

    /**
     * Route plotting taps the ship-only GalaxyMapOpen bind, so it works only in the main-ship cockpit.
     */
    @Override
    public boolean isVisibleForLLM(Status status) {
        return status.isInMainShip();
    }

    @Override
    public String execute(JsonObject params, String responseText) {
        Coordinates here = locationManager.getGalacticCoordinates();
        if (here == null) return StringUtls.localizedResponse("handler.pirate.positionUnknown");

        MassacrePair pair = resolve(here);
        if (pair == null) return StringUtls.localizedResponse("handler.pirate.noPairsKnown", DEFAULT_RANGE_LY);

        if (pair.providerSystem().equalsIgnoreCase(here.primaryStar())) {
            return StringUtls.localizedResponse("handler.pirate.alreadyAtProvider",
                    pair.targetSystem(), HuntingGroundSpeech.join(pair.stations()));
        }

        // Non-terminal announcement: the route plotting below must still run, so the finding is voiced
        // via filler rather than returned.
        VegaRuntime.narrator().filler(HuntingGroundSpeech.pair(pair), false);

        RoutePlotter plotter = new RoutePlotter();
        String result = plotter.plotRoute(pair.providerSystem());
        ReminderManager.getInstance().setReminder(
                StringUtls.localizedResponse("handler.pirate.seekProviderReminder", pair.targetSystem()),
                pair.providerSystem()
        );
        return result;
    }

    /**
     * The pairing to fly to, or null when nothing on file matches.
     */
    private MassacrePair resolve(Coordinates here) {
        MassacrePair forOpenContracts = firstFor(openContractTargets(), here);
        if (forOpenContracts != null) return forOpenContracts;

        MassacrePair forThisSystem = firstOf(huntingGrounds.providersFor(here.primaryStar(), here));
        if (forThisSystem != null) return forThisSystem;

        return firstOf(huntingGrounds.bestPairs(here, DEFAULT_RANGE_LY));
    }

    /**
     * The hunting grounds the commander's open massacre contracts send them to.
     */
    private List<String> openContractTargets() {
        return missionManager.getMissions(missionManager.getPirateMissionTypes()).values().stream()
                .filter(Objects::nonNull)
                .map(MissionDto::getDestinationSystem)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private MassacrePair firstFor(List<String> targetSystems, Coordinates here) {
        for (String targetSystem : targetSystems) {
            MassacrePair pair = firstOf(huntingGrounds.providersFor(targetSystem, here));
            if (pair != null) return pair;
        }
        return null;
    }

    private MassacrePair firstOf(List<MassacrePair> pairs) {
        return pairs.isEmpty() ? null : pairs.getFirst();
    }
}
