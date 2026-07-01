package elite.intel.gameapi.inputs;

import elite.intel.ai.hands.events.GameInputSequenceEvent;
import elite.intel.ai.hands.events.GameInputStep;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.db.managers.GlobalSettingsManager;
import elite.intel.eventbus.GameControllerBus;
import elite.intel.eventbus.GameEventBus;
import elite.intel.session.Status;
import elite.intel.util.StringUtls;

import static elite.intel.ai.hands.Bindings.GameCommand.*;

public class PreFtlChecks {


    public static void preJumpCheck(Status status, String message) {
        GameEventBus.publish(new MissionCriticalAnnouncementEvent(message));
        GlobalSettingsManager settingsManager = GlobalSettingsManager.getInstance();
        if (status.isHardpointsDeployed() && !status.isInSupercruise() && settingsManager.getAutoHardpointsRetractForFtl()) {
            GameEventBus.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.preFtl.retractingHardpoints")));
            GameControllerBus.publish(GameInputSequenceEvent.of(
                    GameInputStep.bindingTap(BINDING_HARDPOINTS_TOGGLE.getGameBinding()),
                    GameInputStep.delay(2000)
            ));
        }
        if (status.isLandingGearDown() && !status.isInSupercruise() && settingsManager.getAutoLandingGearUpForFtl()) {
            GameEventBus.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.preFtl.retractingGear")));
            GameControllerBus.publish(GameInputSequenceEvent.of(
                    GameInputStep.bindingTap(BINDING_LANDING_GEAR_TOGGLE.getGameBinding()),
                    GameInputStep.delay(2000)
            ));
        }
        if (status.isCargoScoopDeployed() && !status.isInSupercruise() && settingsManager.getAutoCargoScoopRetractForFtl()) {
            GameEventBus.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.preFtl.closingCargo")));
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

        ///NOTE: Commented out until FDev fixes their Nomad related bugs
//        if (!status.isInSupercruise() && status.isFighterOut() && settingsManager.getAutoFighterOutFighterDocking()) {
//            GameControllerBus.publish(GameInputSequenceEvent.single(GameInputStep.bindingTap(BINDING_REQUEST_REQUEST_DOCK.getGameBinding())));
//            GameEventBus.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.supercruise.fighterOut")));
//        }

        if (settingsManager.getAutoSpeedUpForFtl()) {
            GameControllerBus.publish(GameInputSequenceEvent.single(GameInputStep.bindingTap(BINDING_SET_SPEED100.getGameBinding())));
        }
    }
}
