package elite.intel.bindforge.storage;

import elite.intel.bindforge.devicefiles.DeviceEntry;
import elite.intel.bindforge.devicefiles.DeviceEntry.HardwareId;
import elite.intel.bindforge.devicefiles.FrontierStockButtonMaps;
import elite.intel.bindforge.devicefiles.FrontierStockDevices;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static elite.intel.bindforge.storage.WorkingCopyTest.bytes;
import static elite.intel.bindforge.storage.WorkingCopyTest.text;
import static org.junit.jupiter.api.Assertions.*;

/**
 * What each Alias Designer action does to the working copy's files, as an edit not applied (Storage S4a): the device,
 * its {@code .buttonMap} and every binds file naming it change together.
 */
class DeviceEditsTest {

    private static final String BINDS = "Custom.4.2.binds";
    private static final Map<String, String> FIRE = Map.of("Joy_1", "FIRE");

    private final FrontierStockDevices stock = FrontierStockDevices.getInstance();
    private final FrontierStockButtonMaps shipped = FrontierStockButtonMaps.getInstance();

    @Test
    void labelsAreTheDevicesWholeButtonMap() throws IOException {
        ConfigurationFiles files = DeviceEdits.withLabels(set(), "LVWAP", FIRE);

        assertEquals(FIRE, DeviceEdits.labels(files, "LVWAP"));
        assertEquals(Set.of("LVWAP.buttonMap"), files.buttonMaps().keySet(), "the old spelling is replaced");
        assertTrue(DeviceEdits.withLabels(files, "LVWAP", Map.of()).buttonMaps().isEmpty(), "no label, no file");
    }

    /** The identity question's yes (A12, Storage S4b): new hardware, nothing else - the bindings follow the name. */
    @Test
    void aRetargetChangesOnlyTheElementsHardware() throws IOException {
        ConfigurationFiles before = set();

        ConfigurationFiles files = DeviceEdits.retarget(before, "LVWAP", new HardwareId("3344", "83f4"), "3344", "83F5")
                .orElseThrow();

        assertEquals(List.of("LVWAP", "T-Rudder"), names(files), "in place");
        assertEquals(new HardwareId("3344", "83F5"), DeviceEdits.elements(files).getFirst().primary());
        assertTrue(text(files.deviceMappings()).contains("<SupportsIcons"), "every other child kept");
        assertEquals(binds(before), binds(files), "the bindings name the device, so they stay");
        assertEquals(before.buttonMaps().keySet(), files.buttonMaps().keySet());
    }

    /** A device that changed between the question and the answer is left alone, and says so. */
    @Test
    void aRetargetOfADeviceThatChangedChangesNothing() throws IOException {
        assertTrue(DeviceEdits.retarget(set(), "LVWAP", new HardwareId("3344", "83F3"), "3344", "83F5").isEmpty(),
                "its hardware is no longer what the question saw");
        assertTrue(DeviceEdits.retarget(set(), "Gone", new HardwareId("3344", "83F4"), "3344", "83F5").isEmpty());
    }

    /** Several answers are one edit: each made on what the last left, a changed device skipped and listed. */
    @Test
    void retargetAllMakesEachAndListsTheStaleOnes() throws IOException {
        DeviceEdits.Retarget lvwap = new DeviceEdits.Retarget("LVWAP", new HardwareId("3344", "83F4"), "3344", "83F5");
        DeviceEdits.Retarget gone = new DeviceEdits.Retarget("Gone", new HardwareId("3344", "0001"), "3344", "0002");
        DeviceEdits.Retarget again = new DeviceEdits.Retarget("LVWAP", new HardwareId("3344", "83F4"), "3344", "83F6");

        DeviceEdits.Retargeted done = DeviceEdits.retargetAll(set(), List.of(lvwap, gone, again));

        assertEquals(List.of(lvwap), done.made());
        assertEquals(List.of(gone, again), done.stale(), "the second saw LVWAP on hardware the first had moved");
        assertEquals(new HardwareId("3344", "83F5"), DeviceEdits.elements(done.files()).getFirst().primary());
    }

