package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.commands.IntelCommand;
import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.db.dao.ShipSettingsDao;
import elite.intel.db.managers.ShipSettingsManager;
import elite.intel.gameapi.DiscoveryScanner;
import elite.intel.session.PlayerSession;
import elite.intel.session.Status;

/**
 * Stage-4b self-describing command for "honk the system". Delegates to {@link DiscoveryScanner},
 * which holds the discovery-scanner trigger until the scan registers.
 */
@RegisterCommand
public final class RunSystemScanCommand implements IntelCommand {
    public static final String ID = "run_discovery_scan";

    @Override
    public String llmDescription() {
        return "Fire the discovery scanner for a quick system-wide 'honk' revealing the number of bodies and signal sources. Preliminary scan, not the detailed FSS.";
    }

    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final ShipSettingsManager shipSettingsManager = ShipSettingsManager.getInstance();

    @Override
    public String id() {
        return ID;
    }

    /** Discovery scanner: fired while flying the main ship (normal space or supercruise); not docked/landed. */
    @Override
    public boolean isVisibleForLLM(Status status) {
        return status.isInMainShip() && !status.isDocked() && !status.isLanded();
    }

    @Override
    public String execute(JsonObject params, String responseText) {
        ShipSettingsDao.ShipSettings shipSettings = shipSettingsManager.getSettings(playerSession.getShipLoadout().getShipId());
        DiscoveryScanner.honk(shipSettings);
        return null;
    }
}
