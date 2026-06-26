package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.eventbus.GameEventBus;
import elite.intel.gameapi.journal.events.dto.TargetLocation;
import elite.intel.session.PlayerSession;
import elite.intel.util.StringUtls;

/**
 * Stage-4b self-describing command for "cancel navigation".
 */
@RegisterCommand
public final class CancelNavigationCommand implements IntelCommand {
    public static final String ID = "cancel_navigation";

    @Override public String llmDescription() { return "Cancel the current navigation route."; }


    private final PlayerSession playerSession = PlayerSession.getInstance();

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        playerSession.setTracking(new TargetLocation(false));
        GameEventBus.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.navigate.navigationOff")));
    }
}
