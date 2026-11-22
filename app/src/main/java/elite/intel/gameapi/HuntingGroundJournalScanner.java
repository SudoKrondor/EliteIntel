package elite.intel.gameapi;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import elite.intel.db.managers.HuntingGroundManager;
import elite.intel.db.util.Database;
import elite.intel.util.json.GsonFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Reads the commander's journal archive and learns every hunting ground and every pirate massacre
 * contract in it.
 * <p>
 * WHY this is not part of {@link JournalPreScanner}: that reads the last two journals at every start
 * to restore session state, and it publishes real events onto a bus of real subscribers. This reads
 * years of flying, once, on demand, and writes nothing but two tables. A commander with a long
 * archive should not pay for it on every launch, which is why it is a command rather than a startup
 * step.
 * <p>
 * Re-running it is safe and cheap. Contracts are keyed on the game's own {@code MissionID} so the
 * same one lands on the same row however many times it is read, resource site counts only ever rise,
 * and the scan remembers which journal it last finished so a second run reads only what is new.
 * <p>
 * It reads only the open commander's journals. Contracts land in the commander's own file, and a folder
 * shared by several commanders would otherwise file one commander's contracts under another. The
 * bookmark lives in the commander's file too, so each commander's first run reads their whole archive.
 */
public class HuntingGroundJournalScanner {

    private static final Logger log = LogManager.getLogger(HuntingGroundJournalScanner.class);

    private final HuntingGroundManager huntingGrounds;
    private final Supplier<Optional<String>> commander;

    public HuntingGroundJournalScanner() {
        this(HuntingGroundManager.getInstance(), HuntingGroundJournalScanner::openCommander);
    }

    /**
     * Seam for tests.
     *
     * @param commander the FID whose journals to read, or empty to read every journal (no journal has
     *                  named a commander yet, so there is no one to filter by)
     */
    public HuntingGroundJournalScanner(HuntingGroundManager huntingGrounds, Supplier<Optional<String>> commander) {
        this.huntingGrounds = huntingGrounds;
        this.commander = commander;
    }

    private static Optional<String> openCommander() {
        return Database.isCommanderKnown() ? Optional.of(Database.currentCommander()) : Optional.empty();
    }

    /**
     * Reads every journal not already read and records what it finds.
     */
    public Result scan(Path journalDir) {
        HuntingGroundManager.Knowledge before = huntingGrounds.knowledge();

        List<Path> journals = journalsToRead(journalDir);
        if (journals.isEmpty()) {
            return new Result(0, before, before);
        }

        HuntingGroundTally tally = new HuntingGroundTally(huntingGrounds, true);
        for (Path journal : journals) {
            readJournal(journal, tally);
        }
        tally.persistResourceSites();
        rememberProgress(journals.getLast());

        HuntingGroundManager.Knowledge after = huntingGrounds.knowledge();
        log.info("Hunting ground scan: {} journal(s) read, now know {} ground(s) and {} contract(s)",
                journals.size(), after.huntingGrounds(), after.contracts());
        return new Result(journals.size(), before, after);
    }

    // ---------------------------------------------------------------- which files

    /**
     * The open commander's journals still to read, oldest first.
     * <p>
     * The bookmarked file is read again rather than skipped: it may have been the one the game was
     * still writing when the scan ran, and re-reading it costs one file and cannot double anything.
     */
    private List<Path> journalsToRead(Path journalDir) {
        List<Path> all;
        try {
            all = JournalFiles.listOldestFirst(journalDir);
        } catch (IOException e) {
            log.warn("Hunting ground scan: cannot list {}: {}", journalDir, e.getMessage());
            return List.of();
        }

        int from = bookmarkIndex(all);
        return commander.get()
                .map(fid -> JournalCommander.ownedBy(all, from, fid))
                .orElseGet(() -> all.subList(from, all.size()));
    }

    /**
     * Where the bookmarked journal sits in {@code all}, or 0 to read from the start.
     */
    private int bookmarkIndex(List<Path> all) {
        String bookmark = huntingGrounds.lastScannedJournal();
        if (bookmark == null || bookmark.isBlank()) return 0;

        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getFileName().toString().equals(bookmark)) return i;
        }
        // WHY: the bookmarked file is gone - archived or deleted - so there is no telling what was
        // read. Reading everything again is the only honest answer, and it costs nothing but time.
        return 0;
    }

    private void rememberProgress(Path lastRead) {
        huntingGrounds.rememberScannedJournal(lastRead.getFileName().toString(), Instant.now().toString());
    }

    // ---------------------------------------------------------------- reading

    private void readJournal(Path journal, HuntingGroundTally tally) {
        try (BufferedReader reader = Files.newBufferedReader(journal, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String eventName = HuntingGroundTally.eventNameOf(line);
                if (eventName == null || !tally.wants(eventName)) continue;
                JsonObject json = parse(line);
                if (json != null) tally.accept(eventName, json);
            }
        } catch (IOException e) {
            log.warn("Hunting ground scan: cannot read {}: {}", journal.getFileName(), e.getMessage());
        }
    }

    private JsonObject parse(String line) {
        String sanitized = line.replaceAll("[\\p{Cntrl}\\p{Cc}\\p{Cf}]", "").trim();
        if (!sanitized.startsWith("{") || !sanitized.endsWith("}")) return null;
        try {
            return GsonFactory.getGson().fromJson(sanitized, JsonObject.class);
        } catch (JsonParseException e) {
            log.debug("Hunting ground scan: skipping malformed line: {}", e.getMessage());
            return null;
        }
    }

    /**
     * What one run of the scan did.
     *
     * @param before what was known when it started, so the caller can say what was new
     */
    public record Result(int journalsRead,
                         HuntingGroundManager.Knowledge before,
                         HuntingGroundManager.Knowledge after) {

        public int huntingGroundsLearned() {
            return after.huntingGrounds() - before.huntingGrounds();
        }

        public int contractsLearned() {
            return after.contracts() - before.contracts();
        }
    }
}
