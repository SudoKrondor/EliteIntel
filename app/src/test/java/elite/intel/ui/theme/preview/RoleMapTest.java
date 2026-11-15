package elite.intel.ui.theme.preview;

import elite.intel.ui.theme.ColorTheme;
import elite.intel.ui.theme.ThemeRole;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoleMapTest {

    private static final ColorTheme CODES = RoleMap.codeTheme();

    /** Two roles sharing a code would make a click on one pick the other. */
    @Test
    void everyRoleHasACodeOfItsOwn() {
        Set<Integer> codes = Arrays.stream(ThemeRole.values())
                .map(role -> CODES.color(role).getRGB())
                .collect(Collectors.toSet());

        assertEquals(ThemeRole.values().length, codes.size());
    }

    @Test
    void aPixelInARolesCodeIsThatRole() {
        BufferedImage image = filled(ThemeRole.DANGER);

        assertEquals(Optional.of(ThemeRole.DANGER), new RoleMap(image).roleAt(5, 5));
    }

    /** A curve's anti-aliased edge is a blend no role painted: the role around it is the one meant. */
    @Test
    void anEdgePixelTakesTheRoleMostOfItsNeighboursCarry() {
        BufferedImage image = filled(ThemeRole.SUCCESS);
        image.setRGB(5, 5, 0x123457);

        assertEquals(Optional.of(ThemeRole.SUCCESS), new RoleMap(image).roleAt(5, 5));
    }

    /** A glyph stroke is a pixel wide: pointing at the background beside it still means the text. */
    @Test
    void onABackgroundThePointerReachesTheNearestPart() {
        BufferedImage image = filled(ThemeRole.APPLICATION_BACKGROUND, 40);
        paint(image, 20, 20, ThemeRole.PRIMARY_TEXT);
        paint(image, 15, 20, ThemeRole.DANGER);

        RoleMap map = new RoleMap(image);

        assertEquals(Optional.of(ThemeRole.PRIMARY_TEXT), map.roleAt(18, 20, 4), "the nearer of two parts");
        assertEquals(Optional.of(ThemeRole.APPLICATION_BACKGROUND), map.roleAt(5, 5, 4), "away from every part");
    }

    /** Text on a button: near the text the text wins, further into the padding the button's fill is reachable. */
    @Test
    void onAPartTheSmallestPartCloseByWins() {
        BufferedImage image = filled(ThemeRole.APPLICATION_BACKGROUND, 40);
        // WHY 10 x 3: a button's fill, kept under the 2.5% that would make it a surface
        for (int y = 13; y < 16; y++) for (int x = 10; x < 20; x++) paint(image, x, y, ThemeRole.DANGER);
        paint(image, 12, 14, ThemeRole.PRIMARY_TEXT);

        RoleMap map = new RoleMap(image);

        assertEquals(Optional.of(ThemeRole.PRIMARY_TEXT), map.roleAt(13, 14, 4));
        assertEquals(Optional.of(ThemeRole.DANGER), map.roleAt(18, 14, 4));
    }

    @Test
    void aPixelNoRolePaintedNearbyIsNoRole() {
        BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        fill(image, new Color(0x123457));

        assertEquals(Optional.empty(), new RoleMap(image).roleAt(5, 5));
    }

    @Test
    void outsideTheImageIsNoRole() {
        RoleMap map = new RoleMap(filled(ThemeRole.DANGER));

        assertEquals(Optional.empty(), map.roleAt(-1, 0));
        assertEquals(Optional.empty(), map.roleAt(0, 10));
    }

    /** Pointing at a colour must leave its parts as they are and dim the rest. */
    @Test
    void theSpotlightVeilsEverythingButTheChosenRoles() {
        BufferedImage image = filled(ThemeRole.SUCCESS);
        image.setRGB(3, 3, CODES.color(ThemeRole.DANGER).getRGB());
        Color veil = new Color(0, 0, 0, 0x8C);

        BufferedImage layer = new RoleMap(image).spotlight(java.util.List.of(ThemeRole.DANGER), veil);

        assertEquals(0, layer.getRGB(3, 3) >>> 24, "the chosen role is left clear");
        assertEquals(veil.getRGB(), layer.getRGB(0, 0), "the rest is veiled");
    }

    /** A background covers much of the preview; a part on it covers little. Only the part is spotlit. */
    @Test
    void aRoleCoveringMuchOfThePreviewIsASurface() {
        BufferedImage image = filled(ThemeRole.APPLICATION_BACKGROUND);
        image.setRGB(3, 3, CODES.color(ThemeRole.DANGER).getRGB());

        RoleMap map = new RoleMap(image);

        assertTrue(map.isSurface(ThemeRole.APPLICATION_BACKGROUND));
        assertFalse(map.isSurface(ThemeRole.DANGER));
        assertFalse(map.isSurface(ThemeRole.SUCCESS), "a role that paints nothing is no surface");
    }

    /** A swatch hides everything but its parts, backgrounds included, under a cover that stands off its colour. */
    @Test
    void isolatingRolesHidesEverythingElseOpaquely() {
        BufferedImage image = filled(ThemeRole.APPLICATION_BACKGROUND, 40);
        paint(image, 3, 3, ThemeRole.DANGER);
        paint(image, 4, 4, ThemeRole.PRIMARY_TEXT);

        BufferedImage layer = new RoleMap(image)
                .isolate(java.util.List.of(ThemeRole.APPLICATION_BACKGROUND, ThemeRole.DANGER), Color.WHITE);

        assertEquals(0, layer.getRGB(0, 0) >>> 24, "a background in the swatch is left clear");
        assertEquals(0, layer.getRGB(3, 3) >>> 24, "a part in the swatch is left clear");
        assertEquals(0xFF, layer.getRGB(4, 4) >>> 24, "the rest is hidden, not dimmed");
    }

    @Test
    void theCoverIsDarkBehindALightColourAndLightBehindADarkOne() {
        assertTrue(luminance(RoleMap.coverAgainst(Color.WHITE)) < 0.2);
        assertTrue(luminance(RoleMap.coverAgainst(new Color(0x0A0F1E))) > 0.5);
    }

    @Test
    void aRoleThePreviewNeverShowsPaintsNothing() {
        RoleMap map = new RoleMap(filled(ThemeRole.DANGER));

        assertTrue(map.paints(ThemeRole.DANGER));
        assertFalse(map.paints(ThemeRole.SUCCESS));
    }

    private static double luminance(Color c) {
        return (c.getRed() + c.getGreen() + c.getBlue()) / (3 * 255.0);
    }

    private static void paint(BufferedImage image, int x, int y, ThemeRole role) {
        image.setRGB(x, y, CODES.color(role).getRGB());
    }

    private static BufferedImage filled(ThemeRole role, int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        fill(image, CODES.color(role));
        return image;
    }

    private static BufferedImage filled(ThemeRole role) {
        BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        fill(image, CODES.color(role));
        return image;
    }

    private static void fill(BufferedImage image, Color color) {
        Graphics2D g = image.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        g.dispose();
    }
}
