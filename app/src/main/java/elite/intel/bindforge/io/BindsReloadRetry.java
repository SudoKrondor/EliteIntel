package elite.intel.bindforge.io;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.time.Duration;

/**
 * The bindings watcher's reload after a {@code .binds} changed: wait for the game to finish writing, work out which
 * file to read, and - when that cannot be worked out - try again in each quiet second, up to a limit.
 * <p>
 * WHY a lookup that fails is no reason to stop: the game saves {@code StartPreset.4.start} by emptying it and writing
 * it again, so a read at the moment of the change finds no preset and {@link BindingsLoader#bindsFileToRead()} throws.
 * That exception used to end the monitor thread, and the assistant then pressed the keys of the old file until
 * Elite-Intel was restarted (seen 2026-11-11, on a save from the game's controls screen).
 * <p>
 * One per watch loop, used on its thread only.
 */
final class BindsReloadRetry {

    private static final Logger log = LogManager.getLogger(BindsReloadRetry.class);

    /** Works out which binds file to read. */
    @FunctionalInterface
    interface Lookup {
        File bindsFileToRead() throws IOException;
    }

    /** Reads {@code file} if it needs reading; {@code changed} is the {@code .binds} that changed, or null on a retry. */
    @FunctionalInterface
    interface Reload {
        void reload(File file, String changed);
    }

    private final Lookup lookup;
    private final Reload reload;
    private final int retries;
    private final Duration settle;
    /** How many more quiet seconds a reload that could not find its file is tried again in; 0 when none is owed. */
    private int retriesLeft;

    /**
     * @param retries how many quiet seconds a failed lookup is tried again in
     * @param settle  how long to wait for the game to finish writing before each lookup
     */
    BindsReloadRetry(Lookup lookup, Reload reload, int retries, Duration settle) {
        this.lookup = lookup;
        this.reload = reload;
        this.retries = retries;
        this.settle = settle;
    }

    /** A {@code .binds} in the folder changed: reload now, or owe the retries when the file cannot be worked out. */
    void changed(String name) throws InterruptedException {
        retriesLeft = attempt(name) ? 0 : retries;
    }

    /**
     * A second passed with no folder event: one retry, if one is owed.
     *
     * @return whether a retry read the file just now - the caller then takes in its conflicts
     */
    boolean quietSecond() throws InterruptedException {
        if (retriesLeft == 0) return false;
        if (attempt(null)) {
            retriesLeft = 0;
            return true;
        }
        if (--retriesLeft == 0) {
            log.error("Gave up working out which binds file to read after a change; the bindings in use are the last "
                    + "ones read, until the bindings folder changes again");
        }
        return false;
    }

    private boolean attempt(String changed) throws InterruptedException {
        Thread.sleep(settle.toMillis());
        File file;
        try {
            file = lookup.bindsFileToRead();
        } catch (IOException e) {
            log.warn("Could not work out which binds file to read after a change in the bindings folder; "
                    + "trying again: {}", e.getMessage());
            return false;
        }
        reload.reload(file, changed);
        return true;
    }
}
