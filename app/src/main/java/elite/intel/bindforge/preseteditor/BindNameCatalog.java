package elite.intel.bindforge.preseteditor;

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
 * The BindNames a line of {@code StartPreset.4.start} can hold: the player's own binds files, from the bindings
 * folder, and the ones Frontier ships, from each installation's {@code ControlSchemes} (Alan, 2026-10-17 - a section
 * can sit on a factory binds file, and switching back to one is an ordinary edit).
 * <p>
 * Read-only, and read fresh on every call: the folder is the record, and the game changes it.
 */
public class BindNameCatalog {

    private static final Logger log = LogManager.getLogger(BindNameCatalog.class);

    private static final Comparator<Path> BY_FILE_NAME = Comparator.comparing(path -> path.getFileName().toString());

    private final BindingsLoader bindingsLoader;

    public BindNameCatalog() {
        this(new BindingsLoader());
    }

    BindNameCatalog(BindingsLoader bindingsLoader) {
        this.bindingsLoader = bindingsLoader;
    }

    /**
     * Every binds file in the bindings folder and the given {@code ControlSchemes} folders, by BindName.
     * <p>
     * <strong>A BindName is offered once.</strong> The same factory binds file in several installations is one
     * entry with a file from each. A player's binds file with the BindName of a factory one shadows it, with a
     * warning - no factory binds file has a version suffix, so the case is not expected.
     *
     * @param bindingsDir           the player's bindings folder
     * @param controlSchemesFolders each installation's {@code ControlSchemes}; one that cannot be listed is
     *                              skipped with a warning, because an unplugged drive must not empty the list
     * @throws IOException if the bindings folder cannot be listed
     */
    public BindNameList list(Path bindingsDir, Collection<Path> controlSchemesFolders) throws IOException {
        Map<String, Gathered> byName = new LinkedHashMap<>();
        List<NotOfferedFile> notOffered = new ArrayList<>();

        List<Path> userFiles = new ArrayList<>(bindingsLoader.listAllBindsFiles(bindingsDir));
        userFiles.sort(BY_FILE_NAME);
        gather(userFiles, BindsFileOrigin.USER, byName, notOffered);

        for (Path controlSchemes : controlSchemesFolders) {
            List<Path> factoryFiles;
            try {
                factoryFiles = factoryBindsFiles(controlSchemes);
            } catch (IOException e) {
                log.warn("Could not list the factory binds files in {}: {}", controlSchemes, e.getMessage());
                continue;
            }
            gather(factoryFiles, BindsFileOrigin.FACTORY, byName, notOffered);
        }

        List<BindNameEntry> entries = byName.values().stream()
                .map(gathered -> new BindNameEntry(gathered.name, gathered.origin, gathered.files))
                .sorted(Comparator.comparing(BindNameEntry::bindName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(BindNameEntry::bindName))
                .toList();
        return new BindNameList(entries, notOffered);
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

    private static void gather(List<Path> files, BindsFileOrigin origin, Map<String, Gathered> byName,
                               List<NotOfferedFile> notOffered) {
        for (Path file : files) {
            switch (BindsContent.judge(file)) {
                case BindsContent.NotOffered(NotOfferedReason reason, String detail) ->
                        notOffered.add(new NotOfferedFile(file, origin, reason, detail));
                case BindsContent.Offered(String bindName) -> add(byName, bindName, origin, file);
            }
        }
    }

    private static void add(Map<String, Gathered> byName, String name, BindsFileOrigin origin, Path file) {
        Gathered existing = byName.get(name);
        if (existing == null) {
            byName.put(name, new Gathered(name, origin, file));
        } else if (existing.origin == origin) {
            if (origin == BindsFileOrigin.USER) {
                // WHY: kept, not dropped - the game upgrades an old version in place, so two files for one BindName
                // in a live folder is a folder something other than the game has been at.
                log.warn("Two files in the bindings folder answer to BindName '{}': {} and {}",
                        name, existing.files.getFirst(), file);
            }
            existing.files.add(file);
        } else {
            log.warn("BindName '{}' is both the player's ({}) and factory ({}); offering the player's",
                    name, existing.files.getFirst(), file);
        }
    }

    private static List<Path> factoryBindsFiles(Path controlSchemes) throws IOException {
        try (var stream = Files.list(controlSchemes)) {
            return stream
                    .filter(path -> path.getFileName().toString().endsWith(BindName.BINDS_EXTENSION))
                    .filter(Files::isRegularFile)
                    .sorted(BY_FILE_NAME)
                    .toList();
        }
    }

    /** An entry while it is being gathered: the files grow as installations are read. */
    private static final class Gathered {
        private final String name;
        private final BindsFileOrigin origin;
        private final List<Path> files = new ArrayList<>();

        private Gathered(String name, BindsFileOrigin origin, Path first) {
            this.name = name;
            this.origin = origin;
            files.add(first);
        }
    }
}
