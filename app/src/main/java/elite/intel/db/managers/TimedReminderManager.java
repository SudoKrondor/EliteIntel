package elite.intel.db.managers;

import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.eventbus.GameEventBus;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public final class TimedReminderManager {

    private static volatile TimedReminderManager instance;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "timed-reminder");
        t.setDaemon(true);
        return t;
    });

    private final List<ScheduledFuture<?>> pending = new ArrayList<>();

    private TimedReminderManager() {
    }

    public static TimedReminderManager getInstance() {
        TimedReminderManager result = instance;
        if (result == null) {
            synchronized (TimedReminderManager.class) {
                result = instance;
                if (result == null) {
                    instance = result = new TimedReminderManager();
                }
            }
        }
        return result;
    }

    public synchronized void schedule(String text, int minutes) {
        ScheduledFuture<?> future = scheduler.schedule(
                () -> GameEventBus.publish(new MissionCriticalAnnouncementEvent("Reminder: " + text)),
                minutes,
                TimeUnit.MINUTES
        );
        pending.add(future);
    }

    public synchronized void clearAll() {
        pending.forEach(f -> f.cancel(false));
        pending.clear();
    }

    public synchronized boolean hasPending() {
        pending.removeIf(ScheduledFuture::isDone);
        return !pending.isEmpty();
    }
}