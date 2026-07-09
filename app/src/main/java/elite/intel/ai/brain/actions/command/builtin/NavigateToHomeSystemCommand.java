package elite.intel.ai.brain.actions.command.builtin;

import elite.intel.companion.CompanionRuntime;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.eventbus.GameEventBus;
import elite.intel.gameapi.inputs.RoutePlotter;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.session.PlayerSession;
import elite.intel.session.Status;
import elite.intel.util.StringUtls;

/**
 * Self-describing "navigate to home system" command.
 * Owns its own execution: body migrated 1:1 from the legacy NavigateToHomeHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class NavigateToHomeSystemCommand implements IntelCommand {
    public static final String ID = "navigate_to_home_system";

    @Override
    public String llmDescription() {
        return "Plot a route to the commander's saved home system.";
    }


    private final PlayerSession playerSession = PlayerSession.getInstance();

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
        CompanionRuntime.narrator().filler(StringUtls.localizedLlm("handler.navigate.plottingHome"), false);
        LocationDto location = playerSession.getHomeSystem();
        if (location.getBodyId() == -1) {
            return StringUtls.localizedLlm("handler.navigate.homeNotSet");
        }
        RoutePlotter plotter = new RoutePlotter();
        return plotter.plotRoute(location.getStarName());
    }
}
