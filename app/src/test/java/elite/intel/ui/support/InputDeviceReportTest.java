package elite.intel.ui.support;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Linux parser is fed a real {@code /proc/bus/input/devices} shape: a gaming mouse that also presents
 * a keyboard and a consumer-control interface, a gaming keyboard that also presents a pointer, a PS/2
 * board, and the power buttons that register as keyboards without being one.
 */
class InputDeviceReportTest {

    private static final String LISTING = """
            I: Bus=0019 Vendor=0000 Product=0001 Version=0000
            N: Name="Power Button"
            P: Phys=LNXPWRBN/button/input0
            H: Handlers=kbd event1
            B: EV=3
            B: KEY=8000 10000000000000 0
            
            I: Bus=0003 Vendor=1b1c Product=1b5c Version=0111
            N: Name="Corsair CORSAIR NIGHTSWORD RGB Gaming Mouse"
            H: Handlers=mouse0 event3 js0
            B: EV=17
            B: REL=903
            
            I: Bus=0003 Vendor=1b1c Product=1b5c Version=0111
            N: Name="Corsair CORSAIR NIGHTSWORD RGB Gaming Mouse Consumer Control"
            H: Handlers=kbd event4
            B: EV=1f
            
            I: Bus=0003 Vendor=1b1c Product=1b5c Version=0111
            N: Name="Corsair CORSAIR NIGHTSWORD RGB Gaming Mouse Keyboard"
            H: Handlers=sysrq kbd event8
            B: EV=100013
            
            I: Bus=0003 Vendor=1b1c Product=1b95 Version=0111
            N: Name="Corsair CORSAIR K95 RGB PLATINUM XT Mechanical Gaming Keyboard"
            H: Handlers=sysrq kbd event9 leds
            B: EV=12001f
            
            I: Bus=0003 Vendor=1b1c Product=1b95 Version=0111
            N: Name="Corsair CORSAIR K95 RGB PLATINUM XT Mechanical Gaming Keyboard"
            H: Handlers=mouse1 event10 js1
            B: EV=17
            
            I: Bus=0011 Vendor=0001 Product=0001 Version=ab41
            N: Name="AT Translated Set 2 keyboard"
            H: Handlers=sysrq kbd event2 leds
            B: EV=120013
            """;

    @Test
    void foldsInterfacesIntoDevicesAndJudgesRolesByCapability(@TempDir Path tmp) throws IOException {
        Path listing = Files.writeString(tmp.resolve("devices"), LISTING);

        String report = InputDeviceReport.linux(listing);

        assertEquals("""
                Keyboard: Corsair CORSAIR K95 RGB PLATINUM XT Mechanical Gaming Keyboard [usb 1b1c:1b95]
                Keyboard: AT Translated Set 2 keyboard [ps/2 0001:0001]
                Mouse: Corsair CORSAIR NIGHTSWORD RGB Gaming Mouse [usb 1b1c:1b5c]""", report);
    }

    @Test
    void anEmptyListingSaysSoPerRole(@TempDir Path tmp) throws IOException {
        Path listing = Files.writeString(tmp.resolve("devices"), "");

        assertEquals("Keyboard: none recognised\nMouse: none recognised", InputDeviceReport.linux(listing));
    }

    @Test
    void aListingThatCannotBeReadIsOneLine(@TempDir Path tmp) {
        String report = InputDeviceReport.linux(tmp.resolve("missing"));

        assertTrue(report.startsWith("Input devices: could not be read ("), report);
    }
}
