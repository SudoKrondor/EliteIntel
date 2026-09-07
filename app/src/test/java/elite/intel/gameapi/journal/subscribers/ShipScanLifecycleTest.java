package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.eventbus.GameEventBus;
import elite.intel.gameapi.journal.events.BountyEvent;
import elite.intel.gameapi.journal.events.LoadGameEvent;
import elite.intel.gameapi.journal.events.ShipTargetedEvent;
import elite.intel.session.PlayerSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class ShipScanLifecycleTest {

    private static final String PILOT_A = "$Pirate_Alpha;";
    private static final String SHIP_A = "sidewinder";
    private static final String FACTION_A = "Gang A";
    private static final String PILOT_B = "$Pirate_Beta;";
    private static final String SHIP_B = "cobra";
    private static final String FACTION_B = "Gang B";

    private final PlayerSession session = PlayerSession.getInstance();
    private final ShipTargetedEventSubscriber targetSubscriber = new ShipTargetedEventSubscriber();
    private final BountyEventSubscriber bountySubscriber = new BountyEventSubscriber();
    private final LoadGameEventSubscriber loadGameSubscriber = new LoadGameEventSubscriber();
    private final List<String> announcements = new CopyOnWriteArrayList<>();
    private volatile String targetAnnouncement;
    private volatile CountDownLatch bountyNarrated;

    @BeforeEach
    void registerRecorderAndClearScans() {
        session.clearShipScans();
        GameEventBus.register(this);
    }

    @AfterEach
    void unregisterRecorderAndClearScans() {
        GameEventBus.unregister(this);
        session.clearShipScans();
    }

    @Subscribe
    public void recordAnnouncement(MissionCriticalAnnouncementEvent event) {
        String text = event.getText();
        announcements.add(text);
        CountDownLatch completion = bountyNarrated;
        if (completion != null && !text.equals(targetAnnouncement)) completion.countDown();
    }

    @Test
    void unrelatedBountyPreservesTheCompletedScanAndDoesNotReannounceIt() throws InterruptedException {
        ShipTargetedEvent targetA = wantedScan(PILOT_A, "Pirate Alpha", SHIP_A, "Sidewinder", FACTION_A);
        ShipScanIdentity identityA = identity(PILOT_A, SHIP_A, FACTION_A);
        ShipScanIdentity identityB = identity(PILOT_B, SHIP_B, FACTION_B);

        targetSubscriber.onShipTargetedEvent(targetA);
        rememberTargetAnnouncement();
        // Seed B so the virtual thread's exact eviction transition is observable without using
        // its spoken bounty callout as a proxy for completion.
        session.putShipScan(identityB.key(), identityB.preimage());

        beginBountyWait();
        bountySubscriber.onBountyEvent(bounty(PILOT_B, SHIP_B, FACTION_B));
        awaitTrue(() -> session.getShipScan(identityB.key()) == null,
                "the bounty target's scan key was not evicted");
        assertNotNull(session.getShipScan(identityA.key()), "target A's scan key must be preserved");
        awaitBountyCompletion();

        targetSubscriber.onShipTargetedEvent(targetA);

        assertEquals(1L, targetAnnouncementCount(),
                "scan A -> bounty B -> scan A must announce A only once: " + announcements);
    }

    @Test
    void matchingBountyEvictsTheCompletedScanAndAllowsItToReannounce() throws InterruptedException {
        ShipTargetedEvent targetA = wantedScan(PILOT_A, "Pirate Alpha", SHIP_A, "Sidewinder", FACTION_A);
        ShipScanIdentity identityA = identity(PILOT_A, SHIP_A, FACTION_A);

        targetSubscriber.onShipTargetedEvent(targetA);
        rememberTargetAnnouncement();

        beginBountyWait();
        bountySubscriber.onBountyEvent(bounty(PILOT_A, SHIP_A, FACTION_A));
        awaitTrue(() -> session.getShipScan(identityA.key()) == null,
                "the matching bounty did not evict target A's scan key");
        awaitBountyCompletion();

        targetSubscriber.onShipTargetedEvent(targetA);

        assertEquals(2L, targetAnnouncementCount(),
                "scan A -> bounty A -> scan A must announce A twice: " + announcements);
    }

    @Test
    void wantedScanWithoutRawPilotIdentityStillAnnounces() {
        ShipTargetedEvent scan = wantedScan(null, "Pirate Alpha", SHIP_A, "Sidewinder", FACTION_A);

        targetSubscriber.onShipTargetedEvent(scan);
        targetSubscriber.onShipTargetedEvent(scan);

        assertEquals(2, announcements.size(),
                "narration must remain available when a raw PilotName cannot support dedupe");
    }

    @Test
    void wantedScanWithoutRawShipIdentityStillAnnounces() {
        ShipTargetedEvent scan = wantedScan(PILOT_A, "Pirate Alpha", null, "Sidewinder", FACTION_A);

        targetSubscriber.onShipTargetedEvent(scan);
        targetSubscriber.onShipTargetedEvent(scan);

        assertEquals(2, announcements.size(),
                "narration must remain available when a raw Ship cannot support dedupe");
    }

    @Test
    void loadGameClearsPreExistingShipScanState() throws InterruptedException {
        String legacyKey = "legacy-localised-identity-key";
        session.putShipScan(legacyKey, "Pilot Localised|Ship Localised|Faction Localised");

        loadGameSubscriber.onEvent(loadGame());

        awaitTrue(() -> session.getShipScan(legacyKey) == null,
                "LoadGame did not clear the prior session's ship-scan cache");
        assertNull(session.getShipScan(legacyKey));
    }

    private void rememberTargetAnnouncement() {
        assertEquals(1, announcements.size(), "the initial completed Wanted scan should announce");
        targetAnnouncement = announcements.getFirst();
    }

    private void beginBountyWait() {
        bountyNarrated = new CountDownLatch(1);
    }

    private void awaitBountyCompletion() throws InterruptedException {
        // Eviction is asserted separately from the cache key; this latch only prevents the rest of
        // the virtual-thread work from leaking announcements into the next test's recorder.
        assertTrue(bountyNarrated.await(2, TimeUnit.SECONDS),
                "the bounty subscriber's virtual thread did not finish");
    }

    private long targetAnnouncementCount() {
        return announcements.stream().filter(targetAnnouncement::equals).count();
    }

    private static ShipScanIdentity identity(String pilot, String ship, String faction) {
        return ShipScanIdentity.fromRaw(pilot, ship, faction).orElseThrow();
    }

    private static ShipTargetedEvent wantedScan(String pilotName, String localisedPilotName,
                                                 String ship, String localisedShip, String faction) {
        JsonObject json = baseEvent("ShipTargeted");
        json.addProperty("TargetLocked", true);
        if (ship != null) json.addProperty("Ship", ship);
        json.addProperty("Ship_Localised", localisedShip);
        json.addProperty("ScanStage", 3);
        if (pilotName != null) json.addProperty("PilotName", pilotName);
        json.addProperty("PilotName_Localised", localisedPilotName);
        json.addProperty("PilotRank", "Competent");
        json.addProperty("ShieldHealth", 100);
        json.addProperty("HullHealth", 100);
        json.addProperty("LegalStatus", "Wanted");
        json.addProperty("Faction", faction);
        json.addProperty("Bounty", 12_000);
        return new ShipTargetedEvent(json);
    }

    private static BountyEvent bounty(String pilotName, String target, String victimFaction) {
        JsonObject json = baseEvent("Bounty");
        json.addProperty("PilotName", pilotName);
        json.addProperty("Target", target);
        json.addProperty("VictimFaction", victimFaction);
        json.addProperty("TotalReward", 12_000L);
        JsonObject reward = new JsonObject();
        reward.addProperty("Faction", "Federation");
        reward.addProperty("Reward", 12_000L);
        JsonArray rewards = new JsonArray();
        rewards.add(reward);
        json.add("Rewards", rewards);
        return new BountyEvent(json);
    }

    private static LoadGameEvent loadGame() {
        JsonObject json = baseEvent("LoadGame");
        json.addProperty("FID", "F1234567");
        json.addProperty("Commander", "CMDR Scan Reset");
        json.addProperty("Ship", "sidewinder");
        json.addProperty("ShipID", 1);
        json.addProperty("ShipName", "Test Ship");
        json.addProperty("ShipIdent", "TEST-1");
        json.addProperty("gameversion", "4.0.0");
        json.addProperty("build", "test-build");
        return new LoadGameEvent(json);
    }

    private static JsonObject baseEvent(String event) {
        JsonObject json = new JsonObject();
        json.addProperty("timestamp", Instant.now().toString());
        json.addProperty("event", event);
        return json;
    }

    private static void awaitTrue(BooleanSupplier condition, String failureMessage) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline) fail(failureMessage);
            Thread.sleep(10);
        }
    }
}
