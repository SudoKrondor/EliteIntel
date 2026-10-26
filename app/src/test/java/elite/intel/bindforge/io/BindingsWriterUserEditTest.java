package elite.intel.bindforge.io;

import elite.intel.bindforge.model.BindingInput;
import elite.intel.bindforge.model.BindingSlotType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The writer's user-edit path (Foundation F4): a deliberate edit in the bindings editor may write any device, with
 * modifiers on any device and Hold, and an axis with Inverted and Deadzone.
 */
class BindingsWriterUserEditTest {

    private static final String BINDS = """
            <Root>
            \t<YawAxisRaw>
            \t\t<Binding Device="{NoDevice}" Key="" />
            \t\t<Inverted Value="0" />
            \t\t<Deadzone Value="0.40000001" />
            \t</YawAxisRaw>
            \t<UpThrustButton>
            \t\t<Primary Device="Throttle" Key="Joy_5" />
            \t\t<Secondary Device="Keyboard" Key="Key_R">
            \t\t\t<Hold Value="1" />
            \t\t</Secondary>
            \t</UpThrustButton>
            \t<ToggleCargoScoop>
            \t\t<Primary Device="Keyboard" Key="Key_Home" />
            \t\t<Secondary Device="vJoy" Key="Joy_11">
            \t\t\t<Modifier Device="Throttle" Key="Joy_1" />
            \t\t</Secondary>
            \t\t<ToggleOn Value="1" />
            \t</ToggleCargoScoop>
            \t<OddButton>
            \t\t<Primary Device="Throttle" Key="Joy_2">
            \t\t\t<Unknown Value="1" />
            \t\t</Primary>
            \t\t<Secondary Device="{NoDevice}" Key="" />
            \t</OddButton>
            </Root>
            """;

    @TempDir
    Path tempDir;

    private final BindingsWriter writer = new BindingsWriter();

    @Test
    void aControllerSlotCanBeReboundToAnotherController() throws IOException {
        Path file = binds();

        BindingSaveResult result = writer.assignSlot(slot(file, "UpThrustButton", BindingSlotType.PRIMARY,
                new BindingInput("Stick", "Joy_POV1Up"), List.of(), false));

        assertEquals(BindingSaveResult.SAVED, result);
        assertEquals(BINDS.replace("<Primary Device=\"Throttle\" Key=\"Joy_5\" />",
                "<Primary Device=\"Stick\" Key=\"Joy_POV1Up\" />"), read(file));
    }

    @Test
    void modifiersOnAnyDeviceAndHoldAreWrittenWithTheFilesIndentation() throws IOException {
        Path file = binds();

        BindingSaveResult result = writer.assignSlot(slot(file, "UpThrustButton", BindingSlotType.PRIMARY,
                new BindingInput("Throttle", "Joy_6"),
                List.of(new BindingInput("Keyboard", "Key_LeftShift"), new BindingInput("Stick", "Joy_3")), true));

        assertEquals(BindingSaveResult.SAVED, result);
        assertEquals(BINDS.replace("\t\t<Primary Device=\"Throttle\" Key=\"Joy_5\" />\n", """
                \t\t<Primary Device="Throttle" Key="Joy_6">
                \t\t\t<Hold Value="1" />
                \t\t\t<Modifier Device="Keyboard" Key="Key_LeftShift" />
                \t\t\t<Modifier Device="Stick" Key="Joy_3" />
                \t\t</Primary>
                """), read(file));
    }

    @Test
    void holdComesOffWhenTheEditSaysSo() throws IOException {
        Path file = binds();

        assertEquals(BindingSaveResult.SAVED, writer.assignSlot(slot(file, "UpThrustButton", BindingSlotType.SECONDARY,
                new BindingInput("Keyboard", "Key_R"), List.of(), false)));
        assertTrue(read(file).contains("\t\t<Secondary Device=\"Keyboard\" Key=\"Key_R\" />\n\t</UpThrustButton>"));
    }

    @Test
    void aChordAnotherSlotUsesIsNotRefused() throws IOException {
        Path file = binds();

        // Key_Home is ToggleCargoScoop's Primary; the keyboard path would answer KEY_OCCUPIED.
        assertEquals(BindingSaveResult.SAVED, writer.assignSlot(slot(file, "UpThrustButton", BindingSlotType.PRIMARY,
                new BindingInput("Keyboard", "Key_Home"), List.of(), false)));
    }

