package elite.intel.bindforge.devicefiles;

import elite.intel.bindforge.devicefiles.DeviceFilesPush.Target;
import elite.intel.bindforge.devicefiles.LabelMerge.Collision;
import elite.intel.bindforge.install.GameInstallation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The re-merge reads the master and each drifted installation as the sides of one label merge (Alan, 2026-10-08). */
class LabelReMergeTest {

    @TempDir
    Path root;

    @Test
    void theMasterIsOneSideAndEachInstallationAnother() throws IOException {
        Target steam = installation(1, "Steam");
        Target epic = installation(2, "Epic");
        writeButtonMap(steam, Map.of("Joy_1", "TRIGGER", "Joy_3", "HAT"));
        writeButtonMap(epic, Map.of("Joy_1", "LV TRIGGER"));

        LabelMerge merge = LabelReMerge.read("LVWAP", Map.of("Joy_1", "LV TRIGGER", "Joy_2", "PINKY"), List.of(steam, epic));

        assertEquals(List.of(LabelReMerge.MASTER, "1", "2"), merge.sides());
        Collision joy1 = merge.collisions().getFirst();
        assertEquals(1, merge.collisions().size());
        assertEquals("Joy_1", joy1.input());
        assertEquals(List.of("LV TRIGGER", "TRIGGER"), joy1.choices(), "the master and Epic agree: one choice");
        assertEquals(Map.of("Joy_1", "TRIGGER", "Joy_2", "PINKY", "Joy_3", "HAT"),
                merge.merged(Map.of("Joy_1", "TRIGGER")),
                "a label only one side holds is kept - the master's Joy_2 and Steam's Joy_3");
    }

    @Test
    void anInstallationWithNoButtonMapOrNoFolderGivesNoSide() throws IOException {
        Target steam = installation(1, "Steam");
        Target gone = new Target(3, "FRONTIER", root.resolve("Gone").resolve("ControlSchemes"), false);

        LabelMerge merge = LabelReMerge.read("LVWAP", Map.of("Joy_1", "LV TRIGGER"), List.of(steam, gone));

        assertEquals(List.of(LabelReMerge.MASTER), merge.sides());
        assertTrue(merge.collisions().isEmpty());
    }

    /** Merging without it could keep the master's label over one the user never saw. */
    @Test
    void aButtonMapThatCannotBeReadStopsTheReMergeAndSaysWhich() throws IOException {
        Target steam = installation(1, "Steam");
        Path file = GameInstallation.buttonMapIn(steam.controlSchemes(), "LVWAP");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "<Root><Joy_1>TRIGGER", StandardCharsets.UTF_8);

        IOException e = assertThrows(IOException.class,
                () -> LabelReMerge.read("LVWAP", Map.of("Joy_1", "LV TRIGGER"), List.of(steam)));
        assertTrue(e.getMessage().contains("STEAM"), e.getMessage());
    }

    private Target installation(long id, String folder) throws IOException {
        Path controlSchemes = root.resolve(folder).resolve("ControlSchemes");
        Files.createDirectories(controlSchemes);
        return new Target(id, folder.toUpperCase(), controlSchemes, false);
    }

    /** LVWAP's {@code .buttonMap} in the installation. */
    private static void writeButtonMap(Target target, Map<String, String> labels) throws IOException {
        Path file = GameInstallation.buttonMapIn(target.controlSchemes(), "LVWAP");
        Files.createDirectories(file.getParent());
        Files.write(file, ButtonMapWriter.write(labels));
    }
}
