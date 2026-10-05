package elite.intel.bindforge.devicefiles;

import elite.intel.bindforge.devicefiles.DeviceMappingsMerge.Result;
import elite.intel.bindforge.devicefiles.DeviceMappingsMerge.UserEntry;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Merging the user's elements into a {@code DeviceMappings.xml}: everything already there survives, and the
 * user's elements lead the file, because the first element claiming a VID/PID is the one the game uses.
 */
class DeviceMappingsMergeTest {

    /**
     * Built rather than copied from anyone's install: a comment inside {@code <GamePad>}, a
     * {@code <SupportsIcons>}, an {@code <Alternative>}, and Frontier's inconsistent tab layout - the things a
     * regenerating writer drops.
     */
    private static final String SPECIMEN = """
            <?xml version="1.0" encoding="UTF-8" ?>
            <Root>
            \t<GamePad>
            \t\t<PID>028E</PID><VID>045E</VID>
            \t\t<!-- XB1 Controller -->
            \t\t<Alternative><PID>02FF</PID><VID>045E</VID></Alternative>
            \t</GamePad>
            \t<SaitekX52>\t<PID>075C</PID>\t<VID>06A3</VID>\t<SupportsIcons>SaitekX52</SupportsIcons> </SaitekX52>
            \t<T-Rudder><PID>B679</PID><VID>044F</VID></T-Rudder>
            </Root>
            """;

    private static final UserEntry RVWAP = new UserEntry("RVWAP", "3344", "43F4");

    @Test
    void theUsersElementLeadsTheFileAndEverythingElseIsKept() throws IOException {
        Result result = DeviceMappingsMerge.merge(bytes(SPECIMEN), List.of(RVWAP));

        assertTrue(result.changed());
        assertEquals(List.of("RVWAP", "GamePad", "SaitekX52", "T-Rudder"), names(result));
        String written = text(result);
        assertTrue(written.contains("<!-- XB1 Controller -->"), written);
        assertTrue(written.contains("<SupportsIcons>SaitekX52</SupportsIcons>"), written);
        assertEquals(2, entry(result, "GamePad").hardware().size(), "the <Alternative> pair survives");
    }

    /** All 51 of Frontier's entries and every pair they claim come through, with the user's on top. */
    @Test
    void frontiersStockFileLosesNothing() throws IOException {
        byte[] stock = stockFile();

        Result result = DeviceMappingsMerge.merge(stock, List.of(RVWAP));

        List<DeviceEntry> before = DeviceMappingsParser.parse(new ByteArrayInputStream(stock), "stock");
        List<DeviceEntry> after = DeviceMappingsParser.parse(new ByteArrayInputStream(result.content()), "merged");
        assertEquals(before.size() + 1, after.size());
        assertEquals("RVWAP", after.getFirst().name());
        assertEquals(before, after.subList(1, after.size()));
        assertEquals(supportsIconsIn(new String(stock, StandardCharsets.UTF_8)), supportsIconsIn(text(result)));
    }

    /** The case measured on 2026-10-03: an entry below Frontier's for the same pedals was ignored by the game. */
    @Test
    void aUserElementBelowFrontiersEntryForTheSameHardwareMovesToTheTop() throws IOException {
        String userBelow = SPECIMEN.replace("</Root>",
                "\t<MyRudder><PID>B679</PID><VID>044F</VID></MyRudder>\n</Root>");

        Result result = DeviceMappingsMerge.merge(bytes(userBelow),
                List.of(new UserEntry("MyRudder", "044F", "B679")));

        assertTrue(result.changed());
        assertEquals(List.of("MyRudder", "GamePad", "SaitekX52", "T-Rudder"), names(result));
    }

    @Test
    void anExistingElementGetsTheMastersHardwareAndKeepsItsOtherChildren() throws IOException {
        String handEdited = SPECIMEN.replace("<Root>", """
                <Root>
                \t<RVWAP><PID>1111</PID><VID>3344</VID><Alternative><PID>2222</PID><VID>3344</VID></Alternative><Undocumented>x</Undocumented></RVWAP>""");

        Result result = DeviceMappingsMerge.merge(bytes(handEdited), List.of(RVWAP));

        String written = text(result);
        assertTrue(written.contains("<PID>43F4</PID>"), written);
        assertFalse(written.contains("<PID>1111</PID>"), written);
        assertTrue(entry(result, "RVWAP").hardware().contains(new DeviceEntry.HardwareId("3344", "2222")), written);
        assertTrue(written.contains("<Undocumented>x</Undocumented>"), written);
    }

    /**
     * A file already saying what the master says is handed back byte for byte - not re-laid-out, which would
     * cost a write and an Edit History entry for nothing the game could see. Hex case is not a difference:
     * Frontier's own file has no case convention.
     */
    @Test
    void aFileThatAlreadyMatchesIsReturnedUntouched() throws IOException {
        byte[] alreadyMerged = bytes(SPECIMEN.replace("<Root>", "<Root>\n\t<RVWAP><PID>43f4</PID><VID>3344</VID></RVWAP>"));

        Result result = DeviceMappingsMerge.merge(alreadyMerged, List.of(RVWAP));

        assertFalse(result.changed());
        assertArrayEquals(alreadyMerged, result.content());
    }

    @Test
    void theFileIsWrittenWithCrlfAndFourSpaces() throws IOException {
        String written = text(DeviceMappingsMerge.merge(bytes(SPECIMEN), List.of(RVWAP)));

        assertTrue(written.startsWith("""
                <?xml version="1.0" encoding="UTF-8"?>\r
                <Root>\r
                    <RVWAP>\r
                        <PID>43F4</PID>\r
                        <VID>3344</VID>\r
                    </RVWAP>\r
                """), written);
        assertFalse(written.contains("\t"), written);
        assertFalse(written.replace("\r\n", "").contains("\n"), written);
    }

    @Test
    void aFileWhoseRootIsNotRootIsRefused() {
        IOException refused = assertThrows(IOException.class,
                () -> DeviceMappingsMerge.merge(bytes("<DeviceMappings/>"), List.of(RVWAP)));

        assertTrue(refused.getMessage().contains("<DeviceMappings>"), refused.getMessage());
    }

    @Test
    void aNameXmlCannotHoldIsRefusedRatherThanWritten() {
        assertThrows(IOException.class,
                () -> DeviceMappingsMerge.merge(bytes(SPECIMEN), List.of(new UserEntry("Left Stick", "3344", "83F4"))));
    }

    private static byte[] stockFile() throws IOException {
        try (InputStream xml = DeviceMappingsMergeTest.class.getResourceAsStream(FrontierStockDevices.RESOURCE)) {
            return Objects.requireNonNull(xml, FrontierStockDevices.RESOURCE + " is missing").readAllBytes();
        }
    }

    private static List<String> names(Result result) throws IOException {
        return DeviceMappingsParser.parse(new ByteArrayInputStream(result.content()), "merged").stream()
                .map(DeviceEntry::name)
                .toList();
    }

    private static DeviceEntry entry(Result result, String name) throws IOException {
        return DeviceMappingsParser.parse(new ByteArrayInputStream(result.content()), "merged").stream()
                .filter(entry -> entry.name().equals(name))
                .findFirst()
                .orElseThrow();
    }

    private static String text(Result result) {
        return new String(result.content(), StandardCharsets.UTF_8);
    }

    private static byte[] bytes(String xml) {
        return xml.getBytes(StandardCharsets.UTF_8);
    }

    private static int supportsIconsIn(String xml) {
        return xml.split("<SupportsIcons>", -1).length - 1;
    }
}
