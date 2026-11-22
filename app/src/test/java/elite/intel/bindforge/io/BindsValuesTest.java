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
    void aRepeatedAxisIsNotEditable() throws IOException {
        BindsValues values = read(axis("YawAxisAlternate", "A", "Joy_XAxis", "0", "0")
                + axis("YawAxisAlternate", "A", "Joy_YAxis", "0", "0"));

        assertFalse(values.axes().get("YawAxisAlternate").editable());
    }

    /**
     * Every binds file the game saves holds {@code MouseGUI} twice; it is one setting, shown by its first copy, and the
     * writer writes every copy (bind-editor.md, <em>Toggles</em>, Q4 settled 2026-11-11).
     */
    @Test
    void aRepeatedPlainSettingIsOneEditableSettingShownByItsFirstCopy() throws IOException {
        BindsValues values = read("<MouseGUI Value=\"1\" /><EnableMenuGroups Value=\"0\" /><MouseGUI Value=\"0\" />");

        assertTrue(values.settings().get("MouseGUI").editable());
        assertEquals("1", values.settings().get("MouseGUI").value());
    }

    @Test
    void aRepeatedSettingWithOneCopyInAnotherShapeIsNotEditable() throws IOException {
        BindsValues values = read("<MouseGUI Value=\"1\" /><MouseGUI Value=\"1\" Extra=\"2\" />");

        assertFalse(values.settings().get("MouseGUI").editable());
    }

    @Test
    void theGameSavedReferenceFileHoldsMouseGuiTwiceAndItIsEditable() throws IOException {
        BindsValues values = BindsValues.of(Files.readAllBytes(
                Path.of("../docs/02-features/bindforge/reference-data/Custom.4.2.binds")));

        assertTrue(values.settings().get("MouseGUI").editable());
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
