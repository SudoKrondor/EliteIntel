package elite.intel.bindforge.devicefiles;

import elite.intel.bindforge.devicefiles.ButtonMapGeneration.Controller;
import elite.intel.bindforge.devicefiles.ButtonMapGeneration.Outcome;
import elite.intel.bindforge.devicefiles.ButtonMapGeneration.Result;
import elite.intel.bindforge.devicefiles.DeviceFilesPush.Target;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The startup {@code .buttonMap}: created for a connected controller an installation already names, never over
 * a file that exists, never under a name nobody gave.
 */
class ButtonMapGenerationTest {

    private static final String DEVICE_MAPPINGS = """
            <?xml version="1.0" encoding="UTF-8" ?>
            <Root>
            \t<LVWAP><PID>83F4</PID><VID>3344</VID></LVWAP>
            \t<GamePad>
            \t\t<PID>028E</PID><VID>045E</VID>
            \t\t<Alternative><PID>02FF</PID><VID>045E</VID></Alternative>
            \t</GamePad>
            \t<T-Rudder><PID>B679</PID><VID>044F</VID></T-Rudder>
            \t<VPCPanel><PID>0259</PID><VID>3344</VID></VPCPanel>
            \t<SaitekX52><PID>075C</PID><VID>06A3</VID><SupportsIcons>SaitekX52</SupportsIcons>
            \t\t<Alternative><PID>0255</PID><VID>06A3</VID><SupportsIcons>SaitekX52</SupportsIcons></Alternative>
            \t</SaitekX52>
            \t<RVWAP>
            \t\t<PID>03F5</PID><VID>3344</VID>
            \t\t<Alternative><PID>43F4</PID><VID>3344</VID></Alternative>
            \t</RVWAP>
            </Root>
            """;

    private static final Controller T_RUDDER = new Controller("044F", "B679", 0, 3);

    @TempDir
    Path temp;

    @Test
    void aNamedControllerWithNoFileGetsOneInEveryInstallation() throws IOException {
        Target steam = installation(1, "STEAM", DEVICE_MAPPINGS);
        Target epic = installation(2, "EPIC", DEVICE_MAPPINGS);

        List<Result> results = generate(List.of(steam, epic), Set.of(), T_RUDDER);

        for (Result result : results) assertEquals(Outcome.CREATED, result.outcome());
        for (Target target : List.of(steam, epic)) {
            String written = read(buttonMap(target, "T-Rudder"));
            assertTrue(written.contains("<Joy_XAxis>X Axis</Joy_XAxis>"), written);
            assertTrue(written.contains("<Joy_RZAxis>RZ Axis</Joy_RZAxis>"), written);
        }
    }

    /** Frontier's own map, or one the user wrote, stays exactly as it is. */
    @Test
    void anExistingFileIsNeverTouched() throws IOException {
        Target steam = installation(1, "STEAM", DEVICE_MAPPINGS);
        Path frontiers = buttonMap(steam, "VPCPanel");
        Files.createDirectories(frontiers.getParent());
        Files.writeString(frontiers, "<Root><Joy_UAxis>A1</Joy_UAxis></Root>");

        List<Result> results = generate(List.of(steam), Set.of(), new Controller("3344", "0259", 41, 2));

        assertEquals(Outcome.ALREADY_THERE, results.getFirst().outcome());
        assertEquals("<Root><Joy_UAxis>A1</Joy_UAxis></Root>", read(frontiers));
    }

    /** Startup never names anything: a controller with no entry waits for onboarding. */
    @Test
    void aControllerWithNoEntryGetsNothing() throws IOException {
        Target steam = installation(1, "STEAM", DEVICE_MAPPINGS);

        List<Result> results = generate(List.of(steam), Set.of(), new Controller("1234", "5678", 12, 2));

        assertEquals(Outcome.NOT_NAMED, results.getFirst().outcome());
        assertFalse(Files.exists(steam.controlSchemes().resolve("DeviceButtonMaps")));
    }

    /** A stick matched through an Alternative pair goes by that element's name; hex case does not matter. */
    @Test
    void anAlternativePairNamesTheController() throws IOException {
        Target steam = installation(1, "STEAM", DEVICE_MAPPINGS);

        List<Result> results = generate(List.of(steam), Set.of(), new Controller("3344", "43f4", 24, 3));

        assertEquals("RVWAP", results.getFirst().deviceName());
        assertTrue(Files.isRegularFile(buttonMap(steam, "RVWAP")));
    }

    /** A device the game shows with its own icons keeps them: Button 3 would put text where an icon was. */
    @Test
    void aDeviceWithSupportsIconsGetsNothingEvenThroughAnAlternative() throws IOException {
        Target steam = installation(1, "STEAM", DEVICE_MAPPINGS);

        List<Result> results = generate(List.of(steam), Set.of(), new Controller("06A3", "0255", 30, 7));

        assertEquals(Outcome.GAME_DRAWS_IT, results.getFirst().outcome());
        assertFalse(Files.exists(buttonMap(steam, "SaitekX52")));
    }

