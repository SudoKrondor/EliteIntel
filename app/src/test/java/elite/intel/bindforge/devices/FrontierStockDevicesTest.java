package elite.intel.bindforge.devices;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Frontier's shipped device list is the only thing that separates their entries from the player's, so this
 * covers both that it still is what was captured, and that it answers the two questions asked of it.
 */
class FrontierStockDevicesTest {

    /** Recorded in reference-data/FrontierStock-README.md, alongside the capture it describes. */
    private static final String DOCUMENTED_SHA256 =
            "d0a9ad8eda58e009f356fbccf8202a8e24593299c496840a4fb1f9261e72c7d0";
    private static final int DOCUMENTED_SIZE = 9643;

    private static final Path DOCS_COPY = Path.of(
            "docs/02-features/bindforge/reference-data/FrontierStock-DeviceMappings.xml");

    private final FrontierStockDevices stock = FrontierStockDevices.getInstance();

    /**
     * The file lives twice - as evidence in the docs, and as a resource the application reads - and the two
     * drifting apart would be invisible. The shipped copy would quietly stop being what the README says
     * Frontier shipped, and every provenance answer built on it would be wrong without anything failing.
     */
    @Test
    void theShippedCopyIsStillTheCaptureTheDocsRecord() throws IOException {
        byte[] shipped = shippedBytes();

        assertEquals(DOCUMENTED_SIZE, shipped.length, "the capture is a fixed artefact, not a file to edit");
        assertEquals(DOCUMENTED_SHA256, sha256(shipped),
                "the shipped reference no longer matches the fingerprint recorded in FrontierStock-README.md");

        Path docsCopy = projectRoot().resolve(DOCS_COPY);
        assertTrue(Files.isRegularFile(docsCopy), "the evidence copy has gone from " + docsCopy);
        assertArrayEquals(Files.readAllBytes(docsCopy), shipped,
                "the docs copy and the shipped copy have diverged - they are the same evidence");
    }

    @Test
    void theCaptureHoldsEveryEntryTheReadmeCounts() {
        assertEquals(51, stock.entries().size(), "51 device elements");
        assertEquals(136, stock.entries().stream().mapToInt(e -> e.hardware().size()).sum(),
                "136 VID/PID tuples, because an entry owns a set of them rather than one");
    }

    /**
     * Both accessors claim the file's own order, and an immutable {@code Map} cannot supply it - its
     * iteration order is unspecified, hash-derived, and salted per JVM run, so a list built from one would
     * come out shuffled differently on every launch. A size check would never notice. These are the first
     * five and last two elements of the capture, read from the file itself.
     */
    @Test
    void theEntriesComeBackInTheOrderTheFileListsThem() {
        List<String> names = stock.names();

        assertEquals(List.of("GamePad", "BlackWidow", "SaitekAV8R03", "SaitekX55Joystick", "SaitekX56Joystick"),
                names.subList(0, 5));
        assertEquals(List.of("VPCPanel", "VPCThrottle"), names.subList(names.size() - 2, names.size()));
        assertEquals(names, stock.entries().stream().map(DeviceEntry::name).toList(),
                "names() and entries() describe the same list in the same order");
    }

    /**
     * A repeated element tag would mean a later capture arrived broken, and the loss would otherwise be
     * silent: one entry replaces the other, the count drops, and the sha256 guard reports only that the bytes
     * changed - which is exactly what a legitimate re-capture looks like. The device whose entry vanished
     * would then look like the user's, and BindForge would offer to rename something Frontier owns.
     */
    @Test
    void aReferenceNamingOneElementTwiceIsRefused() {
        List<DeviceEntry> duplicated = List.of(
                new DeviceEntry("T-Rudder", Set.of(new DeviceEntry.HardwareId("044F", "B679"))),
                new DeviceEntry("T-Rudder", Set.of(new DeviceEntry.HardwareId("044F", "0BF3"))));

        IllegalStateException refused =
                assertThrows(IllegalStateException.class, () -> new FrontierStockDevices(duplicated));

        assertTrue(refused.getMessage().contains("T-Rudder"), "and it names which tag, so it can be found");
    }

