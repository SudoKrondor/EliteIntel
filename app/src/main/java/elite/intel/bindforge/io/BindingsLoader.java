package elite.intel.bindforge.io;

import elite.intel.bindforge.install.InstallationRegistry;
import elite.intel.bindforge.model.BindingSection;
import elite.intel.session.PlayerSession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Which binds file Elite-Intel's assistant reads its keys from: the one {@code StartPreset.4.start} names (storage.md,
 * <em>Settled with Storage S13a's sources</em>, D6 and its 2026-11-08 amendment; Storage S13c).
 * <p>
 * The lines are taken in order, General first. Each resolves to its preset's true binds file -
 * {@code <name>.<major>.<minor>.binds} - in the bindings folder, else to that preset's factory binds file in the
 * {@code ControlSchemes} of the first reachable installation that holds it; a line resolving to neither passes to the
 * next. <strong>A plain {@code <name>.binds} in the bindings folder never counts</strong>, and no file is ever read
 * because it is the newest.
 */
public class BindingsLoader {
    private static final Logger log = LogManager.getLogger(BindingsLoader.class);

    /**
     * A preset's true binds files, the one read first: the highest version in the file name, then name order (Alan,
     * 2026-11-08, Storage S13c - the game upgrades a file in place, so two versions are a hand rename, which gets no
     * guard of its own; principle 13).
     */
    private static final Comparator<Path> HIGHEST_VERSION_FIRST =
            Comparator.comparingLong((Path file) -> FileNames.version(nameOf(file))).reversed()
                    .thenComparing(BindingsLoader::nameOf);

    private final Supplier<List<Path>> factoryFolders;

    /** Factory binds files from the stored list's reachable installations. */
    public BindingsLoader() {
        this(InstallationRegistry::reachableControlSchemes);
    }

    /**
     * @param factoryFolders the {@code ControlSchemes} folders to look in for a factory binds file, in the order they
     *                       are tried - asked only when a line's preset has no true binds file
     */
    public BindingsLoader(Supplier<List<Path>> factoryFolders) {
        this.factoryFolders = Objects.requireNonNull(factoryFolders, "factoryFolders");
    }

    public File getLatestBindsFile() throws IOException {
        return getLatestBindsFile(PlayerSession.getInstance().getBindingsDir());
    }

    /**
     * Same resolution against a caller-supplied directory, for callers that already hold one (and for tests,
     * which cannot lean on the session singleton). Which {@code .binds} the app decided to read is not
     * reproducible from the directory alone - the active preset drives it - so anything that needs to name
     * that file must come through here rather than re-deriving it.
     *
     * @throws IOException when no line resolves to a file - nothing is read - or the bindings folder cannot be listed
     */
    public File getLatestBindsFile(Path bindingsDir) throws IOException {
        List<Path> controlSchemes = null;
        for (String presetName : presetNamesInFileOrder(bindingsDir)) {
            Optional<Path> own = trueBindsFileOf(bindingsDir, presetName);
            if (own.isPresent()) {
                log.info("Selected bindings file for preset '{}': {}", presetName, own.get().getFileName());
                return own.get().toFile();
            }
            if (controlSchemes == null) controlSchemes = factoryFolders.get();
            Optional<Path> factory = factoryBindsFileOf(controlSchemes, presetName);
            if (factory.isPresent()) {
                log.info("Selected factory bindings file for preset '{}': {}", presetName, factory.get());
                return factory.get().toFile();
            }
            log.warn("No binds file for preset '{}' in the bindings folder or any reachable installation", presetName);
        }
        throw new IOException("No preset in the StartPreset file in " + bindingsDir
                + " has a binds file in the bindings folder or a reachable installation");
    }

    private static Optional<Path> trueBindsFileOf(Path bindingsDir, String presetName) throws IOException {
        try (var stream = Files.list(bindingsDir)) {
            return stream
                    .filter(file -> isFileOfPreset(nameOf(file), presetName))
                    .min(HIGHEST_VERSION_FIRST);
        }
    }

    /**
     * The first {@code ControlSchemes} folder's factory binds file named for {@code presetName}, ignoring case - found
     * by file name, since Frontier's own files always carry their name as {@code PresetName}.
     */
    private static Optional<Path> factoryBindsFileOf(List<Path> controlSchemesFolders, String presetName) {
        for (Path controlSchemes : controlSchemesFolders) {
            try (var stream = Files.list(controlSchemes)) {
                Optional<Path> found = stream
                        .filter(Files::isRegularFile)
                        .filter(file -> bindNameOf(nameOf(file))
                                .filter(name -> FileNames.sameFile(name, presetName)).isPresent())
                        .min(Comparator.comparing(BindingsLoader::nameOf));
                if (found.isPresent()) return found;
            } catch (IOException | UncheckedIOException e) {
                // WHY: one installation that cannot be listed must not hide the factory file another one holds.
                log.warn("Could not list the factory binds files in {}: {}", controlSchemes, e.getMessage());
            }
        }
        return Optional.empty();
    }

    /**
     * The BindName a file of either folder answers to: a true binds file's, or {@code <name>.binds}'s - the form
     * Frontier's factory files are named in. Empty for any other name.
     */
    private static Optional<String> bindNameOf(String fileName) {
        return FileNames.bindName(fileName).or(() -> FileNames.plainBindName(fileName));
    }

    private static String nameOf(Path file) {
        return file.getFileName().toString();
    }

