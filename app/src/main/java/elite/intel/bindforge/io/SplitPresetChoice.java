package elite.intel.bindforge.io;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Which binds file the user chose for Elite-Intel's assistant to read when the four lines of {@code StartPreset.4.start}
 * name different ones (Storage S13f; storage.md, <em>Settled with Storage S13f's sources</em>). Held for this run only:
 * the question is asked again at the first BindForge click of every run while the lines differ, so nothing is stored
 * (Krondor, 2026-11-10).
 * <p>
 * {@link BindingsLoader} reads it, and only while it is still one of the lines.
 */
public final class SplitPresetChoice {

    private static final AtomicReference<String> CHOSEN = new AtomicReference<>();

    private SplitPresetChoice() {
    }

    /** The BindName chosen this run, if any. */
    public static Optional<String> chosen() {
        return Optional.ofNullable(CHOSEN.get());
    }

    /** The user chose {@code bindName}. */
    public static void choose(String bindName) {
        CHOSEN.set(Objects.requireNonNull(bindName, "bindName"));
    }

    /** No choice - for tests, which share the run. */
    static void forget() {
        CHOSEN.set(null);
    }
}
