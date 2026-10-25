package elite.intel.bindforge.storage;

import elite.intel.bindforge.devicefiles.ButtonMapReader;
import elite.intel.bindforge.devicefiles.DeviceEntry.HardwareId;
import elite.intel.bindforge.devicefiles.DeviceMappingsParser;
import elite.intel.bindforge.devicefiles.FirstSetupPlan;
import elite.intel.bindforge.devicefiles.FrontierStockButtonMaps;
import elite.intel.bindforge.devicefiles.FrontierStockDevices;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The working copy first-time startup makes (storage.md rules 2-3; alias-designer.md, <em>How the reconciled files are
 * made</em>, Storage S2b, 2026-10-25): identical installations taken as they are, one installation used for all, or a
 * merge - one {@code DeviceMappings.xml} with the kept elements copied whole, the {@code .buttonMap}s, and the
 * bindings moved off every losing name.
 */
class FirstSetupFilesTest {

    private static final long STEAM = 1;
    private static final long EPIC = 2;
    private static final String BINDS = "Custom.4.2.binds";
    private static final String PRESET = "StartPreset.4.start";

    @Test
    void identicalInstallationsGiveTheFirstOnesFilesAndEveryBindsFileAsItIs() throws IOException {
        byte[] mappings = mappings("<RVWAP><PID>03F5</PID><VID>3344</VID></RVWAP>");
        Map<String, byte[]> maps = Map.of("RVWAP.buttonMap", map("<Joy_1>TRIGGER</Joy_1>"));
        FirstSetupReading reading = reading(binds("RVWAP"),
                installation(STEAM, "STEAM", mappings, maps), installation(EPIC, "EPIC", mappings.clone(), maps));

        assertTrue(reading.identical());
        ConfigurationFiles files = FirstSetupFiles.identical(reading);

        assertArrayEquals(mappings, files.deviceMappings());
        assertEquals(List.of("RVWAP.buttonMap"), List.copyOf(files.buttonMaps().keySet()));
        assertEquals(List.of(BINDS, PRESET), List.copyOf(files.bindings().keySet()));
    }

    @Test
    void withNoInstallationTheWorkingCopyHoldsTheBindsAndPresetFilesAlone() throws IOException {
        ConfigurationFiles files = FirstSetupFiles.identical(reading(binds("RVWAP")));

        assertFalse(files.hasDeviceMappings());
        assertEquals(2, files.bindings().size());
    }

    @Test
    void usingOneInstallationTakesItsDeviceFilesWhole() throws IOException {
        byte[] steamMappings = mappings("<RVWAP><PID>03F5</PID><VID>3344</VID></RVWAP>");
        Map<String, byte[]> steamMaps = Map.of("RVWAP.buttonMap", map("<Joy_1>TRIGGER</Joy_1>"),
                "Orphan.buttonMap", map("<Joy_1>X</Joy_1>"));
        FirstSetupReading reading = reading(binds("RVWAP"),
                installation(STEAM, "STEAM", steamMappings, steamMaps),
                installation(EPIC, "EPIC", mappings("<LVWAP><PID>83F4</PID><VID>3344</VID></LVWAP>"), Map.of()));

        ConfigurationFiles files = FirstSetupFiles.useOne(reading, STEAM);

        assertArrayEquals(steamMappings, files.deviceMappings());
        assertEquals(steamMaps.keySet(), files.buttonMaps().keySet(), "whole - an orphan included");
    }

    /** Not offered, since the working copy would then hold no DeviceMappings.xml (Alan, 2026-10-25). */
    @Test
    void anInstallationWithNoDeviceMappingsCannotBeUsedForAll() throws IOException {
        FirstSetupReading reading = reading(binds("RVWAP"),
                installation(STEAM, "STEAM", mappings("<RVWAP><PID>03F5</PID><VID>3344</VID></RVWAP>"), Map.of()),
                installation(EPIC, "EPIC", null, Map.of()));

        assertThrows(IllegalArgumentException.class, () -> FirstSetupFiles.useOne(reading, EPIC));
    }

