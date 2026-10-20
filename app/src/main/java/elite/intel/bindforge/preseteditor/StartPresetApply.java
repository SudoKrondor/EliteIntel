package elite.intel.bindforge.preseteditor;

import elite.intel.bindforge.io.BindingsLoader;
import elite.intel.bindforge.io.StartPresetSections;
import elite.intel.bindforge.model.BindingSection;
import elite.intel.db.managers.BindForgeSettingsManager;
import elite.intel.io.AtomicFiles;
import elite.intel.io.TimestampedBackups;
import elite.intel.util.AppPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.IntSupplier;

/**
 * Apply for the preset file: the draft written to the game's {@code StartPreset.4.start} (Alan, 2026-10-20, P3).
 * <ol>
 *   <li><strong>Merged per line.</strong> A line the draft changed takes the draft's value; any other line keeps the
 *       game's, so a change made in the game to another line survives. A line both changed differently is a
 *       conflict, and nothing is written.</li>
 *   <li><strong>Every line must name a binds file.</strong> A BindName no binds file answers to - an empty line
 *       included - is refused on all four lines, not only the changed ones: the game loads something else without a
 *       word, and the user finds out in the cockpit.</li>
 *   <li><strong>The replaced file is kept in Edit History</strong>, beside the {@code .binds} copies, and pruned to the
 *       user's retention. No copy, no write.</li>
 *   <li><strong>Written through {@link AtomicFiles}</strong> in the game's own form: four lines, CRLF between them,
 *       no newline after the last.</li>
 *   <li><strong>The written file becomes the master</strong> and the draft is dropped.</li>
 * </ol>
 * Values the game's file already holds are not written; the game's file becomes the master as it is.
 */
public class StartPresetApply {

    private static final Logger log = LogManager.getLogger(StartPresetApply.class);

    private final StartPresetStore store;
    private final BindingsLoader bindingsLoader;
    private final BindNameCatalog catalog;
    private final TimestampedBackups backups;
    private final Path historyDir;
    private final IntSupplier retention;

    StartPresetApply(StartPresetStore store, BindingsLoader bindingsLoader, BindNameCatalog catalog,
                     TimestampedBackups backups, Path historyDir, IntSupplier retention) {
        this.store = store;
        this.bindingsLoader = bindingsLoader;
        this.catalog = catalog;
        this.backups = backups;
        this.historyDir = historyDir;
        this.retention = retention;
    }

    /**
     * Apply over the default store, with Edit History in {@link AppPaths#getBindingsBackupDir()} - where the
     * {@code .binds} Apply keeps its copies - and the user's retention.
     */
    public static StartPresetApply atDefaultLocation() throws IOException {
        BindingsLoader bindingsLoader = new BindingsLoader();
        return new StartPresetApply(StartPresetStore.atDefaultLocation(), bindingsLoader,
                new BindNameCatalog(bindingsLoader), new TimestampedBackups(), AppPaths.getBindingsBackupDir(),
                BindForgeSettingsManager.getInstance()::getEditHistoryRetention);
    }

    /**
     * What one Apply did, or why it did nothing.
     */
    public sealed interface Outcome {
    }

    /**
     * The game's file was written.
     *
     * @param gameFile the file written
     * @param kept     the copy of what it held before, in Edit History
     * @param written  the four values now in it
     */
    public record Applied(Path gameFile, Path kept, StartPresetSections written) implements Outcome {
    }

    /**
     * The game's file already held what Apply would write. Nothing was written; it is now the master, and the draft
     * is gone.
     */
    public record AlreadyInGame(Path gameFile) implements Outcome {
    }

    /**
     * No saved draft differs from the master. Nothing was touched - a change in the game alone is P5's to adopt or
     * revert, never Apply's.
     */
    public record NothingToApply() implements Outcome {
    }

    /**
     * Refused: these sections would name a BindName no binds file answers to, or nothing at all.
     *
     * @param sections in file order
     * @param values   the BindName each would have held, in the same order; empty for an empty line
     */
    public record UnknownBindNames(List<BindingSection> sections, List<String> values) implements Outcome {
        public UnknownBindNames {
            sections = List.copyOf(sections);
            values = List.copyOf(values);
        }
    }

    /**
     * Refused: the game and the draft changed these lines to different values.
     */
    public record Conflicts(List<StartPresetComparison.Line> lines) implements Outcome {
        public Conflicts {
            lines = List.copyOf(lines);
        }
    }

    /**
     * Refused: there is a draft but no master to tell its changes by. Only something outside BindForge leaves one.
     */
    public record NoMaster() implements Outcome {
    }

