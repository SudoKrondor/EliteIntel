package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.gameapi.journal.events.LaunchDroneEvent;
import elite.intel.session.PlayerSession;

/**
 * Keeps the stored hold's limpet count current while limpets are being flown.
 * <p>
 * Launching a limpet spends one out of the cargo hold, but the game does not rewrite
 * {@code Cargo.json} for it - the only evidence is the {@code LaunchDrone} journal line. Left
 * alone, the count the app holds stays at whatever the last cargo snapshot said, so a miner
 * watching the HUD sees a full limpet rack while actually running dry. Each launch is therefore
 * subtracted here, and every real {@code Cargo} event overwrites the whole snapshot again -
 * meaning any drift this introduces is corrected by the game itself moments later.
 * <p>
 * Deliberately silent: this is bookkeeping, not an announcement. A limpet launch is a
 * once-a-minute occurrence during a mining run and nobody wants it narrated.
 * <p>
 * Replayed journal lines never reach the live bus ({@code JournalParser} drops them), so the
 * limpets a previous session launched are not subtracted from this session's hold.
 */
@SuppressWarnings("unused")//registered in SubscriberRegistration
public class LaunchDroneSubscriber {

    private final PlayerSession playerSession = PlayerSession.getInstance();

    @Subscribe
    public void onLaunchDrone(LaunchDroneEvent event) {
        playerSession.recordDroneLaunched();
    }
}
