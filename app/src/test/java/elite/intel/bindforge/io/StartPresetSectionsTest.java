package elite.intel.bindforge.io;

import elite.intel.bindforge.model.BindingSection;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class StartPresetSectionsTest {

    @Test
    void fourLineSpecimenReadsASplitPresetInSectionOrder() throws IOException {
        StartPresetSections sections = StartPresetSections.parse(specimen("StartPreset.4.start"));

        assertEquals(new StartPresetSections("KeyboardMouseOnly", "Custom", "Custom", "Custom", false), sections);
    }

    @Test
    void singleLineSpecimenAppliesItsNameToAllFourSections() throws IOException {
        StartPresetSections sections = StartPresetSections.parse(specimen("StartPreset.start"));

        assertEquals(new StartPresetSections("Custom", "Custom", "Custom", "Custom", true), sections);
        assertTrue(sections.singleLine());
    }

    @Test
    void crlfAndLfReadTheSame() {
        assertEquals(StartPresetSections.parse("A\nB\nC\nD"), StartPresetSections.parse("A\r\nB\r\nC\r\nD"));
    }

    @Test
    void finalLineEndingAndBlankLinesAfterTheFourthChangeNothing() {
        StartPresetSections sections = StartPresetSections.parse("A\r\nB\r\nC\r\nD\r\n\r\n");

        assertEquals(new StartPresetSections("A", "B", "C", "D", false), sections);
    }

    @Test
    void singleLineWithFinalNewlineIsStillTheSingleLineForm() {
        StartPresetSections sections = StartPresetSections.parse("Custom\r\n");

        assertEquals(new StartPresetSections("Custom", "Custom", "Custom", "Custom", true), sections);
        assertTrue(sections.singleLine());
    }

    @Test
    void theSingleLineFormEqualsFourIdenticalLines() {
        StartPresetSections single = StartPresetSections.parse("Custom");
        StartPresetSections four = StartPresetSections.parse("Custom\nCustom\nCustom\nCustom");

        assertEquals(single, four);
        assertEquals(single.hashCode(), four.hashCode());
        assertNotEquals(single.singleLine(), four.singleLine());
        assertNotEquals(four, StartPresetSections.parse("Custom\nCustom\nCustom\nOther"));
    }

    @Test
    void onlyALeadingByteOrderMarkIsStripped() {
        assertEquals("A", StartPresetSections.withoutByteOrderMark("﻿A"));
        assertEquals("A﻿", StartPresetSections.withoutByteOrderMark("A﻿"));
        assertEquals("", StartPresetSections.withoutByteOrderMark(""));
    }

    @Test
    void nameFollowedByBlankLinesIsFourLinesWithSectionsMissingNotOneForAll() {
        // A four-line file that lost three lines must not read as a valid uniform preset.
        assertEquals(new StartPresetSections("A", "", "", "", false), StartPresetSections.parse("A\n\n\n"));
        assertEquals(new StartPresetSections("A", "B", "", "", false), StartPresetSections.parse("A\nB\n\n"));
    }

    @Test
    void singleBlankLineIsNotTheSingleLineForm() {
        assertEquals(new StartPresetSections("", "", "", "", false), StartPresetSections.parse("  \r\n"));
    }

    @Test
    void singleLineWithDifferingNamesIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> new StartPresetSections("A", "B", "C", "D", true));
        assertDoesNotThrow(() -> new StartPresetSections("A", "A", "A", "A", true));
    }

    @Test
    void interiorBlankLineIsAnEmptySectionAndNothingShifts() {
        StartPresetSections sections = StartPresetSections.parse("A\n\nC\nD");

        assertEquals(new StartPresetSections("A", "", "C", "D", false), sections);
    }

    @Test
    void missingSectionsAreEmpty() {
        assertEquals(new StartPresetSections("A", "B", "", "", false), StartPresetSections.parse("A\nB"));
        assertEquals(new StartPresetSections("A", "B", "C", "", false), StartPresetSections.parse("A\nB\nC\n"));
    }

    @Test
    void linesBeyondTheFourthAreIgnored() {
        assertEquals(new StartPresetSections("A", "B", "C", "D", false), StartPresetSections.parse("A\nB\nC\nD\nE"));
    }

    @Test
    void emptyOrBlankFileHasFourEmptySections() {
        StartPresetSections empty = new StartPresetSections("", "", "", "", false);
        assertEquals(empty, StartPresetSections.parse(""));
        assertEquals(empty, StartPresetSections.parse("\r\n  \r\n"));
    }

    @Test
    void namesAreTrimmedAndALeadingBomIsDropped() {
        StartPresetSections sections = StartPresetSections.parse("\uFEFF  A \r\n\tB\r\nC  \r\nD");

        assertEquals(new StartPresetSections("A", "B", "C", "D", false), sections);
    }

    @Test
    void nameIsKeptAsWrittenWithoutStrippingAnything() {
        // The line already holds the preset name; a "(2)" or a dot in it is part of the name.
        StartPresetSections sections = StartPresetSections.parse("Custom.4.2 (2)");

        assertEquals("Custom.4.2 (2)", sections.general());
    }

    @Test
    void forSectionMapsEachOfTheFourAndRefusesOther() {
        StartPresetSections sections = new StartPresetSections("G", "S", "R", "F", false);

        assertEquals("G", sections.forSection(BindingSection.GENERAL));
        assertEquals("S", sections.forSection(BindingSection.SHIP));
        assertEquals("R", sections.forSection(BindingSection.SRV));
        assertEquals("F", sections.forSection(BindingSection.ON_FOOT));
        assertThrows(IllegalArgumentException.class, () -> sections.forSection(BindingSection.OTHER));
    }

    private static String specimen(String name) throws IOException {
        try (InputStream in = StartPresetSectionsTest.class.getResourceAsStream("/bindforge/startpreset/" + name)) {
            assertNotNull(in, "missing test resource " + name);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
