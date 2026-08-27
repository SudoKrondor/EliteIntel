package elite.intel.util.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.time.Instant;
import java.time.ZonedDateTime;

/**
 * Since Java 17 we have to use a custom Gson instance to handle the Instant type.
 * If we use new Gson() as is, we will have errors parsing Instant values everywhere.
 */
public final class GsonFactory {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .registerTypeAdapter(String.class, new JournalTextTypeAdapter())
            .registerTypeAdapter(Instant.class, new InstantTypeAdapter())
            .registerTypeAdapter(ZonedDateTime.class, new ZonedDateTimeTypeAdapter())
            //.setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE)
            .create();

    public static Gson getGson() {
        return GSON;
    }

    /**
     * Converts an object to a JsonObject using the configured Gson instance.
     *
     * @param object The object to serialize.
     * @return A JsonObject representation of the object.
     */
    public static JsonObject toJsonObject(Object object) {
        return GSON.toJsonTree(object).getAsJsonObject();
    }

    // Prevent instantiation
    private GsonFactory() {
    }
}