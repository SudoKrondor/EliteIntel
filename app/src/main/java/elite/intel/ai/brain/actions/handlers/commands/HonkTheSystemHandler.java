package elite.intel.ai.brain.actions.handlers.commands;

import com.google.gson.JsonObject;
import elite.intel.ai.hands.events.GameInputSequenceEvent;
import elite.intel.ai.hands.events.GameInputStep;
import elite.intel.db.dao.ShipSettingsDao;
import elite.intel.db.managers.ShipSettingsManager;
import elite.intel.gameapi.FireGroups;
import elite.intel.gameapi.GameControllerBus;
import elite.intel.session.PlayerSession;
import elite.intel.session.Status;

import static elite.intel.ai.hands.Bindings.GameCommand.*;
import static elite.intel.gameapi.FireGroups.fireGroupInSettings;

public class HonkTheSystemHandler implements CommandHandler {

    public static final int SCAN_HOLD_MS = 4900;
    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final ShipSettingsManager shipSettingsManager = ShipSettingsManager.getInstance();
    private final Status status = Status.getInstance();

    @Override
    public void handle(String action, JsonObject params, String responseText) {
        ShipSettingsDao.ShipSettings shipSettings = shipSettingsManager.getSettings(playerSession.getShipLoadout().getShipId());

        boolean isHudInCombatMode = !status.isAnalysisMode();
        /// Change to analysis mode
        if (isHudInCombatMode) {
            GameControllerBus.publish(GameInputSequenceEvent.single(GameInputStep.bindingTap(BINDING_ACTIVATE_ANALYSIS_MODE.getGameBinding())));
        }

        /// Switch fire-group
        FireGroups.cycleToGroup(fireGroupInSettings(shipSettings));

        /// Scan
        int honkTrigger = shipSettings.getHonkTrigger(); /// 1 primary, 2 secondary
        if (honkTrigger == 1) {
            GameControllerBus.publish(GameInputSequenceEvent.single(GameInputStep.bindingHold(BINDING_PRIMARY_FIRE.getGameBinding(), SCAN_HOLD_MS)));
        } else {
            GameControllerBus.publish(GameInputSequenceEvent.single(GameInputStep.bindingHold(BINDING_SECONDARY_FIRE.getGameBinding(), SCAN_HOLD_MS)));
        }

        /// change back to combat mode - if the user was in combat mode
        if (isHudInCombatMode) {
            GameControllerBus.publish(GameInputSequenceEvent.single(GameInputStep.bindingTap(BINDING_ACTIVATE_COMBAT_MODE.getGameBinding())));
        }
    }
}
