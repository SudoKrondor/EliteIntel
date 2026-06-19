package elite.intel.ai.hands;

import elite.intel.ai.hands.events.GameInputSequenceEvent;
import elite.intel.ai.hands.events.GameInputStep;

import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.db.managers.GlobalSettingsManager;
import elite.intel.gameapi.EventBusManager;
import elite.intel.gameapi.GameControllerBus;
import elite.intel.session.Status;
import elite.intel.util.StringUtls;

import static elite.intel.ai.hands.Bindings.GameCommand.*;

public class PreFtlChecks {


    public static void preJumpCheck(Status status, String message) {
        EventBusManager.publish(new MissionCriticalAnnouncementEvent(message));
        GlobalSettingsManager settingsManager = GlobalSettingsManager.getInstance();
        if (status.isHardpointsDeployed() && !status.isInSupercruise() && settingsManager.getAutoHardpointsRetractForFtl()) {
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.preFtl.retractingHardpoints")));
            GameControllerBus.publish(GameInputSequenceEvent.of(
                    GameInputStep.bindingTap(BINDING_HARDPOINTS_TOGGLE.getGameBinding()),
                    GameInputStep.delay(2000)
            ));
        }
        if (status.isLandingGearDown() && !status.isInSupercruise() && settingsManager.getAutoLandingGearUpForFtl()) {
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.preFtl.retractingGear")));
            GameControllerBus.publish(GameInputSequenceEvent.of(
                    GameInputStep.bindingTap(BINDING_LANDING_GEAR_TOGGLE.getGameBinding()),
                    GameInputStep.delay(2000)
            ));
        }
        if (status.isCargoScoopDeployed() && !status.isInSupercruise() && settingsManager.getAutoCargoScoopRetractForFtl()) {
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.preFtl.closingCargo")));
            GameControllerBus.publish(GameInputSequenceEvent.of(
                    GameInputStep.bindingTap(BINDING_TOGGLE_CARGO_SCOOP.getGameBinding()),
                    GameInputStep.delay(2000)
            ));
        }

        if (status.isLightsOn() && settingsManager.getAutoLightsForFtl()) {
            GameControllerBus.publish(GameInputSequenceEvent.single(GameInputStep.bindingTap(BINDING_SHIP_LIGHTS_TOGGLE.getGameBinding())));
        }

        if (status.isNightVision() && settingsManager.getAutoNightVisionOff()) {
            GameControllerBus.publish(GameInputSequenceEvent.single(GameInputStep.bindingTap(BINDING_NIGHT_VISION_TOGGLE.getGameBinding())));
        }

        if (!status.isInSupercruise() && status.isFighterOut() && settingsManager.getAutoFighterOutFighterDocking()) {
            GameControllerBus.publish(GameInputSequenceEvent.single(GameInputStep.bindingTap(BINDING_REQUEST_REQUEST_DOCK.getGameBinding())));
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.supercruise.fighterOut")));
        }

        if (settingsManager.getAutoSpeedUpForFtl()) {
            GameControllerBus.publish(GameInputSequenceEvent.single(GameInputStep.bindingTap(BINDING_SET_SPEED100.getGameBinding())));
        }
    }
}
