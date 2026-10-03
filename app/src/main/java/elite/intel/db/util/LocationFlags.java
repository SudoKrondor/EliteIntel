package elite.intel.db.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import elite.intel.util.json.GsonFactory;

import java.util.Map;
import java.util.function.Supplier;

/**
 * The commander's half of a location row: the fields of {@code LocationDto} that record what this commander did
 * there, as opposed to what the place is.
 * <p>
 * A location row is one JSON blob. Nearly all of it is a galaxy fact every commander shares (the body's physics,
 * its signals, the station's services), and it lives in the shared file. These few fields are the exception, so
 * they are split off into the commander's {@code location_visit} table on write and merged back on read.
 * <p>
 * WHY in Java rather than with SQLite's JSON functions: those rewrite a document compactly, and several location
 * queries match the stored text with {@code LIKE '%"locationType": "STATION"%'}, which depends on the pretty
 * printing Gson writes. The shared row must stay byte-for-byte in Gson's own format.
 */
public final class LocationFlags {

    /**
     * Each commander field and the value it has when the commander has done nothing there.
     */
    private static final Map<String, Supplier<JsonElement>> DEFAULTS = Map.of(
            "ourDiscovery", () -> new JsonPrimitive(false),
            "weMappedIt", () -> new JsonPrimitive(false),
            "bioScansCompleted", () -> new JsonPrimitive(false),
            "partialBioSamples", JsonArray::new,
            "isHomeSystem", () -> new JsonPrimitive(false)
    );

    private LocationFlags() {
    }

    /**
     * A location's JSON cut in two.
     *
     * @param sharedJson the row with every commander field at its default, for the shared file
     * @param flags      the commander fields that differ from their default, or null when none do
     */
    public record Split(String sharedJson, String flags) {
    }

    public static Split split(String json) {
        JsonObject row = GsonFactory.getGson().fromJson(json, JsonObject.class);
        JsonObject flags = new JsonObject();
        for (Map.Entry<String, Supplier<JsonElement>> field : DEFAULTS.entrySet()) {
            JsonElement fallback = field.getValue().get();
            JsonElement value = row.get(field.getKey());
            if (value != null && !value.isJsonNull() && !value.equals(fallback)) {
                flags.add(field.getKey(), value);
            }
            if (row.has(field.getKey())) {
                row.add(field.getKey(), fallback);
            }
        }
        return new Split(GsonFactory.getGson().toJson(row), flags.isEmpty() ? null : GsonFactory.getGson().toJson(flags));
    }

    /**
     * The shared row with this commander's fields laid over it, or the shared row untouched when they have none.
     */
    public static String merge(String sharedJson, String flags) {
        if (flags == null || flags.isBlank() || sharedJson == null) return sharedJson;
        JsonObject row = GsonFactory.getGson().fromJson(sharedJson, JsonObject.class);
        JsonObject own = GsonFactory.getGson().fromJson(flags, JsonObject.class);
        for (Map.Entry<String, JsonElement> field : own.entrySet()) {
            if (DEFAULTS.containsKey(field.getKey())) {
                row.add(field.getKey(), field.getValue());
            }
        }
        return GsonFactory.getGson().toJson(row);
    }
}
