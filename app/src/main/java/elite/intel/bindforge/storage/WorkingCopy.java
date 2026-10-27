package elite.intel.bindforge.storage;

import elite.intel.bindforge.install.GameInstallation;
import elite.intel.io.AtomicFiles;
import elite.intel.util.AppPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * The working copy: the game input configuration files in Elite-Intel's own folder, the source of truth once first
 * setup is complete (principle 7; storage.md rule 4). Laid out like the game's, so a user browsing to it recognises
 * what they see (overview.md, <em>Where the working copy lives</em>):
 * <pre>
 * {@link AppPaths#getBindingsWorkingDir()}\
 *   Bindings\                        every .binds and StartPreset.N.start
 *   ControlSchemes\DeviceMappings.xml
 *   ControlSchemes\DeviceButtonMaps\ every .buttonMap
 * </pre>
 * <strong>Read whole and written whole, in one step</strong> (storage.md, <em>How the files are written</em>,
 * 2026-10-24): the new set is written into a folder beside it, marked complete, then swapped in. A write
 * that stops before the mark leaves the old set; one that stops after it is finished by the next read or write. So
 * the working copy is never half one set and half another.
 * <p>
 * Only APPLY, first setup, RESTORE and the check's reconcile write it (principles.md, <em>Words</em>), each through
 * {@link WritePath}. Anything else in the folder - the Bind Editor's own copies until Storage S5 - is left alone.
 * Touches the disk - not for the EDT.
 */
public final class WorkingCopy {

    private static final Logger log = LogManager.getLogger(WorkingCopy.class);

    static final String BINDINGS = "Bindings";
    static final String CONTROL_SCHEMES = "ControlSchemes";
    /** The two folders a set is made of, swapped in this order. */
    private static final List<String> FOLDERS = List.of(BINDINGS, CONTROL_SCHEMES);

    /** Where the next set is written before it is swapped in. */
    static final String NEXT = ".working-copy-next";
    /** Where the set being replaced is put while the next one is swapped in. */
    static final String PREVIOUS = ".working-copy-previous";
    /** Written last into {@link #NEXT}: from then on the write counts as made, and is finished if it stops. */
    static final String COMPLETE = "complete";

    /** One write at a time, and no read while one runs - every store over one folder shares it. */
    private static final Object LOCK = new Object();

    private final Path root;

    /** The working copy in {@link AppPaths#getBindingsWorkingDir()}. */
    public static WorkingCopy stored() throws IOException {
        return new WorkingCopy(AppPaths.getBindingsWorkingDir());
    }

    WorkingCopy(Path root) {
        this.root = root;
    }

    /**
     * Whether there is a working copy - none before first setup has created it.
     *
     * @throws IOException if a write that stopped part way cannot be finished
     */
    public boolean exists() throws IOException {
        synchronized (LOCK) {
            finishInterruptedWrite();
            return Files.isDirectory(root.resolve(BINDINGS));
        }
    }

    /**
     * Reads the whole working copy. Files of any other kind in its folders are not part of it, and not read.
     *
     * @throws NoSuchFileException if there is no working copy
     * @throws IOException         if any of its files cannot be read - never a partial set, which could lack the one
     *                             file that mattered
     */
    public ConfigurationFiles read() throws IOException {
        synchronized (LOCK) {
            finishInterruptedWrite();
            Path bindings = root.resolve(BINDINGS);
            if (!Files.isDirectory(bindings)) throw new NoSuchFileException(bindings.toString(), null, "no working copy");
            Path controlSchemes = root.resolve(CONTROL_SCHEMES);
            Path deviceMappings = GameInstallation.deviceMappingsIn(controlSchemes);
            return new ConfigurationFiles(
                    readFiles(bindings, Backups::isBindingsFile),
                    Files.isRegularFile(deviceMappings) ? Files.readAllBytes(deviceMappings) : null,
                    readFiles(GameInstallation.deviceButtonMapsIn(controlSchemes),
                            file -> file.getFileName().toString().endsWith(GameInstallation.BUTTON_MAP_SUFFIX)));
        }
    }

    /**
     * Replaces the whole working copy with {@code files}, in one step. A file the new set lacks is gone afterwards.
     *
     * @throws IOException if it cannot be written. Thrown before the new set is marked complete, the working copy is
     *                     as it was; thrown after, the next read or write finishes the swap
     */
    public void write(ConfigurationFiles files) throws IOException {
        synchronized (LOCK) {
            finishInterruptedWrite();
            Path next = root.resolve(NEXT);
            try {
                writeSet(next, files);
                AtomicFiles.write(next.resolve(COMPLETE), new byte[0]);
            } catch (IOException e) {
                deleteQuietly(next);
                throw e;
            }
            finishInterruptedWrite();
            log.info("Working copy written: {} binds and preset files, {}, {} button maps", files.bindings().size(),
                    files.hasDeviceMappings() ? "DeviceMappings.xml" : "no DeviceMappings.xml",
                    files.buttonMaps().size());
        }
    }

    private static void writeSet(Path folder, ConfigurationFiles files) throws IOException {
        deleteTree(folder);
        Path bindings = folder.resolve(BINDINGS);
        Path controlSchemes = folder.resolve(CONTROL_SCHEMES);
        Path buttonMaps = GameInstallation.deviceButtonMapsIn(controlSchemes);
        // WHY: both folders always, even empty - an empty folder in the new set still replaces the old one's files.
        Files.createDirectories(bindings);
        Files.createDirectories(buttonMaps);
        // WHY: AtomicFiles for its flush, so the set marked complete is on disk before it is swapped in.
        for (Map.Entry<String, byte[]> file : files.bindings().entrySet()) {
            AtomicFiles.write(bindings.resolve(file.getKey()), file.getValue());
        }
        if (files.hasDeviceMappings()) {
            AtomicFiles.write(GameInstallation.deviceMappingsIn(controlSchemes), files.deviceMappings());
        }
        for (Map.Entry<String, byte[]> file : files.buttonMaps().entrySet()) {
            AtomicFiles.write(buttonMaps.resolve(file.getKey()), file.getValue());
        }
    }

    /**
     * Ends whatever a stopped write left: a set marked complete is swapped in, folder by folder, from wherever it
     * stopped; one not marked complete is thrown away. Idempotent, and nothing to do after a write that finished.
     */
    private void finishInterruptedWrite() throws IOException {
        Path next = root.resolve(NEXT);
        Path previous = root.resolve(PREVIOUS);
        if (Files.isRegularFile(next.resolve(COMPLETE))) {
            for (String folder : FOLDERS) {
                swapIn(next.resolve(folder), root.resolve(folder), previous.resolve(folder));
            }
        } else if (Files.exists(next)) {
            log.warn("A working copy write stopped before it was complete; the working copy is as it was before it");
        }
        deleteTree(next);
        deleteTree(previous);
    }

    /** Moves {@code incoming} to {@code current}, putting what is there aside first. Nothing to do once it is in. */
    private static void swapIn(Path incoming, Path current, Path aside) throws IOException {
        if (!Files.exists(incoming)) return;
        if (Files.exists(current)) {
            deleteTree(aside);
            Files.createDirectories(aside.getParent());
            Files.move(current, aside);
        }
        Files.move(incoming, current);
    }

    private static Map<String, byte[]> readFiles(Path folder, Predicate<Path> belongs)
            throws IOException {
        Map<String, byte[]> files = new HashMap<>();
        if (!Files.isDirectory(folder)) return files;
        List<Path> found;
        try (Stream<Path> listing = Files.list(folder)) {
            found = listing.filter(Files::isRegularFile).filter(belongs).toList();
        }
        for (Path file : found) {
            files.put(file.getFileName().toString(), Files.readAllBytes(file));
        }
        return files;
    }

    private static void deleteTree(Path folder) throws IOException {
        if (!Files.exists(folder)) return;
        List<Path> deepestFirst;
        try (Stream<Path> walk = Files.walk(folder)) {
            deepestFirst = walk.sorted(Comparator.reverseOrder()).toList();
        }
        for (Path path : deepestFirst) {
            Files.delete(path);
        }
    }

    private static void deleteQuietly(Path folder) {
        try {
            deleteTree(folder);
        } catch (IOException e) {
            // WHY: the write has already failed and says so; the half-written folder is not marked complete, so the
            // next read or write throws it away.
            log.warn("Could not remove the half-written working copy at {}", folder, e);
        }
    }
}
