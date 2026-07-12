package elite.intel.diagnostics;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import elite.intel.eventbus.GameEventBus;
import elite.intel.eventbus.UiBus;
import elite.intel.gameapi.UserInputEvent;
import elite.intel.gameapi.journal.EventRegistry;
import elite.intel.gameapi.journal.events.BaseEvent;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;
import elite.intel.ui.controller.ManagedService;
import elite.intel.ui.event.LanguageChangedEvent;
import elite.intel.util.AppPaths;
import elite.intel.util.json.GsonFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Watches the diagnostics phrase input file and feeds each newly appended line into the companion the same
 * way the microphone does, at conversation speed. Two line kinds:
 * <ul>
 *   <li>a plain-text line becomes a {@link UserInputEvent} - a spoken phrase;</li>
 *   <li>a JSON line carrying an {@code "event"} field is turned into a game event via {@link EventRegistry}
 *       and published exactly as {@code JournalParser} does, so a tester can set the game context (e.g. a
 *       {@code Status} flags event) that a command needs to be visible to the router.</li>
 * </ul>
 * Phrases are fed one at a time: {@link DiagnosticsPacer} blocks between them until the previous turn's LLM
 * and TTS have settled. Context events apply near-instantly, so they only get a short fixed delay. Runs on a
 * single daemon thread and reads only lines appended after startup (the file is truncated at boot).
 */
public final class DiagnosticsInputTailer implements ManagedService {

    private static final long POLL_MS = 200;
    private static final long EVENT_APPLY_MS = 300; // let a context event apply before the next line
    private static final long LANG_APPLY_MS = 8_000; // language switch restarts TTS (~5s delayed) - let it settle
    /** UTF-8 BOM PowerShell prepends to the first written line; stripped so a leading directive still parses. */
    private static final String BOM = Character.toString(0xFEFF);

    private volatile boolean running;
    private Thread worker;
    private int consumed; // number of input lines already processed

    @Override
    public void start() {
        running = true;
        worker = new Thread(this::run, "diagnostics-input-tailer");
        worker.setDaemon(true);
        worker.start();
    }

    @Override
    public void stop() {
        running = false;
        if (worker != null) {
            worker.interrupt();
            worker = null;
        }
    }

    private void run() {
        DiagnosticsLog.write("DIAG tailer watching input");
        while (running) {
            try {
                for (String line : readNewLines()) {
                    String trimmed = line.replace(BOM, "").trim(); // drop a PowerShell BOM so a leading @directive parses
                    if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                        continue; // blank line or a '#' comment
                    }
                    if (trimmed.startsWith("@")) {
                        Thread.sleep(applyDirective(trimmed));
                    } else if (isEventJson(trimmed)) {
                        publishEvent(trimmed);
                        Thread.sleep(EVENT_APPLY_MS);
                    } else {
                        feedPhrase(trimmed);
                        DiagnosticsPacer.getInstance().awaitTurnSettled();
                        // Terminal marker for this phrase's turn: fired once the turn has started and gone quiet,
                        // i.e. after all of its DIAG dispatch line(s) and the DIAG speaking=false that ends its
                        // (now audible) TTS reply. A reliable single anchor the skill keys turn-completion off.
                        DiagnosticsLog.write("DIAG turn-done");
                    }
                }
                Thread.sleep(POLL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                DiagnosticsLog.write("DIAG tailer error: " + e.getMessage());
                sleepQuietly();
            }
        }
    }

    /** Returns lines appended since the last poll; resets tracking if the file was truncated/shrank. */
    private List<String> readNewLines() throws IOException {
        Path file = AppPaths.getDiagnosticsInputFile();
        if (!Files.exists(file)) {
            return List.of();
        }
        List<String> all = Files.readAllLines(file, StandardCharsets.UTF_8);
        if (all.size() < consumed) {
            consumed = 0; // file was rewritten/truncated externally; re-read from the start
        }
        if (all.size() <= consumed) {
            return List.of();
        }
        List<String> fresh = new ArrayList<>(all.subList(consumed, all.size()));
        consumed = all.size();
        return fresh;
    }

