package elite.intel.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Keeps timestamped copies of a file before it is replaced: {@code <name>.<yyyyMMdd-HHmmss>.bak}, with
 * {@code -1}, {@code -2}... appended when two copies land in the same second.
 * <p>
 * Every name ends in {@code .bak}, so a copy never matches the suffix the original's reader looks for
 * (a {@code .binds} copy is not mistaken for a loadable profile).
 */
public class TimestampedBackups {

    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final int MAX_SAME_SECOND_COPIES = 100;

    private final Clock clock;

    public TimestampedBackups() {
        this(Clock.systemDefaultZone());
    }

    public TimestampedBackups(Clock clock) {
        this.clock = clock;
    }

    /**
     * Copies {@code file} into {@code targetDirectory} under a new timestamped name, creating the
     * directory if needed.
     *
     * @return the path of the copy
     * @throws IOException if the copy fails or every same-second name is taken
     */
    public Path create(Path file, Path targetDirectory) throws IOException {
        Files.createDirectories(targetDirectory);
        String baseName = file.getFileName() + "." + ZonedDateTime.now(clock).format(TIMESTAMP);
        for (int attempt = 0; attempt < MAX_SAME_SECOND_COPIES; attempt++) {
            String suffix = attempt == 0 ? "" : "-" + attempt;
            Path backup = targetDirectory.resolve(baseName + suffix + ".bak");
            if (!Files.exists(backup)) {
                return Files.copy(file, backup, StandardCopyOption.COPY_ATTRIBUTES);
            }
        }
        throw new IOException("Could not create a unique backup filename for " + file);
    }

    /**
     * Deletes the oldest copies of {@code fileName} in {@code directory} until at most {@code keep} remain.
     * Only names this class creates for that file are considered; anything else in the folder is left alone.
     *
     * @throws IllegalArgumentException if {@code keep} is negative
     */
    public void prune(Path directory, String fileName, int keep) throws IOException {
        if (keep < 0) {
            throw new IllegalArgumentException("keep must not be negative: " + keep);
        }
        List<Path> newestFirst = copiesOf(directory, fileName).stream()
                .sorted(Comparator.comparing(BackupName::timestamp)
                        .thenComparingInt(BackupName::sameSecondIndex)
                        .reversed())
                .map(BackupName::path)
                .toList();
        for (Path stale : newestFirst.subList(Math.min(keep, newestFirst.size()), newestFirst.size())) {
            Files.deleteIfExists(stale);
        }
    }

    private List<BackupName> copiesOf(Path directory, String fileName) throws IOException {
        if (!Files.isDirectory(directory)) {
            return List.of();
        }
        Pattern pattern = Pattern.compile(Pattern.quote(fileName) + "\\.(\\d{8}-\\d{6})(?:-(\\d+))?\\.bak");
        try (Stream<Path> entries = Files.list(directory)) {
            return entries.map(path -> BackupName.parse(path, pattern))
                    .flatMap(Stream::ofNullable)
                    .toList();
        }
    }

    // WHY: the timestamp alone cannot order copies taken in the same second ("x.20261003-120000.bak" sorts
    // after "x.20261003-120000-1.bak" as text), so the same-second index is compared as a number.
    private record BackupName(Path path, String timestamp, int sameSecondIndex) {
        static BackupName parse(Path path, Pattern pattern) {
            Matcher matcher = pattern.matcher(path.getFileName().toString());
            if (!matcher.matches()) {
                return null;
            }
            int index = matcher.group(2) == null ? 0 : Integer.parseInt(matcher.group(2));
            return new BackupName(path, matcher.group(1), index);
        }
    }
}
