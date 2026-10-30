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
 * Changing the user's elements in a {@code DeviceMappings.xml}: everything else already there survives, and the user's
 * elements lead the file, because the first element claiming a VID/PID is the one the game uses.
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

    /** All 51 of Frontier's entries and every pair they claim come through, with the user's on top. */
    @Test
    void frontiersStockFileLosesNothing() throws IOException {
        byte[] stock = stockFile();

        Result result = DeviceMappingsMerge.insert(stock, List.of(RVWAP), List.of("RVWAP"));

        List<DeviceEntry> before = DeviceMappingsParser.parse(new ByteArrayInputStream(stock), "stock");
        List<DeviceEntry> after = DeviceMappingsParser.parse(new ByteArrayInputStream(result.content()), "merged");
        assertEquals(before.size() + 1, after.size());
        assertEquals("RVWAP", after.getFirst().name());
        assertEquals(before, after.subList(1, after.size()));
        assertEquals(supportsIconsIn(new String(stock, StandardCharsets.UTF_8)), supportsIconsIn(text(result)));
    }

    /** The identity question's yes: only the element's own pair changes (Storage S4b). */
    @Test
    void aRetargetGivesTheElementNewHardwareAndKeepsItsOtherChildren() throws IOException {
        String handEdited = SPECIMEN.replace("<Root>", """
                <Root>
                \t<RVWAP><PID>1111</PID><VID>3344</VID><Alternative><PID>2222</PID><VID>3344</VID></Alternative><Undocumented>x</Undocumented></RVWAP>""");

        Result result = DeviceMappingsMerge.retarget(bytes(handEdited), "RVWAP", "3344", "43F4");

        String written = text(result);
        assertTrue(written.contains("<PID>43F4</PID>"), written);
        assertFalse(written.contains("<PID>1111</PID>"), written);
        assertTrue(entry(result, "RVWAP").hardware().contains(new DeviceEntry.HardwareId("3344", "2222")), written);
        assertTrue(written.contains("<Undocumented>x</Undocumented>"), written);
    }

    /**
     * A retarget to the hardware the element already claims hands the file back byte for byte - not re-laid-out,
     * which would change its bytes for nothing the game could see. Hex case is not a difference: Frontier's own file
     * has no case convention.
     */
    @Test
    void aRetargetToTheSameHardwareReturnsTheFileUntouched() throws IOException {
        byte[] already = bytes(SPECIMEN.replace("<Root>", "<Root>\n\t<RVWAP><PID>43f4</PID><VID>3344</VID></RVWAP>"));

        Result result = DeviceMappingsMerge.retarget(already, "RVWAP", "3344", "43F4");

        assertFalse(result.changed());
        assertArrayEquals(already, result.content());
    }

    @Test
    void theFileIsWrittenWithCrlfAndFourSpaces() throws IOException {
        String written = text(DeviceMappingsMerge.insert(bytes(SPECIMEN), List.of(RVWAP), List.of("RVWAP")));

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
                () -> DeviceMappingsMerge.insert(bytes("<DeviceMappings/>"), List.of(RVWAP), List.of("RVWAP")));

        assertTrue(refused.getMessage().contains("<DeviceMappings>"), refused.getMessage());
    }

    @Test
    void aNameXmlCannotHoldIsRefusedRatherThanWritten() {
        assertThrows(IOException.class,
                () -> DeviceMappingsMerge.insert(bytes(SPECIMEN), List.of(new UserEntry("Left Stick", "3344", "83F4")),
                        List.of("Left Stick")));
    }

    /** CLEAR, applied: the user's element goes, and every other element, comment and child is kept. */
    @Test
    void removingTheUsersElementKeepsEverythingElse() throws IOException {
        Result inserted = DeviceMappingsMerge.insert(bytes(SPECIMEN), List.of(RVWAP), List.of("RVWAP"));

        Result result = DeviceMappingsMerge.remove(inserted.content(), List.of("RVWAP"));

        assertTrue(result.changed());
        assertEquals(List.of("GamePad", "SaitekX52", "T-Rudder"), names(result));
        String written = text(result);
        assertTrue(written.contains("<!-- XB1 Controller -->"), written);
        assertTrue(written.contains("<SupportsIcons>SaitekX52</SupportsIcons>"), written);
    }

    /** The name has left the user's set, so a second copy further down goes with the first. */
    @Test
    void removingANameTakesEveryCopyOfIt() throws IOException {
        String twice = """
                <?xml version="1.0" encoding="UTF-8" ?>
                <Root>
                \t<RVWAP><PID>43F4</PID><VID>3344</VID></RVWAP>
                \t<T-Rudder><PID>B679</PID><VID>044F</VID></T-Rudder>
                \t<RVWAP><PID>03F5</PID><VID>3344</VID></RVWAP>
                </Root>
                """;

        assertEquals(List.of("T-Rudder"), names(DeviceMappingsMerge.remove(bytes(twice), List.of("RVWAP"))));
    }

    @Test
    void removingANameTheFileDoesNotHoldChangesNothing() throws IOException {
        Result result = DeviceMappingsMerge.remove(bytes(SPECIMEN), List.of("RVWAP", "rudder"));

        assertFalse(result.changed());
        assertArrayEquals(bytes(SPECIMEN), result.content(), "untouched, not re-laid-out");
    }

    @Test
    void removingFromSomethingThatIsNotADeviceMappingsFileIsRefused() {
        assertThrows(IOException.class,
                () -> DeviceMappingsMerge.remove(bytes("<Bindings><RVWAP/></Bindings>"), List.of("RVWAP")));
    }

    /**
     * Onboarding's insert: with no user element leading the file, the new one goes at the very top (A6b), and
     * everything else in the file is kept.
     */
    @Test
    void anInsertedElementLeadsAFileWithNoUserElements() throws IOException {
        Result result = DeviceMappingsMerge.insert(bytes(SPECIMEN), List.of(RVWAP), List.of("RVWAP"));

        assertTrue(result.changed());
        assertEquals(List.of("RVWAP", "GamePad", "SaitekX52", "T-Rudder"), names(result));
        String written = text(result);
        assertTrue(written.contains("<!-- XB1 Controller -->"), written);
        assertTrue(written.contains("<SupportsIcons>SaitekX52</SupportsIcons>"), written);
        assertEquals(2, entry(result, "GamePad").hardware().size(), "the <Alternative> pair survives");
    }

    /**
     * An insert goes at its place in the given order among the user's leading elements, and leaves every other
     * element as it is, a user element with a different PID included: bringing it into line would be a change nobody
     * asked for.
     */
    @Test
    void anInsertTakesItsPlaceInTheOrderAndChangesNothingElse() throws IOException {
        String file = """
                <?xml version="1.0" encoding="UTF-8" ?>
                <Root>
                \t<AAA><PID>0001</PID><VID>1234</VID></AAA>
                \t<CCC><PID>9999</PID><VID>1234</VID></CCC>
                \t<T-Rudder><PID>B679</PID><VID>044F</VID></T-Rudder>
                </Root>
                """;

        Result result = DeviceMappingsMerge.insert(bytes(file), List.of(new UserEntry("BBB", "1234", "0002")),
                List.of("AAA", "BBB", "CCC"));

        assertEquals(List.of("AAA", "BBB", "CCC", "T-Rudder"), names(result));
        assertEquals("9999", entry(result, "CCC").primary().pid(), "another device's PID is not brought into line");
    }

    @Test
    void anElementAlreadyThereIsNotInsertedAgain() throws IOException {
        byte[] file = bytes(SPECIMEN);

        Result result = DeviceMappingsMerge.insert(file, List.of(new UserEntry("T-Rudder", "1234", "0002")),
                List.of("T-Rudder"));

        assertFalse(result.changed());
        assertArrayEquals(file, result.content(), "untouched, not re-laid-out");
    }

    // ------------------------------------------------------------------ copyIn (UNDO CLEAR, Storage S4a)

    /**
     * UNDO CLEAR puts the element back whole - its {@code <SupportsIcons>} with it, which an insert would lose - in its
     * place among the user's elements leading the file.
     */
    @Test
    void aCopiedElementComesBackWholeInItsPlaceAmongTheUsers() throws IOException {
        String source = """
                <?xml version="1.0" encoding="UTF-8" ?>
                <Root>
                \t<AAA><PID>0001</PID><VID>1234</VID></AAA>
                \t<MyX52><PID>075C</PID><VID>06A3</VID><SupportsIcons>SaitekX52</SupportsIcons></MyX52>
                \t<CCC><PID>0003</PID><VID>1234</VID></CCC>
                \t<T-Rudder><PID>B679</PID><VID>044F</VID></T-Rudder>
                </Root>
                """;
        String cleared = """
                <?xml version="1.0" encoding="UTF-8" ?>
                <Root>
                \t<AAA><PID>0001</PID><VID>1234</VID></AAA>
                \t<CCC><PID>0003</PID><VID>1234</VID></CCC>
                \t<T-Rudder><PID>B679</PID><VID>044F</VID></T-Rudder>
                </Root>
                """;

        Result result = DeviceMappingsMerge.copyIn(bytes(cleared), bytes(source), "MyX52",
                List.of("AAA", "MyX52", "CCC"));

        assertTrue(result.changed());
        assertEquals(List.of("AAA", "MyX52", "CCC", "T-Rudder"), names(result));
        assertEquals(1, supportsIconsIn(text(result)), "its <SupportsIcons> came with it");
        assertEquals("075C", entry(result, "MyX52").primary().pid());
    }

    /** An element already under that tag is left alone - the file is returned untouched. */
    @Test
    void anElementAlreadyPresentIsNotCopiedIn() throws IOException {
        byte[] file = bytes(SPECIMEN);

        Result result = DeviceMappingsMerge.copyIn(file,
                bytes("<Root><SaitekX52><PID>0001</PID><VID>1234</VID></SaitekX52></Root>"), "SaitekX52",
                List.of("SaitekX52"));

        assertFalse(result.changed());
        assertArrayEquals(file, result.content(), "untouched, not re-laid-out");
    }

    @Test
    void anElementTheSourceLacksIsRefused() {
        assertThrows(IOException.class, () -> DeviceMappingsMerge.copyIn(bytes(SPECIMEN), bytes("<Root/>"), "MyX52",
                List.of("MyX52")));
    }

    // ------------------------------------------------------------------ rename (A11)

    /** The element keeps its place and every child it has; only its tag changes. */
    @Test
    void aRenameKeepsTheElementsPlaceAndChildren() throws IOException {
        Result result = DeviceMappingsMerge.rename(bytes(SPECIMEN), "SaitekX52", "MyX52");

        assertTrue(result.changed());
        assertEquals(List.of("GamePad", "MyX52", "T-Rudder"), names(result));
        assertTrue(text(result).contains("<SupportsIcons>SaitekX52</SupportsIcons>"), text(result));
        assertEquals(1, supportsIconsIn(text(result)), "nothing was copied");
    }

    /** A second copy under the old tag goes, as CLEAR takes one: two elements under the new name would leave one dead. */
    @Test
    void aRenameRemovesASecondCopyOfTheOldName() throws IOException {
        String twice = SPECIMEN.replace("</Root>",
                "\t<T-Rudder><PID>0001</PID><VID>1234</VID></T-Rudder>\n</Root>");

        Result result = DeviceMappingsMerge.rename(bytes(twice), "T-Rudder", "MyRudder");

        assertEquals(List.of("GamePad", "SaitekX52", "MyRudder"), names(result));
        assertTrue(entry(result, "MyRudder").hardware().stream().anyMatch(id -> id.pid().equals("B679")),
                "the first copy is the one kept");
    }

    /** Already under the new name - renamed by hand, or half-taken before: the old copies go, the new one stays. */
    @Test
    void aRenameWhoseNewNameIsAlreadyThereRemovesTheOldOne() throws IOException {
        String both = SPECIMEN.replace("<Root>", "<Root>\n\t<MyRudder><PID>B679</PID><VID>044F</VID></MyRudder>");

        Result result = DeviceMappingsMerge.rename(bytes(both), "T-Rudder", "MyRudder");

        assertEquals(List.of("MyRudder", "GamePad", "SaitekX52"), names(result));
    }

    /**
     * Review, 2026-10-15: the game finds an element ignoring case, so an installation holding {@code <t-rudder>} still
     * holds the device renamed from {@code T-Rudder} - left alone, it would keep leading the file for that VID/PID.
     */
    @Test
    void aRenameFindsTheOldNameInAnyCase() throws IOException {
        String lower = SPECIMEN.replace("<T-Rudder>", "<t-rudder>").replace("</T-Rudder>", "</t-rudder>");

        Result result = DeviceMappingsMerge.rename(bytes(lower), "T-Rudder", "MyRudder");

        assertEquals(List.of("GamePad", "SaitekX52", "MyRudder"), names(result));
    }

    /** A case-only rename already taken: the element spelled as the new name is never an old copy of itself. */
    @Test
    void aCaseOnlyRenameAlreadyTakenChangesNothing() throws IOException {
        byte[] file = bytes(SPECIMEN);

        Result result = DeviceMappingsMerge.rename(file, "T-RUDDER", "T-Rudder");

        assertFalse(result.changed());
        assertArrayEquals(file, result.content());
    }

    @Test
    void aRenameOfANameTheFileDoesNotHoldChangesNothing() throws IOException {
        byte[] file = bytes(SPECIMEN);

        Result result = DeviceMappingsMerge.rename(file, "LVWAP", "LeftStick");

        assertFalse(result.changed());
        assertArrayEquals(file, result.content());
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
