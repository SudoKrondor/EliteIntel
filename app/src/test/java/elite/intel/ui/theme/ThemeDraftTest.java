package elite.intel.ui.theme;

import org.junit.jupiter.api.Test;

import java.awt.*;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ThemeDraftTest {

    private static final Color PICKED = new Color(0x3B9EFF);

    @Test
    void changingEveryPartChangesEveryRoleShowingThatColor() {
        ThemeDraft draft = new ThemeDraft(ColorTheme.DEFAULT);
        List<ThemeRole> sharingAccent = draft.rolesColored(draft.color(ThemeRole.PRIMARY_ACTION));

        draft.changeEveryRoleColoredLike(ThemeRole.PRIMARY_ACTION, PICKED);

        assertTrue(sharingAccent.size() > 1, "the accent is shared by several roles in Default");
        for (ThemeRole role : sharingAccent) assertEquals(PICKED.getRGB() & 0xFFFFFF, draft.color(role).getRGB() & 0xFFFFFF, role.name());
    }

    @Test
    void changingOnePartLeavesTheRolesThatSharedItsColor() {
        ThemeDraft draft = new ThemeDraft(ColorTheme.DEFAULT);
        Color accent = draft.color(ThemeRole.PRIMARY_ACTION);

        draft.changeRole(ThemeRole.SHIP_NAME_TEXT, PICKED);

        assertEquals(accent, draft.color(ThemeRole.PRIMARY_ACTION));
        assertEquals(PICKED.getRGB() & 0xFFFFFF, draft.color(ThemeRole.SHIP_NAME_TEXT).getRGB() & 0xFFFFFF);
    }

    /** The picker has no alpha: a see-through role must stay see-through. */
    @Test
    void aPickedColorKeepsTheAlphaOfTheColorItReplaces() {
        ThemeDraft draft = new ThemeDraft(ColorTheme.DEFAULT);
        int accentAlpha = draft.color(ThemeRole.PRIMARY_ACTION).getAlpha();

        draft.changeRole(ThemeRole.PRIMARY_ACTION, PICKED);

        assertTrue(accentAlpha < 0xFF, "the accent is see-through in Default");
        assertEquals(accentAlpha, draft.color(ThemeRole.PRIMARY_ACTION).getAlpha());
    }

    @Test
    void aDraftIsDirtyUntilRevertedOrSaved() {
        ThemeDraft draft = new ThemeDraft(new ColorTheme("Mine", "", Map.of()));
        draft.changeRole(ThemeRole.DANGER, PICKED);
        assertTrue(draft.isDirty());

        draft.revert();
        assertFalse(draft.isDirty());

        draft.changeRole(ThemeRole.DANGER, PICKED);
        draft.markSaved(draft.current());
        assertFalse(draft.isDirty());
    }

    /** The orange theme as first made: a see-through accent, five texts at 3.3:1. Fix all clears every one. */
    @Test
    void fixAllLeavesNothingHardToRead() {
        Color seeThroughOrange = new Color(0xA6FF6600, true);
        ThemeDraft draft = new ThemeDraft(new ColorTheme("Orange", "", Map.of(
                ThemeRole.APPLICATION_BACKGROUND, new Color(0x10141C),
                ThemeRole.PRIMARY_ACTION, seeThroughOrange,
                ThemeRole.INFORMATION, seeThroughOrange,
                ThemeRole.SHIP_NAME_TEXT, seeThroughOrange,
                ThemeRole.CREDITS_TEXT, seeThroughOrange,
                ThemeRole.SELECTED_TEXT, new Color(0x10141C))));
        assertFalse(ThemeContrast.check(draft.current()).isEmpty());

        draft.makeAllReadable();

        assertEquals(List.of(), ThemeContrast.check(draft.current()));
    }

    /**
     * Purple Rain, as a commander made it: white selected text on a light purple accent (4.4:1), and dark purple
     * selected tabs. White on the accent and the accent on the near-black background cannot both reach 4.5:1, so the
     * fix balances them. It must never turn the text black, which once left the selected tabs unreadable.
     */
    @Test
    void aFixNeverMakesTheSelectedTabsHarderToRead() {
        Color darkPurpleFill = new Color(0xBF6600CC, true);
        ThemeDraft draft = new ThemeDraft(new ColorTheme("Purple Rain", "", Map.of(
                ThemeRole.APPLICATION_BACKGROUND, new Color(0x0C040C),
                ThemeRole.SELECTED_TEXT, Color.WHITE,
                ThemeRole.PRIMARY_ACTION, new Color(0x9A57DE),
                ThemeRole.MAIN_TAB_ACTIVE_BACKGROUND, darkPurpleFill,
                ThemeRole.SECTION_TAB_ACTIVE_BACKGROUND, darkPurpleFill,
                ThemeRole.PRIMARY_BUTTON_PRESSED_BACKGROUND, darkPurpleFill)));

        ThemeDraft.Fix fix = draft.makeReadable(ThemeRole.SELECTED_TEXT, ThemeRole.PRIMARY_ACTION);

        assertEquals(ThemeDraft.Fix.IMPROVED, fix);
        assertEquals(Color.WHITE, draft.color(ThemeRole.SELECTED_TEXT));
        for (ThemeRole tab : List.of(ThemeRole.MAIN_TAB_ACTIVE_BACKGROUND, ThemeRole.SECTION_TAB_ACTIVE_BACKGROUND)) {
            double ratio = ThemeContrast.ratio(draft.current(), new ThemeContrast.Pair(ThemeRole.SELECTED_TEXT, tab));
            assertTrue(ratio > 10, tab + " is only " + ratio);
        }
        double accentPair = ThemeContrast.ratio(draft.current(),
                new ThemeContrast.Pair(ThemeRole.SELECTED_TEXT, ThemeRole.PRIMARY_ACTION));
        assertTrue(accentPair > 4.4, "the accent pair got better: " + accentPair);
    }

    @Test
    void everyRoleIsListedUnderTheColorItShows() {
        ThemeDraft draft = new ThemeDraft(ColorTheme.DEFAULT);

        int listed = draft.colorsInUse().values().stream().mapToInt(List::size).sum();

        assertEquals(ThemeRole.values().length, listed);
    }
}
