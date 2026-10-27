package elite.intel.bindforge.storage;

import elite.intel.eventbus.UiBus;
import elite.intel.ui.event.BindForgeEditsChangedEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Consumer;

/**
 * The edits not applied: the working copy with every change the user has made on BindForge's screens, held in memory
 * and shared by the tabs until APPLY writes them or DISCARD throws them away (principles.md, <em>Words</em>; storage.md
 * rules 6-7; Storage S4a). Nothing here writes a file.
 * <p>
 * <strong>Default labels are held apart</strong> (alias-designer.md, <em>Default labels</em>; Alan, 2026-10-24): the
 * {@code .buttonMap}s BindForge works out for a connected, named controller that has none are written at the next
 * APPLY, but they are not the user's edits, so they never light the APPLY / DISCARD bar on their own.
 * <p>
 * <strong>The working copy is read again whenever there are no edits</strong>, so what the screens show is never older
 * than the last write. Every write through {@link WritePath} - APPLY, first-time startup, RESTORE - puts its set here,
 * which drops the edits: applied, they are the working copy; restored over, they are thrown away (storage.md rule 10).
 * <p>
 * One lock for everything; changes publish {@link BindForgeEditsChangedEvent} on the calling thread, after the lock is
 * released. Touches the disk when it reads the working copy - not for the EDT.
 */
public final class Edits {

    private static final Logger log = LogManager.getLogger(Edits.class);

    private static final Edits STORED = new Edits(() -> WorkingCopy.stored().read(),
            () -> UiBus.publish(new BindForgeEditsChangedEvent()));

    /** Reads the working copy. */
    @FunctionalInterface
    interface Source {
        ConfigurationFiles read() throws IOException;
    }

    /** One change to the edits: the current set in, the changed set out. */
    @FunctionalInterface
    public interface Edit {
        ConfigurationFiles apply(ConfigurationFiles current) throws IOException;
    }

    /** Thrown by {@link #change} while an APPLY or another write is under way - its set would drop the change. */
    public static final class WriteRunningException extends IllegalStateException {
        WriteRunningException() {
            super("your control files are being written - try again when it has finished");
        }
    }

    private final Source source;
    private final Runnable changed;

    private ConfigurationFiles applied;
    private ConfigurationFiles edited;
    private Map<String, byte[]> defaults = Map.of();
    private boolean writing;

    /** The edits every BindForge screen shares. */
    public static Edits stored() {
        return STORED;
    }

    Edits(Source source, Runnable changed) {
        this.source = source;
        this.changed = changed;
    }

    /**
     * The working copy with the edits on it - read again first when there are none.
     *
     * @throws java.nio.file.NoSuchFileException if there is no working copy yet - first-time startup makes it
     * @throws IOException                        if it cannot be read
     */
    public ConfigurationFiles current() throws IOException {
        synchronized (this) {
            if (edited != null && !hasEditsLocked()) {
                applied = null;
                edited = null;
            }
            if (edited == null) {
                applied = source.read();
                edited = applied;
            }
            return edited;
        }
    }

    /** The working copy the edits started from - what the last write left. Read as {@link #current()} reads it. */
    public ConfigurationFiles applied() throws IOException {
        synchronized (this) {
            current();
            return applied;
        }
    }

    /** Whether there are edits not applied - default labels alone are not. */
    public synchronized boolean hasEdits() {
        return hasEditsLocked();
    }

    private boolean hasEditsLocked() {
        return edited != null && applied != null && !edited.sameAs(applied);
    }

    /**
     * Changes the edits.
     *
     * @throws WriteRunningException while a write runs
     * @throws IOException           if the working copy cannot be read, or the edit cannot be made - nothing changes
     */
    public void change(Edit edit) throws IOException {
        synchronized (this) {
            if (writing) throw new WriteRunningException();
            edited = Objects.requireNonNull(edit.apply(current()), "edit");
        }
        log.info("BindForge edits changed; edits not applied: {}", hasEdits());
        changed.run();
    }

    /** DISCARD: every edit thrown away. Default labels stay - they are not the user's. */
    public void discard() {
        synchronized (this) {
            edited = applied;
        }
        log.info("BindForge edits discarded");
        changed.run();
    }

    /**
     * The default labels, by {@code .buttonMap} file name - replaced whole by the screen that works them out. A map
     * the edits hold under the same name, in any spelling, wins.
     */
    public void defaultLabels(Map<String, byte[]> maps) {
        synchronized (this) {
            defaults = Map.copyOf(maps);
        }
    }

    /** What APPLY writes: the edits, with the default labels for every device that has no {@code .buttonMap}. */
    public ConfigurationFiles toApply() throws IOException {
        synchronized (this) {
            ConfigurationFiles files = current();
            Map<String, byte[]> maps = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            maps.putAll(files.buttonMaps());
            defaults.forEach(maps::putIfAbsent);
            return files.withButtonMaps(new TreeMap<>(maps));
        }
    }

    /**
     * Runs a write - APPLY's - with no change let in meanwhile, so nothing the user does while it runs is dropped by
     * the set it leaves.
     */
    public <T> T whileWriting(WriteStep<T> write) throws IOException {
        synchronized (this) {
            if (writing) throw new WriteRunningException();
            writing = true;
        }
        changed.run();
        try {
            return write.run();
        } finally {
            synchronized (this) {
                writing = false;
            }
            changed.run();
        }
    }

    /** Whether a write is running - the bar stays dimmed meanwhile. */
    public synchronized boolean writing() {
        return writing;
    }

    /** A write run by {@link #whileWriting}. */
    @FunctionalInterface
    public interface WriteStep<T> {
        T run() throws IOException;
    }

    /**
     * A write put {@code files} in the working copy: they are what the edits now start from, and any edit is gone -
     * applied, or thrown away by RESTORE or first-time startup. Called by {@link WritePath}.
     */
    void written(ConfigurationFiles files) {
        synchronized (this) {
            applied = files;
            edited = files;
            defaults = Map.of();
        }
        changed.run();
    }

    /** {@link #written} as {@link WritePath} takes it. */
    static Consumer<ConfigurationFiles> onWritten() {
        return STORED::written;
    }
}