    @Test
    void theSameValueWritesNothing() throws IOException {
        Path file = binds();
        FileTime before = Files.getLastModifiedTime(file);

        assertEquals(BindingSaveResult.NO_CHANGE, writer.assignSlot(slot(file, "ToggleCargoScoop",
                BindingSlotType.SECONDARY, new BindingInput("vJoy", "Joy_11"),
                List.of(new BindingInput("Throttle", "Joy_1")), false)));
        assertEquals(BindingSaveResult.NO_CHANGE, writer.assignSlot(slot(file, "UpThrustButton",
                BindingSlotType.SECONDARY, new BindingInput("Keyboard", "Key_R"), List.of(), true)));
        assertEquals(before, Files.getLastModifiedTime(file));
        assertEquals(BINDS, read(file));
    }

    @Test
    void clearingWritesTheEmptySlotAndLeavesToggleOn() throws IOException {
        Path file = binds();
        SlotAssignment clear = SlotAssignment.clear(file, "ToggleCargoScoop", BindingSlotType.SECONDARY,
                Files.getLastModifiedTime(file), Files.size(file));

        assertEquals(BindingSaveResult.SAVED, writer.assignSlot(clear));
        assertTrue(read(file).contains(
                "\t\t<Secondary Device=\"{NoDevice}\" Key=\"\" />\n\t\t<ToggleOn Value=\"1\" />"));
    }

    @Test
    void tokensThatDoNotSuitAButtonSlotAreRefused() throws IOException {
        Path file = binds();
        List<BindingInput> refused = List.of(
                new BindingInput("Throttle", "Joy_XAxis"),
                new BindingInput("Throttle", "Joy_POV5Up"),
                new BindingInput("Throttle", "Joy_0"),
                new BindingInput("Mouse", "Pos_Mouse_XAxis"),
                new BindingInput("Keyboard", "Joy_1"),
                new BindingInput("keyboard", "Joy_1"),
                new BindingInput("MOUSE", "Mouse_1"),
                new BindingInput("Keyboard", "Key_"),
                new BindingInput("{NoDevice}", "Key_A"),
                new BindingInput("Left Stick", "Joy_1"),
                new BindingInput("Throttle", "Joy_1\" Evil=\"x"));

        for (BindingInput input : refused) {
            assertEquals(BindingSaveResult.UNKNOWN_KEY, writer.assignSlot(slot(file, "UpThrustButton",
                    BindingSlotType.PRIMARY, input, List.of(), false)), input.toString());
        }
        assertEquals(BindingSaveResult.UNKNOWN_KEY, writer.assignSlot(slot(file, "UpThrustButton",
                BindingSlotType.PRIMARY, new BindingInput("Throttle", "Joy_1"),
                List.of(new BindingInput("Stick", "Joy_YAxis")), false)));
        assertEquals(BINDS, read(file));
    }

    @Test
    void buttonInputsTheGameWritesAreAccepted() throws IOException {
        List<BindingInput> accepted = List.of(
                new BindingInput("Mouse", "Mouse_4"),
                new BindingInput("Mouse", "Neg_Mouse_ZAxis"),
                new BindingInput("334443F4", "Neg_Joy_RYAxis"),
                new BindingInput("T-Rudder", "Pos_Joy_UAxis"),
                new BindingInput("GamePad", "GamePad_FaceDown"),
                new BindingInput("GamePad", "GamePad_FaceRight"),
                new BindingInput("GamePad", "GamePad_FaceLeft"),
                new BindingInput("GamePad", "GamePad_FaceUp"),
                new BindingInput("GamePad", "GamePad_Back"),
                new BindingInput("GamePad", "GamePad_LBumper"),
                new BindingInput("GamePad", "GamePad_RBumper"),
                new BindingInput("GamePad", "GamePad_LThumb"),
                new BindingInput("GamePad", "GamePad_RThumb"),
                new BindingInput("GamePad", "GamePad_DPadUp"),
                new BindingInput("GamePad", "GamePad_DPadRight"),
                new BindingInput("GamePad", "GamePad_DPadDown"),
                new BindingInput("GamePad", "GamePad_DPadLeft"),
                new BindingInput("Keyboard", "Key_Numpad_Enter"),
                // A non-English layout writes the character itself.
                new BindingInput("Keyboard", "Key_ß"));

        for (BindingInput input : accepted) {
            Path file = binds();
            assertEquals(BindingSaveResult.SAVED, writer.assignSlot(slot(file, "UpThrustButton",
                    BindingSlotType.PRIMARY, input, List.of(), false)), input.toString());
        }
    }

