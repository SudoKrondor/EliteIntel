package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.hands.events.GameInputSequenceEvent;
import elite.intel.ai.hands.events.GameInputStep;
import elite.intel.db.managers.GlobalSettingsManager;
import elite.intel.eventbus.GameControllerBus;
import elite.intel.gameapi.journal.events.LiftoffEvent;
import elite.intel.session.PlayerSession;
import elite.intel.session.Status;

import static elite.intel.ai.hands.Bindings.GameCommand.BINDING_LANDING_GEAR_TOGGLE;

@SuppressWarnings("unused")
public class LiftoffEventSubscriber {

    private final GlobalSettingsManager globalSettingsManager = GlobalSettingsManager.getInstance();
    private final PlayerSession playerSession = PlayerSession.getInstance();
    @Subscribe
    public void onLiftoffEvent(LiftoffEvent event) {
        if (globalSettingsManager.getAutoGearUpOnTakeOff()) {
            Thread.ofVirtual().start(() -> {
                Status status = Status.getInstance();
                if (status.isInMainShip() && status.isLandingGearDown()) {
                    GameControllerBus.publish(GameInputSequenceEvent.single(GameInputStep.bindingTap(BINDING_LANDING_GEAR_TOGGLE.getGameBinding())));
                }
            });
        }

        playerSession.setShipAutoDeparted(!event.isPlayerControlled());
    }
}
