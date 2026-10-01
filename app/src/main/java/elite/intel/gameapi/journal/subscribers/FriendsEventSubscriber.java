package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.brain.vega.VegaRuntime;
import elite.intel.gameapi.journal.events.FileheaderEvent;
import elite.intel.gameapi.journal.events.FriendsEvent;
import elite.intel.gameapi.journal.events.ShutdownEvent;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static elite.intel.util.StringUtls.localizedEvent;

/**
 * Announces friends coming online or going offline - only when that actually changed, and without reading a
 * long friends list out name by name.
 * <p>
 * WHY it remembers: the game re-sends the whole online list during a session (after undocking, among others),
 * with nobody's status changed. Spoken as it arrived, a commander with four friends online heard the roll call
 * twice in two minutes (support bundle of 2026-09-30). So each friend's last status is kept for the game session
 * and a repeat is silent.
 * <p>
 * WHY the lines are fixed: the journal gives a name and a status and nothing else. Phrased by the LLM, the
 * repeat came back as "online in Perilous Range" - one friend's name turned into a place. A finished, localized
 * line cannot invent anything, and several friends at once collapse into one sentence.
 * <p>
 * Only Online and Offline are spoken. The friend-request statuses (Requested, Declined, Added, Lost) are kept
 * for the change check but not voiced.
 */
@SuppressWarnings("unused")
public class FriendsEventSubscriber {

    private static final long BATCH_DELAY_MS = 2000;
    static final String ONLINE = "Online";
    static final String OFFLINE = "Offline";

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "FriendsBatch-Thread");
        t.setDaemon(true);
        return t;
    });

    // name → status; duplicate names within the window are overwritten with the latest status
    private final Map<String, String> pending = new LinkedHashMap<>();
    // name → last status seen this game session
    private final Map<String, String> known = new HashMap<>();
    private ScheduledFuture<?> flushTask;

    /**
     * Who came online and who went offline in one batch, in arrival order.
     */
    record Changes(List<String> cameOnline, List<String> wentOffline) {
    }

    @Subscribe
    public synchronized void onFriendsEvent(FriendsEvent event) {
        pending.put(event.getName(), event.getStatus());
        if (flushTask != null && !flushTask.isDone()) {
            flushTask.cancel(false);
        }
        flushTask = scheduler.schedule(this::flush, BATCH_DELAY_MS, TimeUnit.MILLISECONDS);
    }

    /**
     * A new game launch starts a new journal at part 1, and the game sends the online list afresh. Later parts
     * are the same session continuing in a new file, so they keep what is known.
     */
    @Subscribe
    public synchronized void onFileheader(FileheaderEvent event) {
        if (event.getPart() <= 1) {
            known.clear();
        }
    }

    @Subscribe
    public synchronized void onShutdown(ShutdownEvent event) {
        known.clear();
    }

    private synchronized void flush() {
        Changes changes = applyBatch(pending, known);
        pending.clear();
        speak(changes.cameOnline(), "event.friends.online.single", "event.friends.online.several");
        speak(changes.wentOffline(), "event.friends.offline.single", "event.friends.offline.several");
    }

    /**
     * Folds one batch into {@code known} and returns only the real changes. A status equal to the last one seen
     * is a repeat. Offline counts only for a friend last seen online: one the session never saw online has
     * nothing to report going away.
     */
    static Changes applyBatch(Map<String, String> batch, Map<String, String> known) {
        List<String> cameOnline = new ArrayList<>();
        List<String> wentOffline = new ArrayList<>();
        batch.forEach((name, status) -> {
            String previous = known.put(name, status);
            if (status == null || status.equals(previous)) {
                return;
            }
            if (ONLINE.equals(status)) {
                cameOnline.add(name);
            } else if (OFFLINE.equals(status) && ONLINE.equals(previous)) {
                wentOffline.add(name);
            }
        });
        return new Changes(cameOnline, wentOffline);
    }

    private static void speak(List<String> names, String singleKey, String severalKey) {
        if (names.isEmpty()) {
            return;
        }
        String phrase = names.size() == 1
                ? localizedEvent(singleKey, names.get(0))
                : localizedEvent(severalKey);
        VegaRuntime.narrator().announce(phrase, false);
    }
}
