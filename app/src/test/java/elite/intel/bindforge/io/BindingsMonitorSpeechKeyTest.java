package elite.intel.bindforge.io;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * What the bindings monitor tells the commander when it cannot work out which binds file to read. None of it sends
 * them to the folder setting: by the time the monitor reads, the folder is the one they chose (Krondor, 2026-11-16).
 */
class BindingsMonitorSpeechKeyTest {

    @Test
    void aStockPresetWithNoInstallationAsksForTheInstallation() {
        assertEquals(Optional.of("speech.warning.bindingsStockPresetNoInstallation"),
                keyFor(BindsFileNotFoundException.Reason.NO_INSTALLATION));
    }

    @Test
    void aPresetWhoseFileIsGoneSaysSo() {
        assertEquals(Optional.of("speech.warning.bindingsPresetFileMissing"),
                keyFor(BindsFileNotFoundException.Reason.PRESET_FILE_MISSING));
    }

    /** The folder is SetupCheck's and the folder field's to speak about, and the game empties the file as it saves. */
    @Test
    void noPresetFileIsLeftToTheFolderChecks() {
        assertEquals(Optional.empty(), keyFor(BindsFileNotFoundException.Reason.NO_PRESET_FILE));
    }

    @Test
    void aFolderThatCannotBeListedIsInaccessible() {
        assertEquals(Optional.of("speech.warning.bindingsDirectoryInaccessible"),
                BindingsMonitor.speechKeyFor(new IOException("denied")));
    }

    private static Optional<String> keyFor(BindsFileNotFoundException.Reason reason) {
        return BindingsMonitor.speechKeyFor(new BindsFileNotFoundException(reason, "test"));
    }
}