    /**
     * The sections of the game's controls whose preset {@code readFile} is not, each with the preset it names, in the
     * file's order. A blank line is left out. Empty when every section matches, or the folder has no readable
     * {@code StartPreset}.
     * <p>
     * Only one file is read, so a split preset always leaves a section whose keys the app cannot see, and a clean
     * conflict check on the file read proves nothing about them (support bundle 2026-11-06: map panning moved off
     * W/A/S/D in Custom while General stayed on KeyboardMouseOnly, no warning, and route plotting failed every time).
     * <p>
     * A section counts as read when its preset is the one {@code readFile} is a file of, from the bindings folder or
     * from {@code ControlSchemes} (Storage S13c).
     *
     * @param readFile the file {@link #getLatestBindsFile(Path)} returned
     */
    public Map<BindingSection, String> sectionsNotIn(Path bindingsDir, File readFile) {
        try {
            Optional<Path> startPresetFile = findStartPresetFile(bindingsDir);
            if (startPresetFile.isEmpty()) {
                return Map.of();
            }
            StartPresetSections sections = StartPresetSections.parse(Files.readString(startPresetFile.get()));
            Map<BindingSection, String> notRead = new EnumMap<>(BindingSection.class);
            for (BindingSection section : List.of(
                    BindingSection.GENERAL, BindingSection.SHIP, BindingSection.SRV, BindingSection.ON_FOOT)) {
                String preset = sections.forSection(section);
                // WHY bindNameOf, not isFileOfPreset: the file read may be a factory one, named <name>.binds. The
                // loader never returns a plain file from the bindings folder, so the wider rule cannot let one in.
                boolean read = bindNameOf(readFile.getName()).filter(name -> FileNames.sameFile(name, preset))
                        .isPresent();
                if (!preset.isEmpty() && !read) {
                    notRead.put(section, preset);
                }
            }
            return notRead;
        } catch (IOException | UncheckedIOException e) {
            // WHY: this only decides whether to add a warning, and it runs inside service startup.
            log.warn("Could not compare the StartPreset sections with {}: {}", readFile.getName(), e.getMessage());
            return Map.of();
        }
    }

    /**
     * Which file in the bindings folder is a preset's true binds file. The preset's
     * name is matched ignoring case, as the game matches it (Alan, 2026-11-07, Storage S13a D6), against the file's
     * BindName as the rest of BindForge reads it ({@link FileNames#bindName}): {@code Custom.Old.4.2.binds} is BindName
     * {@code Custom.Old}, not a file of preset {@code Custom}, and a name that is not {@code <name>.<major>.<minor>.binds}
     * is no preset's file (Storage S13aR, D7).
     */
    private static boolean isFileOfPreset(String fileName, String presetName) {
        return FileNames.bindName(fileName).filter(name -> FileNames.sameFile(name, presetName)).isPresent();
    }

    /**
     * Lists every file in {@code bindingsDir} whose name ends {@code .binds}, plain ones included - for
     * {@code SetupCheck}, which asks only whether the folder holds any. Not how the file to read is chosen
     * (Storage S13c removed the newest-file fallback that used it).
     */
    public List<Path> listAllBindsFiles(Path bindingsDir) throws IOException {
        try (var stream = Files.list(bindingsDir)) {
            return stream.filter(p -> p.toString().endsWith(".binds")).toList();
        }
    }

    /**
     * Locates the {@code StartPreset.*.start} file (e.g. {@code StartPreset.4.start}) in {@code bindingsDir}, if present.
     * <p>
     * When the folder holds both {@code StartPreset.start} and a numbered one, which it returns depends on listing
     * order. It is kept as it is for its existing callers; BindForge picks the preset file from the working copy
     * ({@code bindforge.storage.PresetEdits}).
     */
    public Optional<Path> findStartPresetFile(Path bindingsDir) throws IOException {
        try (var stream = Files.list(bindingsDir)) {
            return stream
                    .filter(p -> {
                        String name = p.getFileName().toString();
                        return name.startsWith("StartPreset.") && name.endsWith(".start");
                    })
                    .findFirst();
        }
    }

    public String getActivePresetName() {
        return findActivePresetName(PlayerSession.getInstance().getBindingsDir());
    }

    // The active preset is the first name in StartPreset.*.start (e.g. StartPreset.4.start), or "" when it has none.
    private String findActivePresetName(Path bindingsDir) {
        return presetNamesInFileOrder(bindingsDir).stream().findFirst().orElse("");
    }

    // The names StartPreset.*.start holds, in its own order - General, Ship, SRV, On Foot - blanks and repeats left out.
    private List<String> presetNamesInFileOrder(Path bindingsDir) {
        try {
            Optional<Path> startPresetFile = findStartPresetFile(bindingsDir);

            if (startPresetFile.isEmpty()) {
                log.warn("No StartPreset.*.start file found in {}", bindingsDir);
                return List.of();
            }

            StartPresetSections sections = StartPresetSections.parse(Files.readString(startPresetFile.get()));
            List<String> names = Stream.of(sections.general(), sections.ship(), sections.srv(), sections.onFoot())
                    .filter(name -> !name.isEmpty())
                    .distinct()
                    .toList();

            if (names.isEmpty()) {
                log.warn("StartPreset file {} was empty", startPresetFile.get().getFileName());
            } else {
                log.info("Presets from {}: {}", startPresetFile.get().getFileName(), names);
            }
            return names;

        } catch (IOException e) {
            log.warn("Could not read StartPreset file: {}", e.getMessage());
            return List.of();
        }
    }
}
