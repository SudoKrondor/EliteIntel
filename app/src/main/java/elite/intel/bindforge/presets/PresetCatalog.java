package elite.intel.bindforge.presets;

import elite.intel.bindforge.install.GameInstallation;
import elite.intel.bindforge.io.BindingsLoader;
import elite.intel.db.dao.BindForgeInstallationsDao.InstallationRow;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The presets a {@code StartPreset} line can name: the player's own, from the bindings folder, and the ones
 * Frontier ships, from each installation's {@code ControlSchemes} (Alan, 2026-10-17 - a section can sit on a
 * factory preset, and switching back to one is an ordinary edit).
 * <p>
 * Read-only, and read fresh on every call: the folder is the record, and the game changes it.
 */
public class PresetCatalog {

    private static final Logger log = LogManager.getLogger(PresetCatalog.class);

    private static final Comparator<Path> BY_FILE_NAME = Comparator.comparing(path -> path.getFileName().toString());

    private final BindingsLoader bindingsLoader;

    public PresetCatalog() {
        this(new BindingsLoader());
    }

    PresetCatalog(BindingsLoader bindingsLoader) {
        this.bindingsLoader = bindingsLoader;
    }

    /**
     * Every preset in the bindings folder and the given {@code ControlSchemes} folders.
     * <p>
     * <strong>A name is offered once.</strong> The same factory preset in several installations is one entry with
     * a file from each. A player's preset with the name of a factory one shadows it, with a warning - no shipped
     * preset has a version suffix, so the case is not expected.
     *
     * @param bindingsDir           the player's bindings folder
     * @param controlSchemesFolders each installation's {@code ControlSchemes}; one that cannot be listed is
     *                              skipped with a warning, because an unplugged drive must not empty the list
     * @throws IOException if the bindings folder cannot be listed
     */
    public PresetList list(Path bindingsDir, Collection<Path> controlSchemesFolders) throws IOException {
        Map<String, Gathered> byName = new LinkedHashMap<>();
        List<NotOfferedFile> notOffered = new ArrayList<>();

        List<Path> userFiles = new ArrayList<>(bindingsLoader.listAllBindsFiles(bindingsDir));
        userFiles.sort(BY_FILE_NAME);
        gather(userFiles, PresetOrigin.USER, byName, notOffered);

        for (Path controlSchemes : controlSchemesFolders) {
            List<Path> factoryFiles;
            try {
                factoryFiles = factoryPresetFiles(controlSchemes);
            } catch (IOException e) {
                log.warn("Could not list the factory presets in {}: {}", controlSchemes, e.getMessage());
                continue;
            }
            gather(factoryFiles, PresetOrigin.FACTORY, byName, notOffered);
        }

        List<PresetEntry> presets = byName.values().stream()
                .map(gathered -> new PresetEntry(gathered.name, gathered.origin, gathered.files))
                .sorted(Comparator.comparing(PresetEntry::name, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(PresetEntry::name))
                .toList();
        return new PresetList(presets, notOffered);
    }

    /**
     * The {@code ControlSchemes} folder of every installation whose folder is there. A missing one is left out
     * rather than listed as empty.
     */
    public static List<Path> controlSchemesOf(Collection<InstallationRow> rows) {
        return rows.stream()
                .filter(row -> !row.missing())
                .map(row -> GameInstallation.controlSchemesUnder(Path.of(row.rootPath())))
                .toList();
    }

    private static void gather(List<Path> files, PresetOrigin origin, Map<String, Gathered> byName,
                               List<NotOfferedFile> notOffered) {
        for (Path file : files) {
            switch (BindsContent.judge(file)) {
                case BindsContent.NotOffered(NotOfferedReason reason, String detail) ->
                        notOffered.add(new NotOfferedFile(file, origin, reason, detail));
                case BindsContent.Offered(String name) -> add(byName, name, origin, file);
            }
        }
    }

    private static void add(Map<String, Gathered> byName, String name, PresetOrigin origin, Path file) {
        Gathered existing = byName.get(name);
        if (existing == null) {
            byName.put(name, new Gathered(name, origin, file));
        } else if (existing.origin == origin) {
            if (origin == PresetOrigin.USER) {
                // WHY: kept, not dropped - the game upgrades an old version in place, so two files for one name
                // in a live folder is a folder something other than the game has been at.
                log.warn("Two files in the bindings folder answer to preset '{}': {} and {}",
                        name, existing.files.getFirst(), file);
            }
            existing.files.add(file);
        } else {
            log.warn("Preset '{}' is both the player's ({}) and shipped ({}); offering the player's",
                    name, existing.files.getFirst(), file);
        }
    }

    private static List<Path> factoryPresetFiles(Path controlSchemes) throws IOException {
        try (var stream = Files.list(controlSchemes)) {
            return stream
                    .filter(path -> path.getFileName().toString().endsWith(PresetName.BINDS_EXTENSION))
                    .filter(Files::isRegularFile)
                    .sorted(BY_FILE_NAME)
                    .toList();
        }
    }

    /** An entry while it is being gathered: the files grow as installations are read. */
    private static final class Gathered {
        private final String name;
        private final PresetOrigin origin;
        private final List<Path> files = new ArrayList<>();

        private Gathered(String name, PresetOrigin origin, Path first) {
            this.name = name;
            this.origin = origin;
            files.add(first);
        }
    }
}
