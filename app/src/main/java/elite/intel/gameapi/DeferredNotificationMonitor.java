package elite.intel.gameapi;

import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.db.dao.DeferredNotificationDao;
import elite.intel.db.managers.DeferredNotificationManager;
import elite.intel.eventbus.GameEventBus;
import elite.intel.ui.controller.ManagedService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class DeferredNotificationMonitor implements Runnable, ManagedService {


    private static final Logger log = LogManager.getLogger(DeferredNotificationMonitor.class);
    private static DeferredNotificationMonitor intance;
    private final DeferredNotificationManager manager = DeferredNotificationManager.getInstance();
    private Thread processingThread;
    private volatile boolean isRunning;

    public static synchronized DeferredNotificationMonitor getInstance() {
        if (intance == null) {
            intance = new DeferredNotificationMonitor();
        }
        return intance;
    }

    public void start() {
        if (processingThread != null && processingThread.isAlive()) {
            log.warn("DeferredNotificationMonitor is already running");
            return;
        }
        isRunning = true;
        processingThread = new Thread(this, "DeferredNotificationMonitor");
        processingThread.start();
    }

    public void stop() {
        if (processingThread == null || !processingThread.isAlive()) {
            log.warn("DeferredNotificationMonitor is not running");
            return;
        }
        isRunning = false;
        processingThread.interrupt();
        try {
            processingThread.join(5000); // Wait up to 5 seconds for clean shutdown
            log.info("DeferredNotificationMonitor stopped");
        } catch (InterruptedException e) {
            log.error("Interrupted while waiting for DeferredNotificationMonitor to stop", e);
            Thread.currentThread().interrupt(); // Restore interrupted status
        }
        processingThread = null;
    }

    @Override public void run() {
        while (isRunning) {
            List<DeferredNotificationDao.DeferredNotification> list = manager.get();
            for (DeferredNotificationDao.DeferredNotification notification : list) {
                GameEventBus.publish(new MissionCriticalAnnouncementEvent(notification.getNotification()));
                manager.delete(notification);
            }
            try {
                //noinspection BusyWait
                Thread.sleep(5 * 1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
