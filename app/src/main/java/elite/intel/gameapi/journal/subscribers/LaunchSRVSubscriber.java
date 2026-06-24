package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.hands.Bindings;
import elite.intel.ai.hands.events.GameInputSequenceEvent;
import elite.intel.ai.hands.events.GameInputStep;
import elite.intel.db.managers.GlobalSettingsManager;
import elite.intel.eventbus.GameControllerBus;
import elite.intel.gameapi.journal.events.LaunchSRVEvent;
import elite.intel.session.Status;
import elite.intel.util.SleepNoThrow;

public class LaunchSRVSubscriber {

    private final Status status = Status.getInstance();
    private final GlobalSettingsManager globalSettingsManager = GlobalSettingsManager.getInstance();
    @Subscribe
    public void onLaunchSRVEvent(LaunchSRVEvent event) {
        if (globalSettingsManager.getAutoLightsOffForSrvDeployment()) {
            Thread.ofVirtual().start(() -> {
                SleepNoThrow.sleep(6000);
                if (status.isSrvHighBeam() || status.isLightsOn()) {
                    if (status.isSrvHighBeam()) {
                        toggleLights(Bindings.GameCommand.BINDING_BUGGY_LIGHTS_TOGGLE.getGameBinding());
                    } else {
                        toggleLights(Bindings.GameCommand.BINDING_BUGGY_LIGHTS_TOGGLE.getGameBinding());
                        toggleLights(Bindings.GameCommand.BINDING_BUGGY_LIGHTS_TOGGLE.getGameBinding());
                    }
                }
            });
        }
    }

    private void toggleLights(String binding) {
        GameControllerBus.publish(GameInputSequenceEvent.single(GameInputStep.bindingTap(binding)));
    }
}