    /** "Epic has an entry Steam does not - LVWAP. 131 bindings in DualVirpilDawnTreader.4.2.binds use it." */
    @Test
    void usingOneNamesTheEntriesItRemovesThatTheBindingsUse() throws IOException {
        FirstSetupReading reading = reading(binds("LVWAP", "LVWAP", "lvwap"),
                installation(STEAM, "STEAM", mappings("<RVWAP><PID>03F5</PID><VID>3344</VID></RVWAP>"), Map.of()),
                installation(EPIC, "EPIC", mappings("<LVWAP><PID>83F4</PID><VID>3344</VID></LVWAP>"
                        + "<Spare><PID>0001</PID><VID>1234</VID></Spare>"), Map.of()));

        List<FirstSetupFiles.Loss> losses = FirstSetupFiles.lossesUsingOne(reading, STEAM);

        assertEquals(1, losses.size(), "Spare is used by no binding, so it is simply overwritten");
        FirstSetupFiles.Loss loss = losses.getFirst();
        assertEquals("LVWAP", loss.name());
        assertEquals(List.of("EPIC"), loss.holders());
        assertEquals(Map.of(BINDS, 3), loss.bindings(), "every spelling counts, as the game finds it ignoring case");
        assertTrue(FirstSetupFiles.lossesUsingOne(reading, EPIC).isEmpty(), "Steam's RVWAP is used by no binding");
    }

    /**
     * The measured outage, merged: each installation holds a stick the other lacks. Both lead the one file, each copied
     * whole from its own installation, and Frontier's entries are kept as the base file holds them.
     */
    @Test
    void aMergeTakesEveryKeptElementWholeFromItsOwnInstallation() throws IOException {
        FirstSetupReading reading = reading(binds("RVWAP"),
                installation(STEAM, "STEAM", mappings("<RVWAP><PID>03F5</PID><VID>3344</VID></RVWAP>"
                        + "<T-Rudder><PID>B679</PID><VID>044F</VID></T-Rudder>"), Map.of()),
                installation(EPIC, "EPIC", mappings("<LVWAP><PID>83F4</PID><VID>3344</VID><SupportsIcons /></LVWAP>"
                        + "<T-Rudder><PID>B679</PID><VID>044F</VID></T-Rudder>"), Map.of()));

        FirstSetupFiles.Merged merged = FirstSetupFiles.merge(reading, Map.of());

        Map<String, HardwareId> elements = DeviceMappingsParser.primaries(merged.files().deviceMappings());
        assertEquals(List.of("LVWAP", "RVWAP", "T-Rudder"), List.copyOf(elements.keySet()),
                "the user's elements first, Frontier's beneath them");
        String text = new String(merged.files().deviceMappings(), StandardCharsets.UTF_8);
        assertTrue(text.contains("SupportsIcons"), "Epic's element came with every child it has");
        assertEquals(1, count(text, "<T-Rudder"), "Frontier's entry once");
        assertEquals(0, merged.bindingsMoved());
    }

    /** A16 on the working copy: the losing name leaves the file, and every binding naming it follows the winner. */
    @Test
    void aLosingNamesBindingsMoveToTheWinnerInEveryBindsFile() throws IOException {
        FirstSetupReading reading = reading(binds("RightStick", "RightStick", "Keyboard"),
                installation(STEAM, "STEAM", mappings("<RVWAP><PID>03F5</PID><VID>3344</VID></RVWAP>"), Map.of()),
                installation(EPIC, "EPIC", mappings("<RightStick><PID>03F5</PID><VID>3344</VID></RightStick>"),
                        Map.of()));
        FirstSetupPlan.Row question = reading.plan().rows().getFirst();

        FirstSetupFiles.Merged merged = FirstSetupFiles.merge(reading, Map.of(question.id(), "RVWAP"));

        assertEquals(List.of("RVWAP"), List.copyOf(
                DeviceMappingsParser.primaries(merged.files().deviceMappings()).keySet()));
        assertEquals(Map.of("RightStick", "RVWAP"), merged.moves());
        assertEquals(2, merged.bindingsMoved());
        String binds = new String(merged.files().bindings().get(BINDS), StandardCharsets.UTF_8);
        assertFalse(binds.contains("RightStick"));
        assertEquals(2, count(binds, "Device=\"RVWAP\""));
        assertArrayEquals(reading.bindings().get(PRESET), merged.files().bindings().get(PRESET),
                "the preset file names binds files, not devices, and is kept as it is");
    }

