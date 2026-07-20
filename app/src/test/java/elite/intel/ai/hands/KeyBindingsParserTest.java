package elite.intel.ai.hands;

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
}