    @Test
    void aSlotWithUnknownStructureIsNeverRewritten() throws IOException {
        Path file = binds();

        assertEquals(BindingSaveResult.UNSUPPORTED_XML, writer.assignSlot(slot(file, "OddButton",
                BindingSlotType.PRIMARY, new BindingInput("Throttle", "Joy_3"), List.of(), false)));
        assertEquals(BINDS, read(file));
    }

    @Test
    void aFileChangedSinceItWasReadIsRefused() throws IOException {
        Path file = binds();
        SlotAssignment edit = new SlotAssignment(file, "UpThrustButton", BindingSlotType.PRIMARY,
                new BindingInput("Throttle", "Joy_6"), List.of(), false, FileTime.fromMillis(0), Files.size(file));

        assertEquals(BindingSaveResult.STALE_FILE, writer.assignSlot(edit));
        assertEquals(BINDS, read(file));
    }

    @Test
    void anUnknownBindingIsNotFound() throws IOException {
        Path file = binds();

        assertEquals(BindingSaveResult.BINDING_NOT_FOUND, writer.assignSlot(slot(file, "NoSuchButton",
                BindingSlotType.PRIMARY, new BindingInput("Throttle", "Joy_6"), List.of(), false)));
    }

    @Test
    void impossibleRequestsAreRejectedWhenBuilt() {
        Path file = tempDir.resolve("x.binds");
        BindingInput input = new BindingInput("Throttle", "Joy_1");
        List<BindingInput> four = List.of(new BindingInput("Throttle", "Joy_2"), new BindingInput("Throttle", "Joy_3"),
                new BindingInput("Throttle", "Joy_4"), new BindingInput("Throttle", "Joy_5"));

        assertThrows(IllegalArgumentException.class, () -> new SlotAssignment(file, "B", BindingSlotType.PRIMARY,
                input, four, false, FileTime.fromMillis(0), 0));
        assertThrows(IllegalArgumentException.class, () -> new SlotAssignment(file, "B", BindingSlotType.PRIMARY,
                input, List.of(new BindingInput("throttle", "joy_1")), false, FileTime.fromMillis(0), 0));
        assertThrows(IllegalArgumentException.class, () -> new SlotAssignment(file, "B", BindingSlotType.PRIMARY,
                null, List.of(), true, FileTime.fromMillis(0), 0));
        assertThrows(IllegalArgumentException.class, () -> new AxisAssignment(file, "A", null, false, 1.5f,
                FileTime.fromMillis(0), 0));
        assertThrows(IllegalArgumentException.class, () -> new AxisAssignment(file, "A", null, false, Float.NaN,
                FileTime.fromMillis(0), 0));
    }

    @Test
    void anAxisTakesAControllersWholeAxisWithInvertedAndDeadzone() throws IOException {
        Path file = binds();

        BindingSaveResult result = writer.assignAxis(axis(file, "YawAxisRaw",
                new BindingInput("Stick", "Joy_VAxis"), true, 0.05f));

        assertEquals(BindingSaveResult.SAVED, result);
        assertEquals(BINDS.replace("""
                \t\t<Binding Device="{NoDevice}" Key="" />
                \t\t<Inverted Value="0" />
                \t\t<Deadzone Value="0.40000001" />
                """, """
                \t\t<Binding Device="Stick" Key="Joy_VAxis" />
                \t\t<Inverted Value="1" />
                \t\t<Deadzone Value="0.05000000" />
                """), read(file));
    }

    @Test
    void aDeadzoneEqualAsAFloatKeepsItsText() throws IOException {
        Path file = binds();

        assertEquals(BindingSaveResult.NO_CHANGE,
                writer.assignAxis(axis(file, "YawAxisRaw", null, false, 0.4f)));
        assertEquals(BindingSaveResult.SAVED,
                writer.assignAxis(axis(file, "YawAxisRaw", new BindingInput("Stick", "Joy_XAxis"), false, 0.4f)));
        assertTrue(read(file).contains("<Deadzone Value=\"0.40000001\" />"));
    }

