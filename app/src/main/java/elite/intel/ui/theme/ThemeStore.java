package elite.intel.ui.theme;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/**
 * Finds a colour theme by the id the active-theme setting stores ({@code game_session.uiTheme}).
 * <ul>
 *     <li>an empty id is the Default theme;</li>
 *     <li>{@code builtin:<name>} is a theme shipped in the jar, {@code /themes/<name>.json};</li>
 *     <li>anything else is the name of a file in the commander's themes folder.</li>
 * </ul>
 */
public final class ThemeStore {

    private static final Logger log = LogManager.getLogger(ThemeStore.class);

    public static final String DEFAULT_ID = "";
    static final String BUILT_IN_PREFIX = "builtin:";
    /** The built-in theme for low vision and glare: black, white, and brighter frames and colours. */
    public static final String HIGH_CONTRAST_ID = BUILT_IN_PREFIX + "high-contrast";
    /** The built-in orange theme: a nod to the game's own HUD colours, the app's look before V1.2. */
    public static final String CLOCKWORK_ORANGE_ID = BUILT_IN_PREFIX + "clockwork-orange";
    public static final String HACKERS_GREEN = BUILT_IN_PREFIX + "hackers-green";
    public static final String COFFEE = BUILT_IN_PREFIX + "coffee";
    public static final String PURPLE_RAIN = BUILT_IN_PREFIX + "purple-rain";
    /** The built-in theme in FrameShift Command's colours: navy, with orange frames and cyan information. */
    public static final String FRAMESHIFT = BUILT_IN_PREFIX + "frameshift";
    private static final String BUILT_IN_FOLDER = "/themes/";
    private static final String FILE_SUFFIX = ".json";

    private final Path folder;

    /**
     * @param folder the commander's themes folder ({@code AppPaths.getThemesDir()})
     */
    public ThemeStore(Path folder) {
        this.folder = folder;
    }

    /**
     * The theme {@code id} names, or Default when it cannot be loaded - logged, never thrown, because the app must
     * start whatever happened to a theme file. The setting is left as it is: a file on a drive that is not mounted
     * yet may be back on the next start.
     */
    public ColorTheme loadOrDefault(String id) {
        try {
            return load(id);
        } catch (IOException | ThemeFileException e) {
            log.error("Could not load colour theme '{}', using Default instead", id, e);
            return ColorTheme.DEFAULT;
        }
    }

    /** The theme {@code id} names. */
    public ColorTheme load(String id) throws IOException, ThemeFileException {
        if (id == null || id.isBlank()) return ColorTheme.DEFAULT;
        if (id.startsWith(BUILT_IN_PREFIX)) return loadBuiltIn(id.substring(BUILT_IN_PREFIX.length()));
        return ThemeFile.read(commanderFile(id));
    }

    private static ColorTheme loadBuiltIn(String name) throws IOException, ThemeFileException {
        String resource = BUILT_IN_FOLDER + name + FILE_SUFFIX;
        try (InputStream in = ThemeStore.class.getResourceAsStream(resource)) {
            if (in == null) throw new ThemeFileException("No built-in theme " + resource);
            return ThemeFile.parse(new String(in.readNBytes((int) ThemeFile.MAX_BYTES), StandardCharsets.UTF_8));
        }
    }

    /**
     * WHY the check: the id comes from the database, and a restored or hand-edited one must not reach outside the
     * themes folder.
     */
    private Path commanderFile(String id) throws ThemeFileException {
        Path file = folder.resolve(id).normalize();
        if (!folder.normalize().equals(file.getParent())) {
            throw new ThemeFileException("A theme id names a file in the themes folder, not a path: " + id);
        }
        return file;
    }
}
