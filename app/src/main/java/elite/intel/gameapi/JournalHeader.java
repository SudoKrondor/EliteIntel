package elite.intel.gameapi;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import elite.intel.util.json.GsonFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * The {@code Fileheader} line every journal file opens with, read straight off disk.
 * <p>
 * WHY off disk rather than off the bus: {@code BaseEvent.isReplay()} drops every journal line stamped before
 * the app started, so the header never arrives live when the app is started while the game is already
 * running - which is the common case. Anything that must know what the header states at startup has to read
 * it, and the file the newest {@code .log} holds is the session {@code JournalParser} is following.
 * <p>
 * States a fact about the client, never a decision about it: callers ask for the field they care about and
 * decide for themselves what a missing one means.
 */
public final class JournalHeader {

    private static final Logger log = LogManager.getLogger(JournalHeader.class);

    private static final String JOURNAL_SUFFIX = ".log";
    private static final String FILEHEADER_EVENT = "Fileheader";

    private JournalHeader() {
    }

    /**
     * The header of the newest journal in {@code journalDir}, or empty when there is no folder, no journal in
     * it, or its first line is not a readable {@code Fileheader}.
     */
    public static Optional<JsonObject> ofNewestJournal(Path journalDir) {
        return newestJournal(journalDir).flatMap(JournalHeader::firstLineOf).flatMap(JournalHeader::parse);
    }

    /**
     * Parses a journal's first line into its {@code Fileheader} object, or empty when that line is not one.
     * Tolerates the byte-order mark and control characters the game writes, exactly as the live parser does.
     */
    public static Optional<JsonObject> parse(String headerLine) {
        if (headerLine == null) return Optional.empty();
        String sanitized = headerLine.replaceAll("[\\p{Cntrl}\\p{Cc}\\p{Cf}]", "").trim();
        int start = sanitized.indexOf('{');
        if (start < 0) return Optional.empty();
        try {
            JsonElement json = GsonFactory.getGson().fromJson(sanitized.substring(start), JsonElement.class);
            if (json == null || !json.isJsonObject()) return Optional.empty();
            JsonObject header = json.getAsJsonObject();
            if (!header.has("event") || !FILEHEADER_EVENT.equals(header.get("event").getAsString())) {
                return Optional.empty();
            }
            return Optional.of(header);
        } catch (RuntimeException e) {
            log.warn("Unreadable journal header: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * A header field as a string, or empty when the header does not carry it as one.
     */
    public static Optional<String> string(JsonObject header, String field) {
        if (header == null || !header.has(field) || !header.get(field).isJsonPrimitive()) return Optional.empty();
        return Optional.of(header.get(field).getAsString());
    }

    /**
     * A header field as a boolean, or empty when the header does not carry it as one.
     */
    public static Optional<Boolean> bool(JsonObject header, String field) {
        if (header == null || !header.has(field) || !header.get(field).isJsonPrimitive()) return Optional.empty();
        return Optional.of(header.get(field).getAsBoolean());
    }

    private static Optional<Path> newestJournal(Path journalDir) {
        if (journalDir == null || !Files.isDirectory(journalDir)) return Optional.empty();
        try (Stream<Path> files = Files.list(journalDir)) {
            return files.filter(p -> p.toString().endsWith(JOURNAL_SUFFIX))
                    .max(Comparator.comparingLong(p -> p.toFile().lastModified()));
        } catch (IOException e) {
            log.warn("Cannot list journal folder {}: {}", journalDir, e.getMessage());
            return Optional.empty();
        }
    }

    private static Optional<String> firstLineOf(Path journal) {
        try (BufferedReader reader = Files.newBufferedReader(journal, StandardCharsets.UTF_8)) {
            return Optional.ofNullable(reader.readLine());
        } catch (IOException e) {
            log.warn("Cannot read the header of {}: {}", journal, e.getMessage());
            return Optional.empty();
        }
    }
}
