package elite.intel.bindforge.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static elite.intel.bindforge.storage.BindingsComparisonTest.binds;
import static elite.intel.bindforge.storage.WorkingCopyTest.bytes;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The check when Elite-Intel starts (storage.md rule 15; Storage S8a): reading only, it says something only when
 * first-time startup is complete and a live binds or preset file differs from the working copy - and stays quiet when
 * it cannot tell.
 */
class StartupCheckTest {

    private static final String CUSTOM = "Custom.4.2.binds";
    private static final ConfigurationFiles WORKING_COPY = new ConfigurationFiles(
            Map.of(CUSTOM, bytes(binds("Custom", "Key_A", "Joy_XAxis", ""))), null, Map.of());

    @TempDir
    Path bindings;

    @Test
    void aFileChangedOutsideBindForgeIsSaid() throws IOException {
        Files.writeString(bindings.resolve(CUSTOM), binds("Custom", "Key_Q", "Joy_XAxis", ""));

        assertTrue(new StartupCheck(() -> true, () -> WORKING_COPY, () -> bindings).changedOutside());
    }

    @Test
    void filesAsBindForgeLastWroteThemSayNothing() throws IOException {
        Files.writeString(bindings.resolve(CUSTOM), binds("Custom", "Key_A", "Joy_XAxis", "").replace("\r\n", "\n"));

        assertFalse(new StartupCheck(() -> true, () -> WORKING_COPY, () -> bindings).changedOutside());
    }

    /** Before first-time startup there is no working copy to compare with - nothing is read. */
    @Test
    void beforeFirstTimeStartupNothingIsRead() {
        assertFalse(new StartupCheck(() -> false, () -> {
            throw new AssertionError("the working copy is not read before first-time startup");
        }, () -> bindings).changedOutside());
    }

    /** A hint only: a working copy or bindings folder that cannot be read says nothing - the first click tells. */
    @Test
    void whatCannotBeReadSaysNothing() {
        assertFalse(new StartupCheck(() -> true, () -> {
            throw new IOException("locked");
        }, () -> bindings).changedOutside());
        assertFalse(new StartupCheck(() -> true, () -> WORKING_COPY, () -> bindings.resolve("gone")).changedOutside());
    }
}
