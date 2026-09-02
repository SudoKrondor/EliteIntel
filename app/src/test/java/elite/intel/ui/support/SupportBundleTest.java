package elite.intel.ui.support;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The bundle is the whole bug report, so what matters is that it survives the state it is collected in:
 * the commander pressing the button is, by definition, in a session where something is wrong.
 */
class SupportBundleTest {

    private static Map<String, String> unzip(Path zip) throws IOException {
        Map<String, String> entries = new HashMap<>();
        try (ZipInputStream in = new ZipInputStream(Files.newInputStream(zip), StandardCharsets.UTF_8)) {
            for (ZipEntry entry = in.getNextEntry(); entry != null; entry = in.getNextEntry()) {
                entries.put(entry.getName(), new String(in.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
        return entries;
    }

    private static Path write(Path dir, String name, String content) throws IOException {
        Path file = dir.resolve(name);
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }

    @Test
    void collectsEverySource(@TempDir Path tmp) throws IOException {
        Path logs = Files.createDirectory(tmp.resolve("logs"));
        Path journalDir = Files.createDirectory(tmp.resolve("journal"));
        Path bindingsDir = Files.createDirectory(tmp.resolve("bindings"));
        Path appLog = write(logs, "elite-intel.log", "app log line");
        write(logs, "elite-intel_2026-08-04_1.log", "previous session");
        write(journalDir, "Journal.2026-08-04T100000.01.log", "{\"event\":\"Fileheader\"}");
        // The game's live state files sit beside the journal, and a complete bundle carries them too.
        for (String state : List.of("Backpack.json", "Cargo.json", "Market.json", "ModulesInfo.json",
                "NavRoute.json", "Outfitting.json", "ShipLocker.json", "Shipyard.json", "Status.json")) {
            write(journalDir, state, "{}");
        }
        write(bindingsDir, "Custom.4.0.binds", "<Root/>");
        Path zip = tmp.resolve("bundle.zip");

        SupportBundle.Result result = SupportBundle.writeTo(zip,
                new SupportBundle.Sources("1.1.0", "system log line", appLog, journalDir, bindingsDir));

        Map<String, String> entries = unzip(zip);
        assertTrue(result.omitted().isEmpty(), () -> "omitted: " + result.omitted());
        assertEquals("system log line", entries.get(SupportBundle.SESSION_LOG_ENTRY));
        assertEquals("app log line", entries.get("elite-intel.log"));
        assertEquals("previous session", entries.get("elite-intel_2026-08-04_1.log"));
        assertEquals("{\"event\":\"Fileheader\"}", entries.get("Journal.2026-08-04T100000.01.log"));
        assertEquals("<Root/>", entries.get("Custom.4.0.binds"));
        assertEquals("{}", entries.get("Status.json"));
    }

    /**
     * The appender rolls on startup, so the session that broke is in the rolled file as soon as the commander
     * restarts - which is the first thing anyone does. Only the newest is worth carrying.
     */
    @Test
    void takesTheNewestRolledApplicationLog(@TempDir Path tmp) throws IOException {
        Path logs = Files.createDirectory(tmp.resolve("logs"));
        Path appLog = write(logs, "elite-intel.log", "current session");
        Path older = write(logs, "elite-intel_2026-08-03_1.log", "two sessions ago");
        Path newer = write(logs, "elite-intel_2026-08-04_2.log", "the session that broke");
        Files.setLastModifiedTime(older, java.nio.file.attribute.FileTime.fromMillis(1_000));
        Files.setLastModifiedTime(newer, java.nio.file.attribute.FileTime.fromMillis(2_000));
        Path zip = tmp.resolve("bundle.zip");

        SupportBundle.writeTo(zip,
                new SupportBundle.Sources("1.1.0", "log", appLog, null, null));

        Map<String, String> entries = unzip(zip);
        assertEquals("current session", entries.get("elite-intel.log"));
        assertEquals("the session that broke", entries.get("elite-intel_2026-08-04_2.log"));
        assertFalse(entries.containsKey("elite-intel_2026-08-03_1.log"));
    }

    /**
     * A first-ever session has nothing rolled yet, and that bundle is complete. Reporting it as missing would
     * make every healthy first bundle read as damaged - and would train the reader to ignore the omission list.
     */
    @Test
    void aMissingRolledLogIsNotReportedAsAnOmission(@TempDir Path tmp) throws IOException {
        Path logs = Files.createDirectory(tmp.resolve("logs"));
        Path appLog = write(logs, "elite-intel.log", "first session");
        Path zip = tmp.resolve("bundle.zip");

        SupportBundle.Result result = SupportBundle.writeTo(zip,
                new SupportBundle.Sources("1.1.0", "system log line", appLog, null, null));

        assertTrue(result.omitted().stream().noneMatch(line -> line.contains("previous application log")),
                () -> "omitted: " + result.omitted());
        // Still said out loud in the bundle, so "none existed" cannot be read as "collection failed".
        assertTrue(unzip(zip).get(SupportBundle.INFO_ENTRY).contains("No previous (rolled) application log"));
    }

    /**
     * The commander is running the game, so the journal directory holds a session's worth of files. Only the
     * one the app is actually tailing is worth sending, and that is the newest.
     */
    @Test
    void takesOnlyTheNewestJournal(@TempDir Path tmp) throws IOException {
        Path journalDir = Files.createDirectory(tmp.resolve("journal"));
        Path older = write(journalDir, "Journal.old.log", "older");
        Path newer = write(journalDir, "Journal.new.log", "newer");
        Files.setLastModifiedTime(older, java.nio.file.attribute.FileTime.fromMillis(1_000));
        Files.setLastModifiedTime(newer, java.nio.file.attribute.FileTime.fromMillis(2_000));
        Path zip = tmp.resolve("bundle.zip");

        SupportBundle.writeTo(zip,
                new SupportBundle.Sources("1.1.0", "log", null, journalDir, null));

        Map<String, String> entries = unzip(zip);
        assertTrue(entries.containsKey("Journal.new.log"));
        assertFalse(entries.containsKey("Journal.old.log"));
    }

    /**
     * The failure being reported can be the reason a directory is unreadable, so a missing source must cost
     * that one entry and nothing else. A bundle missing two sources still diagnoses; an abort does not.
     */
    @Test
    void stillWritesWhatItHasWhenSourcesAreMissing(@TempDir Path tmp) throws IOException {
        Path appLog = write(tmp, "elite-intel.log", "app log line");
        Path zip = tmp.resolve("bundle.zip");

        SupportBundle.Result result = SupportBundle.writeTo(zip,
                new SupportBundle.Sources("1.1.0", "system log line", appLog,
                        tmp.resolve("no-such-journal-dir"), null));

        Map<String, String> entries = unzip(zip);
        assertEquals("app log line", entries.get("elite-intel.log"));
        assertEquals("system log line", entries.get(SupportBundle.SESSION_LOG_ENTRY));
        assertEquals(2, result.included().size(), () -> "included: " + result.included());
        assertEquals(2, result.omitted().size(), () -> "omitted: " + result.omitted());
    }

    /**
     * Whoever opens the zip never saw the message on screen, so the bundle has to say for itself what is
     * absent - otherwise "no journal in here" is indistinguishable from "this commander has no journal".
     * <p>
     * Even with every source missing the file is still written, and still worth sending: it names the app
     * version and the paths that failed. Reporting this as "not saved" would contradict the zip on disk.
     */
    @Test
    void namesEveryOmissionInsideTheBundle(@TempDir Path tmp) throws IOException {
        Path zip = tmp.resolve("bundle.zip");

        SupportBundle.Result result = SupportBundle.writeTo(zip,
                new SupportBundle.Sources("1.1.0", "", null, null, null));

        String info = unzip(zip).get(SupportBundle.INFO_ENTRY);
        assertNotNull(info);
        assertTrue(info.contains("1.1.0"), info);
        assertTrue(result.included().isEmpty(), () -> "included: " + result.included());
        assertEquals(4, result.omitted().size(), () -> "omitted: " + result.omitted());
        for (String omission : result.omitted()) {
            assertTrue(info.contains(omission), () -> "manifest is missing: " + omission);
        }
        assertTrue(Files.exists(zip), "the bundle is written even when nothing could be collected");
    }

    /**
     * A file under the ceiling is carried byte for byte. The limit is a parameter so this can be proved
     * without writing 32 MB to disk in a unit test.
     */
    @Test
    void aFileUnderTheCeilingIsCarriedWhole(@TempDir Path tmp) throws IOException {
        Path file = write(tmp, "small.log", "line one\nline two\n");

        assertEquals("line one\nline two\n",
                new String(SupportBundle.readTail(file, 1024), StandardCharsets.UTF_8));
    }

    /**
     * Over the ceiling the bundle keeps the END of the file: every source here is append-only, so the
     * failure being reported is at the end, and the memory the read costs stays bounded either way.
     */
    @Test
    void anOversizedFileIsCutToItsTailAndSaysSo(@TempDir Path tmp) throws IOException {
        Path file = write(tmp, "huge.log", "oldest\nolder\nmiddle\nnewer\nnewest\n");

        String kept = new String(SupportBundle.readTail(file, 20), StandardCharsets.UTF_8);

        assertTrue(kept.startsWith("### Elite Intel:"), kept);
        assertTrue(kept.contains("huge.log"), kept);
        assertTrue(kept.endsWith("newest\n"), kept);
        assertFalse(kept.contains("oldest"), "the head is what gets dropped");
        // The cut lands mid-record, and half a record read as a whole one is worse than no record.
        assertFalse(kept.contains("\nddle") || kept.contains("ddle\n"),
                "a partial first record must be dropped, not handed over: " + kept);
    }

    /**
     * An unreadable file must be reported as unreadable, not left as a truncated entry in the zip.
     */
    @Test
    void reportsAnUnreadableSourceInsteadOfWritingAPartialEntry(@TempDir Path tmp) throws IOException {
        Path journalDir = Files.createDirectory(tmp.resolve("journal"));
        // A directory named like a journal file: listing finds it, reading it fails.
        Files.createDirectory(journalDir.resolve("Journal.trap.log"));
        Path zip = tmp.resolve("bundle.zip");

        SupportBundle.Result result = SupportBundle.writeTo(zip,
                new SupportBundle.Sources("1.1.0", "system log line", null, journalDir, null));

        assertFalse(unzip(zip).containsKey("Journal.trap.log"));
        assertTrue(result.included().contains(SupportBundle.SESSION_LOG_ENTRY));
        assertTrue(result.omitted().stream().anyMatch(line -> line.startsWith("journal")),
                () -> "omitted: " + result.omitted());
    }

    /**
     * The journal says what HAPPENED; these say what IS. Half the questions a bug report raises are answered
     * by comparing the two - what is in the hold right now against the market on the pad.
     */
    @Test
    void theGamesLiveStateFilesTravelWithTheJournal(@TempDir Path tmp) throws IOException {
        Path journalDir = Files.createDirectory(tmp.resolve("journal-state"));
        Files.writeString(journalDir.resolve("Journal.2026-08-24T05.log"), "{}");
        Files.writeString(journalDir.resolve("Status.json"), "{\"Flags\":16777224}");
        Files.writeString(journalDir.resolve("Cargo.json"), "{\"Count\":640}");
        Files.writeString(journalDir.resolve("Market.json"), "{\"MarketID\":4278665219}");

        Path zip = tmp.resolve("state.zip");
        SupportBundle.Result result = SupportBundle.writeTo(zip,
                new SupportBundle.Sources("1.1.0", "log", null, journalDir, null));

        Map<String, String> entries = unzip(zip);
        assertEquals("{\"Flags\":16777224}", entries.get("Status.json"));
        assertEquals("{\"Count\":640}", entries.get("Cargo.json"));
        assertEquals("{\"MarketID\":4278665219}", entries.get("Market.json"));
        assertTrue(result.included().containsAll(List.of("Status.json", "Cargo.json", "Market.json")));
    }

    /**
     * One line for the lot. A Horizons commander who has never opened a shipyard is missing four of these
     * perfectly normally, and a line each would make a healthy bundle read as a broken one.
     */
    @Test
    void stateFilesTheGameNeverWroteAreReportedOnASingleLine(@TempDir Path tmp) throws IOException {
        Path journalDir = Files.createDirectory(tmp.resolve("journal-sparse"));
        Files.writeString(journalDir.resolve("Journal.2026-08-24T05.log"), "{}");
        Files.writeString(journalDir.resolve("Status.json"), "{}");

        Path zip = tmp.resolve("sparse.zip");
        SupportBundle.Result result = SupportBundle.writeTo(zip,
                new SupportBundle.Sources("1.1.0", "log", null, journalDir, null));

        List<String> aboutState = result.omitted().stream().filter(line -> line.startsWith("game state")).toList();
        assertEquals(1, aboutState.size(), aboutState.toString());
        assertTrue(aboutState.getFirst().contains("Market.json"));
        assertFalse(aboutState.getFirst().contains("Status.json"), "Status.json was there");
    }

    /**
     * The journal entry has already said there is no directory; saying it twice tells no one anything.
     */
    @Test
    void noJournalDirectoryMeansNoSecondComplaintAboutStateFiles(@TempDir Path tmp) throws IOException {
        Path zip = tmp.resolve("nodir.zip");
        SupportBundle.Result result = SupportBundle.writeTo(zip,
                new SupportBundle.Sources("1.1.0", "log", null, null, null));

        assertTrue(result.omitted().stream().noneMatch(line -> line.startsWith("game state")),
                result.omitted().toString());
    }
}
