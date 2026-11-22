package elite.intel.ui.screen.bindings;

import elite.intel.bindforge.devices.DeviceNames;
import elite.intel.bindforge.storage.Rename;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * File Manager's RENAME judges a typed name as S12's Name box did (file-manager.md, <em>Game Install Locations</em>, R4;
 * Storage S16): the controller name's rule against the set's own BindNames loosely, and a factory BindName whatever its
 * case.
 */
class BindsFileSectionTest {

    private static final String FILE = "Custom.4.2.binds";
    private static final Rename.Names NAMES = new Rename.Names(List.of("KeyboardMouseOnly", "SaitekX56"),
            List.of("Custom", "SrvOnly"), List.of("Custom", "SrvOnly"));

    @Test
    void aNewNameNothingClashesWithIsAccepted() {
        assertTrue(BindsFileSection.refusal(FILE, "DualVirpil", NAMES).isEmpty());
    }

    @Test
    void theFilesOwnNameIsNotRefused() {
        assertTrue(BindsFileSection.refusal(FILE, "Custom", NAMES).isEmpty());
    }

    @Test
    void aNameTooShortIsRefused() {
        assertEquals(DeviceNames.Problem.TOO_SHORT, problem("ab"));
    }

    @Test
    void aNameWithAPeriodIsRefused() {
        assertEquals(DeviceNames.Problem.INVALID_CHARACTER, problem("My.Binds"));
    }

    @Test
    void anotherFileOfTheSetIsRefusedLoosely() {
        Optional<DeviceNames.Verdict> refusal = BindsFileSection.refusal(FILE, "srv_only", NAMES);

        assertEquals(DeviceNames.Problem.CLASHES, refusal.orElseThrow().problem());
        assertEquals("SrvOnly", refusal.orElseThrow().clashesWith());
    }

    @Test
    void aRenameChangingOnlyCaseIsRefused() {
        assertEquals(DeviceNames.Problem.CLASHES, problem("custom"));
    }

    @Test
    void aFactoryBindNameIsRefusedWhateverItsCase() {
        Optional<DeviceNames.Verdict> refusal = BindsFileSection.refusal(FILE, "keyboardmouseonly", NAMES);

        assertEquals(DeviceNames.Problem.CLASHES, refusal.orElseThrow().problem());
        assertEquals("KeyboardMouseOnly", refusal.orElseThrow().clashesWith());
    }

    @Test
    void aFactoryBindNameWithPunctuationAddedIsAccepted() {
        assertTrue(BindsFileSection.refusal(FILE, "Keyboard_Mouse_Only", NAMES).isEmpty());
    }

    @Test
    void aRenameBackToTheWorkingCopysNameIsAccepted() {
        Rename.Names renamedOnce = new Rename.Names(List.of(), List.of("Mine", "SrvOnly"), List.of("Custom", "SrvOnly"));

        assertTrue(BindsFileSection.refusal("Mine.4.2.binds", "Custom", renamedOnce).isEmpty());
    }

    /** The problem {@code typed} is refused for, renaming {@link #FILE} - {@code null} when it is accepted. */
    private static DeviceNames.Problem problem(String typed) {
        return BindsFileSection.refusal(FILE, typed, NAMES).map(DeviceNames.Verdict::problem).orElse(null);
    }
}
