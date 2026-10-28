package elite.intel.bindforge.io;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Covers how a slot's press-and-hold flag is read out of a .binds file. Elite writes it as a
 * {@code <Hold Value="1" />} child element; reading only the {@code Hold="1"} attribute made every
 * long-press binding execute as a tap.
 */
class KeyBindingsParserTest {

    @TempDir
    Path tempDir;

    private File bindsFile(String body) throws Exception {
        Path file = tempDir.resolve("Custom.4.2.binds");
        Files.writeString(file, "<Root PresetName=\"Test\" MajorVersion=\"4\" MinorVersion=\"2\">\n"
                + body
                + "\n</Root>\n");
        return file.toFile();
    }

    @Test
    void holdChildElementMarksTheBindingAsHeld() throws Exception {
        File file = bindsFile("""
                	<UseBoostJuice>
                		<Primary Device="Keyboard" Key="Key_Tab">
                			<Hold Value="1" />
                		</Primary>
                		<Secondary Device="{NoDevice}" Key="" />
                	</UseBoostJuice>
                """);

        Map<String, KeyBindingsParser.KeyBinding> bindings =
                KeyBindingsParser.getInstance().parseBindings(file);

        KeyBindingsParser.KeyBinding binding = bindings.get("UseBoostJuice");
        assertNotNull(binding, "keyboard binding should be executable");
        assertTrue(binding.hold, "<Hold Value=\"1\"/> child element must set the hold flag");
    }

    @Test
    void holdAttributeIsStillHonoured() throws Exception {
        File file = bindsFile("""
                	<UseBoostJuice>
                		<Primary Device="Keyboard" Key="Key_Tab" Hold="1" />
                		<Secondary Device="{NoDevice}" Key="" />
                	</UseBoostJuice>
                """);

        Map<String, KeyBindingsParser.KeyBinding> bindings =
                KeyBindingsParser.getInstance().parseBindings(file);

        assertTrue(bindings.get("UseBoostJuice").hold, "attribute form must keep working");
    }

    @Test
    void aSlotWithoutAHoldFlagIsATap() throws Exception {
        File file = bindsFile("""
                	<UseBoostJuice>
                		<Primary Device="Keyboard" Key="Key_Tab" />
                		<Secondary Device="{NoDevice}" Key="" />
                	</UseBoostJuice>
                """);

        Map<String, KeyBindingsParser.KeyBinding> bindings =
                KeyBindingsParser.getInstance().parseBindings(file);

        assertFalse(bindings.get("UseBoostJuice").hold, "no Hold flag means a plain tap");
    }

    @Test
    void holdValueZeroIsNotAHold() throws Exception {
        File file = bindsFile("""
                	<UseBoostJuice>
                		<Primary Device="Keyboard" Key="Key_Tab">
                			<Hold Value="0" />
                		</Primary>
                		<Secondary Device="{NoDevice}" Key="" />
                	</UseBoostJuice>
                """);

        Map<String, KeyBindingsParser.KeyBinding> bindings =
                KeyBindingsParser.getInstance().parseBindings(file);

        assertFalse(bindings.get("UseBoostJuice").hold, "Value=\"0\" must not count as a hold");
    }

    @Test
    void holdOnANonKeyboardSlotDoesNotLeakIntoTheExecutableMap() throws Exception {
        File file = bindsFile("""
                	<ToggleReverseThrottleInput>
                		<Primary Device="T16000MTHROTTLE" DeviceIndex="0" Key="Joy_3">
                			<Hold Value="1" />
                		</Primary>
                		<Secondary Device="Keyboard" Key="Key_R" />
                	</ToggleReverseThrottleInput>
                """);

        Map<String, KeyBindingsParser.KeyBinding> bindings =
                KeyBindingsParser.getInstance().parseBindings(file);

        KeyBindingsParser.KeyBinding binding = bindings.get("ToggleReverseThrottleInput");
        assertNotNull(binding, "the keyboard secondary should still be executable");
        assertFalse(binding.hold, "the joystick primary's hold must not carry over to the keyboard slot");
    }

