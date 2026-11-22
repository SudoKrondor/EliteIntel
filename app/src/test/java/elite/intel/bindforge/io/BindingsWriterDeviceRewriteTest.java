package elite.intel.bindforge.io;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The {@code .binds} device rewrite (Foundation F2): moving a device's references when its name changes - naming a
 * device the file knows by hex, renaming one, and clearing one back to hex. The rewrite works on text; the edits not
 * applied hold every binds file, and {@code bindforge.storage.BindsFiles} runs it over each (Storage S5 removed the
 * rewrite across files on disk, uncalled since S4a).
 */
class BindingsWriterDeviceRewriteTest {

    private static final String BINDS = """
            <?xml version="1.0" encoding="UTF-8" ?>
            <Root PresetName="Custom" MajorVersion="4" MinorVersion="2">
            \t<YawAxisRaw>
            \t\t<Binding Device="334443F4" Key="Joy_XAxis" />
            \t\t<Inverted Value="0" />
            \t\t<Deadzone Value="0.00000000" />
            \t</YawAxisRaw>
            \t<UpThrustButton>
            \t\t<Primary Device="Throttle" Key="Joy_5">
            \t\t\t<Modifier Device="throttle" Key="Joy_1" />
            \t\t</Primary>
            \t\t<Secondary Device='Throttle' Key="Neg_Joy_YAxis" />
            \t</UpThrustButton>
            \t<FocusLeftPanel>
            \t\t<Primary Device="Keyboard" Key="Key_Q">
            \t\t\t<Modifier Device="Throttle" Key="Joy_2" />
            \t\t</Primary>
            \t\t<Secondary Device="ThrottleX" Key="Joy_3" SomeDevice="Throttle" />
            \t</FocusLeftPanel>
            </Root>
            """;

    private final BindingsWriter writer = new BindingsWriter();

    @Test
    void renameMovesEveryDeviceAttributeIgnoringCaseAndNothingElse() {
        RewrittenXml result =
                writer.rewriteDeviceReferences(BINDS, DeviceReferenceRewrite.rename("Throttle", "LeftStick"));

        assertEquals(4, result.references());
        assertEquals(withThrottleAs("LeftStick"), result.xml());
    }

    /**
     * Storage S2b (Alan, 2026-10-25): a case-only change rewrites every other spelling to the one chosen, and a
     * reference already spelled that way is neither rewritten nor counted.
     */
    @Test
    void aCaseOnlyRenameRewritesTheOtherSpellingsOnly() {
        RewrittenXml result =
                writer.rewriteDeviceReferences(BINDS, DeviceReferenceRewrite.rename("throttle", "Throttle"));

        assertEquals(1, result.references(), "only the lower-case modifier changes");
        assertEquals(BINDS.replace("Device=\"throttle\"", "Device=\"Throttle\""), result.xml());
    }

    @Test
    void hardwareIdIsMatchedVidThenPidIgnoringCase() {
        RewrittenXml result = writer.rewriteDeviceReferences(BINDS,
                DeviceReferenceRewrite.hardwareIdToName("3344", "43f4", "Stick"));

        assertEquals(1, result.references());
        assertTrue(result.xml().contains("<Binding Device=\"Stick\" Key=\"Joy_XAxis\" />"));
    }

    @Test
    void theReverseOrderIsMovedOnlyFromTheValueFound() {
        String reversed = BINDS.replace("334443F4", "43F43344");

        assertEquals(0, writer.rewriteDeviceReferences(reversed,
                DeviceReferenceRewrite.hardwareIdToName("3344", "43F4", "Stick")).references());
        assertEquals(1, writer.rewriteDeviceReferences(reversed,
                new DeviceReferenceRewrite(Set.of("43F43344"), "Stick")).references());
    }

    @Test
    void clearWritesVidThenPidUpperCase() {
        RewrittenXml result = writer.rewriteDeviceReferences(BINDS,
                DeviceReferenceRewrite.nameToHardwareId("Throttle", "3344", "83f3"));

        assertEquals(4, result.references());
        assertEquals(withThrottleAs("334483F3"), result.xml());
    }

    @Test
    void textWithNoReferenceComesBackUnchanged() {
        RewrittenXml result =
                writer.rewriteDeviceReferences(BINDS, DeviceReferenceRewrite.rename("T-Rudder", "Pedals"));

        assertEquals(0, result.references());
        assertEquals(BINDS, result.xml());
    }

    @Test
    void reservedNamesAndInvalidNamesAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> DeviceReferenceRewrite.rename("Keyboard", "Keys"));
        assertThrows(IllegalArgumentException.class, () -> DeviceReferenceRewrite.rename("Throttle", "mouse"));
        assertThrows(IllegalArgumentException.class, () -> DeviceReferenceRewrite.rename("Throttle", "{NoDevice}"));
        assertThrows(IllegalArgumentException.class, () -> DeviceReferenceRewrite.rename("Throttle", "Left Stick"));
        assertThrows(IllegalArgumentException.class, () -> DeviceReferenceRewrite.rename("Throttle", "Bad\"Name"));
        assertThrows(IllegalArgumentException.class, () -> DeviceReferenceRewrite.rename("Throttle", "Throttle"));
        assertThrows(IllegalArgumentException.class, () -> DeviceReferenceRewrite.hardwareIdToName("334", "43F4", "X"));
        assertThrows(IllegalArgumentException.class, () -> new DeviceReferenceRewrite(Set.of(), "X"));
    }

    @Test
    void anUnterminatedTagIsRefusedRatherThanRewrittenInPart() {
        String broken = BINDS.replace("<Secondary Device='Throttle' Key=\"Neg_Joy_YAxis\" />",
                "<Secondary Device='Throttle' Key=\"Neg_Joy_YAxis");

        assertThrows(IllegalArgumentException.class,
                () -> writer.rewriteDeviceReferences(broken, DeviceReferenceRewrite.rename("Throttle", "LeftStick")));
    }

    // BINDS with its four Throttle references - two slots and two modifiers, either quote, either case - renamed, and
    // the near misses (ThrottleX, a SomeDevice attribute) left alone.
    private static String withThrottleAs(String name) {
        return BINDS
                .replace("<Primary Device=\"Throttle\" Key=\"Joy_5\">", "<Primary Device=\"" + name + "\" Key=\"Joy_5\">")
                .replace("<Modifier Device=\"throttle\"", "<Modifier Device=\"" + name + "\"")
                .replace("<Secondary Device='Throttle'", "<Secondary Device='" + name + "'")
                .replace("<Modifier Device=\"Throttle\" Key=\"Joy_2\"", "<Modifier Device=\"" + name + "\" Key=\"Joy_2\"");
    }
}
