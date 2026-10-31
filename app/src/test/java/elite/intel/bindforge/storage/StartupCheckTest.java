package elite.intel.bindforge.storage;

import elite.intel.db.dao.BindForgeInstallationsDao.InstallationRow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static elite.intel.bindforge.storage.BindingsComparisonTest.binds;
import static elite.intel.bindforge.storage.WorkingCopyTest.bytes;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The check when Elite-Intel starts (storage.md rule 15; Storage S8a): reading only, it says something only when
 * first-time startup is complete and a live binds or preset file, or an installation's device files, differ from the
 * working copy - and stays quiet when it cannot tell.
 */
class StartupCheckTest {

    private static final String CUSTOM = "Custom.4.2.binds";
    private static final ConfigurationFiles WORKING_COPY = new ConfigurationFiles(
            Map.of(CUSTOM, bytes(binds("Custom", "Key_A", "Joy_XAxis", ""))), null, Map.of());

    /** Every installation matching the working copy. */
    private static final DeviceCheck.Source NONE_DIFFER = files -> new DeviceCheck.Found(List.of());

    @TempDir
    Path bindings;

    @Test
    void aFileChangedOutsideBindForgeIsSaid() throws IOException {
        Files.writeString(bindings.resolve(CUSTOM), binds("Custom", "Key_Q", "Joy_XAxis", ""));

        assertTrue(new StartupCheck(() -> true, () -> WORKING_COPY, () -> bindings, NONE_DIFFER).changedOutside());
    }

    @Test
    void filesAsBindForgeLastWroteThemSayNothing() throws IOException {
        Files.writeString(bindings.resolve(CUSTOM), binds("Custom", "Key_A", "Joy_XAxis", "").replace("\r\n", "\n"));

        assertFalse(new StartupCheck(() -> true, () -> WORKING_COPY, () -> bindings, NONE_DIFFER).changedOutside());
    }

    /** An installation's device files changed outside BindForge are said too (Storage S9a). */
    @Test
    void anInstallationChangedOutsideBindForgeIsSaid() throws IOException {
        Files.writeString(bindings.resolve(CUSTOM), binds("Custom", "Key_A", "Joy_XAxis", ""));

        assertTrue(new StartupCheck(() -> true, () -> WORKING_COPY, () -> bindings,
                files -> new DeviceCheck.Found(List.of(steam(DeviceCheck.State.RESET_MISSING)))).changedOutside());
    }

    /** One that cannot be read at all is logged, not said - the first click names it. */
    @Test
    void anInstallationThatCannotBeReadSaysNothing() throws IOException {
        Files.writeString(bindings.resolve(CUSTOM), binds("Custom", "Key_A", "Joy_XAxis", ""));

        assertFalse(new StartupCheck(() -> true, () -> WORKING_COPY, () -> bindings,
                files -> new DeviceCheck.Found(List.of(steam(DeviceCheck.State.UNREADABLE)))).changedOutside());
    }

    /** Before first-time startup there is no working copy to compare with - nothing is read. */
    @Test
    void beforeFirstTimeStartupNothingIsRead() {
        assertFalse(new StartupCheck(() -> false, () -> {
            throw new AssertionError("the working copy is not read before first-time startup");
        }, () -> bindings, NONE_DIFFER).changedOutside());
    }

    /** A hint only: a working copy or bindings folder that cannot be read says nothing - the first click tells. */
    @Test
    void whatCannotBeReadSaysNothing() {
        assertFalse(new StartupCheck(() -> true, () -> {
            throw new IOException("locked");
        }, () -> bindings, NONE_DIFFER).changedOutside());
        assertFalse(new StartupCheck(() -> true, () -> WORKING_COPY, () -> bindings.resolve("gone"), NONE_DIFFER)
                .changedOutside());
    }

    // ---- helpers

    private DeviceCheck.Judged steam(DeviceCheck.State state) {
        InstallationRow row = new InstallationRow(1, "STEAM", bindings.resolve("Steam").toString(), false, false);
        if (state == DeviceCheck.State.UNREADABLE) return new DeviceCheck.Judged(row, state, null, "locked");
        return new DeviceCheck.Judged(row, state, new DeviceFiles(null, Map.of()), null);
    }
}