    /** A rename moves the element, its labels and every binding naming it, together (A11). */
    @Test
    void aRenameMovesTheElementItsLabelsAndItsBindings() throws IOException {
        DeviceEdits.Changed changed = DeviceEdits.rename(set(), "LVWAP", "LeftStick", Map.of());

        ConfigurationFiles files = changed.files();
        assertEquals(List.of("LeftStick", "T-Rudder"), names(files), "renamed in place, still leading the file");
        assertTrue(text(files.deviceMappings()).contains("<SupportsIcons"), "every child kept");
        assertEquals(Map.of("Joy_1", "LEFT TRIGGER"), DeviceEdits.labels(files, "LeftStick"),
                "with no labels given, the old file's own");
        assertTrue(DeviceEdits.buttonMapFile(files, "LVWAP").isEmpty());
        assertEquals(2, changed.bindingsMoved());
        assertFalse(binds(files).contains("\"LVWAP\""));
        assertTrue(binds(files).contains("Device=\"LeftStick\""));
    }

    /** In .binds LVWAP and Lvwap are not one name, so a case-only rename rewrites every binding (2026-10-24). */
    @Test
    void aCaseOnlyRenameRewritesTheBindingsToo() throws IOException {
        DeviceEdits.Changed changed = DeviceEdits.rename(set(), "LVWAP", "Lvwap", FIRE);

        assertEquals(2, changed.bindingsMoved());
        assertEquals(List.of("Lvwap", "T-Rudder"), names(changed.files()));
    }

    /**
     * Onboarding's FINISH (Alan, 2026-10-26): the element after the user's others, its default labels, and every
     * binding naming it by hardware id moved to the name - here an old id it replaces, too (A17).
     */
    @Test
    void namingAControllerAddsItsElementLabelsAndMovesItsBindings() throws IOException {
        DeviceEdits.Changed changed = DeviceEdits.name(set(), "RVWAP", "3344", "03F5", FIRE,
                List.of("334403F5", "334403F4"), stock);

        ConfigurationFiles files = changed.files();
        assertEquals(List.of("LVWAP", "RVWAP", "T-Rudder"), names(files), "after the user's, ahead of Frontier's");
        assertEquals(FIRE, DeviceEdits.labels(files, "RVWAP"));
        assertEquals(2, changed.bindingsMoved());
        assertTrue(binds(files).contains("Device=\"RVWAP\""));
        assertFalse(binds(files).contains("334403F"));
    }

    @Test
    void aButtonMapAlreadyUnderTheNameIsKept() throws IOException {
        ConfigurationFiles files = set().withButtonMaps(Map.of("RVWAP.buttonMap", map("MINE")));

        ConfigurationFiles named = DeviceEdits.name(files, "RVWAP", "3344", "03F5", FIRE, List.of(), stock).files();

        assertEquals(Map.of("Joy_1", "MINE"), DeviceEdits.labels(named, "RVWAP"));
    }

    @Test
    void namingNeedsADeviceMappingsToAddTo() {
        ConfigurationFiles none = new ConfigurationFiles(Map.of(BINDS, bytes(BINDS_XML)), null, Map.of());

        assertThrows(IOException.class,
                () -> DeviceEdits.name(none, "RVWAP", "3344", "03F5", FIRE, List.of(), stock));
    }

    /**
     * A built-in renamed is shadowed (A15): the user's element first, Frontier's left in place, the bindings naming
     * Frontier's element moved, and Frontier's name's labels gone with it when it covers nothing else.
     */
    @Test
    void renamingABuiltInShadowsIt() throws IOException {
        ConfigurationFiles files = set().withButtonMaps(Map.of("T-Rudder.buttonMap", map("PEDALS")));

        DeviceEdits.Changed changed =
                DeviceEdits.shadow(files, "T-Rudder", "MyRudder", "044F", "B679", FIRE, false, stock, shipped);

        assertEquals(List.of("MyRudder", "LVWAP", "T-Rudder"), names(changed.files()));
        assertEquals(1, changed.bindingsMoved());
        assertEquals(FIRE, DeviceEdits.labels(changed.files(), "MyRudder"));
        assertTrue(DeviceEdits.buttonMapFile(changed.files(), "T-Rudder").isEmpty());
    }

    /** Frontier's element covering other controllers too keeps its labels: they are still called by its name. */
    @Test
    void shadowingASharedBuiltInKeepsItsLabels() throws IOException {
        ConfigurationFiles files = set().withButtonMaps(Map.of("T-Rudder.buttonMap", map("PEDALS")));

        ConfigurationFiles shadowed =
                DeviceEdits.shadow(files, "T-Rudder", "MyRudder", "044F", "B679", FIRE, true, stock, shipped).files();

        assertEquals(Map.of("Joy_1", "PEDALS"), DeviceEdits.labels(shadowed, "T-Rudder"));
    }

