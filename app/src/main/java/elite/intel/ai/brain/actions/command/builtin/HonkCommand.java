package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.db.dao.ShipSettingsDao;
import elite.intel.db.managers.ShipSettingsManager;
import elite.intel.gameapi.DiscoveryScanner;
import elite.intel.session.PlayerSession;

/**
 * Stage-4b self-describing command for "honk the system". Delegates to {@link DiscoveryScanner},
 * which holds the discovery-scanner trigger until the scan registers.
 */
@RegisterCommand
public final class HonkCommand implements IntelCommand {
    public static final String ID = "discovery_scan_honk";

    @Override public String llmDescription() { return "Fire the discovery scanner (honk) to map the system."; }

    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final ShipSettingsManager shipSettingsManager = ShipSettingsManager.getInstance();

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        ShipSettingsDao.ShipSettings shipSettings = shipSettingsManager.getSettings(playerSession.getShipLoadout().getShipId());
        DiscoveryScanner.honk(shipSettings);
    }
}