    /** A pad on GamePad is bound by the game's own GamePad_ tokens, so Joy_ labels would never show. */
    @Test
    void aGamePadGetsNothing() throws IOException {
        Target steam = installation(1, "STEAM", DEVICE_MAPPINGS);

        List<Result> results = generate(List.of(steam), Set.of(), new Controller("045e", "02ff", 10, 6));

        assertEquals(Outcome.GAME_DRAWS_IT, results.getFirst().outcome());
        assertEquals("GamePad", results.getFirst().deviceName());
        assertFalse(Files.exists(buttonMap(steam, "GamePad")));
    }

    /** The first element claiming the VID/PID is the one the game uses (domain doc §1.2d). */
    @Test
    void theFirstMatchingElementGivesTheName() throws IOException {
        String shadowed = DEVICE_MAPPINGS.replace("<Root>",
                "<Root>\n\t<Pedals><PID>B679</PID><VID>044F</VID></Pedals>");
        Target steam = installation(1, "STEAM", shadowed);

        List<Result> results = generate(List.of(steam), Set.of(), T_RUDDER);

        assertEquals("Pedals", results.getFirst().deviceName());
        assertFalse(Files.exists(buttonMap(steam, "T-Rudder")));
    }

    /** Until first setup, installations can name a controller differently; each gets its own name's file. */
    @Test
    void eachInstallationUsesItsOwnName() throws IOException {
        Target steam = installation(1, "STEAM", DEVICE_MAPPINGS);
        Target epic = installation(2, "EPIC", DEVICE_MAPPINGS.replace("LVWAP", "LeftStick"));

        generate(List.of(steam, epic), Set.of(), new Controller("3344", "83F4", 30, 3));

        assertTrue(Files.isRegularFile(buttonMap(steam, "LVWAP")));
        assertTrue(Files.isRegularFile(buttonMap(epic, "LeftStick")));
    }

    /** A missing file for a device the master labels is drift for the startup check, not a gap to fill. */
    @Test
    void aDeviceTheMasterLabelsIsLeftToApply() throws IOException {
        Target steam = installation(1, "STEAM", DEVICE_MAPPINGS);

        List<Result> results = generate(List.of(steam), Set.of("lvwap"), new Controller("3344", "83F4", 30, 3));

        assertEquals(Outcome.MASTER_HAS_LABELS, results.getFirst().outcome());
        assertEquals(buttonMap(steam, "LVWAP"), results.getFirst().file());
        assertFalse(Files.exists(buttonMap(steam, "LVWAP")));
    }

    @Test
    void aMissingInstallationIsSkippedWhileTheOthersAreWritten() throws IOException {
        Target steam = installation(1, "STEAM", DEVICE_MAPPINGS);
        Target gone = new Target(2, "EPIC", temp.resolve("unplugged").resolve("ControlSchemes"), false);

        List<Result> results = generate(List.of(gone, steam), Set.of(), T_RUDDER);

        assertEquals(Outcome.SKIPPED_MISSING, results.get(0).outcome());
        assertEquals(Outcome.CREATED, results.get(1).outcome());
    }

    @Test
    void anUnreadableDeviceMappingsFailsThatInstallationOnly() throws IOException {
        Target broken = installation(1, "STEAM", "<Root><LVWAP>");
        Target epic = installation(2, "EPIC", DEVICE_MAPPINGS);

        List<Result> results = generate(List.of(broken, epic), Set.of(), T_RUDDER);

        assertEquals(Outcome.FAILED, results.get(0).outcome());
        assertEquals(Outcome.CREATED, results.get(1).outcome());
    }

    @Test
    void aControllerReportingNothingGetsNoFile() throws IOException {
        Target steam = installation(1, "STEAM", DEVICE_MAPPINGS);

        List<Result> results = generate(List.of(steam), Set.of(), new Controller("044F", "B679", 0, 0));

        assertEquals(Outcome.NOTHING_TO_LABEL, results.getFirst().outcome());
        assertFalse(Files.exists(buttonMap(steam, "T-Rudder")));
    }

    /** The write itself refuses to replace, so a file that appears while it runs is kept. */
    @Test
    void theCreateOnlyWriteNeverReplacesAndLeavesNoTempFile() throws IOException {
        Path target = temp.resolve("DeviceButtonMaps").resolve("Mine.buttonMap");

        assertTrue(ButtonMapGeneration.createNew(target, "first".getBytes(StandardCharsets.UTF_8)));
        assertFalse(ButtonMapGeneration.createNew(target, "second".getBytes(StandardCharsets.UTF_8)));

        assertEquals("first", read(target));
        try (Stream<Path> files = Files.list(target.getParent())) {
            assertEquals(List.of(target), files.toList());
        }
    }

    private static List<Result> generate(List<Target> targets, Set<String> masterLabelled, Controller controller) {
        return new ButtonMapGeneration(() -> targets, () -> masterLabelled).generateFor(controller);
    }

    private Target installation(long id, String storefront, String deviceMappings) throws IOException {
        Path controlSchemes = temp.resolve(storefront).resolve("ControlSchemes");
        Files.createDirectories(controlSchemes);
        Files.writeString(controlSchemes.resolve("DeviceMappings.xml"), deviceMappings);
        return new Target(id, storefront, controlSchemes, false);
    }

    private static Path buttonMap(Target target, String name) {
        return target.controlSchemes().resolve("DeviceButtonMaps").resolve(name + ".buttonMap");
    }

    private static String read(Path file) throws IOException {
        return Files.readString(file, StandardCharsets.UTF_8);
    }
}
