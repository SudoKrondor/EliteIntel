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
        image.setRGB(6, 5, CODES.color(ThemeRole.WARNING).getRGB());

        assertEquals(Optional.of(ThemeRole.SUCCESS), new RoleMap(image).roleAt(5, 5));
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