    /**
     * Storage S2b (Alan, 2026-10-25): names differing only in case are not the same name in .binds, so every binds file
     * takes the winner's exact spelling - and a reference already spelled that way is not counted as moved.
     */
    @Test
    void aCaseOnlyConflictRewritesEveryOtherSpellingToTheWinners() throws IOException {
        FirstSetupReading reading = reading(binds("lvwap", "LVWAP"),
                installation(STEAM, "STEAM", mappings("<LVWAP><PID>83F4</PID><VID>3344</VID></LVWAP>"), Map.of()),
                installation(EPIC, "EPIC", mappings("<lvwap><PID>83F4</PID><VID>3344</VID></lvwap>"), Map.of()));
        FirstSetupPlan.Row question = reading.plan().rows().getFirst();

        FirstSetupFiles.Merged merged = FirstSetupFiles.merge(reading, Map.of(question.id(), "LVWAP"));

        assertEquals(1, merged.bindingsMoved());
        String binds = new String(merged.files().bindings().get(BINDS), StandardCharsets.UTF_8);
        assertEquals(2, count(binds, "Device=\"LVWAP\""));
        assertFalse(binds.contains("lvwap"));
    }

    @Test
    void aBindsFilesByteOrderMarkIsKeptWhenItIsRewritten() throws IOException {
        byte[] plain = binds("RightStick").get(BINDS);
        byte[] withMark = new byte[plain.length + 3];
        withMark[0] = (byte) 0xEF;
        withMark[1] = (byte) 0xBB;
        withMark[2] = (byte) 0xBF;
        System.arraycopy(plain, 0, withMark, 3, plain.length);
        FirstSetupReading reading = reading(Map.of(BINDS, withMark, PRESET, preset()),
                installation(STEAM, "STEAM", mappings("<RVWAP><PID>03F5</PID><VID>3344</VID></RVWAP>"), Map.of()),
                installation(EPIC, "EPIC", mappings("<RightStick><PID>03F5</PID><VID>3344</VID></RightStick>"),
                        Map.of()));

        FirstSetupFiles.Merged merged = FirstSetupFiles.merge(reading,
                Map.of(reading.plan().rows().getFirst().id(), "RVWAP"));

        byte[] written = merged.files().bindings().get(BINDS);
        assertArrayEquals(Arrays.copyOf(withMark, 3), Arrays.copyOf(written, 3));
    }

    /** Kept byte for byte when its labels came through unchanged; written afresh only when they were merged. */
    @Test
    void aButtonMapKeepsItsBytesUnlessItsLabelsWereMerged() throws IOException {
        byte[] steamLabels = ("<?xml version=\"1.0\"?><Root>  <Joy_1>TRIGGER</Joy_1>  </Root>")
                .getBytes(StandardCharsets.UTF_8);
        FirstSetupReading reading = reading(binds("RVWAP"),
                installation(STEAM, "STEAM", mappings("<RVWAP><PID>03F5</PID><VID>3344</VID></RVWAP>"
                                + "<LVWAP><PID>83F4</PID><VID>3344</VID></LVWAP>"),
                        Map.of("RVWAP.buttonMap", steamLabels, "LVWAP.buttonMap", map("<Joy_1>A</Joy_1>"))),
                installation(EPIC, "EPIC", mappings("<RVWAP><PID>03F5</PID><VID>3344</VID></RVWAP>"
                                + "<LVWAP><PID>83F4</PID><VID>3344</VID></LVWAP>"),
                        Map.of("LVWAP.buttonMap", map("<Joy_1>B</Joy_1>"))));

        FirstSetupFiles.Merged merged = FirstSetupFiles.merge(reading,
                Map.of(FirstSetupPlan.labelAnswerKey("LVWAP", "Joy_1"), "B"));

        assertArrayEquals(steamLabels, merged.files().buttonMaps().get("RVWAP.buttonMap"), "unchanged, so as found");
        assertEquals(Map.of("Joy_1", "B"), ButtonMapReader.read(merged.files().buttonMaps().get("LVWAP.buttonMap")));
    }

