package elite.intel.bindforge.devicefiles;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Parsed against the real shipped file wherever possible, because the shapes that matter - an entry owning
 * eighty hardware ids, hex in three different cases - are ones a hand-written fixture would not think to
 * produce.
 */
class DeviceMappingsParserTest {

    /**
     * Frontier's shipped copy, kept in the documentation set as evidence. Resolved from either the module or
     * the repository root, because the test's working directory depends on how it was launched.
     */
    private static final Path STOCK = stockReference();

    private static Path stockReference() {
        String relative = "docs/02-features/bindforge/reference-data/FrontierStock-DeviceMappings.xml";
        Path fromModule = Path.of("..").resolve(relative);
        return Files.isRegularFile(fromModule) ? fromModule : Path.of(relative);
    }

    @TempDir
    Path folder;

    @Test
    void readsFrontiersOwnFile() throws IOException {
        List<DeviceEntry> entries = DeviceMappingsParser.parse(STOCK);

        assertEquals(51, entries.size(), "the stock file ships 51 device elements");
        assertEquals(136, entries.stream().mapToInt(entry -> entry.hardware().size()).sum(),
                "carrying 136 VID/PID pairs between them");
    }

    /**
     * A device element owns a set of hardware ids, not one. GamePad alone carries a primary pair plus 79
     * alternatives - which is why the entry, not the id, is the identity.
     */
    @Test
    void anEntryOwnsEveryAlternativeAsWellAsItsPrimaryPair() throws IOException {
        DeviceEntry gamePad = byName(DeviceMappingsParser.parse(STOCK)).get("GamePad");

        assertEquals(80, gamePad.hardware().size(), "one primary pair plus 79 alternatives");
        assertTrue(gamePad.hardware().contains(new DeviceEntry.HardwareId("045E", "028E")),
                "including its primary pair");
    }

    /**
     * The pairs come back in file order, primary first. Through {@code Set.copyOf} they did not: its order is
     * salted per run, so "the first pair" of a multi-pair entry was a different pair on every launch.
     */
    @Test
    void anEntrysPairsKeepFileOrderWithThePrimaryFirst() throws IOException {
        DeviceEntry gamePad = byName(DeviceMappingsParser.parse(STOCK)).get("GamePad");
        List<DeviceEntry.HardwareId> pairs = List.copyOf(gamePad.hardware());

        assertEquals(new DeviceEntry.HardwareId("045E", "028E"), pairs.getFirst(), "the primary pair first");
        assertEquals(new DeviceEntry.HardwareId("045E", "028E"), gamePad.primary());

        DeviceEntry dualShock = byName(DeviceMappingsParser.parse(STOCK)).get("DualShock4");
        assertEquals(List.of(
                        new DeviceEntry.HardwareId("054C", "0BA0"),
                        new DeviceEntry.HardwareId("054C", "05C4"),
                        new DeviceEntry.HardwareId("054C", "09CC")),
                List.copyOf(dualShock.hardware()), "the adaptor, then the two controllers, as the file lists them");
    }

    /**
     * The primary is the element's own pair, read from the element - not the first pair it claims. An element with
     * only alternatives has none, rather than its first alternative standing in (review, 2026-10-08).
     */
    @Test
    void anElementWithOnlyAlternativesHasNoPrimary() throws IOException {
        Path file = folder.resolve("DeviceMappings.xml");
        Files.writeString(file, """
                <Root>
                	<OnlyAlternatives>
                		<Alternative><PID>05C4</PID><VID>054C</VID></Alternative>
                		<Alternative><PID>09CC</PID><VID>054C</VID></Alternative>
                	</OnlyAlternatives>
                </Root>
                """);

        DeviceEntry entry = DeviceMappingsParser.parse(file).getFirst();

        assertNull(entry.primary());
        assertEquals(2, entry.hardware().size(), "both alternatives are still claimed");
    }

    /**
     * Frontier's own file spells hex in whatever case it likes - audited at 39 uppercase, 35 lowercase and 62
     * digits-only. Two entries naming the same hardware differently are the same hardware.
     */
    @Test
    void hardwareIdsCompareWithoutRegardToHexCase() {
        assertEquals(new DeviceEntry.HardwareId("045e", "0719"),
                new DeviceEntry.HardwareId("045E", "0719"));
        assertEquals(Set.of(new DeviceEntry.HardwareId("3344", "83f4")),
                Set.of(new DeviceEntry.HardwareId("3344", "83F4")));
    }

    @Test
    void readsAUserAddedEntryThatSitsAmongTheShippedOnes() throws IOException {
        DeviceEntry panel = byName(DeviceMappingsParser.parse(STOCK)).get("VPCPanel");

        assertEquals(Set.of(new DeviceEntry.HardwareId("3344", "0259")), panel.hardware());
    }

    @Test
    void anInstallationWithNoDeviceMappingsGivesNoEntriesRatherThanFailing() throws IOException {
        assertEquals(List.of(), DeviceMappingsParser.parseIfPresent(folder.resolve("DeviceMappings.xml")));
    }

    @Test
    void aFileThatIsNotXmlIsReportedRatherThanReturningNothing() throws IOException {
        Path broken = Files.writeString(folder.resolve("DeviceMappings.xml"), "this is not xml");

        IOException thrown = assertThrows(IOException.class, () -> DeviceMappingsParser.parse(broken));

        assertTrue(thrown.getMessage().contains("DeviceMappings.xml"), "the message names the file");
    }

    /** The primary pair is what Apply writes and the master holds; alternatives are not it. */
    @Test
    void primariesAreEachElementsOwnPairInDocumentOrderWithoutItsAlternatives() throws IOException {
        byte[] xml = """
                <Root>
                	<GamePad><PID>028E</PID><VID>045E</VID>
                		<Alternative><PID>028F</PID><VID>045E</VID></Alternative></GamePad>
                	<RVWAP><PID>43f4</PID><VID>3344</VID></RVWAP>
                	<Bare/>
                	<RVWAP><PID>FFFF</PID><VID>FFFF</VID></RVWAP>
                </Root>
                """.getBytes(java.nio.charset.StandardCharsets.UTF_8);

        Map<String, DeviceEntry.HardwareId> primaries = DeviceMappingsParser.primaries(xml);

        assertEquals(List.of("GamePad", "RVWAP", "Bare"), List.copyOf(primaries.keySet()));
        assertEquals(new DeviceEntry.HardwareId("045E", "028E"), primaries.get("GamePad"));
        assertEquals(new DeviceEntry.HardwareId("3344", "43F4"), primaries.get("RVWAP"), "the first wins, as in the game");
        assertEquals(new DeviceEntry.HardwareId("", ""), primaries.get("Bare"));
    }

    private Map<String, DeviceEntry> byName(List<DeviceEntry> entries) {
        return entries.stream().collect(Collectors.toMap(DeviceEntry::name, Function.identity()));
    }
}
