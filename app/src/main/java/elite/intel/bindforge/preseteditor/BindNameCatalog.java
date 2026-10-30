package elite.intel.bindforge.preseteditor;

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
import java.util.TreeMap;

/**
 * The BindNames a line of {@code StartPreset.4.start} can hold: the player's own binds files, from the set APPLY writes,
 * and the ones Frontier ships, from each installation's {@code ControlSchemes} (Alan, 2026-10-17 - a section can sit on
 * a factory binds file, and switching back to one is an ordinary edit).
 * <p>
 * <strong>The player's binds files come from the edits, not the bindings folder</strong> (Storage S6, Alan,
 * 2026-10-28): the edits are what APPLY writes, so a binds file renamed or created there is one a line can name before
 * it reaches the game. The factory ones are read fresh from disk on every call, since the game changes them.
 */
public final class BindNameCatalog {

    private static final Logger log = LogManager.getLogger(BindNameCatalog.class);

    private static final Comparator<Path> BY_FILE_NAME = Comparator.comparing(path -> path.getFileName().toString());

    private BindNameCatalog() {
    }

    /**
     * Every binds file in {@code userFiles} and the given {@code ControlSchemes} folders, by BindName.
     * <p>
     * <strong>A BindName is offered once.</strong> The same factory binds file in several installations is one
     * entry with a file from each. A player's binds file with the BindName of a factory one shadows it, with a
     * warning - no factory binds file has a version suffix, so the case is not expected.
     *
     * @param userFiles             the player's binds files from the set, by file name - every name of the form
     *                              {@code <name>.<major>.<minor>.binds}, since the set holds no other (Storage S6R2).
     *                              Each is listed as a {@link Path} of its name alone, since it is in the set, not yet
     *                              necessarily on disk
     * @param controlSchemesFolders each installation's {@code ControlSchemes}; one that cannot be listed is
     *                              skipped with a warning, because an unplugged drive must not empty the list
     */
    public static BindNameList list(Map<String, byte[]> userFiles, Collection<Path> controlSchemesFolders) {
        Map<String, Gathered> byName = new LinkedHashMap<>();
        List<NotOfferedFile> notOffered = new ArrayList<>();

        new TreeMap<>(userFiles).forEach((name, content) ->
                judged(Path.of(name), BindsContent.judge(name, content), BindsFileOrigin.USER, byName, notOffered));

        for (Path controlSchemes : controlSchemesFolders) {
            List<Path> factoryFiles;
            try {
                factoryFiles = factoryBindsFiles(controlSchemes);
            } catch (IOException e) {
                log.warn("Could not list the factory binds files in {}: {}", controlSchemes, e.getMessage());
                continue;
            }
            for (Path file : factoryFiles) {
                judged(file, BindsContent.judge(file), BindsFileOrigin.FACTORY, byName, notOffered);
            }
        }

        List<BindNameEntry> entries = byName.values().stream()
                .map(gathered -> new BindNameEntry(gathered.name, gathered.origin, gathered.files))
                .sorted(Comparator.comparing(BindNameEntry::bindName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(BindNameEntry::bindName))
                .toList();
        return new BindNameList(entries, notOffered);
    }

    private static void judged(Path file, BindsContent.Judgement judgement, BindsFileOrigin origin,
                               Map<String, Gathered> byName, List<NotOfferedFile> notOffered) {
        switch (judgement) {
            case BindsContent.NotOffered(NotOfferedReason reason, String detail) ->
                    notOffered.add(new NotOfferedFile(file, origin, reason, detail));
            case BindsContent.Offered(String bindName) -> add(byName, bindName, origin, file);
        }
    }

    private static void add(Map<String, Gathered> byName, String name, BindsFileOrigin origin, Path file) {
        Gathered existing = byName.get(name);
        if (existing == null) {
            byName.put(name, new Gathered(name, origin, file));
        } else if (existing.origin == origin) {
            if (origin == BindsFileOrigin.USER) {
                // WHY: kept, not dropped - the game upgrades an old version in place, so two files for one BindName
                // is a set something other than the game has been at.
                log.warn("Two of the player's binds files answer to BindName '{}': {} and {}",
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
