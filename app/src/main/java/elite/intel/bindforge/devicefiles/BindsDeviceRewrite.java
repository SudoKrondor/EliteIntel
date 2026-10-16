package elite.intel.bindforge.devicefiles;

import elite.intel.bindforge.io.BindingsWorkingCopyRepository;
import elite.intel.bindforge.io.BindingsWriter;
import elite.intel.bindforge.io.DeviceReferenceRewrite;
import elite.intel.bindforge.io.DeviceRewriteResult;
import elite.intel.util.AppPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Moves the bindings that name a device by one reference to another, across {@code .binds} - Apply's half of
 * onboarding a controller {@code .binds} names by hex (Alan, 2026-10-14, A6b), and of renaming a device (A11). Once an
 * entry exists, {@code .binds} is expected to name the device by it (format §4.0).
 * <p>
 * <strong>Every {@code .binds} in the bindings folder</strong>, not only the active preset, since the game validates
 * them all - and <strong>each one's working copy first</strong>, the game's own files last (Krondor, 2026-10-13):
 * writing a game file makes the bindings monitor reload, which copies it over a working copy holding no draft, so a
 * working copy still to come would read as changed and the whole rewrite would be put back. A rewritten working copy
 * then has its baseline recorded again, or the next {@code .binds} apply would refuse it as a conflict.
 * <p>
 * A hardware-id move is built from the values {@link BindsDeviceReferences} found, so a reference written in either
 * order or case is moved as written; a rename matches its old name ignoring case. All or nothing across the files, as
 * {@link BindingsWriter} promises.
 */
final class BindsDeviceRewrite implements DeviceApply.BindsRewrite {

    private static final Logger log = LogManager.getLogger(BindsDeviceRewrite.class);
    private static final String BINDS_SUFFIX = ".binds";

    private final Path bindingsFolder;
    private final BindingsWorkingCopyRepository workingCopies;
    private final BindingsWriter writer;
    private final Path backupDirectory;

    BindsDeviceRewrite(Path bindingsFolder, BindingsWorkingCopyRepository workingCopies, BindingsWriter writer,
                           Path backupDirectory) {
        this.bindingsFolder = bindingsFolder;
        this.workingCopies = workingCopies;
        this.writer = writer;
        this.backupDirectory = backupDirectory;
    }

    /** The user's bindings folder, Elite-Intel's working copies, and the {@code .binds} backup folder. */
    static BindsDeviceRewrite stored(Path bindingsFolder) throws IOException {
        return new BindsDeviceRewrite(bindingsFolder, new BindingsWorkingCopyRepository(), new BindingsWriter(),
                AppPaths.getBindingsBackupDir());
    }

    @Override
    public Map<String, Integer> hardwareIdCounts() throws IOException {
        return BindsDeviceReferences.hardwareIdCounts(bindingsFolder);
    }

    @Override
    public Map<String, Integer> nameCounts() throws IOException {
        return BindsDeviceReferences.referenceCounts(bindingsFolder);
    }

    @Override
    public DeviceApply.Rewritten rewrite(DeviceReferenceRewrite rewrite) {
        List<Path> gameFiles;
        try {
            gameFiles = bindsFiles();
        } catch (IOException e) {
            log.warn("Could not list the .binds files to move {} to {}: {}", rewrite.from(), rewrite.to(), e.getMessage());
            return new DeviceApply.Rewritten(DeviceRewriteResult.Status.READ_FAILED, 0);
        }
        List<Path> files = new ArrayList<>();
        List<WorkingCopy> copies = new ArrayList<>();
        for (Path game : gameFiles) {
            String preset = game.getFileName().toString();
            if (workingCopies.exists(preset)) {
                Path copy = workingCopies.getWorkingCopyPath(preset);
                files.add(copy);
                copies.add(new WorkingCopy(preset, copy, game));
            }
        }
        files.addAll(gameFiles);

        DeviceRewriteResult result = writer.rewriteDeviceReferences(files, rewrite, backupDirectory);
        if (result.status() != DeviceRewriteResult.Status.REWRITTEN) {
            return new DeviceApply.Rewritten(result.status(), 0);
        }
        recordBaselines(copies, result);
        return new DeviceApply.Rewritten(result.status(), inGameFiles(gameFiles, result));
    }

    /** The references the rewrite moved in the game's own files - each working copy holds the same ones again. */
    private static int inGameFiles(List<Path> gameFiles, DeviceRewriteResult result) {
        Set<Path> game = new HashSet<>();
        for (Path file : gameFiles) game.add(realOrAbsolute(file));
        return result.references().entrySet().stream()
                .filter(entry -> game.contains(realOrAbsolute(entry.getKey())))
                .mapToInt(Map.Entry::getValue)
                .sum();
    }

    private static Path realOrAbsolute(Path file) {
        try {
            return file.toRealPath();
        } catch (IOException e) {
            return file.toAbsolutePath().normalize();
        }
    }

    // WHY: only the working copies the rewrite touched. One it did not touch kept its own baseline, and recording
    // a new one would hide a draft whose game file changed underneath it.
    private void recordBaselines(List<WorkingCopy> copies, DeviceRewriteResult result) {
        Set<Path> rewritten = Set.copyOf(result.references().keySet());
        for (WorkingCopy copy : copies) {
            try {
                if (!rewritten.contains(copy.path().toRealPath())) continue;
                workingCopies.recordGameFileBaseline(copy.preset(), copy.game());
            } catch (IOException e) {
                log.warn("Moved the bindings in {}, but could not record its baseline - its next apply may be "
                        + "refused as a conflict: {}", copy.preset(), e.getMessage());
            }
        }
    }

    private List<Path> bindsFiles() throws IOException {
        try (Stream<Path> files = Files.list(bindingsFolder)) {
            return files.filter(Files::isRegularFile)
                    .filter(file -> file.getFileName().toString().endsWith(BINDS_SUFFIX))
                    .sorted()
                    .toList();
        }
    }

    private record WorkingCopy(String preset, Path path, Path game) {
    }
}