    /**
     * Unlike a repeated name, two entries claiming one VID/PID pair is something Frontier's file could
     * legitimately do, so the earlier entry wins rather than the load failing.
     */
    @Test
    void twoEntriesClaimingOneHardwarePairResolveToTheFirst() {
        DeviceEntry.HardwareId shared = new DeviceEntry.HardwareId("044F", "B679");
        FrontierStockDevices stock = new FrontierStockDevices(List.of(
                new DeviceEntry("First", Set.of(shared)),
                new DeviceEntry("Second", Set.of(shared))));

        assertEquals("First", stock.covering("044F", "B679").orElseThrow().name());
    }

    @Test
    void anEntryFrontierShipsIsRecognisedByName() {
        assertTrue(stock.ships("T-Rudder"));
        assertTrue(stock.ships("GamePad"));
        assertTrue(stock.ships("VPCPanel"), "looks like a personal addition, and is Frontier's");
    }

    /**
     * The developer's own sticks. They are the case the reference exists for: present in his file, absent
     * from Frontier's, and indistinguishable from a stock entry without this comparison.
     */
    @Test
    void anEntryThePlayerAddedIsNotFound() {
        assertFalse(stock.ships("LVWAP"));
        assertFalse(stock.ships("RVWAP"));
        assertTrue(stock.entry("LVWAP").isEmpty());
    }

    /**
     * XML tags are case-sensitive and the game reads the tag, so a hand-typed variant is a different element
     * - not Frontier's. The looser comparison belongs to alias-collision checking, which asks something else.
     */
    @Test
    void aNameMatchingOnlyWhenCaseIsIgnoredIsNotFrontiers() {
        assertFalse(stock.ships("t-rudder"));
        assertFalse(stock.ships("gamepad"));
    }

    @Test
    void attachedHardwareIsMatchedToTheEntryCoveringIt() {
        assertEquals("T-Rudder", stock.covering("044F", "B679").orElseThrow().name());
    }

    /**
     * Two physically different controllers resolve to one entry through its alternatives - confirmed against
     * real hardware, a DualShock 4 v1 and a v2. Matching only the primary pair would miss both of them, since
     * the primary is the wireless adaptor neither controller is.
     */
    @Test
    void anAlternativePairFindsTheEntryJustAsThePrimaryDoes() {
        assertEquals("DualShock4", stock.covering("054C", "0BA0").orElseThrow().name(), "the primary pair");
        assertEquals("DualShock4", stock.covering("054C", "05C4").orElseThrow().name(), "the v1 controller");
        assertEquals("DualShock4", stock.covering("054C", "09CC").orElseThrow().name(), "the v2 controller");
    }

    /**
     * Frontier's file has no case convention - {@code GamePad} carries {@code 045E} and {@code 045e} in the
     * same element - so a case-sensitive lookup would miss roughly half of it.
     */
    @Test
    void hardwareIsFoundWhateverCaseTheHexIsWrittenIn() {
        assertNotNull(stock.covering("054c", "05c4").orElse(null), "lowercase, as the file itself writes it");
        assertNotNull(stock.covering("054C", "05C4").orElse(null));
    }

    @Test
    void hardwareFrontierDoesNotCoverIsReportedAsSuch() {
        assertTrue(stock.covering("3344", "83F4").isEmpty(), "a VIRPIL stick is nobody's built-in");
    }

    private static byte[] shippedBytes() throws IOException {
        try (InputStream xml = FrontierStockDevices.class.getResourceAsStream(FrontierStockDevices.RESOURCE)) {
            assertNotNull(xml, "the stock reference is not on the classpath: " + FrontierStockDevices.RESOURCE);
            return xml.readAllBytes();
        }
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required of every JVM", e);
        }
    }

    /**
     * Walks up from the working directory until the docs tree appears, because a test's working directory is
     * the module rather than the repository and hard-coding {@code ../} would break the moment either moves.
     */
    private static Path projectRoot() {
        Path candidate = Path.of("").toAbsolutePath();
        while (candidate != null && !Files.isDirectory(candidate.resolve("docs"))) {
            candidate = candidate.getParent();
        }
        assertNotNull(candidate, "could not find the repository root from " + Path.of("").toAbsolutePath());
        return candidate;
    }
}
