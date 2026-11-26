package elite.intel.ui.theme;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ThemeContrastTest {

    @TempDir
    Path folder;

    @Test
    void blackOnWhiteIsTwentyOneToOne() {
        assertEquals(21.0, ThemeContrast.ratio(Color.BLACK, Color.WHITE), 0.001);
    }

    @Test
    void theShippedDefaultHasNothingHardToRead() {
        assertEquals(List.of(), ThemeContrast.check(ColorTheme.DEFAULT));
    }

    /** High contrast promises more than the minimum: 7:1 for every text it draws. */
    @Test
    void highContrastReadsAtSevenToOneOrBetterEverywhere() throws IOException, ThemeFileException {
        ColorTheme highContrast = new ThemeStore(folder).load(ThemeStore.HIGH_CONTRAST_ID);

        for (ThemeContrast.Pair pair : ThemeContrast.PAIRS) {
            double ratio = ThemeContrast.ratio(highContrast, pair);
            assertTrue(ratio >= 7.0, pair + " is only " + ratio);
        }
    }

    @Test
    void clockworkOrangeHasNothingHardToRead() throws IOException, ThemeFileException {
        assertEquals(List.of(), ThemeContrast.check(new ThemeStore(folder).load(ThemeStore.CLOCKWORK_ORANGE_ID)));
    }

    @Test
    void frameShiftHasNothingHardToRead() throws IOException, ThemeFileException {
        assertEquals(List.of(), ThemeContrast.check(new ThemeStore(folder).load(ThemeStore.FRAMESHIFT)));
    }

    @Test
    void greyOnGreyIsReportedWithItsRatio() {
        ColorTheme murky = new ColorTheme("Murky", "", Map.of(
                ThemeRole.APPLICATION_BACKGROUND, new Color(0x505050),
                ThemeRole.SECONDARY_TEXT, new Color(0x6A6A6A)));

        List<ThemeContrast.Warning> warnings = ThemeContrast.check(murky);

        ThemeContrast.Warning secondary = warnings.stream()
                .filter(warning -> warning.text() == ThemeRole.SECONDARY_TEXT).findFirst().orElseThrow();
        assertEquals(ThemeRole.APPLICATION_BACKGROUND, secondary.background());
        assertTrue(secondary.ratio() < 1.6, "ratio " + secondary.ratio());
    }

    /** Fix it: just far enough to read, and still the same colour family - orange stays orange. */
    @Test
    void aHardToReadOrangeIsMovedJustFarEnoughAndStaysOrange() {
        Color background = new Color(0x10141C);

        Color fixed = ThemeContrast.apart(new Color(0xAC490A), background).orElseThrow();

        double ratio = ThemeContrast.ratio(fixed, background);
        assertTrue(ratio >= ThemeContrast.MINIMUM && ratio < ThemeContrast.MINIMUM + 0.3, "ratio " + ratio);
        assertTrue(fixed.getRed() > fixed.getGreen() && fixed.getGreen() > fixed.getBlue(), "still orange: " + fixed);
    }

    @Test
    void readableColorsAreLeftAlone() {
        assertEquals(Optional.of(Color.WHITE), ThemeContrast.apart(Color.WHITE, new Color(0x10141C)));
    }

    /** White text that cannot get lighter is never flipped to black: that is the background's job. */
    @Test
    void lightTextIsNeverFlippedToDark() {
        assertEquals(Optional.empty(), ThemeContrast.apart(Color.WHITE, new Color(0x9A57DE)));
    }

    /** The selected tabs draw selected text on fills of their own, not on the accent: each is checked. */
    @Test
    void selectedTextIsCheckedOnTheTabFillsToo() {
        ColorTheme theme = new ColorTheme("Tabs", "", Map.of(
                ThemeRole.SELECTED_TEXT, Color.BLACK,
                ThemeRole.PRIMARY_ACTION, new Color(0x9A57DE),
                ThemeRole.MAIN_TAB_ACTIVE_BACKGROUND, new Color(0x4F019C)));

        assertTrue(ThemeContrast.check(theme).stream().anyMatch(warning ->
                warning.text() == ThemeRole.SELECTED_TEXT && warning.background() == ThemeRole.MAIN_TAB_ACTIVE_BACKGROUND));
    }

    /** A see-through colour is judged as it reaches the screen: laid over what it is drawn on. */
    @Test
    void aSeeThroughTextIsJudgedOverItsBackground() {
        Color halfWhite = new Color(255, 255, 255, 128);

        assertEquals(new Color(128, 128, 128), ThemeContrast.opaque(halfWhite, Color.BLACK));
    }
}