    @Test
    void anActionWithAKeyboardSlotIsNotReportedAsHavingNoKeyboardBinding() throws Exception {
        // The commander's real GalaxyMapOpen: a chorded keyboard primary and an empty secondary. The old
        // condition warned for every action that simply had both slot elements, so this one was reported as
        // unbound while the executor was pressing Shift+K for it in the same log.
        File file = bindsFile("""
                	<GalaxyMapOpen>
                		<Primary Device="Keyboard" Key="Key_K">
                			<Modifier Device="Keyboard" Key="Key_LeftShift" />
                		</Primary>
                		<Secondary Device="{NoDevice}" Key="" />
                	</GalaxyMapOpen>
                """);

        KeyBindingsParser.ReadOnlyBindingSlots slots =
                KeyBindingsParser.getInstance().parseReadOnlyBindingSlots(file).get("GalaxyMapOpen");

        assertTrue(slots.primary().keyboardUsable(), "a chorded keyboard primary is pressable");
        assertFalse(KeyBindingsParser.isBoundToNonKeyboardDeviceOnly(slots.primary(), slots.secondary()));
    }

    @Test
    void anActionBoundOnlyToAGamepadIsReportedAsHavingNoKeyboardBinding() throws Exception {
        File file = bindsFile("""
                	<ShipSpotLightToggle>
                		<Primary Device="{NoDevice}" Key="" />
                		<Secondary Device="045E02E3" Key="GamePad_LBumper" />
                	</ShipSpotLightToggle>
                """);

        KeyBindingsParser.ReadOnlyBindingSlots slots =
                KeyBindingsParser.getInstance().parseReadOnlyBindingSlots(file).get("ShipSpotLightToggle");

        assertTrue(KeyBindingsParser.isBoundToNonKeyboardDeviceOnly(slots.primary(), slots.secondary()),
                "bound to something EliteIntel cannot press - that is the case worth a warning");
    }

    @Test
    void anActionBoundNowhereIsLeftToTheMissingBindingCheck() throws Exception {
        File file = bindsFile("""
                	<ShipSpotLightToggle>
                		<Primary Device="{NoDevice}" Key="" />
                		<Secondary Device="{NoDevice}" Key="" />
                	</ShipSpotLightToggle>
                """);

        KeyBindingsParser.ReadOnlyBindingSlots slots =
                KeyBindingsParser.getInstance().parseReadOnlyBindingSlots(file).get("ShipSpotLightToggle");

        assertFalse(KeyBindingsParser.isBoundToNonKeyboardDeviceOnly(slots.primary(), slots.secondary()),
                "an unassigned action is the missing-binding check's business, not this warning's");
    }

    /** The Bind Editor reads the edits not applied, held in memory (Storage S5) - the same slots as from a file. */
    @Test
    void aBindsFileHeldInMemoryReadsAsTheFileDoes() throws Exception {
        File file = bindsFile("""
                	<UseBoostJuice>
                		<Primary Device="Keyboard" Key="Key_Tab">
                			<Hold Value="1" />
                		</Primary>
                		<Secondary Device="Throttle" Key="Joy_4" />
                	</UseBoostJuice>
                """);
        byte[] content = Files.readAllBytes(file.toPath());
        byte[] withBom = new byte[content.length + 3];
        withBom[0] = (byte) 0xEF;
        withBom[1] = (byte) 0xBB;
        withBom[2] = (byte) 0xBF;
        System.arraycopy(content, 0, withBom, 3, content.length);

        KeyBindingsParser parser = KeyBindingsParser.getInstance();
        String fromFile = described(parser.parseReadOnlyBindingSlots(file));
        assertEquals("UseBoostJuice: Keyboard/Key_Tab hold, Throttle/Joy_4", fromFile);
        assertEquals(fromFile, described(parser.parseReadOnlyBindingSlots(content)));
        assertEquals(fromFile, described(parser.parseReadOnlyBindingSlots(withBom)), "a byte-order mark is read past");
    }

    private static String described(Map<String, KeyBindingsParser.ReadOnlyBindingSlots> slots) {
        StringBuilder text = new StringBuilder();
        slots.forEach((id, pair) -> text.append(id).append(": ").append(described(pair.primary()))
                .append(", ").append(described(pair.secondary())));
        return text.toString();
    }

    private static String described(KeyBindingsParser.ReadOnlyBindingSlot slot) {
        return slot.device() + "/" + slot.key() + (slot.hold() ? " hold" : "");
    }
}
