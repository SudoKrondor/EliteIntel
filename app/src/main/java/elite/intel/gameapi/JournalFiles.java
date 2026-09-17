package elite.intel.gameapi;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Which journal is the newest, decided from the file's NAME rather than its modification time.
 * <p>
 * <b>Why not the modification time.</b> Windows answers a path-based query for the last-write time of a
 * file another process holds open with the directory entry, which is not brought up to date until that
 * process closes the file - so the journal the game is writing right now can report a time OLDER than the
 * journal it closed an hour ago. {@code JournalParser} already documents having tailed a dead journal for a
 * whole session on the strength of that stale answer. Every other reader here made the same choice by the
 * same clock, and each was one flush away from the same mistake: a pre-scan of the wrong two files, a
 * bundle carrying yesterday's journal, a header read from the wrong session.
 * <p>
 * <b>Why the name is safe.</b> The game stamps every journal with the moment it opened the file, in two
 * forms over the years - {@code Journal.2026-09-14T220541.01.log} now, {@code Journal.220914220541.01.log}
 * before the 2022 rename - plus a part counter for a file that rolled. Ordering by that stamp and the part
 * needs nothing from the filesystem, so nothing the filesystem caches can get it wrong. The stamp is local
 * time, which is fine for ordering: two journals are never opened in the same second.
 * <p>
 * A {@code .log} that is not a journal at all sorts below every journal, and among themselves by the
 * modification time these readers used to trust - which keeps a stray file from ever being chosen over a
 * real journal, without pretending to know when it was written.
 */
public final class JournalFiles {

    /**
     * {@code Journal.2026-09-14T220541.01.log} (Odyssey era) or {@code Journal.220914220541.01.log} (before
     * it), with the optional {@code Beta}/{@code Alpha} suffix the test-server builds carry.
     */
    private static final Pattern JOURNAL_NAME = Pattern.compile(
            "^Journal(?:Alpha|Beta)?\\.(?:(\\d{4})-(\\d{2})-(\\d{2})T(\\d{2})(\\d{2})(\\d{2})|(\\d{2})(\\d{2})(\\d{2})(\\d{2})(\\d{2})(\\d{2}))\\.(\\d+)\\.log$");

    private JournalFiles() {
    }

    /**
     * The start stamp the game wrote into the name, as a sortable key, or empty for a file that is not a
     * journal. Two journals compare by this key alone; the part number breaks the tie for a rolled file.
     */
    static Optional<StartStamp> startStamp(Path file) {
        return startStamp(file.getFileName().toString());
    }

    static Optional<StartStamp> startStamp(String fileName) {
        Matcher m = JOURNAL_NAME.matcher(fileName);
        if (!m.matches()) return Optional.empty();
        long stamp;
        if (m.group(1) != null) {
            stamp = Long.parseLong(m.group(1) + m.group(2) + m.group(3) + m.group(4) + m.group(5) + m.group(6));
        } else {
            // Two-digit year: the old form began in 2014 and ended in 2022, so it is always this century.
            stamp = Long.parseLong("20" + m.group(7) + m.group(8) + m.group(9) + m.group(10) + m.group(11) + m.group(12));
        }
        return Optional.of(new StartStamp(stamp, Integer.parseInt(m.group(13))));
    }

    /**
     * {@code yyyyMMddHHmmss} as one number, and the part counter after it.
     */
    record StartStamp(long opened, int part) implements Comparable<StartStamp> {
        @Override
        public int compareTo(StartStamp other) {
            int byOpened = Long.compare(opened, other.opened);
            return byOpened != 0 ? byOpened : Integer.compare(part, other.part);
        }
    }

    /**
     * A file with its sort key worked out once. A real journal always outranks a file whose name says
     * nothing; those fall back on the modification time among themselves.
     * <p>
     * WHY the key is precomputed: the parser re-lists the folder every few seconds for the life of a
     * session, and a long-lived install holds thousands of journals. Matching the name pattern inside the
     * comparator would run it on both operands of every comparison, O(n log n) regex matches per listing,
     * where one match per file is all the ordering needs.
     */
    private record Keyed(Path path, Optional<StartStamp> stamp, long lastModified) implements Comparable<Keyed> {
        static Keyed of(Path path) {
            Optional<StartStamp> stamp = startStamp(path);
            return new Keyed(path, stamp, stamp.isPresent() ? 0 : path.toFile().lastModified());
        }

        @Override
        public int compareTo(Keyed other) {
            if (stamp.isPresent() && other.stamp.isPresent()) return stamp.get().compareTo(other.stamp.get());
            if (stamp.isPresent()) return 1;
            if (other.stamp.isPresent()) return -1;
            return Long.compare(lastModified, other.lastModified);
        }
    }

    /**
     * Every {@code .log} in the folder, oldest first.
     *
     * @throws IOException when the folder cannot be listed - each caller has its own way of saying so
     */
    public static List<Path> listOldestFirst(Path journalDir) throws IOException {
        try (Stream<Path> files = Files.list(journalDir)) {
            return files.filter(p -> p.getFileName().toString().endsWith(".log"))
                    .map(Keyed::of)
                    .sorted()
                    .map(Keyed::path)
                    .toList();
        }
    }

    /**
     * The last {@code count} journals, oldest first - the shape the pre-scans read them in.
     */
    public static List<Path> newest(Path journalDir, int count) throws IOException {
        List<Path> all = listOldestFirst(journalDir);
        return all.subList(Math.max(0, all.size() - count), all.size());
    }

    /**
     * The journal the game is writing, or would be: the newest by name.
     */
    public static Optional<Path> newest(Path journalDir) throws IOException {
        List<Path> all = listOldestFirst(journalDir);
        return all.isEmpty() ? Optional.empty() : Optional.of(all.getLast());
    }
}
