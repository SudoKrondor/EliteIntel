package elite.intel.ui.theme;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * The themes a commander can choose from: the built-in ones, and their own in the themes folder, one JSON file each.
 * <p>
 * A theme's id ({@link Entry#id()}) is what the active-theme setting stores. For the commander's own themes it is the
 * file name, fixed when the theme is created: a rename changes only the name inside the file, so the setting keeps
 * pointing at the theme. Names are kept unique, so the list never shows two themes the commander cannot tell apart.
 */
public final class ThemeLibrary {

    private static final Logger log = LogManager.getLogger(ThemeLibrary.class);

    private static final String FILE_SUFFIX = ".json";
    private static final String FALLBACK_FILE_STEM = "theme";

    /** A theme in the library, by the id the active-theme setting stores. */
    public record Entry(String id, ColorTheme theme, boolean builtIn) {
    }

    private final Path folder;

    /**
     * @param folder the commander's themes folder ({@code AppPaths.getThemesDir()})
     */
    public ThemeLibrary(Path folder) {
        this.folder = folder;
    }

    /**
     * Every theme: the built-in ones first, then the commander's own by name. A file that cannot be read is left out
     * and logged - one broken file must not hide the rest.
     */
    public List<Entry> entries() throws IOException {
        List<Entry> entries = new ArrayList<>();
        entries.add(new Entry(ThemeStore.DEFAULT_ID, ColorTheme.DEFAULT, true));
        builtIn(ThemeStore.HIGH_CONTRAST_ID).ifPresent(entries::add);
        builtIn(ThemeStore.CLOCKWORK_ORANGE_ID).ifPresent(entries::add);
        builtIn(ThemeStore.COFFEE).ifPresent(entries::add);
        builtIn(ThemeStore.HACKERS_GREEN).ifPresent(entries::add);
        builtIn(ThemeStore.PURPLE_RAIN).ifPresent(entries::add);
        builtIn(ThemeStore.FRAMESHIFT).ifPresent(entries::add);
        List<Entry> own = new ArrayList<>();
        try (DirectoryStream<Path> files = Files.newDirectoryStream(folder, "*" + FILE_SUFFIX)) {
            for (Path file : files) readEntry(file).ifPresent(own::add);
        }
        own.sort(Comparator.comparing(entry -> entry.theme().name().toLowerCase(Locale.ROOT)));
        entries.addAll(own);
        return entries;
    }

    /** Adds a theme as a new file. Its name is made unique first. */
    public Entry create(ColorTheme theme) throws IOException {
        ColorTheme named = renamed(theme, uniqueName(theme.name()));
        String id = uniqueFileName(named.name());
        ThemeFile.write(folder.resolve(id), named);
        return new Entry(id, named, false);
    }

    /** Saves a commander's theme over its file. */
    public Entry save(Entry entry, ColorTheme theme) throws IOException {
        requireOwn(entry);
        ThemeFile.write(folder.resolve(entry.id()), theme);
        return new Entry(entry.id(), theme, false);
    }

    /** Renames a commander's theme. Its file, and so its id, stay as they are. */
    public Entry rename(Entry entry, String newName) throws IOException {
        requireOwn(entry);
        String name = newName.strip();
        if (!name.equalsIgnoreCase(entry.theme().name())) name = uniqueName(name);
        return save(entry, renamed(entry.theme(), name));
    }

    /** Deletes a commander's theme. */
    public void delete(Entry entry) throws IOException {
        requireOwn(entry);
        Files.deleteIfExists(folder.resolve(entry.id()));
    }

    /** Writes a theme to a file the commander chose, to share it. */
    public void export(Entry entry, Path target) throws IOException {
        ThemeFile.write(target, entry.theme());
    }

    /** The commander's theme with this name, ignoring case. */
    public Optional<Entry> findByName(String name) throws IOException {
        return entries().stream()
                .filter(entry -> !entry.builtIn() && entry.theme().name().equalsIgnoreCase(name.strip()))
                .findFirst();
    }

    /** {@code name}, or {@code name 2}, {@code name 3} ... - the first no theme in the library has. */
    public String uniqueName(String name) throws IOException {
        String base = name.strip();
        List<String> taken = entries().stream().map(entry -> entry.theme().name().toLowerCase(Locale.ROOT)).toList();
        String candidate = base;
        for (int n = 2; taken.contains(candidate.toLowerCase(Locale.ROOT)); n++) candidate = base + " " + n;
        return candidate;
    }

    private Optional<Entry> builtIn(String id) {
        try {
            return Optional.of(new Entry(id, new ThemeStore(folder).load(id), true));
        } catch (IOException | ThemeFileException e) {
            log.error("Leaving the built-in colour theme {} out of the list: it cannot be read", id, e);
            return Optional.empty();
        }
    }

    private Optional<Entry> readEntry(Path file) {
        try {
            return Optional.of(new Entry(file.getFileName().toString(), ThemeFile.read(file), false));
        } catch (IOException | ThemeFileException e) {
            log.error("Leaving colour theme {} out of the list: it cannot be read", file, e);
            return Optional.empty();
        }
    }

    /**
     * WHY ASCII only: the file name is also an id stored in the database and must work on every file system. A name
     * with nothing left after that - all Cyrillic, say - gets a plain stem; the name inside the file is untouched.
     */
    private String uniqueFileName(String name) {
        String stem = Normalizer.normalize(name, Normalizer.Form.NFD)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
        if (stem.isEmpty()) stem = FALLBACK_FILE_STEM;
        String candidate = stem + FILE_SUFFIX;
        for (int n = 2; Files.exists(folder.resolve(candidate)); n++) candidate = stem + "-" + n + FILE_SUFFIX;
        return candidate;
    }

    private static ColorTheme renamed(ColorTheme theme, String name) {
        return new ColorTheme(name, theme.author(), theme.overrides());
    }

    private static void requireOwn(Entry entry) {
        if (entry.builtIn()) throw new IllegalArgumentException("A built-in theme cannot be changed: " + entry.id());
    }
}
