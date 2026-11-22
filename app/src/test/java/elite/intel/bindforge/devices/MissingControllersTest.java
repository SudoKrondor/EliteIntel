package elite.intel.bindforge.devices;

import elite.intel.bindforge.devicefiles.FrontierStockDevices;
import elite.intel.bindforge.devices.MissingControllers.Missing;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Who the once-per-run warning names: the user's named controllers that are not attached and that bindings name
 * (Alan, 2026-10-11, A13).
 */
class MissingControllersTest {

    private final List<String> frontier = FrontierStockDevices.getInstance().names();

    /** The case the warning exists for: a stick the bindings use, unplugged. */
    @Test
    void aNamedControllerTheBindingsUseIsMissing() {
        List<Missing> missing = MissingControllers.of(
                List.of(absent("LVWAP", "3344", "83F4", "LVWAP")),
                Map.of("LVWAP", 142), frontier);

        assertEquals(List.of(new Missing("LVWAP", 142)), missing);
    }

    @Test
    void anAttachedControllerIsNotMissing() {
        List<Missing> missing = MissingControllers.of(
                List.of(new MyDevice("VIRPIL L", "LVWAP", "3344", "83F4", true, "LVWAP", false)),
                Map.of("LVWAP", 142), frontier);

        assertTrue(missing.isEmpty());
    }

    /** An entry for a stick sold years ago, which no binding uses, is not worth a word on every run. */
    @Test
    void aControllerNoBindingNamesIsNotMentioned() {
        List<Missing> missing = MissingControllers.of(
                List.of(absent("OldStick", "1234", "5678", "OldStick")),
                Map.of("LVWAP", 142), frontier);

        assertTrue(missing.isEmpty());
    }

    /** Before first setup there is no alias; the installation's entry name is the name. */
    @Test
    void beforeFirstSetupTheEntryNameIsUsed() {
        List<Missing> missing = MissingControllers.of(
                List.of(absent("RVWAP", "3344", "03F5", null)),
                Map.of("RVWAP", 12), frontier);

        assertEquals(List.of(new Missing("RVWAP", 12)), missing);
    }

    /** The edits are what the next APPLY writes, so their name wins over the working copy's old one. */
    @Test
    void theEditsNameWinsOverTheEntryName() {
        List<Missing> missing = MissingControllers.of(
                List.of(absent("Lvwap", "3344", "83F4", "LVWAP")),
                Map.of("LVWAP", 3, "Lvwap", 4), frontier);

        assertEquals(1, missing.size());
        assertEquals("LVWAP", missing.getFirst().name());
    }

    /** The game matches Device= ignoring case, so two spellings are one device, and their bindings add up. */
    @Test
    void bindingsAreMatchedIgnoringCase() {
        List<Missing> missing = MissingControllers.of(
                List.of(absent("LVWAP", "3344", "83F4", "LVWAP")),
                Map.of("lvwap", 2, "LVWAP", 5), frontier);

        assertEquals(List.of(new Missing("LVWAP", 7)), missing);
    }

    /** A Frontier device is not one of the user's names, even when the bindings use it. */
    @Test
    void aFrontierNameIsNotMentioned() {
        List<Missing> missing = MissingControllers.of(
                List.of(new MyDevice(null, "T-Rudder", "044F", "B679", false, null, true)),
                Map.of("T-Rudder", 20), frontier);

        assertTrue(missing.isEmpty());
    }

    /**
     * A user's own element shadowing a built-in carries a built-in's VID/PID, so {@code builtIn} is true - but its
     * name is the user's, and a binding naming it fails like any other.
     */
    @Test
    void aUsersElementShadowingABuiltInIsMentioned() {
        List<Missing> missing = MissingControllers.of(
                List.of(new MyDevice(null, "MyPedals", "044F", "B679", false, "MyPedals", true)),
                Map.of("MyPedals", 20), frontier);

        assertEquals(List.of(new Missing("MyPedals", 20)), missing);
    }

    /** A row with no name at all is hardware nobody named - nothing a binding can name. */
    @Test
    void aRowWithNoNameIsSkipped() {
        List<Missing> missing = MissingControllers.of(
                List.of(absent(null, "3344", "83F4", null)), Map.of("LVWAP", 1), frontier);

        assertTrue(missing.isEmpty());
    }

    /**
     * Before first setup two installations can give one name to two PIDs - the stale one from before a firmware
     * update among them. When the stick is plugged in under either, the name resolves, and it is not missing.
     */
    @Test
    void aNameAttachedUnderOtherHardwareIsNotMissing() {
        List<Missing> missing = MissingControllers.of(
                List.of(new MyDevice("VIRPIL L", "LVWAP", "3344", "83F4", true, null, false),
                        absent("LVWAP", "3344", "83F3", null)),
                Map.of("LVWAP", 142), frontier);

        assertTrue(missing.isEmpty());
    }

    @Test
    void aNameOnTwoAbsentRowsIsNamedOnce() {
        List<Missing> missing = MissingControllers.of(
                List.of(absent("LVWAP", "3344", "83F4", null), absent("LVWAP", "3344", "83F3", null)),
                Map.of("LVWAP", 142), frontier);

        assertEquals(List.of(new Missing("LVWAP", 142)), missing);
    }

    @Test
    void severalAreNamedInTheOrderTheListGivesThem() {
        List<Missing> missing = MissingControllers.of(
                List.of(absent("RVWAP", "3344", "03F5", "RVWAP"), absent("LVWAP", "3344", "83F4", "LVWAP")),
                Map.of("LVWAP", 142, "RVWAP", 12), frontier);

        assertEquals(List.of(new Missing("RVWAP", 12), new Missing("LVWAP", 142)), missing);
    }

    private static MyDevice absent(String entryName, String vid, String pid, String alias) {
        return new MyDevice(null, entryName, vid, pid, false, alias, false);
    }
}
