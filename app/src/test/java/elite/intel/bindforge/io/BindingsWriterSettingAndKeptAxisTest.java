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
    void refusesAMissingAnOddlyShapedAndAnUnsafeSetting() {
        byte[] missing = bytes("<Root><Other Value=\"1\" /></Root>");
        byte[] extra = bytes("<Root><MouseSensitivity Value=\"1\" Extra=\"2\" /></Root>");
        byte[] oneCopyOdd = bytes("<Root><MouseGUI Value=\"1\" /><MouseGUI Value=\"1\" Extra=\"2\" /></Root>");

        assertEquals(BindingSaveResult.BINDING_NOT_FOUND, writer.assignSetting(missing, "MouseSensitivity", "2").result());
        assertEquals(BindingSaveResult.UNSUPPORTED_XML, writer.assignSetting(extra, "MouseSensitivity", "2").result());
        assertEquals(BindingSaveResult.UNSUPPORTED_XML, writer.assignSetting(oneCopyOdd, "MouseGUI", "0").result());
        assertEquals(BindingSaveResult.UNKNOWN_KEY, writer.assignSetting(extra, "MouseSensitivity", "\"><x").result());
    }

    /**
     * Every binds file the game saves holds {@code MouseGUI} twice; a change writes both, adding or removing none
     * (bind-editor.md, <em>Toggles</em>, Q4 settled 2026-11-11).
     */
    @Test
    void aRepeatedSettingHasEveryCopyWrittenAndNoneAddedOrRemoved() {
        String file = "<Root>\r\n\t<MouseGUI Value=\"0\" />\r\n\t<EnableMenuGroups Value=\"0\" />\r\n"
                + "\t<MouseGUI Value=\"0\" />\r\n</Root>";

        EditedBinds edited = writer.assignSetting(bytes(file), "MouseGUI", "1");

        assertEquals(BindingSaveResult.SAVED, edited.result());
        assertEquals(file.replace("<MouseGUI Value=\"0\" />", "<MouseGUI Value=\"1\" />"), text(edited));
    }

    @Test
    void copiesThatDifferAreBothWrittenAndCopiesThatAgreeAreNoChange() {
        String differ = "<Root><MouseGUI Value=\"1\" /><EnableMenuGroups Value=\"0\" /><MouseGUI Value=\"0\" /></Root>";

        EditedBinds edited = writer.assignSetting(bytes(differ), "MouseGUI", "1");

        assertEquals(BindingSaveResult.SAVED, edited.result());
        assertEquals(differ.replace("<MouseGUI Value=\"0\" />", "<MouseGUI Value=\"1\" />"), text(edited));
        assertEquals(BindingSaveResult.NO_CHANGE, writer.assignSetting(edited.content(), "MouseGUI", "1").result());
    }

    /**
     * The writer counts the copies {@link BindsValues} reads - the root's direct children - so a commented-out copy
     * is left as it is, and a same-named element nested deeper neither refuses the write nor is written.
     */
    @Test
    void onlyTheRootsOwnChildrenAreCopies() {
        String file = "<Root>\n\t<!-- <MouseGUI Value=\"0\" /> -->\n\t<MouseGUI Value=\"0\" />\n"
                + "\t<Axis><Binding Device=\"{NoDevice}\" Key=\"\" /><MouseGUI>kept</MouseGUI></Axis>\n</Root>";

        EditedBinds edited = writer.assignSetting(bytes(file), "MouseGUI", "1");

        assertEquals(BindingSaveResult.SAVED, edited.result());
        assertEquals(file.replace("\t<MouseGUI Value=\"0\" />", "\t<MouseGUI Value=\"1\" />"), text(edited));
    }

    @Test
    void aSettingOnlyInACommentIsNotFound() {
        byte[] file = bytes("<Root><!-- <MouseGUI Value=\"0\" /> --><Other Value=\"1\" /></Root>");

        assertEquals(BindingSaveResult.BINDING_NOT_FOUND, writer.assignSetting(file, "MouseGUI", "1").result());
    }

    @Test
    void aFileWhoseStructureCannotBeToldIsRefused() {
        byte[] unclosedComment = bytes("<Root><MouseGUI Value=\"0\" /><!-- never closed </Root>");
        byte[] strayEndTag = bytes("<Root></Root></Root><MouseGUI Value=\"0\" />");

        assertEquals(BindingSaveResult.UNSUPPORTED_XML, writer.assignSetting(unclosedComment, "MouseGUI", "1").result());
        assertEquals(BindingSaveResult.UNSUPPORTED_XML, writer.assignSetting(strayEndTag, "MouseGUI", "1").result());
    }

    @Test
    void aFileHoldingOneCopyKeepsOne() {
        String file = "<Root><MouseGUI Value=\"1\" /></Root>";

        EditedBinds edited = writer.assignSetting(bytes(file), "MouseGUI", "0");

        assertEquals("<Root><MouseGUI Value=\"0\" /></Root>", text(edited));
    }

    @Test
    void aValueTheGameWouldNotWriteIsReplaced() {
        EditedBinds edited = writer.assignSetting(bytes("<Root><MouseGUI Value=\"yes\" /></Root>"), "MouseGUI", "1");

        assertEquals("<Root><MouseGUI Value=\"1\" /></Root>", text(edited));
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
