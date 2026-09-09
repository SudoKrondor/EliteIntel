package elite.intel.ai.hands;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Guards the table that turns a .binds XML tag into the control's in-game name.
 * <p>
 * These are the invariants a commander depends on when reading our table against the game's own
 * control screen: no tag names two different rows in the same group, no row is left with an empty
 * label, and an unknown tag degrades to its humanized self instead of vanishing.
 */
class BindingDisplayNamesTest {

    @Test
    void everyEntryIsWellFormed() {
        assertFalse(BindingDisplayNames.all().isEmpty(), "control name table failed to load");

        for (Map.Entry<String, BindingDisplayNames.ControlName> entry : BindingDisplayNames.all().entrySet()) {
            BindingDisplayNames.ControlName control = entry.getValue();
            assertNotNull(control.section(), entry.getKey() + " has no section");
            assertNotEquals(BindingSection.OTHER, control.section(),
                    entry.getKey() + " is in the table, so it must name a real game section, not OTHER");
            assertFalse(control.group() == null || control.group().isBlank(), entry.getKey() + " has no group");
            assertFalse(control.name().isBlank(), entry.getKey() + " has no control name");
        }
    }

    /**
     * Two tags sharing a section, group and name would render as the same row twice, leaving the
     * commander no way to tell which of the two the table means.
     */
    @Test
    void noTwoTagsRenderTheSameRow() {
        Set<String> seen = new LinkedHashSet<>();
        List<String> duplicates = new ArrayList<>();
        for (Map.Entry<String, BindingDisplayNames.ControlName> entry : BindingDisplayNames.all().entrySet()) {
            BindingDisplayNames.ControlName control = entry.getValue();
            String row = control.section() + "|" + control.label();
            if (!seen.add(row)) {
                duplicates.add(entry.getKey() + " -> " + row);
            }
        }
        assertTrue(duplicates.isEmpty(), "duplicate rows: " + duplicates);
    }

    /**
     * File order is the game's row order, so it must be strictly increasing to be usable as a sort key.
     */
    @Test
    void orderFollowsTheFile() {
        int previous = -1;
        for (BindingDisplayNames.ControlName control : BindingDisplayNames.all().values()) {
            assertTrue(control.order() > previous, "order must increase with file position");
            previous = control.order();
        }
    }

    @Test
    void knownTagResolvesToItsInGameName() {
        BindingDisplayNames.ControlName control = BindingDisplayNames.lookup("ExplorationSAANextGenus");
        assertEquals(BindingSection.SHIP, control.section());
        assertEquals("Detailed Surface Scanner", control.group());
        assertEquals("Next Filter", control.name());
        assertEquals("Detailed Surface Scanner / Next Filter", control.label());
    }

    /**
     * A control the game added after this table was written must still show up - under its raw name,
     * flagged as unmapped, never dropped from the table.
     */
    @Test
    void unknownTagFallsBackToItsHumanizedName() {
        BindingDisplayNames.ControlName control = BindingDisplayNames.lookup("SomeFutureControlButton");
        assertEquals(BindingSection.OTHER, control.section());
        assertNull(control.group());
        assertEquals("Some Future Control Button", control.name());
        assertEquals("Some Future Control Button", control.label());
    }

    @Test
    void blankTagIsHandled() {
        assertEquals(BindingSection.OTHER, BindingDisplayNames.lookup(null).section());
        assertEquals("", BindingDisplayNames.label(""));
    }

    @Test
    void searchTextCoversSectionGroupNameAndRawTag() {
        String haystack = BindingDisplayNames.searchText("ExplorationSAANextGenus", "Ship controls");
        assertTrue(haystack.contains("ship controls"), haystack);
        assertTrue(haystack.contains("detailed surface scanner"), haystack);
        assertTrue(haystack.contains("next filter"), haystack);
        assertTrue(haystack.contains("explorationsaanextgenus"), haystack);
    }
}
