package elite.intel.bindforge.io;

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
import java.util.stream.Stream;

/**
 * The BindingsLoader class is responsible for loading binding files from the bindings directory,
 * specifically identifying and retrieving the correct ".binds" file for the active preset.
 * This is useful for applications or systems that leverage the binding configurations from
 * the Elite Dangerous game by Frontier Developments.
 */
public class BindingsLoader {
    private static final Logger log = LogManager.getLogger(BindingsLoader.class);

    public File getLatestBindsFile() throws IOException {
        return getLatestBindsFile(PlayerSession.getInstance().getBindingsDir());
    }

    /**
     * Same resolution against a caller-supplied directory, for callers that already hold one (and for tests,
     * which cannot lean on the session singleton). Which {@code .binds} the app decided to read is not
     * reproducible from the directory alone - the active preset drives it - so anything that needs to name
     * that file must come through here rather than re-deriving it.
     */
    public File getLatestBindsFile(Path bindingsDir) throws IOException {
        // WHY every line and not just the first: a preset can have any name, and General on a factory scheme has no
        // file here at all. The next line then names the file the commander really uses; the newest file in the folder
        // may be a leftover from another setup.
        for (String presetName : presetNamesInFileOrder(bindingsDir)) {
            Optional<Path> matched;
            try (var stream = Files.list(bindingsDir)) {
                matched = stream
                        .filter(p -> isFileOfPreset(p.getFileName().toString(), presetName))
                        .max(Comparator.comparingLong(p -> p.toFile().lastModified()));
            }

            if (matched.isPresent()) {
                log.info("Selected bindings file for preset '{}': {}", presetName, matched.get().getFileName());
                return matched.get().toFile();
            }
            log.warn("No .binds file found for preset '{}'", presetName);
        }

        // Fallback: most recently modified .binds file, when StartPreset names none that exists
        Path latestFilePath = listAllBindsFiles(bindingsDir).stream()
                .max(Comparator.comparingLong(p -> p.toFile().lastModified()))
                .orElseThrow(() -> new IOException("No .binds file found in " + bindingsDir));

        log.info("Selected latest bindings file (fallback): {}", latestFilePath.getFileName());
        return latestFilePath.toFile();
    }

    /**
     * The sections of the game's controls whose preset {@code readFile} is not, each with the preset it names, in the
     * file's order. A blank line is left out. Empty when every section matches, or the folder has no readable
     * {@code StartPreset}.
     * <p>
     * Only one file is read, so a split preset always leaves a section whose keys the app cannot see, and a clean
     * conflict check on the file read proves nothing about them (support bundle 2026-11-06: map panning moved off
     * W/A/S/D in Custom while General stayed on KeyboardMouseOnly, no warning, and route plotting failed every time).
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
                if (!preset.isEmpty() && !isFileOfPreset(readFile.getName(), preset)) {
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
     * One rule for which file is a preset's, so the read and {@link #sectionsNotIn} cannot disagree.
     */
    private static boolean isFileOfPreset(String fileName, String presetName) {
        return fileName.startsWith(presetName + ".") && fileName.endsWith(".binds");
    }

    /**
     * Lists every {@code .binds} file in {@code bindingsDir}, not just the active preset's.
     * Shared by the active-preset fallback above and by anything else (e.g. player backups)
     * that needs the full set rather than just the currently-selected file.
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
