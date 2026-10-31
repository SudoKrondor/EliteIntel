package elite.intel.gameapi.journalimport;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import elite.intel.util.json.GsonFactory;

import java.io.IOException;
import java.util.Optional;

/**
 * What the top of a journal says: that it is one, which client language wrote it, and which commander.
 *
 * @param language      the header's {@code language}, e.g. {@code "English/UK"}, or null when it has none
 * @param fid           the commander's FID from {@code Commander} or {@code LoadGame}, or null - journals from
 *                      before 2018 carry only the name, and a continuation part carries neither
 * @param commanderName the commander's name, or null
 */
record JournalHeaderInfo(String language, String fid, String commanderName) {

    /**
     * How far into a journal to look for the commander - the game names them within the first few lines.
     */
    private static final int HEADER_LINES = 100;

    /**
     * Reads the header, or returns empty when the file is not a journal at all: its first line must be the game's
     * {@code Fileheader}. A file that merely has a journal's name - a renamed text file, a corrupt copy that begins
     * with garbage - is passed over rather than read.
     */
    static Optional<JournalHeaderInfo> read(BoundedLineReader reader) throws IOException {
        String first = reader.readLine();
        JsonObject header = event(first);
        if (header == null || !"Fileheader".equals(text(header, "event"))) return Optional.empty();

        String language = text(header, "language");
        String fid = null;
        String name = null;
        for (int n = 1; n < HEADER_LINES && fid == null; n++) {
            String line = reader.readLine();
            if (line == null) break;
            if (!line.contains("\"Commander\"") && !line.contains("\"LoadGame\"")) continue;
            JsonObject json = event(line);
            if (json == null) continue;
            String event = text(json, "event");
            if ("Commander".equals(event)) {
                name = text(json, "Name");
                fid = text(json, "FID");
            } else if ("LoadGame".equals(event)) {
                if (name == null) name = text(json, "Commander");
                fid = text(json, "FID");
            }
        }
        return Optional.of(new JournalHeaderInfo(language, blankToNull(fid), blankToNull(name)));
    }

    private static JsonObject event(String line) {
        if (line == null) return null;
        String trimmed = line.trim();
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) return null;
        try {
            JsonElement json = GsonFactory.getGson().fromJson(trimmed, JsonElement.class);
            return json != null && json.isJsonObject() ? json.getAsJsonObject() : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static String text(JsonObject json, String field) {
        JsonElement value = json.get(field);
        return value != null && value.isJsonPrimitive() ? value.getAsString() : null;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