    /**
     * Applies an {@code @}-directive and returns how long to settle afterwards before the next line:
     * {@code @lang <CODE>} switches the app's command language via the app's own mechanism (runtime tweak);
     * {@code @visible <actionId>} sets the game context to the first probe state where that action is visible
     * (the deterministic, test-style way to set "where we are" — preferred over guessing a context);
     * {@code @status <context>} sets a named context manually; {@code @fighter on|off} toggles the fighter-out
     * gate. Directives take effect immediately and produce no companion turn.
     */
    private long applyDirective(String line) {
        String body = line.substring(1).trim();
        String[] parts = body.split("\\s+", 2);
        String cmd = parts[0].toLowerCase(Locale.ROOT);
        String arg = parts.length > 1 ? parts[1].trim() : "";
        switch (cmd) {
            case "lang" -> {
                try {
                    Language lang = Language.valueOf(arg.toUpperCase(Locale.ROOT));
                    SystemSession.getInstance().setLanguage(lang);
                    UiBus.publish(new LanguageChangedEvent()); // same path as the settings panel: restarts STT/TTS
                    DiagnosticsLog.write("DIAG lang=" + lang);
                    return LANG_APPLY_MS;
                } catch (IllegalArgumentException e) {
                    DiagnosticsLog.write("DIAG lang unknown=" + arg);
                }
            }
            case "visible" -> {
                // Deterministic context, mirroring the routing test: put the game in the first probe state
                // where the EXPECTED action is visible to the router, so the tester never guesses a context.
                String state = DiagnosticsContext.applyVisibleFor(arg);
                DiagnosticsLog.write("DIAG visible=" + arg + " state=" + state);
            }
            case "status" -> {
                if (DiagnosticsContext.apply(arg.toLowerCase(Locale.ROOT))) {
                    DiagnosticsLog.write("DIAG status=" + arg);
                } else {
                    DiagnosticsLog.write("DIAG status unknown=" + arg);
                }
            }
            case "fighter" -> {
                boolean out = arg.equalsIgnoreCase("on") || arg.equalsIgnoreCase("true");
                DiagnosticsContext.setFighterOut(out);
                DiagnosticsLog.write("DIAG fighter=" + out);
            }
            default -> DiagnosticsLog.write("DIAG directive unknown=" + cmd);
        }
        return EVENT_APPLY_MS;
    }

    private void feedPhrase(String phrase) {
        DiagnosticsPacer.getInstance().markInjected();
        DiagnosticsLog.write("DIAG input=\"" + phrase + "\"");
        GameEventBus.publish(new UserInputEvent(phrase));
    }

    private boolean isEventJson(String line) {
        return line.startsWith("{") && line.endsWith("}") && line.contains("\"event\"");
    }

    /** Deserializes and publishes a journal-style game event, mirroring {@code JournalParser}'s dispatch. */
    private void publishEvent(String line) {
        try {
            JsonElement element = GsonFactory.getGson().fromJson(line, JsonElement.class);
            if (element == null || !element.isJsonObject()) {
                return;
            }
            JsonObject json = element.getAsJsonObject();
            if (!json.has("event")) {
                return;
            }
            // Stamp a fresh timestamp when the tester omitted one, so the event is not treated as a replay.
            if (!json.has("timestamp")) {
                json.addProperty("timestamp", Instant.now().toString());
            }
            String type = json.get("event").getAsString();
            BaseEvent event = EventRegistry.createEvent(type, json);
            if (event != null && !event.isReplay() && !event.isExpired()) {
                DiagnosticsLog.write("DIAG event=" + type);
                GameEventBus.publish(event);
            } else {
                DiagnosticsLog.write("DIAG event skipped=" + type);
            }
        } catch (Exception e) {
            DiagnosticsLog.write("DIAG event parse error: " + e.getMessage());
        }
    }

    private void sleepQuietly() {
        try {
            Thread.sleep(POLL_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
