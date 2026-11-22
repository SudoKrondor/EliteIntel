package elite.intel.bindforge.io;

import java.io.IOException;
import java.util.Objects;

/**
 * {@link BindingsLoader#bindsFileToRead} found no binds file to read, and why - so the commander is told what is
 * actually wrong rather than one sentence for every cause.
 * <p>
 * WHY a reason at all: "check the bindings directory" is right only for {@link Reason#NO_PRESET_FILE}. A fresh install
 * on a stock preset has the right folder and no {@code .binds} in it, and telling that commander to check the folder
 * sent them hunting for another one (Krondor, 2026-11-16).
 */
public final class BindsFileNotFoundException extends IOException {

    /** Why no file could be read. */
    public enum Reason {
        /**
         * The folder has no {@code StartPreset.*.start}, or one naming no preset. The game writes that file the first
         * time it runs, so this is the wrong folder - or the game has not run yet, or is rewriting the file right now.
         */
        NO_PRESET_FILE,
        /**
         * The presets named have no binds file in the bindings folder, and no installation is known to read the game's
         * own files from: a stock preset, with nowhere to read it.
         */
        NO_INSTALLATION,
        /**
         * The presets named have no binds file in the bindings folder or in any reachable installation: a custom
         * preset whose file is gone.
         */
        PRESET_FILE_MISSING
    }

    private final Reason reason;

    BindsFileNotFoundException(Reason reason, String message) {
        super(message);
        this.reason = Objects.requireNonNull(reason, "reason");
    }

    public Reason reason() {
        return reason;
    }
}
