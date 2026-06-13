package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.mouth.subscribers.events.MiningAnnouncementEvent;
import elite.intel.db.dao.MaterialsDao;
import elite.intel.db.managers.MaterialManager;
import elite.intel.db.util.Database;
import elite.intel.gameapi.EventBusManager;
import elite.intel.gameapi.journal.events.MaterialCollectedEvent;
import elite.intel.search.edsm.dto.MaterialsType;
import elite.intel.util.StringUtls;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static elite.intel.util.StringUtls.localizedEvent;
import static elite.intel.util.StringUtls.localizedEventPlural;

public class MaterialCollectedSubscriber {

    private static final int DEBOUNCE_MS = 2000;

    private final MaterialManager materialManager = MaterialManager.getInstance();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final List<String> pending = new ArrayList<>();
    private ScheduledFuture<?> pendingAnnouncement;

    @Subscribe
    public void onMaterialCollected(MaterialCollectedEvent event) {
        materialManager.save(event.getName(), determineType(event.getCategory()), event.getCount());

        MaterialsDao.Material material = Database.withDao(MaterialsDao.class, dao -> dao.findByExactName(StringUtls.capitalizeWords(event.getName())));
        String message = material == null
                ? localizedEvent("event.material.collected", event.getCount(), event.getName())
                : localizedEvent("event.material.collectedTotal", event.getCount(), event.getName(), material.getAmount());

        synchronized (pending) {
            pending.add(message);
            if (pendingAnnouncement != null) {
                pendingAnnouncement.cancel(false);
            }
            pendingAnnouncement = scheduler.schedule(this::flush, DEBOUNCE_MS, TimeUnit.MILLISECONDS);
        }
    }

    private void flush() {
        synchronized (pending) {
            if (pending.isEmpty()) return;
            String announcement = pending.size() == 1
                    ? pending.getFirst()
                    : localizedEventPlural(pending.size(), "event.material.batchCollected");
            pending.clear();
            EventBusManager.publish(new MiningAnnouncementEvent(announcement));
        }
    }

    private MaterialsType determineType(String category) {
        if ("Raw".equalsIgnoreCase(category)) return MaterialsType.GAME_RAW;
        if ("Manufactured".equalsIgnoreCase(category)) return MaterialsType.GAME_MANUFACTURED;
        if ("Encoded".equalsIgnoreCase(category)) return MaterialsType.GAME_ENCODED;
        else return MaterialsType.GAME_UNKNOWN;
    }
}
