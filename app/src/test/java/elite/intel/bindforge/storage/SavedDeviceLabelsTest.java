package elite.intel.bindforge.storage;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The Bind Editor names a controller and shows its labels as the edits do, so a rename or a SAVE in the Alias Designer
 * shows at once (Storage S5, 2026-10-28; names were the working copy's from S4b until then).
 */
class SavedDeviceLabelsTest {

    private static final String DEVICE_MAPPINGS = "<Root>"
            + "<RightStick><PID>0200</PID><VID>231D</VID></RightStick>"
            + "<T-Rudder><PID>B679</PID><VID>044F</VID><Alternative><PID>B678</PID><VID>044F</VID></Alternative>"
            + "</T-Rudder>"
            + "<Shadowed><PID>0200</PID><VID>231D</VID></Shadowed>"
            + "</Root>";

    @Test
    void aDevicesLabelsAreItsButtonMapsFoundIgnoringCase() throws IOException {
        SavedDeviceLabels labels = SavedDeviceLabels.of(set());

        assertEquals(Map.of("Joy_6", "Gear", "Joy_7", ""), labels.labelsOf("RightStick"), "trimmed, as read");
        assertEquals(Optional.of("Gear"), labels.label("rightstick", "Joy_6"), "the game finds an element ignoring case");
    }

    @Test
    void aBlankNameOrAnUnknownInputOrDeviceHasNoLabel() throws IOException {
        SavedDeviceLabels labels = SavedDeviceLabels.of(set());

        assertEquals(Optional.empty(), labels.label("RightStick", "Joy_7"));
        assertEquals(Optional.empty(), labels.label("RightStick", "Joy_9"));
        assertEquals(Optional.empty(), labels.label("VPCThrottle", "Joy_6"));
        assertEquals(Optional.empty(), labels.label(null, "Joy_6"));
        assertEquals(Map.of(), SavedDeviceLabels.NONE.labelsOf("RightStick"));
    }

    /** As the game resolves hardware: the first element claiming it, primary or alternative. */
    @Test
    void hardwareIsNamedByTheFirstElementClaimingIt() throws IOException {
        SavedDeviceLabels labels = SavedDeviceLabels.of(set());

        assertEquals(Optional.of("RightStick"), labels.nameOf("231d", "0200"), "the first, not the shadowed one");
        assertEquals(Optional.of("T-Rudder"), labels.nameOf("044F", "B678"), "an alternative pair counts");
        assertEquals(Optional.empty(), labels.nameOf("3344", "83F4"));
        assertEquals(List.of("RightStick", "T-Rudder", "Shadowed"), labels.names());
    }

    /**
     * Renamed on screen, not applied: the tab takes the new name - the one the edits' binds files now hold, since the
     * rename moved every binding in them - and its labels are under it.
     */
    @Test
    void aDeviceRenamedOnScreenIsNamedAsTheEditsNameIt() throws IOException {
        ConfigurationFiles renamed = DeviceEdits.rename(set(), "RightStick", "MainStick",
                Map.of("Joy_6", "Gear up")).files();

        SavedDeviceLabels labels = SavedDeviceLabels.of(renamed);

        assertEquals(Optional.of("MainStick"), labels.nameOf("231D", "0200"));
        assertEquals(Optional.of("Gear up"), labels.label("MainStick", "Joy_6"));
        assertEquals(Optional.empty(), labels.label("RightStick", "Joy_6"), "the old name is gone from the edits");
    }

    private static ConfigurationFiles set() {
        return new ConfigurationFiles(Map.of(), bytes(DEVICE_MAPPINGS),
                Map.of("rightstick.buttonMap", bytes("<Root><Joy_6>Gear</Joy_6><Joy_7> </Joy_7></Root>")));
    }

    private static byte[] bytes(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }
}
