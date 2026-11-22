package elite.intel.gameapi.journalimport;

import com.google.gson.JsonObject;
import elite.intel.db.managers.HuntingGroundManager;
import elite.intel.db.util.Database;
import elite.intel.eventbus.UiBus;
import elite.intel.gameapi.GameLanguage;
import elite.intel.gameapi.HuntingGroundTally;
import elite.intel.i18n.Language;
import elite.intel.ui.event.ExoMasteryChangedEvent;
import elite.intel.util.AppPaths;
import elite.intel.util.json.GsonFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Imports journals the commander kept elsewhere - a backup folder, a zip, a 7z, a tarball - so the app learns from
 * flying it never saw. See {@link JournalLearners} for what is learned and why nothing else is.
 * <p>
 * Three passes, each with its own stretch of the progress bar:
 * <ol>
 *   <li><b>Collecting</b> ({@link JournalCollector}): every journal found, archives unpacked to a staging folder,
 *       duplicates dropped.</li>
 *   <li><b>Checking</b>: each journal's header read, files that are not journals set aside, and every journal
 *       given to its commander ({@link JournalOwners}).</li>
 *   <li><b>Reading</b>: each commander's journals oldest first, into that commander's own file
 *       ({@link Database#inCommanderFile}). Every commander found is imported in the one run, so a commander with
 *       alts never has to come back for them.</li>
 * </ol>
 * <p>
 * Silent by design: nothing is announced, the dialog shows the summary. Safe to run again on the same journals -
 * contracts are keyed on the game's MissionID, resource site counts and discovery flags only ever rise, a sample
 * or a name is filed once - and safe to cancel, which leaves everything filed so far in place.
 * <p>
 * WHY writes go in short transactions: a journal of scans is thousands of writes, which one at a time take minutes
 * and in one transaction would hold the database locked against the live parser for longer than its five-second
 * busy timeout. A transaction is closed every {@link #TRANSACTION_MILLIS} ms, which is fast and keeps the live app's
 * writes waiting no longer than that.
 */
public final class JournalImporter {

    private static final Logger log = LogManager.getLogger(JournalImporter.class);

    private static final long TRANSACTION_MILLIS = 200;
    private static final String STAGING_FOLDER = "journal-import";
    private static final Pattern CONTROL_CHARACTERS = Pattern.compile("[\\p{Cntrl}\\p{Cc}\\p{Cf}]");

    private static final AtomicBoolean RUNNING = new AtomicBoolean();

    public enum Phase {COLLECTING, CHECKING, READING}

    /**
     * Told how far the import has got. Called from the import's thread, often; the listener throttles.
     */
    @FunctionalInterface
    public interface Listener {
        void progress(Phase phase, long done, long total);
    }

    private final Listener listener;
    private final BooleanSupplier cancelled;
    private final Path stagingDir;

    private JournalImporter(Listener listener, BooleanSupplier cancelled, Path stagingDir) {
        this.listener = listener;
        this.cancelled = cancelled;
        this.stagingDir = stagingDir;
    }

    /**
     * Imports every journal in {@code sources} - folders, archives, or single journals. Blocks until done, so call
     * it off the event dispatch thread.
     *
     * @throws ImportCancelledException      when {@code cancelled} turned true; what was filed before stays
     * @throws ImportAlreadyRunningException when an import is already running
     * @throws IOException                   when the staging folder cannot be written - a full disk, typically
     */
    public static ImportReport run(List<Path> sources, Listener listener, BooleanSupplier cancelled) throws IOException {
        return run(sources, listener, cancelled, AppPaths.getAppDirectory().resolve(STAGING_FOLDER));
    }

    /**
     * Seam for tests, which stage under a folder of their own.
     */
    static ImportReport run(List<Path> sources, Listener listener, BooleanSupplier cancelled, Path stagingDir)
            throws IOException {
        if (!RUNNING.compareAndSet(false, true)) throw new ImportAlreadyRunningException();
        try {
            // Left behind by an import the app was closed in the middle of, if anything.
            deleteRecursively(stagingDir);
            Files.createDirectories(stagingDir);
            return new JournalImporter(listener, cancelled, stagingDir).importAll(sources);
        } finally {
            try {
                deleteRecursively(stagingDir);
            } finally {
                RUNNING.set(false);
            }
        }
    }

    private ImportReport importAll(List<Path> sources) throws IOException {
        JournalCollector collector = new JournalCollector(stagingDir,
                (done, total) -> listener.progress(Phase.COLLECTING, done, total), cancelled);
        List<FoundJournal> found = collector.collect(sources);
        CollectStats stats = collector.stats();
        log.info("Journal import: {} journal(s) found in {} source(s)", found.size(), sources.size());

        List<FoundJournal> journals = new ArrayList<>();
        List<JournalHeaderInfo> headers = new ArrayList<>();
        int checked = 0;
        for (FoundJournal journal : found) {
            checkCancelled();
            Optional<JournalHeaderInfo> header = header(journal);
            if (header.isPresent()) {
                journals.add(journal);
                headers.add(header.get());
            } else {
                stats.notJournals++;
            }
            listener.progress(Phase.CHECKING, ++checked, found.size());
        }

        List<Optional<String>> owners = JournalOwners.resolve(journals, headers);
        Map<String, List<Integer>> byCommander = new LinkedHashMap<>();
        List<Integer> unowned = new ArrayList<>();
        Map<String, String> names = new HashMap<>();
        // Newest journal first, so the commander flying most recently is first in line - see inCommanderFile on the
        // pending file - and then oldest first within each commander.
        for (int i = journals.size() - 1; i >= 0; i--) {
            Optional<String> owner = owners.get(i);
            if (owner.isPresent()) {
                byCommander.computeIfAbsent(owner.get(), fid -> new ArrayList<>()).addFirst(i);
                if (headers.get(i).commanderName() != null)
                    names.putIfAbsent(owner.get(), headers.get(i).commanderName());
            } else {
                unowned.addFirst(i);
            }
        }

        Reading reading = new Reading(journals, headers);
        HuntingGroundManager huntingGrounds = HuntingGroundManager.getInstance();
        int groundsBefore = huntingGrounds.knowledge().huntingGrounds();
        int contractsLearned = 0;
        List<String> commanders = new ArrayList<>();
        for (Map.Entry<String, List<Integer>> commander : byCommander.entrySet()) {
            String fid = commander.getKey();
            if (!Database.isUsableFid(fid)) {
                log.warn("Journal import: commander id {} cannot name a file; their journals teach shared knowledge only", fid);
                unowned.addAll(commander.getValue());
                continue;
            }
            contractsLearned += Database.inCommanderFile(fid, () -> {
                int before = huntingGrounds.knowledge().contracts();
                reading.read(commander.getValue(), true);
                return huntingGrounds.knowledge().contracts() - before;
            });
            commanders.add(names.getOrDefault(fid, fid));
        }
        if (!unowned.isEmpty()) {
            unowned.sort(Comparator.naturalOrder());
            reading.read(unowned, false);
        }
        int groundsLearned = huntingGrounds.knowledge().huntingGrounds() - groundsBefore;
        if (reading.bioSamples > 0) UiBus.publish(new ExoMasteryChangedEvent());

        ImportReport report = new ImportReport(journals.size(), List.copyOf(commanders), unowned.size(),
                stats.notJournals, stats.damaged + reading.damaged, stats.unreadable, stats.unsupported,
                stats.duplicates, groundsLearned, contractsLearned, reading.bioSamples, reading.bodiesScanned);
        log.info("Journal import done: {}", report);
        return report;
    }

    private Optional<JournalHeaderInfo> header(FoundJournal journal) {
        try (BoundedLineReader reader = new BoundedLineReader(journal.open())) {
            return JournalHeaderInfo.read(reader);
        } catch (IOException | RuntimeException e) {
            log.warn("Journal import: cannot read the header of {}: {}", journal.origin(), e.toString());
            return Optional.empty();
        }
    }

    /**
     * The reading pass: one commander's journals at a time, into whatever file is open on this thread.
     */
    private final class Reading {
        private final List<FoundJournal> journals;
        private final List<JournalHeaderInfo> headers;
        private final long totalBytes;
        private long doneBytes;
        int damaged;
        int bioSamples;
        int bodiesScanned;

        Reading(List<FoundJournal> journals, List<JournalHeaderInfo> headers) {
            this.journals = journals;
            this.headers = headers;
            this.totalBytes = journals.stream().mapToLong(FoundJournal::size).sum();
        }

        void read(List<Integer> indexes, boolean commanderKnown) {
            JournalLearners learners = new JournalLearners(commanderKnown);
            for (int index : indexes) {
                FoundJournal journal = journals.get(index);
                Optional<Language> language = GameLanguage.languageOf(headers.get(index).language());
                readJournal(journal, learners, language);
                doneBytes += journal.size();
                listener.progress(Phase.READING, doneBytes, totalBytes);
            }
            Database.inScopedTransaction(learners::finish);
            bioSamples += learners.bioSamples();
            bodiesScanned += learners.bodiesScanned();
        }

        private void readJournal(FoundJournal journal, JournalLearners learners, Optional<Language> language) {
            try (BoundedLineReader reader = new BoundedLineReader(journal.open())) {
                boolean[] more = {true};
                while (more[0]) {
                    checkCancelled();
                    Database.inScopedTransaction(() -> more[0] = readFor(reader, learners, language, journal));
                    listener.progress(Phase.READING, doneBytes + Math.min(reader.bytesRead(), journal.size()), totalBytes);
                }
            } catch (IOException e) {
                log.warn("Journal import: cannot read {}: {}", journal.origin(), e.toString());
                damaged++;
            }
        }

        /**
         * Reads lines for one transaction's worth of time.
         * <p>
         * A journal that breaks off ends here, returning normally, so the lines read before the break are committed
         * with the rest of the transaction rather than rolled back.
         *
         * @return whether the journal has more to read
         */
        private boolean readFor(BoundedLineReader reader, JournalLearners learners, Optional<Language> language,
                                FoundJournal journal) {
            long until = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(TRANSACTION_MILLIS);
            while (System.nanoTime() < until) {
                String line;
                try {
                    line = reader.readLine();
                } catch (IOException | RuntimeException e) {
                    log.warn("Journal import: {} broke off: {}", journal.origin(), e.toString());
                    damaged++;
                    return false;
                }
                if (line == null) return false;
                learn(line, learners, language);
            }
            return true;
        }

        /**
         * One line. A line that is not JSON, or that a learner cannot make sense of, is skipped: an edited or
         * truncated journal must cost that line and no more.
         */
        private void learn(String line, JournalLearners learners, Optional<Language> language) {
            String eventName = HuntingGroundTally.eventNameOf(line);
            if (eventName == null || !learners.wants(eventName)) return;
            String sanitized = CONTROL_CHARACTERS.matcher(line).replaceAll("").trim();
            if (!sanitized.startsWith("{") || !sanitized.endsWith("}")) return;
            try {
                JsonObject json = GsonFactory.getGson().fromJson(sanitized, JsonObject.class);
                learners.accept(eventName, json, language);
            } catch (RuntimeException e) {
                log.debug("Journal import: skipping a {} line: {}", eventName, e.toString());
            }
        }
    }

    private void checkCancelled() {
        if (cancelled.getAsBoolean()) throw new ImportCancelledException();
    }

    private static void deleteRecursively(Path dir) throws IOException {
        if (!Files.exists(dir)) return;
        try (Stream<Path> walk = Files.walk(dir)) {
            for (Path path : walk.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }
}
