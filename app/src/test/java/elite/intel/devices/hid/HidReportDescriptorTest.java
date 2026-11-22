package elite.intel.devices.hid;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** The raw descriptor reader: every field an Input declares, in order, and nothing a constant or array does. */
class HidReportDescriptorTest {

    private static final HidUsage X = new HidUsage(HidUsage.GENERIC_DESKTOP, 0x30);
    private static final HidUsage Y = new HidUsage(HidUsage.GENERIC_DESKTOP, 0x31);
    private static final HidUsage RZ = new HidUsage(HidUsage.GENERIC_DESKTOP, 0x35);
    private static final HidUsage SLIDER = new HidUsage(HidUsage.GENERIC_DESKTOP, 0x36);
    private static final HidUsage HAT = new HidUsage(HidUsage.GENERIC_DESKTOP, 0x39);

    /** A stick laid out as most are: buttons, padding, a hat, padding, then X, Y and its twist on Rz. */
    @Test
    void aStickDeclaresItsButtonsHatAndAxesInOrder() {
        byte[] stick = bytes(
                0x05, 0x01, 0x09, 0x04, 0xA1, 0x01,             // Generic Desktop, Joystick, Collection
                0x05, 0x09, 0x19, 0x01, 0x29, 0x04,             // Button 1 to 4
                0x15, 0x00, 0x25, 0x01, 0x75, 0x01, 0x95, 0x04,
                0x81, 0x02,                                     // Input (Data, Variable)
                0x95, 0x04, 0x81, 0x03,                         // four bits of padding: Input (Constant)
                0x05, 0x01, 0x09, 0x39,                         // Hat switch
                0x15, 0x00, 0x25, 0x07, 0x75, 0x04, 0x95, 0x01,
                0x81, 0x42,                                     // Input (Data, Variable, Null state)
                0x75, 0x04, 0x95, 0x01, 0x81, 0x03,             // padding
                0x09, 0x30, 0x09, 0x31, 0x09, 0x35,             // X, Y, Rz
                0x16, 0x00, 0x80, 0x26, 0xFF, 0x7F, 0x75, 0x10, 0x95, 0x03,
                0x81, 0x02,
                0xC0);

        List<HidUsage> inputs = HidReportDescriptor.inputs(stick);

        assertEquals(List.of(button(1), button(2), button(3), button(4), HAT, X, Y, RZ), inputs);
    }

    /** An array is a list of what is pressed, not one field per input: it names no axis. */
    @Test
    void anArrayInputIsSkipped() {
        byte[] array = bytes(0x05, 0x01, 0x19, 0x30, 0x29, 0x35, 0x75, 0x08, 0x95, 0x02, 0x81, 0x00);

        assertEquals(List.of(), HidReportDescriptor.inputs(array));
    }

    /** Fields past the last usage take it again, as the HID specification says. */
    @Test
    void fieldsPastTheLastUsageRepeatIt() {
        byte[] sliders = bytes(0x05, 0x01, 0x09, 0x30, 0x09, 0x36, 0x75, 0x10, 0x95, 0x03, 0x81, 0x02);

        assertEquals(List.of(X, SLIDER, SLIDER), HidReportDescriptor.inputs(sliders));
    }

    /** More usages than the report count: the kernel gives each usage a field, so evdev has all three axes. */
    @Test
    void usagesPastTheReportCountStillGetFields() {
        byte[] three = bytes(0x05, 0x01, 0x09, 0x30, 0x09, 0x31, 0x09, 0x32, 0x75, 0x10, 0x95, 0x02, 0x81, 0x02);

        assertEquals(List.of(X, Y, new HidUsage(HidUsage.GENERIC_DESKTOP, 0x32)), HidReportDescriptor.inputs(three));
    }

    /** Delimited sets are alternative usages for one control; the kernel keeps only the first set. */
    @Test
    void onlyTheFirstDelimitedSetIsKept() {
        byte[] alternatives = bytes(0x05, 0x01,
                0xA9, 0x01, 0x09, 0x35, 0xA9, 0x00,             // set 1: Rz
                0xA9, 0x01, 0x09, 0x32, 0xA9, 0x00,             // set 2: Z, the alternative
                0x75, 0x10, 0x95, 0x01, 0x81, 0x02);

        assertEquals(List.of(RZ), HidReportDescriptor.inputs(alternatives));
    }

    @Test
    void aDelimiterOpenedInsideAnotherIsRefused() {
        assertThrows(IllegalArgumentException.class,
                () -> HidReportDescriptor.inputs(bytes(0xA9, 0x01, 0xA9, 0x01)));
    }

    /** A four-byte usage names its page itself, whatever page is current. */
    @Test
    void aFourByteUsageCarriesItsOwnPage() {
        byte[] extended = bytes(0x05, 0x09, 0x0B, 0x35, 0x00, 0x01, 0x00, 0x75, 0x10, 0x95, 0x01, 0x81, 0x02);

        assertEquals(List.of(RZ), HidReportDescriptor.inputs(extended));
    }

    /** Pop puts back the page and count Push saved, so the axes after it are on Generic Desktop again. */
    @Test
    void popRestoresThePushedPage() {
        byte[] pushed = bytes(0x05, 0x01, 0x95, 0x01, 0x75, 0x10, 0xA4,  // Generic Desktop, count 1, Push
                0x05, 0x09, 0x09, 0x01, 0x81, 0x02,                         // Button 1
                0xB4,                                                       // Pop
                0x09, 0x30, 0x81, 0x02);                                    // X

        assertEquals(List.of(button(1), X), HidReportDescriptor.inputs(pushed));
    }

    /** A long item is stepped over whole. */
    @Test
    void aLongItemIsSkipped() {
        byte[] withLong = bytes(0xFE, 0x02, 0x10, 0xAA, 0xBB, 0x05, 0x01, 0x09, 0x31, 0x75, 0x10, 0x95, 0x01,
                0x81, 0x02);

        assertEquals(List.of(Y), HidReportDescriptor.inputs(withLong));
    }

    @Test
    void bytesEndingInsideAnItemAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> HidReportDescriptor.inputs(bytes(0x05, 0x01, 0x26, 0xFF)));
    }

    @Test
    void anImpossibleReportCountIsRefused() {
        assertThrows(IllegalArgumentException.class,
                () -> HidReportDescriptor.inputs(bytes(0x97, 0xFF, 0xFF, 0xFF, 0x7F)));
    }

    private static HidUsage button(int id) {
        return new HidUsage(0x09, id);
    }

    private static byte[] bytes(int... values) {
        byte[] bytes = new byte[values.length];
        for (int i = 0; i < values.length; i++) bytes[i] = (byte) values[i];
        return bytes;
    }
}
