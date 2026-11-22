package elite.intel.devices.hid;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Finding the descriptor under {@code /sys} from SDL's evdev node, and saying nothing when it is not there. */
class LinuxHidUsagesTest {

    @TempDir
    Path sys;

    @Test
    void theEvdevNodeLeadsToItsHidDescriptor() throws IOException {
        writeDescriptor("event12", new byte[]{0x05, 0x01, 0x09, 0x35, 0x75, 0x10, (byte) 0x95, 0x01, (byte) 0x81, 0x02});

        Optional<List<HidUsage>> inputs = LinuxHidUsages.read(sys, "/dev/input/event12");

        assertEquals(Optional.of(List.of(new HidUsage(HidUsage.GENERIC_DESKTOP, 0x35))), inputs);
    }

    /** A controller that is not HID has no descriptor beside its input device. */
    @Test
    void noDescriptorIsNothingKnown() {
        assertEquals(Optional.empty(), LinuxHidUsages.read(sys, "/dev/input/event3"));
    }

    @Test
    void aPathThatIsNotAnEvdevNodeIsNothingKnown() {
        assertEquals(Optional.empty(), LinuxHidUsages.read(sys, "/dev/input/js0"));
        assertEquals(Optional.empty(), LinuxHidUsages.read(sys, ""));
        assertEquals(Optional.empty(), LinuxHidUsages.read(sys, null));
    }

    @Test
    void aDescriptorThatCannotBeReadIsNothingKnown() throws IOException {
        writeDescriptor("event4", new byte[]{0x05});

        assertEquals(Optional.empty(), LinuxHidUsages.read(sys, "/dev/input/event4"));
    }

    private void writeDescriptor(String node, byte[] bytes) throws IOException {
        Path descriptor = sys.resolve("class/input").resolve(node).resolve("device/device/report_descriptor");
        Files.createDirectories(descriptor.getParent());
        Files.write(descriptor, bytes);
    }
}
