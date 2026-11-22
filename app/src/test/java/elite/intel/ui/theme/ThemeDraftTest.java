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
        List<ThemeRole> sharingAccent = draft.rolesLike(ThemeRole.PRIMARY_ACTION);

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
     * selected tabs. While the accent was one role, white on it and it on the near-black background could not both
     * reach 4.5:1. The accent fill is its own role now, so the fill darkens and the accent text keeps its contrast.
     * It must never turn the text black, which once left the selected tabs unreadable.
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

        ThemeDraft.Fix fix = draft.makeReadable(ThemeRole.SELECTED_TEXT, ThemeRole.ACCENT_FILL);

        assertEquals(ThemeDraft.Fix.FIXED, fix);
        assertEquals(Color.WHITE, draft.color(ThemeRole.SELECTED_TEXT));
        for (ThemeRole tab : List.of(ThemeRole.MAIN_TAB_ACTIVE_BACKGROUND, ThemeRole.SECTION_TAB_ACTIVE_BACKGROUND)) {
            double ratio = ThemeContrast.ratio(draft.current(), new ThemeContrast.Pair(ThemeRole.SELECTED_TEXT, tab));
            assertTrue(ratio > 10, tab + " is only " + ratio);
        }
        double accentPair = ThemeContrast.ratio(draft.current(),
                new ThemeContrast.Pair(ThemeRole.SELECTED_TEXT, ThemeRole.ACCENT_FILL));
        assertTrue(accentPair >= 4.5, "the accent fill pair reads: " + accentPair);
        assertEquals(new Color(0x9A57DE), draft.color(ThemeRole.PRIMARY_ACTION), "the accent text is left alone");
    }

    @Test
    void everyRoleIsListedUnderTheColorItShows() {
        ThemeDraft draft = new ThemeDraft(ColorTheme.DEFAULT);

        int listed = draft.colorsInUse().stream().mapToInt(group -> group.roles().size()).sum();

        assertEquals(ThemeRole.values().length, listed);
    }

    /** Default draws selected text in the application background's colour: changing one must not change the other. */
    @Test
    void changingEveryPartLeavesTheOtherKindAlone() {
        ThemeDraft draft = new ThemeDraft(ColorTheme.DEFAULT);
        Color background = draft.color(ThemeRole.APPLICATION_BACKGROUND);
        assertEquals(background, draft.color(ThemeRole.SELECTED_TEXT), "Default shares this colour across kinds");

        draft.changeEveryRoleColoredLike(ThemeRole.SELECTED_TEXT, PICKED);

        assertEquals(background, draft.color(ThemeRole.APPLICATION_BACKGROUND));
        assertEquals(PICKED.getRGB() & 0xFFFFFF, draft.color(ThemeRole.SELECTED_TEXT).getRGB() & 0xFFFFFF);
    }

    @Test
    void aColorBothKindsShowIsTwoSwatches() {
        ThemeDraft draft = new ThemeDraft(ColorTheme.DEFAULT);
        Color shared = draft.color(ThemeRole.APPLICATION_BACKGROUND);

        List<ThemeDraft.ColorGroup> groups = draft.colorsInUse().stream()
                .filter(group -> group.color().equals(shared)).toList();

        assertEquals(2, groups.size());
        for (ThemeDraft.ColorGroup group : groups) {
            for (ThemeRole role : group.roles()) assertEquals(group.kind(), role.kind(), role.name());
        }
    }

    /** The accent as text and the accent as a fill are separate: a new text accent leaves selections as they were. */
    @Test
    void theAccentTextAndTheAccentFillChangeApart() {
        ThemeDraft draft = new ThemeDraft(ColorTheme.DEFAULT);
        Color fill = draft.color(ThemeRole.ACCENT_FILL);

        draft.changeEveryRoleColoredLike(ThemeRole.PRIMARY_ACTION, PICKED);

        assertEquals(fill, draft.color(ThemeRole.ACCENT_FILL));
        assertEquals(PICKED.getRGB() & 0xFFFFFF, draft.color(ThemeRole.SHIP_NAME_TEXT).getRGB() & 0xFFFFFF);
    }
}
