package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.eventbus.GameEventBus;
import elite.intel.gameapi.inputs.RoutePlotter;
import elite.intel.gameapi.journal.events.dto.CarrierDataDto;
import elite.intel.session.PlayerSession;
import elite.intel.session.Status;
import elite.intel.util.StringUtls;

/**
 * Self-describing "navigate to squadron carrier" command.
 * Owns its own execution: body migrated 1:1 from the legacy NavigateToMySquadronCarrier,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class NavigateToSquadronCarrierCommand implements IntelCommand {
    public static final String ID = "navigate_to_squadron_carrier";

    @Override public String llmDescription() { return "Plot a route to the squadron carrier."; }


    @Override
    public String id() {
        return ID;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        Status status = Status.getInstance();
        if (status.isInSrv() || status.isInMainShip()) {
            PlayerSession playerSession = PlayerSession.getInstance();
            CarrierDataDto squadronCarrier = playerSession.getSquadronCarrierData();

            if (squadronCarrier == null || squadronCarrier.getStarName() == null || squadronCarrier.getStarName().isEmpty()) {
                GameEventBus.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.navigate.squadronCarrierNotAvailable")));
                return;
            }

            RoutePlotter plotter = new RoutePlotter();
            plotter.plotRoute(squadronCarrier.getStarName());
        } else {
            GameEventBus.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.navigate.notInShipOrSrv")));
        }
    }
}
