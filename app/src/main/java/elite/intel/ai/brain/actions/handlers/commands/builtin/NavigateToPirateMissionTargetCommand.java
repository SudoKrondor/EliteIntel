package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.commands.IntelCommand;
import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.db.managers.MissionManager;
import elite.intel.gameapi.MissionType;
import elite.intel.gameapi.inputs.RoutePlotter;
import elite.intel.gameapi.journal.events.dto.MissionDto;
import elite.intel.gameapi.missions.MissionSelection;
import elite.intel.session.PlayerSession;
import elite.intel.session.Status;
import elite.intel.util.StringUtls;

import java.util.Objects;
import java.util.Set;

/**
 * Self-describing "navigate to pirate mission target" command.
 * Owns its own execution: body migrated 1:1 from the legacy NavigateToPirateMassacreMissionTargetHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class NavigateToPirateMissionTargetCommand implements IntelCommand {
    public static final String ID = "navigate_to_pirate_mission_target";

    @Override
    public String llmDescription() {
        return "Plot a route to the target/hunting system of the active pirate-massacre mission (where the pirates are killed).";
    }


    @Override
    public String id() {
        return ID;
    }

    /** Route plotting taps the ship-only GalaxyMapOpen bind; works only in the main-ship cockpit. */
    @Override
    public boolean isVisibleForLLM(Status status) {
        return status.isInMainShip();
    }

    @Override
    public String execute(JsonObject params, String responseText) {
        MissionManager missionManager = MissionManager.getInstance();

        MissionType[] missionTypes = missionManager.getPirateMissionTypes();
        Set<String> targetFactions = missionManager.getTargetFactions(missionTypes);

        if (targetFactions.isEmpty()) {
            return StringUtls.localizedResponse("handler.pirate.noProvidersMassacre");
        }

        // Shares the selection rule with the generic navigate command and the HUD card, so a stack of
        // massacre contracts resolves to the same one everywhere instead of to whatever the map hands
        // back first - and a contract with no destination is stepped over rather than plotted to.
        MissionDto mission = MissionSelection.toPlotFor(
                missionManager.getMissions(missionTypes).values().stream()
                        .filter(Objects::nonNull)
                        .filter(v -> MissionType.MISSION_PIRATE_MASSACRE.equals(v.getMissionType()))
                        .toList(),
                PlayerSession.getInstance().getPrimaryStarName()).orElse(null);

        if (mission == null) return null;

        RoutePlotter plotter = new RoutePlotter();
        return plotter.plotRoute(mission.getDestinationSystem());
    }
}
