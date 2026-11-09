package elite.intel.bindforge.storage;

import elite.intel.bindforge.install.Storefront;
import elite.intel.bindforge.preseteditor.BindNameEntry;
import elite.intel.db.dao.BindForgeInstallationsDao.InstallationRow;
import elite.intel.db.managers.BindForgeInstallationsManager;
import elite.intel.session.PlayerSession;
import elite.intel.util.StringUtls;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The notice when Elite-Intel starts (Storage S14c; principle 13): what BindForge already detects about the user's
 * files, one line each, in a fixed order, spoken and logged once a run. Nothing is blocked and nothing is asked - no
 * dialog.
 * <ol>
 *     <li><strong>Read only</strong> - an installation is missing, named by its store alone, never its path, which is
 *     no use spoken (Alan, 2026-11-06). The bar and the log keep the path.</li>
 *     <li><strong>Changed outside BindForge</strong>, or a newer game version ({@link StartupCheck}) - <strong>not
 *     while read only</strong>: the read-only line covers it, and the check it points to is off (Alan, 2026-11-06).</li>
 *     <li><strong>A binds file named like a factory one</strong> ({@link BindNameEntry#shadowsFactory()}) - the working
 *     copy's, once one exists, judged against the reachable installations' factory files.</li>
 *     <li><strong>Files the check will move to Leftover User Files</strong> ({@link Leftovers}; Storage S13b, S13d) -
 *     every one, not only plain binds files, but a read-only one, which the check leaves where it is and names once in
 *     its report (Alan, 2026-11-08, Q4 and the S13aR-S13b review's D1). Once a working copy exists, and <strong>not
 *     while read only</strong>, since the check that moves them is off then (Alan, 2026-11-07, Q5).</li>
 * </ol>
 * The missing controller stays the Alias Designer's ({@code MissingControllerWarning}, A13), and the binding warnings
 * stay {@code KeyBindCheck}'s, said before this notice (Alan, 2026-11-06).
 */
public final class StartupNotice {

    private static final Logger log = LogManager.getLogger(StartupNotice.class);

    static final String READ_ONLY_KEY = "speech.bindforge.readOnly";
    static final String CHANGED_OUTSIDE_KEY = "speech.bindforge.changedOutside";
    static final String NAMED_LIKE_FACTORY_KEY = "speech.bindforge.namedLikeFactory";
    static final String NAMED_LIKE_FACTORY_MANY_KEY = "speech.bindforge.namedLikeFactoryMany";
    static final String MANUAL_INSTALLATION_KEY = "speech.bindforge.manualInstallation";
    static final String TO_MOVE_KEY = "speech.bindforge.leftover";
    static final String TO_MOVE_MANY_KEY = "speech.bindforge.leftoverMany";

    private static final AtomicBoolean STARTED = new AtomicBoolean();

    /** One line of the notice. */
    public sealed interface Line {
        /** BindForge is read only; {@code missing} are the installations that make it so. */
        record ReadOnly(List<InstallationRow> missing) implements Line {
            public ReadOnly {
                missing = List.copyOf(missing);
            }
        }

        /** A live file differs from the working copy, or the game brought a newer version. */
        record ChangedOutside() implements Line {
        }

        /** The working copy's binds files whose BindName is a factory one's. */
        record NamedLikeFactory(List<String> bindNames) implements Line {
            public NamedLikeFactory {
                bindNames = List.copyOf(bindNames);
            }
        }

        /** The files in the bindings folder the check will move to Leftover User Files, by name (Storage S13d). */
        record ToMove(List<String> files) implements Line {
            public ToMove {
                files = List.copyOf(files);
            }
        }
    }

    /** The BindNames of the working copy's binds files named like a factory one. */
    @FunctionalInterface
    interface FactoryNamed {
        List<String> bindNames() throws IOException;
    }

    /** The files the check will move from the bindings folder, once a working copy exists; none before. */
    @FunctionalInterface
    interface ToMoveNamed {
        List<String> files() throws IOException;
    }

    private final Supplier<List<InstallationRow>> readOnly;
    private final BooleanSupplier changedOutside;
    private final FactoryNamed factoryNamed;
    private final ToMoveNamed toMoveNamed;

    /** The stored read-only state, the check at start, the working copy, the installations and the bindings folder. */
    public static StartupNotice stored() {
        return new StartupNotice(
                () -> ReadOnlyMode.stored().active() ? ReadOnlyMode.stored().missing() : List.of(),
                () -> StartupCheck.stored().changedOutside(),
                StartupNotice::storedFactoryNamed, StartupNotice::storedToMove);
    }

    StartupNotice(Supplier<List<InstallationRow>> readOnly, BooleanSupplier changedOutside, FactoryNamed factoryNamed,
                  ToMoveNamed toMoveNamed) {
        this.readOnly = readOnly;
        this.changedOutside = changedOutside;
        this.factoryNamed = factoryNamed;
        this.toMoveNamed = toMoveNamed;
    }

    /**
     * Works out the lines on a thread of its own and hands each one's text to {@code say}, in order. Only the first call
     * in a run does anything - the services can be stopped and started again, and the notice is said once.
     */
    public void runInBackground(Consumer<String> say) {
        if (!STARTED.compareAndSet(false, true)) return;
        sayOn(say, task -> Thread.ofPlatform().name("BindForge-StartupNotice").start(task));
    }

    /** {@link #runInBackground}, on the executor given, with no once-a-run guard. */
    void sayOn(Consumer<String> say, Executor on) {
        on.execute(() -> {
            for (Line line : lines()) say.accept(text(line));
        });
    }

    /** The notice's lines, in order; empty when there is nothing to say. On the caller's thread; touches the disk. */
    List<Line> lines() {
        List<Line> lines = new ArrayList<>();
        List<InstallationRow> missing = readOnly.get();
        if (!missing.isEmpty()) {
            lines.add(new Line.ReadOnly(missing));
        } else if (changedOutside.getAsBoolean()) {
            lines.add(new Line.ChangedOutside());
        }
        try {
            List<String> named = factoryNamed.bindNames();
            if (!named.isEmpty()) {
                log.info("Binds files named like a factory one: {}", named);
                lines.add(new Line.NamedLikeFactory(named));
            }
        } catch (IOException | RuntimeException e) {
            // WHY: broad on purpose - a thread of its own at startup, and a hint only; the Bind Editor warns of it too.
            log.warn("Could not work out whether a binds file is named like a factory one", e);
        }
        // WHY: not while read only - the check that moves them is off, as the changed-outside line's is (Alan,
        // 2026-11-07, Storage S13b Q5).
        if (missing.isEmpty()) {
            try {
                List<String> toMove = toMoveNamed.files();
                if (!toMove.isEmpty()) {
                    log.info("Files the check will move from the bindings folder to Leftover User Files: {}", toMove);
                    lines.add(new Line.ToMove(toMove));
                }
            } catch (IOException | RuntimeException e) {
                // WHY: broad on purpose, as above - a hint only; the check names the files too.
                log.warn("Could not look for files that do not belong in the bindings folder", e);
            }
        }
        return lines;
    }

    /** What is said and logged for {@code line}, in the user's language. */
    static String text(Line line) {
        return switch (line) {
            case Line.ReadOnly readOnly -> StringUtls.localizedSpeech(READ_ONLY_KEY, String.join(", ",
                    readOnly.missing().stream().map(row -> store(row.storefront())).distinct().toList()));
            case Line.ChangedOutside ignored -> StringUtls.localizedSpeech(CHANGED_OUTSIDE_KEY);
            case Line.NamedLikeFactory named -> named.bindNames().size() == 1
                    ? StringUtls.localizedSpeech(NAMED_LIKE_FACTORY_KEY, named.bindNames().getFirst())
                    : StringUtls.localizedSpeech(NAMED_LIKE_FACTORY_MANY_KEY, String.join(", ", named.bindNames()));
            case Line.ToMove toMove -> toMove.files().size() == 1
                    ? StringUtls.localizedSpeech(TO_MOVE_KEY, toMove.files().getFirst())
                    : StringUtls.localizedSpeech(TO_MOVE_MANY_KEY, String.join(", ", toMove.files()));
        };
    }

    /**
     * An installation's store as it is said: Steam, Epic, Frontier or Oculus - names, not translated - and <em>a manual
     * installation</em> for one added by hand (Alan, 2026-11-06).
     */
    static String store(String storefront) {
        Storefront store;
        try {
            store = Storefront.valueOf(storefront);
        } catch (IllegalArgumentException e) {
            // WHY: rows are written from the enum, so this is a fault - but one bad row must not silence the read-only
            // warning, which is the reason the notice speaks at all (Storage S13aR, D3). Said as it is stored.
            log.warn("Unknown store '{}' on a stored installation; said as it is stored", storefront);
            return storefront;
        }
        if (store == Storefront.MANUAL) return StringUtls.localizedSpeech(MANUAL_INSTALLATION_KEY);
        String name = store.name().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    private static List<String> storedFactoryNamed() throws IOException {
        WorkingCopy workingCopy = WorkingCopy.stored();
        // WHY: before first-time startup there is no set of the user's to judge, and the live files are not BindForge's.
        if (!workingCopy.exists()) return List.of();
        return factoryNamed(Apply.offered(workingCopy.read(), BindForgeInstallationsManager.getInstance().findAll())
                .list().entries());
    }

    private static List<String> storedToMove() throws IOException {
        // WHY: before first-time startup the live files are not BindForge's, as above.
        if (!WorkingCopy.stored().exists()) return List.of();
        Path folder = PlayerSession.getInstance().getBindingsDir();
        if (folder == null) return List.of();
        return movable(folder, Leftovers.read(folder).files());
    }

    /**
     * {@code names} in {@code folder} but those the check would leave where they are: read-only, or whether they are
     * cannot be read (Alan, 2026-11-08, from the S13aR-S13b review's D1) - named once in the check's report, not at
     * every start.
     */
    static List<String> movable(Path folder, List<String> names) {
        return names.stream().filter(name -> {
            try {
                return !ReadOnlyFlag.isSet(folder.resolve(name));
            } catch (IOException e) {
                log.warn("Could not read whether {} is read-only, so it is left out of the notice", name, e);
                return false;
            }
        }).toList();
    }

    /** The BindNames among {@code entries} that are the user's and named like a factory one. */
    static List<String> factoryNamed(List<BindNameEntry> entries) {
        return entries.stream().filter(BindNameEntry::shadowsFactory).map(BindNameEntry::bindName).toList();
    }
}