    @Test
    void goingBackToFrontiersNameUndoesTheShadow() throws IOException {
        ConfigurationFiles shadowed =
                DeviceEdits.shadow(set(), "T-Rudder", "MyRudder", "044F", "B679", FIRE, false, stock, shipped).files();

        DeviceEdits.Changed back = DeviceEdits.backToFrontier(shadowed, "MyRudder", "T-Rudder", FIRE);

        assertEquals(List.of("LVWAP", "T-Rudder"), names(back.files()));
        assertEquals(1, back.bindingsMoved());
        assertEquals(FIRE, DeviceEdits.labels(back.files(), "T-Rudder"));
        assertTrue(DeviceEdits.buttonMapFile(back.files(), "MyRudder").isEmpty());
    }

    @Test
    void clearTakesOutTheElementAndItsLabels() throws IOException {
        ConfigurationFiles cleared = DeviceEdits.clear(set(), "LVWAP");

        assertEquals(List.of("T-Rudder"), names(cleared), "Frontier's is never touched");
        assertTrue(cleared.buttonMaps().isEmpty());
    }

    /** RESET LABELS removes the user's map - or, where Frontier ships one, puts Frontier's back byte for byte. */
    @Test
    void resetLabelsRemovesTheMapOrPutsFrontiersBack() {
        ConfigurationFiles files = set().withButtonMaps(Map.of(
                "T-Rudder.buttonMap", map("PEDALS"), "VPCPanel.buttonMap", map("MINE")));

        ConfigurationFiles reset = DeviceEdits.resetLabels(DeviceEdits.resetLabels(files, "T-Rudder", shipped),
                "VPCPanel", shipped);

        assertTrue(DeviceEdits.buttonMapFile(reset, "T-Rudder").isEmpty());
        assertArrayEquals(shipped.shippedContent("VPCPanel").orElseThrow(), reset.buttonMaps().get("VPCPanel.buttonMap"));
    }

    /** UNDO CLEAR puts the element back whole, where it led the file, with its labels. */
    @Test
    void undoPutsBackWhatTheWorkingCopyHolds() throws IOException {
        ConfigurationFiles applied = set();
        ConfigurationFiles cleared = DeviceEdits.clear(applied, "LVWAP");

        ConfigurationFiles restored = DeviceEdits.restore(cleared, applied, "LVWAP", stock);

        assertEquals(List.of("LVWAP", "T-Rudder"), names(restored));
        assertTrue(text(restored.deviceMappings()).contains("<SupportsIcons"));
        assertEquals(Map.of("Joy_1", "LEFT TRIGGER"), DeviceEdits.labels(restored, "LVWAP"));
    }

    // ------------------------------------------------------------------ the set

    private static final String BINDS_XML = """
            <Root PresetName="Custom">
              <A><Primary Device="LVWAP" Key="Joy_1" /><Secondary Device="LVWAP" Key="Joy_2" /></A>
              <B><Primary Device="334403F5" Key="Joy_3" /><Secondary Device="334403F4" Key="Joy_4" /></B>
              <C><Binding Device="T-Rudder" Key="Joy_ZAxis" /></C>
            </Root>
            """;

    private static ConfigurationFiles set() {
        return new ConfigurationFiles(
                Map.of(BINDS, bytes(BINDS_XML), "StartPreset.4.start", bytes("Custom")),
                bytes("<Root><LVWAP><PID>83F4</PID><VID>3344</VID><SupportsIcons /></LVWAP>"
                        + "<T-Rudder><PID>B679</PID><VID>044F</VID></T-Rudder></Root>"),
                Map.of("lvwap.buttonMap", map("LEFT TRIGGER")));
    }

    private static byte[] map(String joy1) {
        return bytes("<Root><Joy_1>" + joy1 + "</Joy_1></Root>");
    }

    private static List<String> names(ConfigurationFiles files) throws IOException {
        return DeviceEdits.elements(files).stream().map(DeviceEntry::name).toList();
    }

    private static String binds(ConfigurationFiles files) {
        return text(files.bindings().get(BINDS));
    }
}
