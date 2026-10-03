package elite.intel.bindforge.devices;

import elite.intel.db.dao.BindForgeDeviceMasterDao.DeviceRow;
import elite.intel.devices.model.Device;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The union rule the whole screen rests on: live hardware and file entries, correlated by VID/PID.
 * <p>
 * The GUIDs here were reported by real controllers - see {@link DeviceIdentitiesTest} - so the correlation is
 * exercised on the ids the hardware actually produces rather than on invented ones.
 */
class MyDeviceListTest {

    private static final String VIRPIL_LEFT_GUID = "03002cec44330000f483000000000000";   // 3344:83F4
    private static final String VIRPIL_RIGHT_GUID = "03009ba344330000f503000000000000";  // 3344:03F5
    private static final String T_RUDDER_GUID = "030034554f04000079b6000000000000";      // 044F:B679

    private final FrontierStockDevices stock = FrontierStockDevices.getInstance();

    /**
     * The case the file half cannot cover. The game never writes into {@code DeviceMappings.xml}, so a stick
     * plugged in for the first time appears nowhere until somebody names it - it has to come from the
     * hardware.
     */
    @Test
    void aControllerNoFileMentionsStillAppears() {
        List<MyDevice> rows = MyDeviceList.build(
                List.of(device("VIRPIL Controls L-VP-WBD-CAP", VIRPIL_LEFT_GUID)), List.of(), List.of(), stock);

        assertEquals(1, rows.size());
        MyDevice row = rows.getFirst();
        assertEquals("VIRPIL Controls L-VP-WBD-CAP", row.windowsName());
        assertEquals("3344", row.vid());
        assertEquals("83F4", row.pid());
        assertTrue(row.attached());
        assertNull(row.alias(), "nothing has named it, so it is not added");
        assertNull(row.entryName());
    }

    /**
     * The case the hardware half cannot cover. An entry outlives the stick it names, and dropping it would
     * take away the only sign that bindings still reference something that is gone.
     */
    @Test
    void anEntryForHardwareThatIsNotPluggedInStillAppears() {
        List<MyDevice> rows = MyDeviceList.build(
                List.of(), List.of(entry("LVWAP", "3344", "83F4")), List.of(), stock);

        assertEquals(1, rows.size());
        MyDevice row = rows.getFirst();
        assertFalse(row.attached(), "missing is a status, never a reason to drop the row");
        assertNull(row.windowsName(), "nothing is there to report one");
        assertEquals("LVWAP", row.entryName());
        assertEquals("LVWAP", row.label(), "and the entry name is what keeps the row readable");
    }

    @Test
    void hardwareAndItsEntryAreOneRowRatherThanTwo() {
        List<MyDevice> rows = MyDeviceList.build(
                List.of(device("VIRPIL Controls L-VP-WBD-CAP", VIRPIL_LEFT_GUID)),
                List.of(entry("LVWAP", "3344", "83F4")), List.of(), stock);

        assertEquals(1, rows.size(), "correlated by VID/PID, which is what the two halves share");
        assertEquals("VIRPIL Controls L-VP-WBD-CAP", rows.getFirst().windowsName());
        assertEquals("LVWAP", rows.getFirst().entryName());
        assertTrue(rows.getFirst().attached());
    }

    /**
     * Frontier's file has no case convention at all - {@code GamePad} carries {@code 045E} beside
     * {@code 045e} - so a case-sensitive correlation would split a device into two rows, one attached and one
     * missing, for no reason a user could see.
     */
    @Test
    void correlationHoldsWhateverCaseTheFileWroteTheHexIn() {
        List<MyDevice> rows = MyDeviceList.build(
                List.of(device("VIRPIL Controls L-VP-WBD-CAP", VIRPIL_LEFT_GUID)),
                List.of(entry("LVWAP", "3344", "83f4")), List.of(), stock);

        assertEquals(1, rows.size());
        assertEquals("LVWAP", rows.getFirst().entryName());
    }

    /**
     * The game ships 51 entries, most for hardware nobody owns. Listing them under My Devices would bury the
     * handful that are real.
     */
    @Test
    void frontiersOwnEntriesAreNotListedAsTheUsers() {
        List<MyDevice> rows = MyDeviceList.build(
                List.of(), List.of(entry("T-Rudder", "044F", "B679"), entry("LVWAP", "3344", "83F4")),
                List.of(), stock);

        assertEquals(1, rows.size(), "only the user's entry");
        assertEquals("LVWAP", rows.getFirst().entryName());
    }

    /**
     * A built-in that is actually attached does appear - through the hardware half - marked so the user can
     * see the game already names it and there is nothing to create.
     */
    @Test
    void anAttachedBuiltInIsListedAndMarked() {
        List<MyDevice> rows = MyDeviceList.build(
                List.of(device("Thrustmaster T-Rudder", T_RUDDER_GUID)), List.of(), List.of(), stock);

        assertEquals(1, rows.size());
        assertTrue(rows.getFirst().builtIn(), "Frontier's <T-Rudder> covers 044F:B679");
        assertTrue(rows.getFirst().attached());
    }

    @Test
    void aControllerFrontierDoesNotShipIsNotMarkedBuiltIn() {
        List<MyDevice> rows = MyDeviceList.build(
                List.of(device("VIRPIL Controls L-VP-WBD-CAP", VIRPIL_LEFT_GUID)), List.of(), List.of(), stock);

        assertFalse(rows.getFirst().builtIn());
    }

    @Test
    void theAliasComesFromTheMaster() {
        List<MyDevice> rows = MyDeviceList.build(
                List.of(device("VIRPIL Controls L-VP-WBD-CAP", VIRPIL_LEFT_GUID)), List.of(),
                List.of(masterRow("LeftStick", "3344", "83F4")), stock);

        assertEquals("LeftStick", rows.getFirst().alias(),
                "the master's name, not the one the file happens to hold");
    }

    /** Attached controllers keep the Device Service's order, then the entries nothing is plugged in for. */
    @Test
    void attachedControllersComeFirst() {
        List<MyDevice> rows = MyDeviceList.build(
                List.of(device("Right", VIRPIL_RIGHT_GUID), device("Left", VIRPIL_LEFT_GUID)),
                List.of(entry("SoldLastYear", "046D", "C215")), List.of(), stock);

        assertEquals(List.of("Right", "Left", "SoldLastYear"), rows.stream().map(MyDevice::label).toList());
        assertFalse(rows.get(2).attached());
    }

    /**
     * Every column but the name is derived from the VID/PID, so a controller whose GUID could not be read has
     * nothing to key on - and two of them would merge into one row.
     */
    @Test
    void aControllerWithNoReadableGuidIsLeftOut() {
        List<MyDevice> rows = MyDeviceList.build(
                List.of(device("Nameless", ""), device("Left", VIRPIL_LEFT_GUID)), List.of(), List.of(), stock);

        assertEquals(1, rows.size());
        assertEquals("Left", rows.getFirst().windowsName());
    }

    private static Device device(String name, String guid) {
        return new Device(1, name, 6, 32, "usb-0001", guid);
    }

    private static DeviceEntry entry(String name, String vid, String pid) {
        return new DeviceEntry(name, Set.of(new DeviceEntry.HardwareId(vid, pid)));
    }

    private static DeviceRow masterRow(String deviceName, String vid, String pid) {
        return new DeviceRow(1, deviceName, vid, pid, true, null);
    }
}
