package elite.intel.ai.brain.actions.command.builtin;
import elite.intel.ai.brain.actions.command.CommandIds;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.hands.RoutePlotter;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.gameapi.EventBusManager;
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

    private final PlayerSession playerSession = PlayerSession.getInstance();

    @Override
    public String id() {
        return CommandIds.NAVIGATE_TO_HOME_SYSTEM;
    }

    @Override
    public boolean ownsExecution() {
        return true;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        Status status = Status.getInstance();
        if(status.isInSrv() || status.isInMainShip()) {
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.navigate.plottingHome")));
            LocationDto location = playerSession.getHomeSystem();
            if (location.getBodyId() == -1) {
                EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.navigate.homeNotSet")));
                return;
            }
            RoutePlotter plotter = new RoutePlotter();
            plotter.plotRoute(location.getStarName());
        } else {
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.navigate.notInShipOrSrv")));
        }
    }
}