    /** Frontier's own maps are left out of the plan as shipped, and still go into the working copy as found. */
    @Test
    void frontiersShippedMapsAreKeptAsFound() throws IOException {
        byte[] shipped;
        try (InputStream in = getClass().getResourceAsStream("/bindforge/FrontierStock-VPCPanel.buttonMap.xml")) {
            assertNotNull(in);
            shipped = in.readAllBytes();
        }
        FirstSetupReading reading = reading(binds("RVWAP"),
                installation(STEAM, "STEAM", mappings("<RVWAP><PID>03F5</PID><VID>3344</VID></RVWAP>"),
                        Map.of("VPCPanel.buttonMap", shipped)),
                installation(EPIC, "EPIC", mappings("<LVWAP><PID>83F4</PID><VID>3344</VID></LVWAP>"), Map.of()));

        FirstSetupFiles.Merged merged = FirstSetupFiles.merge(reading, Map.of());

        assertArrayEquals(shipped, merged.files().buttonMaps().get("VPCPanel.buttonMap"));
    }

    @Test
    void aMergeWithAQuestionUnansweredIsRefused() throws IOException {
        FirstSetupReading reading = reading(binds("RVWAP"),
                installation(STEAM, "STEAM", mappings("<RVWAP><PID>03F5</PID><VID>3344</VID></RVWAP>"), Map.of()),
                installation(EPIC, "EPIC", mappings("<RightStick><PID>03F5</PID><VID>3344</VID></RightStick>"),
                        Map.of()));

        assertThrows(IllegalStateException.class, () -> FirstSetupFiles.merge(reading, Map.of()));
    }

    // ------------------------------------------------------------------ fixtures

    private static FirstSetupReading reading(Map<String, byte[]> bindings, InstallationFiles... installations)
            throws IOException {
        return FirstSetupReading.of(Path.of("Bindings"), bindings, List.of(installations), List.of(),
                FrontierStockDevices.getInstance(), FrontierStockButtonMaps.getInstance());
    }

    private static InstallationFiles installation(long id, String storefront, byte[] mappings,
                                                  Map<String, byte[]> buttonMaps) {
        return new InstallationFiles(id, storefront, Path.of(storefront), mappings, buttonMaps);
    }

    private static byte[] mappings(String elements) {
        return ("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\r\n<Root>" + elements + "</Root>\r\n")
                .getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] map(String body) {
        return ("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n<Root>\n" + body + "\n</Root>\n")
                .getBytes(StandardCharsets.UTF_8);
    }

    /** A binds file with one binding per device named, and the preset file beside it. */
    private static Map<String, byte[]> binds(String... devices) {
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\r\n"
                + "<Root PresetName=\"Custom\" MajorVersion=\"4\" MinorVersion=\"2\">\r\n");
        List<String> names = new ArrayList<>(List.of(devices));
        for (int i = 0; i < names.size(); i++) {
            xml.append("\t<Action").append(i).append(">\r\n\t\t<Primary Device=\"").append(names.get(i))
                    .append("\" Key=\"Joy_").append(i + 1).append("\" />\r\n\t</Action").append(i).append(">\r\n");
        }
        xml.append("</Root>\r\n");
        Map<String, byte[]> files = new LinkedHashMap<>();
        files.put(BINDS, xml.toString().getBytes(StandardCharsets.UTF_8));
        files.put(PRESET, preset());
        return files;
    }

    private static byte[] preset() {
        return "Custom\r\nCustom\r\nCustom\r\nCustom".getBytes(StandardCharsets.UTF_8);
    }

    private static int count(String text, String part) {
        int count = 0;
        for (int at = text.indexOf(part); at >= 0; at = text.indexOf(part, at + 1)) count++;
        return count;
    }
}
