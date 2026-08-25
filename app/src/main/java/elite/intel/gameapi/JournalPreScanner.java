package elite.intel.gameapi;

import com.google.common.eventbus.EventBus;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import elite.intel.db.managers.FleetCarrierRouteManager;
import elite.intel.gameapi.journal.EventRegistry;
import elite.intel.gameapi.journal.events.BaseEvent;
import elite.intel.gameapi.journal.subscribers.DockedMarketSubscriber;
import elite.intel.gameapi.journal.subscribers.SilentPersistenceSubscriber;
import elite.intel.session.PlayerSession;
import elite.intel.util.FleetCarrierRouteCalculator;
import elite.intel.util.json.GsonFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Reads the two most recent journal files (previous session + current) before
 * the live JournalParser starts, and silently populates the DB with location
 * and ship data. Runs on its own thread; publishes only to a private EventBus
 * so no live subscribers (TTS, game input, EDSM) are ever triggered.
 *
 * <p>One deliberate exception, after the replay and never during it: a carrier that jumped off its
 * plotted route while the app was down gets that route re-plotted. See
 * {@link #replotCarrierRouteIfLeftBehind}.
 */
public class JournalPreScanner {

    private static final Logger log = LogManager.getLogger(JournalPreScanner.class);
    private static final int JOURNALS_TO_SCAN = 2;

    public static void scan(Path journalDir) {
        log.info("JournalPreScanner: scanning last {} journal(s) in {}", JOURNALS_TO_SCAN, journalDir);

        List<Path> all;
        try {
            all = Files.list(journalDir)
                    .filter(p -> p.getFileName().toString().endsWith(".log"))
                    .sorted(Comparator.comparingLong(p -> p.toFile().lastModified()))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.warn("JournalPreScanner: cannot list {}: {}", journalDir, e.getMessage());
            return;
        }

        if (all.isEmpty()) {
            log.info("JournalPreScanner: no journal files found, skipping");
            return;
        }

        List<Path> toScan = all.subList(Math.max(0, all.size() - JOURNALS_TO_SCAN), all.size());

        EventBus privateBus = new EventBus("pre-scan");
        SilentPersistenceSubscriber persistence = new SilentPersistenceSubscriber();
        privateBus.register(persistence);
        FinancePreScanAccumulator finance = new FinancePreScanAccumulator();
        privateBus.register(finance);
        MaterialsPreScanAccumulator materials = new MaterialsPreScanAccumulator();
        privateBus.register(materials);
        // The pad under the ship is held in memory only, so a commander who quits on one and comes back
        // starts with no pad at all: the live bus never sees that session's Docked, and Location arrives
        // only here. Everything that asks "which market am I standing in" - the construction card's shelves,
        // the carrier ledger's attribution - was silently answered "none" until the next undocking. Replaying
        // the docking events onto the marker costs one field write and leaves it where the journal left it.
        privateBus.register(new DockedMarketSubscriber());

        for (Path file : toScan) {
            processFile(file, privateBus);
        }

        finance.persist();
        materials.persist();
        replotCarrierRouteIfLeftBehind(persistence);

        log.info("JournalPreScanner: done");
    }

    /**
     * The one network call this class makes, and only when the journal proves the carrier jumped off
     * its plotted route while the app was down.
     *
     * <p>WHY it is worth the exception: the stored route then runs from a system the carrier has left,
     * so every leg it reports is unreachable, and nothing repairs that until the next live arrival,
     * which may be hours away or may never come while this route is current. WHY here rather than in
     * the subscriber: the subscriber makes no network calls, and waiting until the replay is finished
     * plots from the carrier's final position instead of once per intermediate hop.
     */
    private static void replotCarrierRouteIfLeftBehind(SilentPersistenceSubscriber persistence) {
        if (!persistence.carrierRouteNeedsReplot()) return;

        FleetCarrierRouteManager route = FleetCarrierRouteManager.getInstance();
        if (route.getFleetCarrierRoute().isEmpty()) return;

        String destination = route.getFinalDestination();
        if (destination == null || destination.isBlank()) return;

        String carrierSystem = PlayerSession.getInstance().getCurrentFleetCarrierSystem();
        if (carrierSystem == null) return;

        log.info("JournalPreScanner: carrier left its route while we were down; re-plotting {} -> {}",
                carrierSystem, destination);
        if (!FleetCarrierRouteCalculator.plotAndStore(carrierSystem, destination)) {
            log.warn("JournalPreScanner: could not re-plot the carrier route from {} to {}; the stored"
                    + " route still starts elsewhere", carrierSystem, destination);
        }
    }

    private static void processFile(Path file, EventBus bus) {
        log.info("JournalPreScanner: processing {}", file.getFileName());
        List<String> lines;
        try {
            lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("JournalPreScanner: cannot read {}: {}", file.getFileName(), e.getMessage());
            return;
        }

        int published = 0;
        for (String raw : lines) {
            String line = raw.startsWith("﻿") ? raw.substring(1) : raw;
            line = line.replaceAll("[\\p{Cntrl}\\p{Cc}\\p{Cf}]", "").trim();
            if (line.isBlank() || !line.startsWith("{") || !line.endsWith("}")) continue;

            try {
                JsonElement element = GsonFactory.getGson().fromJson(line, JsonElement.class);
                if (!element.isJsonObject()) continue;
                JsonObject json = element.getAsJsonObject();
                if (!json.has("event")) continue;

                String eventType = json.get("event").getAsString();
                BaseEvent event = EventRegistry.createEventForPreScan(eventType, json);
                if (event != null) {
                    bus.post(event);
                    published++;
                }
            } catch (Exception e) {
                log.debug("JournalPreScanner: skipping malformed line: {}", e.getMessage());
            }
        }
        log.info("JournalPreScanner: {} events processed from {}", published, file.getFileName());
    }
}
