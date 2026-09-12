package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.brain.vega.VegaRuntime;
import elite.intel.gameapi.journal.events.FriendsEvent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("unused")
public class FriendsEventSubscriber {

    private static final long BATCH_DELAY_MS = 2000;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "FriendsBatch-Thread");
        t.setDaemon(true);
        return t;
    });

    // name → status; duplicate names within the window are overwritten with the latest status
    private final Map<String, String> pending = new LinkedHashMap<>();
    private ScheduledFuture<?> flushTask;

    @Subscribe
    public synchronized void onFriendsEvent(FriendsEvent event) {
        pending.put(event.getName(), event.getStatus());
        if (flushTask != null && !flushTask.isDone()) {
            flushTask.cancel(false);
        }
        flushTask = scheduler.schedule(this::flush, BATCH_DELAY_MS, TimeUnit.MILLISECONDS);
    }

    private synchronized void flush() {
        if (pending.isEmpty()) return;
        StringBuilder data = new StringBuilder();
        pending.forEach((name, status) -> {
            if (!data.isEmpty()) data.append(", ");
            data.append(name).append(" is ").append(status);
        });
        pending.clear();
        // The name is all the journal gives us: nothing in it says who the friend is, so the model must not
        // guess. A pronoun of any kind is a guess - and the guess it makes from a name is wrong more often
        // than not - so each friend is referred to by name and only ever by name.
        String instructions = "Report each friend's name and their new status (online, offline, etc.). One friend per sentence. "
                + "Refer to every friend by name only, every time: never use a pronoun for a friend, and never guess anything about a friend from their name.";
        VegaRuntime.narrator().narrate("Friends: " + data, instructions);
    }
}
