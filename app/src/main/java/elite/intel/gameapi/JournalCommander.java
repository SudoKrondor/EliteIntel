package elite.intel.gameapi;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import elite.intel.db.util.Database;
import elite.intel.util.json.GsonFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Which commander a journal belongs to, read straight off disk.
 * <p>
 * A commander is identified by the FID the {@code Commander} and {@code LoadGame} events carry, never by the name,
 * which is only for display. Each journal belongs to one commander, named near the top of the file.
 * <p>
 * WHY off disk: the commander decides which database file the app opens, and that has to be settled while the
 * database is still starting, long before the journal parser or the event bus exist.
 */
public final class JournalCommander {

    private static final Logger log = LogManager.getLogger(JournalCommander.class);

    /**
     * How far into a journal to look for the commander. The game writes {@code Commander} within the first few
     * lines of a session, so a file that has not named one by here never will (it is a continuation part, or the
     * game went to the menu and quit).
     */
    private static final int HEADER_LINES = 100;

    /**
     * How many journals, newest first, to try before giving up. Continuation parts and menu-only sessions name no
     * commander, so the newest file alone is not enough, but a folder where not one of this many does is not
     * worth reading further at startup.
     */
    private static final int MAX_JOURNALS = 50;

    private static final Pattern PART = Pattern.compile("\\.(\\d{2})\\.log$");

    private JournalCommander() {
    }

    /**
     * The FID of the commander in the newest journal that names one, or empty when the folder holds none (a
     * fresh install, a wrong folder, or a folder that cannot be read).
     * <p>
     * A continuation part does not repeat the {@code Commander} event, so it is skipped and the part before it,
     * which is the same session, answers instead.
     */
    public static Optional<String> newestFid(Path journalDir) {
        if (journalDir == null || !Files.isDirectory(journalDir)) return Optional.empty();
        List<Path> journals;
        try {
            journals = JournalFiles.listOldestFirst(journalDir);
        } catch (IOException e) {
            log.warn("Cannot list journal folder {}: {}", journalDir, e.getMessage());
            return Optional.empty();
        }
        int tried = 0;
        for (int i = journals.size() - 1; i >= 0 && tried < MAX_JOURNALS; i--, tried++) {
            Optional<String> fid = fidOf(journals.get(i));
            if (fid.isPresent()) return fid;
        }
        return Optional.empty();
    }

    /**
     * The newest {@code count} journals that belong to {@code fid}, oldest first: the shape the pre-scans read.
     * <p>
     * This is what keeps the pre-scans from mixing commanders. "The newest N journals" read one commander's
     * session into another's file whenever the two had just been swapped.
     * <p>
     * A file belongs to the commander it names. A continuation part names no one and belongs to whoever the part
     * before it belonged to. A first part that names no one (the game went to the menu and quit) belongs to
     * no one and is never read.
     */
    public static List<Path> newestOwnedBy(Path journalDir, String fid, int count) {
        if (journalDir == null || fid == null || !Files.isDirectory(journalDir)) return List.of();
        List<Path> journals;
        try {
            journals = JournalFiles.listOldestFirst(journalDir);
        } catch (IOException e) {
            log.warn("Cannot list journal folder {}: {}", journalDir, e.getMessage());
            return List.of();
        }
        Map<Integer, Optional<String>> owners = new HashMap<>();
        List<Path> owned = new ArrayList<>();
        int tried = 0;
        for (int i = journals.size() - 1; i >= 0 && owned.size() < count && tried < MAX_JOURNALS; i--, tried++) {
            if (ownerOf(journals, i, owners).filter(fid::equals).isPresent()) {
                owned.addFirst(journals.get(i));
            }
        }
        return owned;
    }

    /**
     * The newest {@code count} journals of the commander whose database is open, oldest first. Before any journal
     * has named a commander there is no one to filter by, and the newest {@code count} are read as they come.
     */
    public static List<Path> newestOfCurrentCommander(Path journalDir, int count) throws IOException {
        if (!Database.isCommanderKnown()) {
            return JournalFiles.newest(journalDir, count);
        }
        return newestOwnedBy(journalDir, Database.currentCommander(), count);
    }

    private static Optional<String> ownerOf(List<Path> journals, int index, Map<Integer, Optional<String>> owners) {
        Optional<String> known = owners.get(index);
        if (known != null) return known;
        Optional<String> owner = fidOf(journals.get(index));
        if (owner.isEmpty() && index > 0 && isContinuationPart(journals.get(index))) {
            owner = ownerOf(journals, index - 1, owners);
        }
        owners.put(index, owner);
        return owner;
    }

    /**
     * {@code Journal.<stamp>.02.log} and later: the game rolled a long session into a new file.
     */
    static boolean isContinuationPart(Path journal) {
        Matcher m = PART.matcher(journal.getFileName().toString());
        return m.find() && Integer.parseInt(m.group(1)) > 1;
    }

    /**
     * The FID named by the {@code Commander} or {@code LoadGame} event near the top of one journal, or empty when
     * the file names none.
     */
    public static Optional<String> fidOf(Path journal) {
        try (BufferedReader reader = Files.newBufferedReader(journal, StandardCharsets.UTF_8)) {
            String line;
            for (int n = 0; n < HEADER_LINES && (line = reader.readLine()) != null; n++) {
                Optional<String> fid = fidOfLine(line);
                if (fid.isPresent()) return fid;
            }
        } catch (IOException | RuntimeException e) {
            log.warn("Cannot read the commander of {}: {}", journal, e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * The FID a single journal line carries, when it is a {@code Commander} or {@code LoadGame} event.
     */
    static Optional<String> fidOfLine(String line) {
        // Cheap filter before parsing: nearly every line is some other event.
        if (line == null || !(line.contains("Commander") || line.contains("LoadGame"))) return Optional.empty();
        int start = line.indexOf('{');
        if (start < 0) return Optional.empty();
        try {
            JsonElement json = GsonFactory.getGson().fromJson(line.substring(start), JsonElement.class);
            if (json == null || !json.isJsonObject()) return Optional.empty();
            JsonObject event = json.getAsJsonObject();
            String name = event.has("event") ? event.get("event").getAsString() : "";
            if (!"Commander".equals(name) && !"LoadGame".equals(name)) return Optional.empty();
            if (!event.has("FID") || !event.get("FID").isJsonPrimitive()) return Optional.empty();
            String fid = event.get("FID").getAsString().trim();
            return fid.isEmpty() ? Optional.empty() : Optional.of(fid);
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }
}
