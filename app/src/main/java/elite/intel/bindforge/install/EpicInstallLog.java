package elite.intel.bindforge.install;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads the install list the new Epic launcher ("eglv2") keeps, {@code installations.jsonl}.
 * <p>
 * The file is a log, not a table: a header line, then one snapshot of an installation per line, keyed by
 * {@code installationId}. A later line for an id replaces the earlier one, and a line carrying
 * {@code "deleted":true} means the user removed that installation. So the file is replayed in order, and an
 * installation the log last marks deleted is dropped even when its folder is still on disk (Alan, 2026-11-23).
 * <p>
 * The game and the small add-on Epic installs with it appear as two installations with the same {@code dir}. Both
 * are returned; the caller already treats one folder found twice as one installation.
 */
final class EpicInstallLog {

    private static final Logger log = LogManager.getLogger(EpicInstallLog.class);

    private EpicInstallLog() {
    }

    /**
     * The install folders the log says are present now, in the order the log first names them. An unreadable file
     * gives an empty list; an unreadable line is skipped.
     */
    static List<Path> liveInstallFolders(Path logFile) {
        List<String> lines;
        try {
            lines = Files.readAllLines(logFile);
        } catch (IOException e) {
            log.warn("Could not read the Epic install log {}: {}", logFile, e.getMessage());
            return List.of();
        }
        Map<String, Path> live = new LinkedHashMap<>();
        for (String line : lines) {
            if (line.isBlank()) continue;
            replay(line, live, logFile);
        }
        return List.copyOf(live.values());
    }

    private static void replay(String line, Map<String, Path> live, Path logFile) {
        try {
            JsonObject entry = JsonParser.parseString(line).getAsJsonObject();
            // WHY: the header line ({"version":..,"entries":..}) carries no id, and is not an installation.
            String id = stringOrNull(entry, "installationId");
            if (id == null) return;
            if (isDeleted(entry)) {
                live.remove(id);
                return;
            }
            String dir = stringOrNull(entry, "dir");
            if (dir == null || dir.isBlank()) return;
            live.put(id, Path.of(dir));
        } catch (JsonParseException | IllegalStateException | UnsupportedOperationException | InvalidPathException e) {
            log.warn("Skipped an unreadable line in the Epic install log {}: {}", logFile, e.getMessage());
        }
    }

    private static String stringOrNull(JsonObject entry, String field) {
        JsonElement value = entry.get(field);
        return value == null || value.isJsonNull() ? null : value.getAsString();
    }

    /** Whether the line records a removal: {@code "deleted":true}, a real boolean, not merely present. */
    private static boolean isDeleted(JsonObject entry) {
        JsonElement value = entry.get("deleted");
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean()
                && value.getAsBoolean();
    }
}
