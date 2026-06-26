package elite.intel.ai.hands;

import elite.intel.session.PlayerSession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * The BindingsLoader class is responsible for loading binding files from the bindings directory,
 * specifically identifying and retrieving the correct ".binds" file for the active preset.
 * This is useful for applications or systems that leverage the binding configurations from
 * the Elite Dangerous game by Frontier Developments.
 */
public class BindingsLoader {
    private static final Logger log = LogManager.getLogger(BindingsLoader.class);

    public File getLatestBindsFile() throws Exception {
        Path bindingsDir = PlayerSession.getInstance().getBindingsDir();

        String presetName = findActivePresetName(bindingsDir);
        if (!presetName.isEmpty()) {
            Optional<Path> matched = Files.list(bindingsDir)
                    .filter(p -> {
                        String name = p.getFileName().toString();
                        return name.startsWith(presetName + ".") && name.endsWith(".binds");
                    })
                    .max(Comparator.comparingLong(p -> p.toFile().lastModified()));

            if (matched.isPresent()) {
                log.info("Selected bindings file for preset '{}': {}", presetName, matched.get().getFileName());
                return matched.get().toFile();
            }
            log.warn("No .binds file found for preset '{}', falling back to most recently modified", presetName);
        }

        // Fallback: most recently modified .binds file
        Path latestFilePath = listAllBindsFiles(bindingsDir).stream()
                .max(Comparator.comparingLong(p -> p.toFile().lastModified()))
                .orElseThrow(() -> new Exception("No .binds file found in " + bindingsDir));

        log.info("Selected latest bindings file (fallback): {}", latestFilePath.getFileName());
        return latestFilePath.toFile();
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

    /** Locates the {@code StartPreset.*.start} file (e.g. {@code StartPreset.4.start}) in {@code bindingsDir}, if present. */
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

    // Reads the active preset name from StartPreset.*.start (e.g. StartPreset.4.start).
    // The file repeats the preset name on multiple lines; we take the first non-empty one.
    private String findActivePresetName(Path bindingsDir) {
        try {
            Optional<Path> startPresetFile = findStartPresetFile(bindingsDir);

            if (startPresetFile.isEmpty()) {
                log.warn("No StartPreset.*.start file found in {}", bindingsDir);
                return "";
            }

            List<String> lines = Files.readAllLines(startPresetFile.get());
            String presetName = lines.stream()
                    .map(String::trim)
                    .filter(l -> !l.isEmpty())
                    .findFirst()
                    .orElse("");

            if (presetName.isEmpty()) {
                log.warn("StartPreset file {} was empty", startPresetFile.get().getFileName());
            } else {
                log.info("Active preset from {}: '{}'", startPresetFile.get().getFileName(), presetName);
            }
            return presetName;

        } catch (IOException e) {
            log.warn("Could not read StartPreset file: {}", e.getMessage());
            return "";
        }
    }
}
