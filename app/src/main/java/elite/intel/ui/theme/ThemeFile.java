package elite.intel.ui.theme;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import elite.intel.io.AtomicFiles;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * A colour theme as a JSON file: the form a theme is kept in, exported in, and shared in.
 * <pre>{@code
 * { "format": 1, "name": "Night Ops", "author": "CMDR Example",
 *   "colors": { "success": "#7CE8A8", "primaryAction": "#A633D7E8" } }
 * }</pre>
 * Colours are {@code #RRGGBB}, or {@code #AARRGGBB} for one drawn see-through. Only the roles that differ from the
 * Default theme are written.
 * <p>
 * Reading is lenient about colours and strict about the file. A role this build does not know, or a colour it cannot
 * read, is dropped and the rest of the theme loads - the same rule the overlay palette follows - because a theme
 * from a newer version must still work here. A file that is not a theme at all, has no name, or was written in a
 * newer {@code format} is refused ({@link ThemeFileException}). A theme from a stranger is data only: colours and two
 * strings, nothing that runs.
 */
public final class ThemeFile {

    private static final Logger log = LogManager.getLogger(ThemeFile.class);

    /** The format this build writes, and the newest it reads. */
    public static final int FORMAT = 1;
    /** No real theme comes near this size. Anything larger is not a theme and is not read into memory. */
    static final long MAX_BYTES = 64 * 1024;

    private static final String FORMAT_KEY = "format";
    private static final String NAME_KEY = "name";
    private static final String AUTHOR_KEY = "author";
    private static final String COLORS_KEY = "colors";
    private static final Pattern HEX_COLOR = Pattern.compile("#(?:[0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})");
    private static final Gson PRETTY = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private ThemeFile() {
    }

    /** Reads a theme file. */
    public static ColorTheme read(Path file) throws IOException, ThemeFileException {
        if (Files.size(file) > MAX_BYTES) {
            throw new ThemeFileException("Larger than " + MAX_BYTES + " bytes, so not a theme: " + file);
        }
        return parse(Files.readString(file, StandardCharsets.UTF_8));
    }

    /** Writes a theme file, replacing any file already there in one step. */
    public static void write(Path file, ColorTheme theme) throws IOException {
        AtomicFiles.write(file, format(theme).getBytes(StandardCharsets.UTF_8));
    }

    /** Reads a theme from its JSON text. */
    public static ColorTheme parse(String json) throws ThemeFileException {
        JsonObject root = parseObject(json);
        requireReadableFormat(root);
        String name = optionalString(root, NAME_KEY);
        if (name.isBlank()) throw new ThemeFileException("A theme file needs a name");
        return new ColorTheme(name.strip(), optionalString(root, AUTHOR_KEY).strip(), readColors(root));
    }

    /** The JSON text of a theme. */
    public static String format(ColorTheme theme) {
        JsonObject colors = new JsonObject();
        for (ThemeRole role : ThemeRole.values()) {
            Color color = theme.overrides().get(role);
            if (color != null) colors.addProperty(role.wireName(), hex(color));
        }
        JsonObject root = new JsonObject();
        root.addProperty(FORMAT_KEY, FORMAT);
        root.addProperty(NAME_KEY, theme.name());
        root.addProperty(AUTHOR_KEY, theme.author());
        root.add(COLORS_KEY, colors);
        return PRETTY.toJson(root) + "\n";
    }

    /** {@code #RRGGBB} for an opaque colour, {@code #AARRGGBB} for one with alpha, upper case. */
    static String hex(Color color) {
        if (color.getAlpha() == 0xFF) return String.format(Locale.ROOT, "#%06X", color.getRGB() & 0xFFFFFF);
        return String.format(Locale.ROOT, "#%08X", color.getRGB());
    }

    /** The colour a {@code #RRGGBB} or {@code #AARRGGBB} string names, or empty when it names none. */
    static Optional<Color> parseHex(String text) {
        if (text == null || !HEX_COLOR.matcher(text).matches()) return Optional.empty();
        long value = Long.parseLong(text.substring(1), 16);
        boolean hasAlpha = text.length() == 9;
        return Optional.of(new Color((int) (hasAlpha ? value : 0xFF000000L | value), true));
    }

    private static JsonObject parseObject(String json) throws ThemeFileException {
        try {
            JsonElement root = JsonParser.parseString(json);
            if (!root.isJsonObject()) throw new ThemeFileException("A theme file is a JSON object");
            return root.getAsJsonObject();
        } catch (JsonParseException e) {
            throw new ThemeFileException("Not a JSON file: " + e.getMessage(), e);
        }
    }

    private static void requireReadableFormat(JsonObject root) throws ThemeFileException {
        JsonElement format = root.get(FORMAT_KEY);
        if (format == null || !format.isJsonPrimitive() || !format.getAsJsonPrimitive().isNumber()) {
            throw new ThemeFileException("Not a theme file: it has no format number");
        }
        int version;
        try {
            version = format.getAsInt();
        } catch (NumberFormatException e) {
            throw new ThemeFileException("Unknown theme format " + format, e);
        }
        if (version > FORMAT) {
            throw new ThemeFileException("Made by a newer version of the app (theme format " + version + ")");
        }
        if (version < 1) throw new ThemeFileException("Unknown theme format " + version);
    }

    private static Map<ThemeRole, Color> readColors(JsonObject root) {
        Map<ThemeRole, Color> colors = new EnumMap<>(ThemeRole.class);
        JsonElement element = root.get(COLORS_KEY);
        if (element == null || !element.isJsonObject()) return colors;

        for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
            Optional<ThemeRole> role = ThemeRole.byWireName(entry.getKey());
            Optional<Color> color = stringValue(entry.getValue()).flatMap(ThemeFile::parseHex);
            if (role.isEmpty() || color.isEmpty()) {
                log.warn("Ignoring theme colour {}={}", entry.getKey(), entry.getValue());
                continue;
            }
            colors.put(role.get(), color.get());
        }
        return colors;
    }

    private static String optionalString(JsonObject root, String key) {
        return stringValue(root.get(key)).orElse("");
    }

    private static Optional<String> stringValue(JsonElement element) {
        if (element instanceof JsonPrimitive primitive && primitive.isString()) return Optional.of(primitive.getAsString());
        return Optional.empty();
    }
}
