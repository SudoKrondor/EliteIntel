package elite.intel.community;

import com.google.common.eventbus.Subscribe;
import elite.intel.community.FirebaseClient.CallFailed;
import elite.intel.community.FirebaseClient.Session;
import elite.intel.community.PublishedFiles.SetsDownload;
import elite.intel.community.PublishedFiles.Switches;
import elite.intel.db.dao.CommunitySyncDao.State;
import elite.intel.db.managers.CommunityManager;
import elite.intel.db.managers.GlobalSettingsManager;
import elite.intel.eventbus.GameEventBus;
import elite.intel.eventbus.UiBus;
import elite.intel.session.SystemSession;
import elite.intel.ui.event.CommunitySyncedEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

/**
 * Trades with the other installs, quietly and in the background. Design: {@code docs/CROWDSOURCED_MASSACRE_SETS.md}.
 * <ul>
 *   <li><b>Stats ping</b>, always: once a day, this install's app version and OS, so we know how many installs
 *       there are. Nothing about the commander.</li>
 *   <li><b>Report</b>, only while the commander shares: the massacre sets every commander on this install has
 *       learned, uploaded when they changed, at most once a day. Turning sharing off withdraws it.</li>
 *   <li><b>Download</b>: once a day, the sets the nightly job checked and published. The pair search uses them only
 *       while the commander shares (give to get). Without sharing they only let VEGA say that sets exist nearby.</li>
 * </ul>
 * WHY everything fails silently: none of it is something the commander asked for at that moment. A step that
 * fails is logged and tried again at the next hourly check, and the app is whole without any of it.
 */
public final class CommunitySync {

    private static final Logger log = LogManager.getLogger(CommunitySync.class);

    /**
     * The security rules refuse a second write to the same document within 20 hours. Uploads and the download
     * follow the same rhythm.
     */
    static final Duration DAILY = Duration.ofHours(20);
    private static final Duration CHECK_EVERY = Duration.ofHours(1);
    private static final Duration FIRST_CHECK = Duration.ofMinutes(2);

    private static volatile CommunitySync instance;

    private final CommunityManager store;
    private final GlobalSettingsManager settings;
    private final FirebaseClient firebase;
    private final PublishedFiles files;
    private final CommunityIdentity identity;
    private boolean started;

    CommunitySync(CommunityManager store, GlobalSettingsManager settings, FirebaseClient firebase, PublishedFiles files) {
        this.store = store;
        this.settings = settings;
        this.firebase = firebase;
        this.files = files;
        this.identity = new CommunityIdentity(firebase, store);
    }

    public static CommunitySync getInstance() {
        if (instance == null) {
            synchronized (CommunitySync.class) {
                if (instance == null) {
                    instance = new CommunitySync(CommunityManager.getInstance(), GlobalSettingsManager.getInstance(),
                            new FirebaseClient(), new PublishedFiles());
                }
            }
        }
        return instance;
    }

    /**
     * Starts the hourly checks, the first a couple of minutes after launch so it does not compete with startup.
     */
    public synchronized void start() {
        if (started) return;
        started = true;
        GameEventBus.register(this);
        Thread.ofVirtual().name("community-sync").start(this::loop);
    }

    @Subscribe
    public void onSharingChanged(MassacreSetSharingChangedEvent event) {
        Thread.ofVirtual().name("community-sync-now").start(() -> runOnce(Instant.now()));
    }

    private void loop() {
        sleep(FIRST_CHECK);
        while (!Thread.currentThread().isInterrupted()) {
            runOnce(Instant.now());
            sleep(CHECK_EVERY);
        }
    }

    /**
     * One pass: whatever is due, each step on its own so one failure does not hold up the others.
     */
    synchronized void runOnce(Instant now) {
        try {
            Switches switches = files.switches();
            State state = store.state();
            boolean sharing = settings.getShareMassacreSets();
            if (switches.heartbeat() && isDue(state.lastPingAt(), now)) step("stats ping", () -> ping(now));
            if (sharing && switches.reportPairs()) step("report upload", () -> report(state, now));
            if (!sharing && state.lastReportAt() != null) step("report withdrawal", this::withdraw);
            if (isDue(state.lastDownloadAt(), now)) step("sets download", () -> download(state, now));
            UiBus.publish(new CommunitySyncedEvent());
        } catch (IOException e) {
            log.warn("Community sync skipped, the website did not answer: {}", e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (RuntimeException e) {
            // WHY: the top of a background thread. Whatever went wrong, the app must keep running and the next
            // hourly check must still happen.
            log.error("Community sync failed", e);
        }
    }

    private void ping(Instant now) throws IOException, InterruptedException {
        Session session = identity.session(now);
        try {
            firebase.writeInstall(session, SystemSession.getInstance().readVersionFromResources(), operatingSystem());
        } catch (CallFailed e) {
            // WHY: refused means this uid already pinged within the day (a restored database forgets that it did).
            // The day is counted either way, so it is recorded as done.
            if (e.status != 403) throw e;
        }
        store.markPinged(now);
    }

    private void report(State state, Instant now) throws IOException, InterruptedException {
        String payload = PairsPayload.encode(store.shareable());
        if (payload.equals("[]")) return;
        String digest = PairsPayload.digest(payload);
        if (digest.equals(state.lastReportDigest())) return;
        if (!isDue(state.lastReportAt(), now)) return;
        firebase.writeReport(identity.session(now), PairsPayload.FORMAT, payload);
        store.markReported(now, digest);
        log.info("Community report uploaded: {} characters", payload.length());
    }

    private void withdraw() throws IOException, InterruptedException {
        firebase.deleteReport(identity.session(Instant.now()));
        store.clearReport();
    }

    private void download(State state, Instant now) throws IOException, InterruptedException {
        SetsDownload download = files.downloadSets(state.setsEtag());
        if (download.sets() != null) {
            store.replaceCrowdSets(download.sets());
            store.saveContributors(download.contributors());
        }
        store.markDownloaded(now, download.etag());
    }

    // ---------------------------------------------------------------- helpers

    private interface Step {
        void run() throws IOException, InterruptedException;
    }

    private static void step(String name, Step step) throws InterruptedException {
        try {
            step.run();
        } catch (IOException e) {
            log.warn("Community {} failed, will retry at the next check: {}", name, e.getMessage());
        }
    }

    static boolean isDue(String lastAt, Instant now) {
        return lastAt == null || !Instant.parse(lastAt).plus(DAILY).isAfter(now);
    }

    private static String operatingSystem() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("windows") ? "windows" : "linux";
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
