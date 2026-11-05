package elite.intel.bindforge.io;

import elite.intel.bindforge.model.BindingInput;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class BindingsWriterSettingAndKeptAxisTest {

    private final BindingsWriter writer = new BindingsWriter();

    private static byte[] bytes(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }

    private static String text(EditedBinds edited) {
        return new String(edited.content(), StandardCharsets.UTF_8);
    }

    @Test
    void writesASettingAndNothingElse() {
        String file = "<Root>\n\t<ThrottleRange Value=\"\" />\n\t<ThrottleIncrement Value=\"0.1\" />\n</Root>";

        EditedBinds edited = writer.assignSetting(bytes(file), "ThrottleRange", "Bindings_ThrottleForewardOnly");

        assertEquals(BindingSaveResult.SAVED, edited.result());
        assertEquals(file.replace("<ThrottleRange Value=\"\"", "<ThrottleRange Value=\"Bindings_ThrottleForewardOnly\""),
                text(edited));
    }

    @Test
    void anEmptySettingValueIsWrittenNotRemoved() {
        EditedBinds edited = writer.assignSetting(bytes("<Root><ThrottleRange Value=\"Bindings_ThrottleForewardOnly\" /></Root>"),
                "ThrottleRange", "");

        assertEquals("<Root><ThrottleRange Value=\"\" /></Root>", text(edited));
    }

    @Test
    void theSameValueIsNoChange() {
        byte[] file = bytes("<Root><MouseSensitivity Value=\"1.00000000\" /></Root>");

        EditedBinds edited = writer.assignSetting(file, "MouseSensitivity", "1.00000000");

        assertEquals(BindingSaveResult.NO_CHANGE, edited.result());
        assertArrayEquals(file, edited.content());
    }

    @Test
    void refusesAMissingARepeatedAndAnUnsafeSetting() {
        byte[] missing = bytes("<Root><Other Value=\"1\" /></Root>");
        byte[] repeated = bytes("<Root><HeadlookSensitivity Value=\"1\" /><HeadlookSensitivity Value=\"2\" /></Root>");
        byte[] extra = bytes("<Root><MouseSensitivity Value=\"1\" Extra=\"2\" /></Root>");

        assertEquals(BindingSaveResult.BINDING_NOT_FOUND, writer.assignSetting(missing, "MouseSensitivity", "2").result());
        assertEquals(BindingSaveResult.UNSUPPORTED_XML, writer.assignSetting(repeated, "HeadlookSensitivity", "3").result());
        assertEquals(BindingSaveResult.UNSUPPORTED_XML, writer.assignSetting(extra, "MouseSensitivity", "2").result());
        assertEquals(BindingSaveResult.UNKNOWN_KEY, writer.assignSetting(extra, "MouseSensitivity", "\"><x").result());
    }

    @Test
    void anAxisKeepsAnInputTheWriterWouldNotAssignWhenOnlyItsDirectionChanges() {
        String file = "<Root><YawAxisRaw>\n<Binding Device=\"GamePad\" Key=\"GamePad_LStickX\" />\n"
                + "<Inverted Value=\"0\" />\n<Deadzone Value=\"0.10000000\" />\n</YawAxisRaw></Root>";

        EditedBinds edited = writer.assignAxis(bytes(file), new AxisAssignment("YawAxisRaw",
                new BindingInput("GamePad", "GamePad_LStickX"), true, 0.1f));

        assertEquals(BindingSaveResult.SAVED, edited.result());
        assertTrue(text(edited).contains("<Inverted Value=\"1\" />"));
        assertTrue(text(edited).contains("Key=\"GamePad_LStickX\""));
    }

    @Test
    void anAxisStillRefusesAMoveOntoSuchAnInput() {
        String file = "<Root><YawAxisRaw>\n<Binding Device=\"Stick\" Key=\"Joy_XAxis\" />\n"
                + "<Inverted Value=\"0\" />\n<Deadzone Value=\"0.10000000\" />\n</YawAxisRaw></Root>";

        assertEquals(BindingSaveResult.UNKNOWN_KEY, writer.assignAxis(bytes(file), new AxisAssignment("YawAxisRaw",
                new BindingInput("GamePad", "GamePad_LStickX"), false, 0.1f)).result());
    }
}
