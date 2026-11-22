package elite.intel.gameapi.journalimport;

import elite.intel.gameapi.JournalFiles;
import elite.intel.gameapi.JournalFiles.StartStamp;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Finds every journal in what the commander picked - folders, archives, archives in folders, archives in archives -
 * and makes each one readable in date order.
 * <p>
 * <b>Nothing in an archive is trusted.</b> No path from an archive is ever used: an entry is kept only when its
 * file name matches the journal name pattern, and it is copied out under a name made here from that pattern match,
 * so an entry called {@code ../../anything} has nowhere to go. Every copy stops at {@link #MAX_ENTRY_BYTES}, so an
 * archive that unpacks to far more than it holds cannot fill the disk. Archives inside archives are opened
 * {@link #MAX_NESTING} levels deep and no further.
 * <p>
 * <b>Why archive entries are copied out at all.</b> The import must read each commander's journals oldest first -
 * a contract's provider is wherever the commander last arrived - and archives are not stored in date order. A zip
 * can be read in any order, but a tar can only be read front to back, once. So every journal in an archive is
 * copied out first, gzip-compressed (journals shrink tenfold or more, so even a huge backup costs little disk), and
 * read in order afterwards. Journals in a plain folder are read where they are.
 * <p>
 * A broken archive or entry is counted and passed over; whatever came out of it before the break is kept.
 */
final class JournalCollector {

    private static final Logger log = LogManager.getLogger(JournalCollector.class);

    /**
     * The most one archive entry may unpack to. A real journal is a few megabytes, a long session's a few tens.
     */
    static final long MAX_ENTRY_BYTES = 1024L * 1024 * 1024;

    /**
     * How deep archives inside archives are opened: a zip of the year's tarballs, but not a zip of zips of zips.
     */
    static final int MAX_NESTING = 2;

    /**
     * How deep into a picked folder to look - a backup sorted by year and month is three levels.
     */
    private static final int MAX_FOLDER_DEPTH = 16;

    private static final Set<String> ARCHIVE_EXTENSIONS = Set.of(
            ".zip", ".7z", ".tar", ".tgz", ".gz", ".bz2", ".tbz", ".tbz2", ".xz", ".txz");

    private final Path stagingDir;
    private final ProgressSink progress;
    private final BooleanSupplier cancelled;
    private final CollectStats stats = new CollectStats();
    private final Map<String, FoundJournal> byName = new HashMap<>();

    private int staged;
    private long totalBytes;
    private long doneBytes;

    /**
     * Told how far the collection has got, in bytes of the picked files.
     */
    @FunctionalInterface
    interface ProgressSink {
        void collected(long done, long total);
    }

    /**
     * @param stagingDir an empty folder this collector may fill; the caller deletes it
     */
    JournalCollector(Path stagingDir, ProgressSink progress, BooleanSupplier cancelled) {
        this.stagingDir = stagingDir;
        this.progress = progress;
        this.cancelled = cancelled;
    }

    CollectStats stats() {
        return stats;
    }

    /**
     * Every distinct journal in {@code sources}, oldest first.
     *
     * @throws ImportCancelledException when {@code cancelled} turned true
     * @throws IOException              when the staging folder cannot be written - a full disk, typically
     */
    List<FoundJournal> collect(List<Path> sources) throws IOException {
        List<Path> files = new ArrayList<>();
        for (Path source : sources) {
            files.addAll(filesIn(source));
        }
        for (Path file : files) {
            totalBytes += sizeOf(file);
        }
        for (Path file : files) {
            checkCancelled();
            long size = sizeOf(file);
            long before = doneBytes;
            considerFile(file, size);
            doneBytes = before + size;
            progress.collected(doneBytes, totalBytes);
        }
        return byName.values().stream()
                .sorted(Comparator.comparing(FoundJournal::stamp))
                .toList();
    }

    // ---------------------------------------------------------------- folders and files

    private List<Path> filesIn(Path source) {
        if (Files.isRegularFile(source)) return List.of(source);
        if (!Files.isDirectory(source)) {
            log.warn("Journal import: {} is neither a file nor a folder", source);
            stats.unreadable++;
            return List.of();
        }
        try (Stream<Path> walk = Files.walk(source, MAX_FOLDER_DEPTH)) {
            return walk.filter(Files::isRegularFile).sorted().toList();
        } catch (IOException | UncheckedIOException e) {
            log.warn("Journal import: cannot list {}: {}", source, e.getMessage());
            stats.unreadable++;
            return List.of();
        }
    }

    private void considerFile(Path file, long size) throws IOException {
        String name = file.getFileName().toString();
        Optional<StartStamp> stamp = JournalFiles.startStamp(name);
        if (stamp.isPresent()) {
            keep(new FoundJournal(name, stamp.get(), size, () -> Files.newInputStream(file), file.toString()));
            return;
        }
        ArchiveFormat format;
        try {
            format = ArchiveFormat.of(file);
        } catch (IOException e) {
            log.warn("Journal import: cannot read {}: {}", file, e.getMessage());
            stats.unreadable++;
            return;
        }
        if (format == ArchiveFormat.NONE) {
            stats.notJournals++;
            return;
        }
        expand(file, name, format, 0, size);
    }

    // ---------------------------------------------------------------- archives

    /**
     * Pulls every journal out of one archive.
     *
     * @param size the archive's share of the progress bar, 0 for one inside another archive
     */
    private void expand(Path archive, String name, ArchiveFormat format, int depth, long size) throws IOException {
        long base = doneBytes;
        try {
            switch (format) {
                case ZIP -> expandZip(archive, depth, base, size);
                case SEVEN_Z -> expandSevenZ(archive, depth, base, size);
                case TAR, GZIP, BZIP2, XZ -> expandStream(archive, name, format, depth, base, size);
                case RAR -> {
                    log.info("Journal import: {} is a RAR archive, which cannot be read", archive);
                    stats.unsupported++;
                }
                case NONE -> stats.notJournals++;
            }
        } catch (StagingException | ImportCancelledException e) {
            throw e;
        } catch (IOException | RuntimeException e) {
            // RuntimeException too: a decoder handed a corrupt block throws whatever it throws.
            log.warn("Journal import: {} is damaged: {}", archive, e.toString());
            stats.damaged++;
        }
    }

    private void expandZip(Path archive, int depth, long base, long size) throws IOException {
        try (ZipFile zip = ZipFile.builder().setPath(archive).get()) {
            List<ZipArchiveEntry> entries = Collections.list(zip.getEntries());
            long done = 0;
            for (ZipArchiveEntry entry : entries) {
                checkCancelled();
                if (!entry.isDirectory()) {
                    if (!zip.canReadEntryData(entry)) {
                        // Password protected, or packed with a method there is no decoder for.
                        if (JournalFiles.startStamp(baseName(entry.getName())).isPresent()) stats.unreadable++;
                        else stats.notJournals++;
                    } else {
                        entry(baseName(entry.getName()), () -> zip.getInputStream(entry), archive, depth);
                    }
                }
                done += Math.max(0, entry.getCompressedSize());
                report(base, Math.min(done, size));
            }
        }
    }

    /**
     * Read front to back: a solid 7z block can only be unpacked in order, and asking for entries out of order
     * would unpack the block again for each.
     */
    private void expandSevenZ(Path archive, int depth, long base, long size) throws IOException {
        try (SevenZFile sevenZ = SevenZFile.builder().setPath(archive).get()) {
            int total = 0;
            for (SevenZArchiveEntry ignored : sevenZ.getEntries()) total++;
            int seen = 0;
            SevenZArchiveEntry entry;
            while ((entry = sevenZ.getNextEntry()) != null) {
                checkCancelled();
                seen++;
                if (!entry.isDirectory() && entry.hasStream()) {
                    entry(baseName(entry.getName()), () -> new SevenZEntryStream(sevenZ), archive, depth);
                }
                report(base, total == 0 ? size : size * seen / total);
            }
        }
    }

    /**
     * A tar, plain or compressed, or a single compressed file - a journal gzipped on its own, say.
     */
    private void expandStream(Path archive, String name, ArchiveFormat format, int depth, long base, long size)
            throws IOException {
        try (CountingInputStream raw = new CountingInputStream(new BufferedInputStream(Files.newInputStream(archive)))) {
            Runnable tick = () -> report(base, Math.min(raw.count(), size));
            InputStream unpacked = new BufferedInputStream(switch (format) {
                case GZIP -> new GzipCompressorInputStream(raw, true);
                case BZIP2 -> new BZip2CompressorInputStream(raw, true);
                case XZ -> new XZCompressorInputStream(raw, true);
                default -> raw;
            }, 64 * 1024);

            unpacked.mark(ArchiveFormat.SNIFF_BYTES);
            byte[] head = unpacked.readNBytes(ArchiveFormat.SNIFF_BYTES);
            unpacked.reset();
            ArchiveFormat inner = format.isCompression() ? ArchiveFormat.sniff(head, head.length) : ArchiveFormat.TAR;

            if (inner == ArchiveFormat.TAR) {
                TarArchiveInputStream tar = new TarArchiveInputStream(unpacked);
                ArchiveEntry entry;
                while ((entry = tar.getNextEntry()) != null) {
                    checkCancelled();
                    if (!entry.isDirectory())
                        entry(baseName(entry.getName()), () -> new NonClosing(tar), archive, depth);
                    tick.run();
                }
            } else {
                // One compressed file: it is named by the archive, less the compression's extension.
                entry(withoutCompressionExtension(name), () -> new NonClosing(unpacked), archive, depth);
                tick.run();
            }
        }
    }

    /**
     * One file inside an archive: a journal is copied out, an archive is opened in turn, anything else is
     * counted and passed over.
     */
    private void entry(String name, FoundJournal.Source content, Path archive, int depth) throws IOException {
        Optional<StartStamp> stamp = JournalFiles.startStamp(name);
        if (stamp.isPresent()) {
            stageJournal(name, stamp.get(), content, archive);
        } else if (depth < MAX_NESTING && looksLikeArchive(name)) {
            expandNested(name, content, archive, depth);
        } else {
            stats.notJournals++;
        }
    }

    private void stageJournal(String name, StartStamp stamp, FoundJournal.Source content, Path archive)
            throws IOException {
        Path copy = stagingDir.resolve((++staged) + ".log.gz");
        Copied copied = copyOut(content, copy, true);
        if (copied.bytes() == 0) {
            Files.deleteIfExists(copy);
            if (copied.broken()) stats.damaged++;
            return;
        }
        if (copied.broken()) stats.damaged++;
        keep(new FoundJournal(name, stamp, copied.bytes(),
                () -> new GZIPInputStream(new BufferedInputStream(Files.newInputStream(copy))),
                archive.getFileName() + "!" + name));
    }

    private void expandNested(String name, FoundJournal.Source content, Path archive, int depth) throws IOException {
        Path copy = stagingDir.resolve((++staged) + ".nested");
        try {
            Copied copied = copyOut(content, copy, false);
            if (copied.broken()) {
                stats.damaged++;
                return;
            }
            ArchiveFormat format = ArchiveFormat.of(copy);
            if (format == ArchiveFormat.NONE) {
                stats.notJournals++;
                return;
            }
            long before = doneBytes;
            expand(copy, name, format, depth + 1, 0);
            doneBytes = before;
        } finally {
            Files.deleteIfExists(copy);
        }
    }

    /**
     * What a copy out of an archive came to.
     *
     * @param broken true when the entry broke off or ran past {@link #MAX_ENTRY_BYTES} - the bytes before that are
     *               in the copy
     */
    private record Copied(long bytes, boolean broken) {
    }

    /**
     * Copies one entry out. A read failure or an oversized entry ends the copy but not the import: the archive is
     * at fault, not the disk. A write failure is the disk, and ends the import.
     */
    private Copied copyOut(FoundJournal.Source content, Path copy, boolean compress) throws IOException {
        long written = 0;
        boolean broken = false;
        try (OutputStream file = Files.newOutputStream(copy);
             OutputStream out = compress ? new GZIPOutputStream(new BufferedOutputStream(file), 64 * 1024)
                     : new BufferedOutputStream(file)) {
            InputStream in;
            try {
                in = content.open();
            } catch (IOException | RuntimeException e) {
                log.warn("Journal import: cannot open an entry of the archive: {}", e.toString());
                return new Copied(0, true);
            }
            try (in) {
                byte[] buffer = new byte[64 * 1024];
                while (true) {
                    int read;
                    try {
                        read = in.read(buffer);
                    } catch (IOException | RuntimeException e) {
                        log.warn("Journal import: an archive entry broke off after {} bytes: {}", written, e.toString());
                        broken = true;
                        break;
                    }
                    if (read < 0) break;
                    int keep = (int) Math.min(read, MAX_ENTRY_BYTES - written);
                    try {
                        out.write(buffer, 0, keep);
                    } catch (IOException e) {
                        throw new StagingException(e);
                    }
                    written += keep;
                    if (written >= MAX_ENTRY_BYTES) {
                        log.warn("Journal import: an archive entry passed {} bytes and was cut off there", MAX_ENTRY_BYTES);
                        broken = true;
                        break;
                    }
                }
            } catch (StagingException e) {
                throw e;
            } catch (IOException e) {
                // Closing the archive's entry stream failed: the bytes are already out, so it changes nothing.
                log.debug("Journal import: closing an archive entry failed: {}", e.toString());
            }
        } catch (StagingException e) {
            throw e;
        } catch (IOException e) {
            throw new StagingException(e);
        }
        return new Copied(written, broken);
    }

    // ---------------------------------------------------------------- bookkeeping

    /**
     * Keeps one copy of each journal. Of two copies, the longer wins: the shorter was taken while the game was still
     * writing it, or was cut short by a damaged archive.
     */
    private void keep(FoundJournal journal) {
        FoundJournal earlier = byName.get(journal.name());
        if (earlier != null) {
            stats.duplicates++;
            if (earlier.size() >= journal.size()) return;
        }
        byName.put(journal.name(), journal);
    }

    private void report(long base, long withinArchive) {
        doneBytes = base + withinArchive;
        progress.collected(doneBytes, totalBytes);
    }

    private void checkCancelled() {
        if (cancelled.getAsBoolean()) throw new ImportCancelledException();
    }

    private static long sizeOf(Path file) {
        try {
            return Files.size(file);
        } catch (IOException e) {
            return 0;
        }
    }

    /**
     * The file name at the end of an archive path, written with either separator.
     */
    static String baseName(String entryName) {
        int slash = Math.max(entryName.lastIndexOf('/'), entryName.lastIndexOf('\\'));
        return slash < 0 ? entryName : entryName.substring(slash + 1);
    }

    private static boolean looksLikeArchive(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return ARCHIVE_EXTENSIONS.stream().anyMatch(lower::endsWith);
    }

    static String withoutCompressionExtension(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        for (String extension : List.of(".gz", ".bz2", ".xz")) {
            if (lower.endsWith(extension)) return name.substring(0, name.length() - extension.length());
        }
        return name;
    }

    /**
     * The staging folder could not be written. Unlike a damaged archive this is no reason to carry on.
     */
    static final class StagingException extends IOException {
        StagingException(IOException cause) {
            super(cause.getMessage(), cause);
        }
    }

    // ---------------------------------------------------------------- streams

    private static final class CountingInputStream extends FilterInputStream {
        private long count;

        CountingInputStream(InputStream in) {
            super(in);
        }

        long count() {
            return count;
        }

        @Override
        public int read() throws IOException {
            int b = super.read();
            if (b >= 0) count++;
            return b;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int n = super.read(b, off, len);
            if (n > 0) count += n;
            return n;
        }

        @Override
        public long skip(long n) throws IOException {
            long skipped = super.skip(n);
            count += skipped;
            return skipped;
        }
    }

    /**
     * An archive's stream for its current entry, which the copy must not close: the archive goes on to the next.
     */
    private static final class NonClosing extends FilterInputStream {
        NonClosing(InputStream in) {
            super(in);
        }

        @Override
        public void close() {
        }
    }

    /**
     * The 7z archive's current entry as a stream.
     */
    private static final class SevenZEntryStream extends InputStream {
        private final SevenZFile sevenZ;

        SevenZEntryStream(SevenZFile sevenZ) {
            this.sevenZ = sevenZ;
        }

        @Override
        public int read() throws IOException {
            return sevenZ.read();
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return sevenZ.read(b, off, len);
        }
    }
}
