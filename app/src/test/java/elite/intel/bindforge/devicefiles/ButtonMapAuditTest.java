package elite.intel.bindforge.devicefiles;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A {@code .buttonMap} is joined to its device by its filename and nothing else, so the audit is really one
 * question asked per file: does an entry of that name exist?
 */
class ButtonMapAuditTest {

    private static final DeviceEntry.HardwareId ANY = new DeviceEntry.HardwareId("3344", "83F4");

    @TempDir
    Path install;

    private Path deviceButtonMaps;

    @BeforeEach
    void layOutTheInstall() throws IOException {
        deviceButtonMaps = Files.createDirectories(install.resolve("DeviceButtonMaps"));
    }

    @Test
    void aFileNamedForAnEntryIsAttachedToIt() throws IOException {
        Path file = buttonMap("VPCPanel");

        ButtonMapAudit.Result result = ButtonMapAudit.audit(List.of(entry("VPCPanel")), deviceButtonMaps);

        assertEquals(List.of(new ButtonMapAudit.Attachment("VPCPanel", file)), result.attached());
        assertTrue(result.orphaned().isEmpty());
        assertTrue(result.isClean());
    }

    /**
     * The recorded case from the developer's Epic install: a {@code .buttonMap} left behind by an earlier
     * attempt, with no entry behind it. The game never reads it, so its labels are never shown.
     */
    @Test
    void aFileNamedForNoEntryIsAnOrphan() throws IOException {
        buttonMap("RVWAP");
        Path orphan = buttonMap("LVWAP");

        ButtonMapAudit.Result result = ButtonMapAudit.audit(List.of(entry("RVWAP")), deviceButtonMaps);

        assertEquals(List.of(orphan), result.orphaned());
        assertEquals(1, result.attached().size());
        assertFalse(result.isClean());
    }

    /**
     * An entry with no button map is the normal case, not a fault - Frontier ships button maps for only a
     * couple of its fifty-one devices.
     */
    @Test
    void anEntryWithNoFileIsReportedWithoutBeingAProblem() throws IOException {
        ButtonMapAudit.Result result =
                ButtonMapAudit.audit(List.of(entry("VPCPanel"), entry("GamePad")), deviceButtonMaps);

        assertEquals(List.of("VPCPanel", "GamePad"), result.entriesWithoutMap());
        assertTrue(result.isClean(), "nothing is orphaned, so there is nothing wrong here");
    }

    /**
     * Horizons ships no DeviceButtonMaps folder at all, and neither does an installation nobody has labelled
     * anything in.
     */
    @Test
    void aMissingFolderIsNoFilesRatherThanAnError() throws IOException {
        ButtonMapAudit.Result result =
                ButtonMapAudit.audit(List.of(entry("VPCPanel")), install.resolve("DeviceButtonMaps"));

        assertTrue(result.attached().isEmpty());
        assertTrue(result.orphaned().isEmpty());
        assertEquals(List.of("VPCPanel"), result.entriesWithoutMap());
    }

    /**
     * Windows cannot hold two files whose names differ only in case, so the match must not depend on it -
     * otherwise the one file present would be read as an orphan.
     */
    @Test
    void theMatchDoesNotDependOnCase() throws IOException {
        buttonMap("vpcpanel");

        ButtonMapAudit.Result result = ButtonMapAudit.audit(List.of(entry("VPCPanel")), deviceButtonMaps);

        assertTrue(result.orphaned().isEmpty());
        assertEquals("VPCPanel", result.attached().get(0).deviceName(),
                "reported under the entry's spelling, not the file's");
        assertTrue(result.entriesWithoutMap().isEmpty());
    }

    @Test
    void filesThatAreNotButtonMapsAreIgnored() throws IOException {
        buttonMap("VPCPanel");
        Files.writeString(deviceButtonMaps.resolve("notes.txt"), "left here by somebody");

        ButtonMapAudit.Result result = ButtonMapAudit.audit(List.of(entry("VPCPanel")), deviceButtonMaps);

        assertTrue(result.isClean());
        assertEquals(1, result.attached().size());
    }

    private Path buttonMap(String stem) throws IOException {
        return Files.writeString(deviceButtonMaps.resolve(stem + ".buttonMap"), "<Root></Root>");
    }

    private DeviceEntry entry(String name) {
        return new DeviceEntry(name, Set.of(ANY));
    }
}