    /**
     * Refused: the game has no preset file. That is a wipe, P5's to judge; Apply only ever replaces a file.
     */
    public record NoGameFile() implements Outcome {
    }

    /**
     * Applies the draft.
     *
     * @param bindingsDir           the player's bindings folder
     * @param controlSchemesFolders each installation's {@code ControlSchemes}, for the factory binds files -
     *                              {@link BindNameCatalog#controlSchemesOf}
     * @throws IOException when a read fails, or the Edit History copy, the write or the promote fails. Nothing is
     *                     written to the game's file unless its copy was kept first; a failure after the write
     *                     leaves the draft standing against the old master, still saved work.
     */
    public Outcome apply(Path bindingsDir, Collection<Path> controlSchemesFolders) throws IOException {
        Objects.requireNonNull(bindingsDir, "bindingsDir");
        Objects.requireNonNull(controlSchemesFolders, "controlSchemesFolders");
        if (!store.hasDraftChanges()) {
            return new NothingToApply();
        }
        Optional<StartPresetSections> master = store.master();
        if (master.isEmpty()) {
            log.warn("StartPreset.4.start draft with no master; not applied");
            return new NoMaster();
        }
        Optional<Path> gameFile = bindingsLoader.findLiveStartPresetFile(bindingsDir);
        if (gameFile.isEmpty()) {
            log.warn("No preset file in {} to apply the StartPreset.4.start draft to", bindingsDir);
            return new NoGameFile();
        }
        StartPresetSections game = bindingsLoader.readStartPresetSections(bindingsDir).orElseThrow(
                () -> new IOException("Preset file " + gameFile.get() + " went away while it was being read"));
        StartPresetSections draft = store.draft().orElseThrow(
                () -> new IOException("StartPreset.4.start draft went away while it was being applied"));

        StartPresetComparison comparison = StartPresetComparison.of(master.get(), draft, game);
        if (!comparison.conflicts().isEmpty()) {
            log.info("StartPreset.4.start not applied; the game and the draft both changed {}", comparison.conflicts());
            return new Conflicts(comparison.conflicts());
        }
        StartPresetSections merged = merged(comparison);

        Optional<UnknownBindNames> unknown = unknownBindNames(merged, catalog.list(bindingsDir, controlSchemesFolders));
        if (unknown.isPresent()) {
            log.info("StartPreset.4.start not applied; no binds file answers to {}", unknown.get());
            return unknown.get();
        }

        if (merged.equals(game)) {
            store.promote(gameFile.get());
            log.info("{} already holds the draft's values; nothing written", gameFile.get());
            return new AlreadyInGame(gameFile.get());
        }

        Path kept = backups.create(gameFile.get(), historyDir);
        backups.prune(historyDir, gameFile.get().getFileName().toString(), retention.getAsInt());
        AtomicFiles.write(gameFile.get(), gameForm(merged).getBytes(StandardCharsets.UTF_8));
        log.info("Applied {} to {}; the replaced file is kept as {}", merged, gameFile.get(), kept);
        store.promote(gameFile.get());
        return new Applied(gameFile.get(), kept, merged);
    }

    /**
     * The file's text as the game writes it (Alan, 2026-10-20, P3): always four lines, even when all four match and
     * whatever form the file was in before - the single-line form is pre-Odyssey, and only the Live game is
     * supported - with CRLF between them and nothing after the last, which is the specimen byte for byte.
     */
    static String gameForm(StartPresetSections sections) {
        return String.join("\r\n", StartPresetLines.values(sections));
    }

    // WHY: a line the draft did not move takes the game's value, never the master's - so a change made in the game
    // to another line is kept rather than written back over (the four lines are independent entities).
    private static StartPresetSections merged(StartPresetComparison comparison) {
        List<String> values = comparison.lines().stream()
                .map(line -> line.draft().equals(line.master()) ? line.game() : line.draft())
                .toList();
        return new StartPresetSections(values.get(0), values.get(1), values.get(2), values.get(3), false);
    }

    private static Optional<UnknownBindNames> unknownBindNames(StartPresetSections sections, BindNameList bindNames) {
        List<BindingSection> unknown = StartPresetLines.SECTIONS.stream()
                .filter(section -> {
                    String value = sections.forSection(section);
                    return value.isEmpty() || bindNames.find(value).isEmpty();
                })
                .toList();
        if (unknown.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new UnknownBindNames(unknown, unknown.stream().map(sections::forSection).toList()));
    }
}
