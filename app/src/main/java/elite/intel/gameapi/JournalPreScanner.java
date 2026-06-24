package elite.intel.gameapi;

import com.google.common.eventbus.EventBus;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import elite.intel.gameapi.journal.EventRegistry;
import elite.intel.gameapi.journal.events.BaseEvent;
import elite.intel.gameapi.journal.subscribers.SilentPersistenceSubscriber;
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
        privateBus.register(new SilentPersistenceSubscriber());
        FinancePreScanAccumulator finance = new FinancePreScanAccumulator();
        privateBus.register(finance);

        for (Path file : toScan) {
            processFile(file, privateBus);
        }

        finance.persist();

        log.info("JournalPreScanner: done");
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
