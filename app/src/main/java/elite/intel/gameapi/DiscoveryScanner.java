package elite.intel.gameapi;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.hands.events.GameInputSequenceEvent;
import elite.intel.ai.hands.events.GameInputStep;
import elite.intel.db.dao.ShipSettingsDao;
import elite.intel.eventbus.GameControllerBus;
import elite.intel.eventbus.GameEventBus;
import elite.intel.gameapi.journal.events.FSSDiscoveryScanEvent;
import elite.intel.session.Status;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static elite.intel.ai.hands.Bindings.GameCommand.*;
import static elite.intel.gameapi.FireGroups.fireGroupInSettings;

/**
 * Fires the discovery scanner ("honk"): switches the HUD to analysis mode, selects the configured
 * honk fire group, holds the fire trigger down until the {@link FSSDiscoveryScanEvent} confirms the
 * sweep registered, then restores combat mode. Holding until the event (rather than a fixed
 * duration) releases the trigger the moment the scan completes; a safety cap releases it if the
 * event never arrives.
 *
 * <p>Shared by the voice command and the honk-on-jump automation so both honk identically.
 */
public final class DiscoveryScanner {

    private static final Logger log = LogManager.getLogger(DiscoveryScanner.class);

    /**
     * Safety cap: release the trigger even if the discovery-scan event never arrives.
     */
    public static final int SCAN_MAX_HOLD_MS = 8000;

    private DiscoveryScanner() {
    }

    /**
     * Runs the full honk sequence for the given ship settings. Blocks the calling thread until the
     * scan registers or the safety cap fires, so call it off the event-dispatch path.
     */
    public static void honk(ShipSettingsDao.ShipSettings shipSettings) {
        boolean isInCombatMode = !Status.getInstance().isAnalysisMode();
        /// Change to analysis mode
        if (isInCombatMode) {
            GameControllerBus.publish(GameInputSequenceEvent.single(GameInputStep.bindingTap(BINDING_ACTIVATE_ANALYSIS_MODE.getGameBinding())));
        }

        /// Switch fire-group
        FireGroups.cycleToGroup(fireGroupInSettings(shipSettings));

        /// Scan: hold the fire trigger until the discovery scan completes (or the safety cap fires)
        int honkTrigger = shipSettings.getHonkTrigger(); /// 1 primary, 2 secondary
        String fireBinding = honkTrigger == 1
                ? BINDING_PRIMARY_FIRE.getGameBinding()
                : BINDING_SECONDARY_FIRE.getGameBinding();
        holdUntilDiscoveryScan(fireBinding);

        /// change back to combat mode - if the user was in combat mode
        if (isInCombatMode) {
            GameControllerBus.publish(GameInputSequenceEvent.single(GameInputStep.bindingTap(BINDING_ACTIVATE_COMBAT_MODE.getGameBinding())));
        }
    }

    /**
     * Presses the fire trigger down, waits for the discovery scan to register, then releases it.
     * The release runs in a {@code finally} block so the trigger never stays stuck down if the
     * wait is interrupted or the scan event never arrives.
     */
    private static void holdUntilDiscoveryScan(String fireBinding) {
        DiscoveryScanLatch scanLatch = new DiscoveryScanLatch();
        GameEventBus.register(scanLatch);
        try {
            GameControllerBus.publish(GameInputSequenceEvent.single(GameInputStep.bindingDown(fireBinding)));
            boolean scanned = scanLatch.await(SCAN_MAX_HOLD_MS);
            if (!scanned) {
                log.warn("Honk: FSSDiscoveryScan not seen within {} ms; releasing trigger anyway", SCAN_MAX_HOLD_MS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            GameControllerBus.publish(GameInputSequenceEvent.single(GameInputStep.bindingUp(fireBinding)));
            GameEventBus.unregister(scanLatch);
        }
    }

    /**
     * Single-shot EventBus listener that trips when the honk's discovery scan registers.
     */
    public static final class DiscoveryScanLatch {
        private final CountDownLatch latch = new CountDownLatch(1);

        @Subscribe
        public void onDiscoveryScan(FSSDiscoveryScanEvent event) {
            latch.countDown();
        }

        boolean await(long timeoutMs) throws InterruptedException {
            return latch.await(timeoutMs, TimeUnit.MILLISECONDS);
        }
    }
}
