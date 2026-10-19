package elite.intel.bindforge.presets;

import elite.intel.bindforge.io.BindingsLoader;
import elite.intel.bindforge.io.StartPresetSections;
import elite.intel.io.AtomicFiles;
import elite.intel.util.AppPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/**
 * The {@code StartPreset} master and draft, in Elite-Intel's own folder (Alan, 2026-10-18, P2).
 * <ul>
 *   <li><strong>The master</strong> is {@code elite-intel/bindings/master/StartPreset.start}: a byte copy of the
 *       game's live file as it was last known good. It is created from the game's file the first time it is
 *       asked for and none exists, and after that only Apply, adopt or revert change it.</li>
 *   <li><strong>The draft</strong> is {@code elite-intel/bindings/draft/StartPreset.start}: what Save wrote,
 *       always four lines, one per section. No file means no draft - the draft is the master.</li>
 * </ul>
 * Nothing here touches the game's folder; writing the game's file is Apply's job.
 */
public class StartPresetStore {

    private static final Logger log = LogManager.getLogger(StartPresetStore.class);

    static final String FILE_NAME = "StartPreset.start";

    private final Path masterFile;
    private final Path draftFile;
    private final BindingsLoader bindingsLoader;

    StartPresetStore(Path bindingsWorkingDir, BindingsLoader bindingsLoader) {
        this.masterFile = bindingsWorkingDir.resolve("master").resolve(FILE_NAME);
        this.draftFile = bindingsWorkingDir.resolve("draft").resolve(FILE_NAME);
        this.bindingsLoader = bindingsLoader;
    }

    /**
     * The store under {@link AppPaths#getBindingsWorkingDir()}.
     */
    public static StartPresetStore atDefaultLocation() throws IOException {
        return new StartPresetStore(AppPaths.getBindingsWorkingDir(), new BindingsLoader());
    }

    /**
     * The master, empty when none has been made yet.
     */
    public Optional<StartPresetSections> master() throws IOException {
        return read(masterFile);
    }

    /**
     * The master, made from the game's live file first when there is none.
     * <p>
     * WHY: without a master the draft and the game's file have nothing to be compared against, and only Elite-Intel's
     * own folder is written. A file wiped before the first read becomes the master - there is no earlier state to
     * know it by.
     *
     * @param bindingsDir the player's bindings folder
     * @return the master, empty only when there is none and the game has no {@code StartPreset} either
     */
    public Optional<StartPresetSections> masterOrSeedFrom(Path bindingsDir) throws IOException {
        Optional<StartPresetSections> existing = master();
        if (existing.isPresent()) {
            return existing;
        }
        Optional<Path> live = bindingsLoader.findLiveStartPresetFile(bindingsDir);
        if (live.isEmpty()) {
            log.info("No StartPreset master, and none in {} to make one from", bindingsDir);
            return Optional.empty();
        }
        Files.createDirectories(masterFile.getParent());
        AtomicFiles.copy(live.get(), masterFile);
        log.info("StartPreset master made from {}", live.get());
        return master();
    }

    /**
     * The draft, empty when there is none.
     */
    public Optional<StartPresetSections> draft() throws IOException {
        return read(draftFile);
    }

    /**
     * What the screen starts from: the draft when there is one, else the master.
     */
    public Optional<StartPresetSections> draftOrMaster() throws IOException {
        Optional<StartPresetSections> draft = draft();
        return draft.isPresent() ? draft : master();
    }

    /**
     * Save: the four values become the draft. Values the same as the master's, line for line, leave no draft at
     * all, so a draft that exists always holds a change.
     *
     * @throws IllegalArgumentException when a value has a line break or surrounding spaces, which would not read
     *                                  back as written
     * @throws IllegalStateException    when there is no master to save against
     */
    public void save(StartPresetSections sections) throws IOException {
        Objects.requireNonNull(sections, "sections");
        for (String value : StartPresetLines.values(sections)) {
            if (!value.equals(value.trim()) || value.contains("\n") || value.contains("\r")) {
                throw new IllegalArgumentException("Not a StartPreset line: '" + value + "'");
            }
        }
        StartPresetSections master = master().orElseThrow(
                () -> new IllegalStateException("No StartPreset master to save a draft against"));
        if (StartPresetLines.same(sections, master)) {
            dropDraft();
            return;
        }
        Files.createDirectories(draftFile.getParent());
        AtomicFiles.write(draftFile, fourLines(sections).getBytes(StandardCharsets.UTF_8));
        log.info("StartPreset draft saved: {}", sections);
    }

    /**
     * Drops the draft, so the draft is the master again. Nothing to drop is not an error.
     */
    public void dropDraft() throws IOException {
        if (Files.deleteIfExists(draftFile)) {
            log.info("StartPreset draft dropped");
        }
    }

    /**
     * Whether a saved draft differs from the master - saved, not yet applied. This is not unsaved work: the
     * screen's dropdowns hold that.
     */
    public boolean hasDraftChanges() throws IOException {
        Optional<StartPresetSections> draft = draft();
        if (draft.isEmpty()) {
            return false;
        }
        Optional<StartPresetSections> master = master();
        return master.isEmpty() || !StartPresetLines.same(draft.get(), master.get());
    }

    private static Optional<StartPresetSections> read(Path file) throws IOException {
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        return Optional.of(StartPresetSections.parse(Files.readString(file, StandardCharsets.UTF_8)));
    }

    // WHY: four lines even when all four match, so a draft never reads back as the single-line form; the form the
    // game's file gets is Apply's decision, not the draft's.
    private static String fourLines(StartPresetSections sections) {
        return sections.general() + "\n" + sections.ship() + "\n" + sections.srv() + "\n" + sections.onFoot() + "\n";
    }
}