    @Test
    void aDeadzoneIsWrittenTheWayTheGameWritesAFloat() throws IOException {
        Path file = binds();

        writer.assignAxis(axis(file, "YawAxisRaw", null, false, 0.3f));

        assertTrue(read(file).contains("<Deadzone Value=\"0.30000001\" />"));
    }

    @Test
    void anAxisRefusesKeysButtonsAndTheMouse() throws IOException {
        Path file = binds();
        List<BindingInput> refused = List.of(
                new BindingInput("Keyboard", "Key_A"),
                new BindingInput("Mouse", "Mouse_1"),
                new BindingInput("Stick", "Joy_3"),
                new BindingInput("Stick", "Neg_Joy_XAxis"),
                new BindingInput("Keyboard", "Joy_XAxis"),
                new BindingInput("mouse", "Joy_XAxis"));

        for (BindingInput input : refused) {
            assertEquals(BindingSaveResult.UNKNOWN_KEY,
                    writer.assignAxis(axis(file, "YawAxisRaw", input, false, 0f)), input.toString());
        }
        assertEquals(BINDS, read(file));
    }

    @Test
    void aHoldAttributeKeepsItsForm() throws IOException {
        String withAttribute = BINDS.replace("<Primary Device=\"Keyboard\" Key=\"Key_Home\" />",
                "<Primary Device=\"Keyboard\" Key=\"Key_Home\" Hold=\"1\" />");
        Path file = binds();
        Files.writeString(file, withAttribute, StandardCharsets.UTF_8);

        assertEquals(BindingSaveResult.NO_CHANGE, writer.assignSlot(slot(file, "ToggleCargoScoop",
                BindingSlotType.PRIMARY, new BindingInput("Keyboard", "Key_Home"), List.of(), true)));
        assertEquals(BindingSaveResult.SAVED, writer.assignSlot(slot(file, "ToggleCargoScoop",
                BindingSlotType.PRIMARY, new BindingInput("Keyboard", "Key_End"), List.of(), true)));
        assertEquals(withAttribute.replace("Key=\"Key_Home\" Hold=\"1\"", "Key=\"Key_End\" Hold=\"1\""),
                read(file));
    }

    @Test
    void aButtonElementIsNotAnAxis() throws IOException {
        Path file = binds();

        assertEquals(BindingSaveResult.UNSUPPORTED_XML,
                writer.assignAxis(axis(file, "UpThrustButton", new BindingInput("Stick", "Joy_XAxis"), false, 0f)));
        assertEquals(BINDS, read(file));
    }

    @Test
    void theByteOrderMarkSurvivesAUserEdit() throws IOException {
        Path file = tempDir.resolve("Custom.4.2.binds");
        byte[] body = BINDS.getBytes(StandardCharsets.UTF_8);
        byte[] content = new byte[body.length + 3];
        content[0] = (byte) 0xEF;
        content[1] = (byte) 0xBB;
        content[2] = (byte) 0xBF;
        System.arraycopy(body, 0, content, 3, body.length);
        Files.write(file, content);

        assertEquals(BindingSaveResult.SAVED, writer.assignSlot(slot(file, "UpThrustButton", BindingSlotType.PRIMARY,
                new BindingInput("Throttle", "Joy_6"), List.of(), false)));
        byte[] written = Files.readAllBytes(file);
        assertEquals((byte) 0xEF, written[0]);
        assertEquals((byte) 0xBB, written[1]);
        assertEquals((byte) 0xBF, written[2]);
    }

    private SlotAssignment slot(Path file, String bindingId, BindingSlotType slotType, BindingInput input,
                                List<BindingInput> modifiers, boolean hold) throws IOException {
        return new SlotAssignment(file, bindingId, slotType, input, modifiers, hold,
                Files.getLastModifiedTime(file), Files.size(file));
    }

    private AxisAssignment axis(Path file, String bindingId, BindingInput input, boolean inverted, float deadzone)
            throws IOException {
        return new AxisAssignment(file, bindingId, input, inverted, deadzone,
                Files.getLastModifiedTime(file), Files.size(file));
    }

    private Path binds() throws IOException {
        Path file = Files.createTempFile(tempDir, "Custom", ".binds");
        Files.writeString(file, BINDS, StandardCharsets.UTF_8);
        return file;
    }

    private String read(Path file) throws IOException {
        return Files.readString(file, StandardCharsets.UTF_8);
    }
}
