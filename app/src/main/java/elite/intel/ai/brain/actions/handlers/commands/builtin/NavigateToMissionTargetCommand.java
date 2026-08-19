package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.ai.brain.actions.handlers.commands.IntelCommand;
import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.ai.brain.vega.CompanionRuntime;
import elite.intel.db.managers.MissionManager;
import elite.intel.db.managers.ReminderManager;
import elite.intel.gameapi.inputs.RoutePlotter;
import elite.intel.gameapi.journal.events.dto.MissionDto;
import elite.intel.gameapi.missions.MissionSelection;
import elite.intel.session.PlayerSession;
import elite.intel.session.Status;
import elite.intel.util.StringUtls;

import java.util.List;
import java.util.Objects;

/**
 * Self-describing "navigate to mission target" command.
 * <p>
 * The hard part is not the plotting, it is deciding which mission the commander meant. A board is taken
 * in stacks and plenty of what is on it has nowhere to fly to - a donation is handed over at the station
 * it was accepted at, so the journal gives it no destination at all. Picking blind out of the stack used
 * to hand the galaxy map a null system name, which {@link RoutePlotter} then dropped on the floor: the
 * commander asked for a route, heard the acknowledgement, and got no route and no explanation.
 * {@link MissionSelection#toPlotFor} does the choosing, so this command and the HUD card agree on what
 * "the active mission" is.
 */
@RegisterCommand
public final class NavigateToMissionTargetCommand implements IntelCommand {
    public static final String ID = "navigate_to_active_mission";

    @Override
    public String llmDescription() {
        return "Plot a route to an active mission's destination; the optional 'key' matches a specific mission by keyword, otherwise the most recent mission that has a destination is used.";
    }


    private final MissionManager missionManager = MissionManager.getInstance();

    private static final String PARAM_KEY = "key";

    private static final List<ActionParameterSpec> PARAMETERS = buildParameters();

    private static List<ActionParameterSpec> buildParameters() {
        ActionParameterSpec key = new ActionParameterSpec(
                PARAM_KEY, "string", false,
                "Optional keyword to pick a specific mission (e.g. faction, commodity, or target name). "
                        + "If omitted, the most recent mission with a destination is used.",
                List.of("massacre", "courier"),
                "Extract a distinguishing keyword from the mission the commander names; otherwise omit it.");
        key.validate();
        return List.of(key);
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
    public List<ActionParameterSpec> parameters() {
        return PARAMETERS;
    }

    @Override
    public String execute(JsonObject params, String responseText) {
        List<MissionDto> candidates = candidates(keyword(params));
        if (candidates.isEmpty()) {
            return StringUtls.localizedResponse("handler.navigate.noMissionsFound");
        }

        MissionDto mission = MissionSelection
                .toPlotFor(candidates, PlayerSession.getInstance().getPrimaryStarName())
                .orElse(null);
        if (mission == null) {
            // There are missions, they simply have nowhere to fly to - which is a different answer to
            // having none, and the commander needs to hear which one it is before they ask again.
            return StringUtls.localizedResponse("handler.navigate.noMissionDestination");
        }

        String system = mission.getDestinationSystem();
        String heading = StringUtls.localizedResponse("handler.navigate.headToSystem", system);

        // The port belongs in its own column, not in the sentence: the HUD overlay draws the reminder as
        // a card and cannot take it back out of the prose.
        ReminderManager.getInstance().setReminder(heading, system, mission.getDestinationStation(), null);

        CompanionRuntime.narrator().filler(heading, false);
        return new RoutePlotter().plotRoute(system);
    }

    /**
     * The missions the keyword names, or the whole board when it names none.
     * <p>
     * A keyword that matches nothing falls back rather than failing, because the commander asking for
     * "the mission" with a word the mission-type keywords do not carry still means the board they are
     * holding.
     */
    private List<MissionDto> candidates(String keyword) {
        if (keyword != null && !keyword.isBlank()) {
            List<MissionDto> matched = missionManager.findByKeyword(keyword).stream()
                    .filter(Objects::nonNull)
                    .toList();
            if (!matched.isEmpty()) return matched;
        }
        return missionManager.getMissions().values().stream()
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * The optional keyword, tolerating an absent parameter and a JSON null alike.
     */
    private static String keyword(JsonObject params) {
        if (params == null) return null;
        JsonElement key = params.get(PARAM_KEY);
        return key == null || key.isJsonNull() ? null : key.getAsString();
    }
}
