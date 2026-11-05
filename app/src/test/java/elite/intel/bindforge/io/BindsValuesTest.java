package elite.intel.bindforge.io;

import elite.intel.bindforge.model.BindingDisplayNames;
import elite.intel.bindforge.model.BindingInput;
import elite.intel.bindforge.model.BindingSection;
import elite.intel.bindforge.model.SettingNumbers;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class BindsValuesTest {

    private static BindsValues read(String body) throws IOException {
        return BindsValues.of(("<Root>" + body + "</Root>").getBytes(StandardCharsets.UTF_8));
    }

    private static String axis(String tag, String device, String key, String inverted, String deadzone) {
        return "<" + tag + "><Binding Device=\"" + device + "\" Key=\"" + key + "\" /><Inverted Value=\"" + inverted
                + "\" /><Deadzone Value=\"" + deadzone + "\" /></" + tag + ">";
    }

    @Test
    void readsAnAxisRow() throws IOException {
        BindsValues.Axis axis = read(axis("YawAxisRaw", "Stick", "Joy_XAxis", "1", "0.05725000")).axes().get("YawAxisRaw");

        assertEquals(Optional.of(new BindingInput("Stick", "Joy_XAxis")), axis.input());
        assertTrue(axis.inverted());
        assertEquals(0.05725f, axis.deadzone());
        assertTrue(axis.editable());
    }

    @Test
    void anUnboundAxisHasNoInput() throws IOException {
        BindsValues.Axis axis = read(axis("YawAxisRaw", "{NoDevice}", "", "0", "0.00000000")).axes().get("YawAxisRaw");

        assertFalse(axis.bound());
        assertTrue(axis.editable());
    }

    @Test
    void aFactoryDeadzoneWordIsShownButNotEditable() throws IOException {
        BindsValues.Axis axis = read(axis("YawAxisRaw", "GamePad", "Pad_LStickX", "0", "DefaultPlatformDeadzone"))
                .axes().get("YawAxisRaw");

        assertTrue(axis.bound());
        assertFalse(axis.editable());
    }

    @Test
    void aRepeatedElementIsNotEditable() throws IOException {
        BindsValues values = read("<HeadlookSensitivity Value=\"1.0\" /><HeadlookSensitivity Value=\"2.0\" />"
                + axis("YawAxisAlternate", "A", "Joy_XAxis", "0", "0") + axis("YawAxisAlternate", "A", "Joy_YAxis", "0", "0"));

        assertFalse(values.settings().get("HeadlookSensitivity").editable());
        assertFalse(values.axes().get("YawAxisAlternate").editable());
    }

    @Test
    void keepsSettingsAsWrittenAndLeavesButtonRowsAlone() throws IOException {
        BindsValues values = read("<ThrottleRange Value=\"\" /><MouseSensitivity Value=\"1.00000000\" />"
                + "<KeyboardLayout>en-US</KeyboardLayout>"
                + "<YawLeftButton><Primary Device=\"Keyboard\" Key=\"Key_A\" /><Secondary Device=\"{NoDevice}\" Key=\"\" /></YawLeftButton>");

        assertEquals("", values.settings().get("ThrottleRange").value());
        assertEquals("1.00000000", values.settings().get("MouseSensitivity").value());
        assertFalse(values.settings().containsKey("KeyboardLayout"));
        assertFalse(values.settings().containsKey("YawLeftButton"));
        assertTrue(values.axes().isEmpty());
    }

    @Test
    void everyAxisAndNumberInARealFileIsNamedAsTheGameNamesIt() throws IOException {
        BindsValues values = BindsValues.of(Files.readAllBytes(
                Path.of("../docs/02-features/bindforge/reference-data/Custom.4.2.binds")));

        assertEquals(70, values.axes().size());
        values.axes().keySet().forEach(id -> assertNotEquals(BindingSection.OTHER,
                BindingDisplayNames.lookup(id).section(), id));
        SettingNumbers.SETTINGS.forEach(id -> assertNotEquals(BindingSection.OTHER,
                BindingDisplayNames.lookup(id).section(), id));
    }

    @Test
    void refusesWhatIsNotABindsFile() {
        assertThrows(IOException.class, () -> BindsValues.of("not xml".getBytes(StandardCharsets.UTF_8)));
    }
}
