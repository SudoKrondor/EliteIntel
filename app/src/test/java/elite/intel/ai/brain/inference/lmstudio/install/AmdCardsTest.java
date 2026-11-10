package elite.intel.ai.brain.inference.lmstudio.install;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AMD cards are read from what the driver publishes: {@code amdgpu}'s files on Linux, the registry and the GPU memory
 * counter on Windows. The Linux tree here is laid out as {@code /sys/class/drm} is.
 */
class AmdCardsTest {

    private static final long GIB = 1024L * 1024 * 1024;

    @Test
    void anAmdCardIsReadWithItsMemory(@TempDir Path drm) throws IOException {
        card(drm, "card1", "0x1002", "0x744c", 24 * GIB, 2 * GIB);

        List<AmdCards.Card> cards = AmdCards.linuxCards(drm);

        assertEquals(List.of(new AmdCards.Card("AMD device 0x744c", OptionalLong.of(24_576))), cards);
        assertEquals(OptionalLong.of(2_048), AmdCards.linuxUsedMib(drm));
    }

    @Test
    void otherVendorsConnectorsAndRenderNodesAreSkipped(@TempDir Path drm) throws IOException {
        card(drm, "card0", "0x10de", "0x2204", 24 * GIB, GIB);
        Files.createDirectories(drm.resolve("card1-DP-1"));
        Files.createDirectories(drm.resolve("renderD128/device"));
        Files.writeString(drm.resolve("renderD128/device/vendor"), "0x1002\n");

        assertTrue(AmdCards.linuxCards(drm).isEmpty());
        assertEquals(OptionalLong.empty(), AmdCards.linuxUsedMib(drm));
    }

    @Test
    void aProductNameIsPreferredWhenTheCardHasOne(@TempDir Path drm) throws IOException {
        Path device = card(drm, "card0", "0x1002", "0x7448", 48 * GIB, 0);
        Files.writeString(device.resolve("product_name"), "Radeon PRO W7900\n");

        assertEquals("Radeon PRO W7900", AmdCards.linuxCards(drm).getFirst().name());
    }

    @Test
    void memoryInUseIsSummedOverEveryAmdCard(@TempDir Path drm) throws IOException {
        card(drm, "card0", "0x1002", "0x164e", 512L * 1024 * 1024, 256L * 1024 * 1024);
        card(drm, "card1", "0x1002", "0x744c", 24 * GIB, 6 * GIB);

        assertEquals(OptionalLong.of(6_144 + 256), AmdCards.linuxUsedMib(drm));
    }

    @Test
    void noDrmFolderIsNoCards(@TempDir Path tmp) {
        assertTrue(AmdCards.linuxCards(tmp.resolve("missing")).isEmpty());
    }

    @Test
    void theWindowsDeviceIdNamesTheVendor() {
        assertTrue(AmdCards.isAmdDeviceId("pci\\ven_1002&dev_744c"));
        assertTrue(AmdCards.isAmdDeviceId("PCI\\VEN_1002&DEV_744C&SUBSYS_0E3A1002"));
        assertFalse(AmdCards.isAmdDeviceId("pci\\ven_10de&dev_2204"));
        assertFalse(AmdCards.isAmdDeviceId(null));
    }

    @Test
    void theRegistryMemoryIsReadAsAQwordOrAsBinary() {
        assertEquals(OptionalLong.of(24_560), AmdCards.registryMib(24_560L * 1024 * 1024));
        byte[] littleEndian = {0, 0, 0, 0, 6, 0, 0, 0}; // 6 << 32 bytes = 24 GiB
        assertEquals(OptionalLong.of(24_576), AmdCards.registryMib(littleEndian));
        assertEquals(OptionalLong.empty(), AmdCards.registryMib(new byte[]{0, 0, 0, 1}));
        assertEquals(OptionalLong.empty(), AmdCards.registryMib(null));
    }

    @Test
    void windowsAdapterMemoryIsSummed() {
        assertEquals(OptionalLong.of(6_144 + 512), AmdCards.sumBytesAsMib(List.of(6 * GIB, 512L * 1024 * 1024)));
        assertEquals(OptionalLong.empty(), AmdCards.sumBytesAsMib(List.of()));
        assertEquals(OptionalLong.empty(), AmdCards.sumBytesAsMib(null));
        assertEquals(OptionalLong.empty(), AmdCards.sumBytesAsMib(Arrays.asList(GIB, null)));
    }

    private static Path card(Path drm, String card, String vendor, String deviceId, long total, long used)
            throws IOException {
        Path device = Files.createDirectories(drm.resolve(card).resolve("device"));
        Files.writeString(device.resolve("vendor"), vendor + "\n");
        Files.writeString(device.resolve("device"), deviceId + "\n");
        Files.writeString(device.resolve("mem_info_vram_total"), total + "\n");
        Files.writeString(device.resolve("mem_info_vram_used"), used + "\n");
        return device;
    }
}
