package elite.intel.ui.theme.preview;

import elite.intel.ui.theme.ColorTheme;
import elite.intel.ui.theme.ThemeRole;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Which colour role painted each pixel of the theme preview, so a click on the preview can pick the role under it.
 * <p>
 * The preview is rendered a second time with every role in a colour of its own, its <b>code</b>
 * ({@link #codeTheme()}), and with text anti-aliasing off. A role is then found by the code at the clicked pixel. The
 * layout of the preview does not depend on its colours, so one role map serves every theme.
 * <p>
 * The codes sit on a coarse grid (each channel one of four levels), far apart from each other. A curve's
 * anti-aliased edge blends two codes into a colour that is no code, or rarely into another code: the lookup trusts
 * an exact code under the pointer, and otherwise takes the code most of the pixels around it carry.
 */
public final class RoleMap {

    private static final int[] LEVELS = {0x18, 0x58, 0x98, 0xD8};
    /** How far from the clicked pixel to look when it is an anti-aliased edge rather than a code. */
    static final int SEARCH_RADIUS = 2;

    private static final Map<ThemeRole, Color> CODES = assignCodes();
    private static final Map<Integer, ThemeRole> ROLE_BY_CODE = invert(CODES);

    /**
     * The share of the preview a role must cover to be a surface. WHY 2.5%: measured on the sample (2026-11-14),
     * the six surfaces each cover 3% or more (the application background 40%) and every other role under 2%.
     */
    static final double SURFACE_SHARE = 0.025;

    /** What {@link #isolate} hides the rest of the preview under: one of the two, whichever stands off the colour. */
    private static final Color COVER_DARK = new Color(0x101010);
    private static final Color COVER_LIGHT = new Color(0xB0B0B0);

    private final BufferedImage image;
    /** How many pixels each role paints: a role that paints none is absent. */
    private final Map<ThemeRole, Integer> areas;
    private final Set<ThemeRole> surfaces;

    /**
     * @param image the preview rendered with {@link #codeTheme()}, in device pixels
     */
    public RoleMap(BufferedImage image) {
        this.image = image;
        this.areas = areas(image);
        this.surfaces = surfaces(areas, image);
    }

    /** Whether a role paints any pixel of the preview. Ten roles never do: they are edited from the swatches only. */
    public boolean paints(ThemeRole role) {
        return areas.containsKey(role);
    }

    /**
     * Whether a role paints a large area of the preview - a background - rather than a part on it. Spotlighting a
     * surface would leave the background clear and dim every widget on it, so the whole view seems to go dark.
     */
    public boolean isSurface(ThemeRole role) {
        return surfaces.contains(role);
    }

    /** A theme that paints every role in its own code, for rendering the role map. */
    public static ColorTheme codeTheme() {
        return new ColorTheme("Role map", "", CODES);
    }

    /**
     * The role painted at a pixel, or empty for a pixel no role painted on its own (an edge between two, or a colour
     * a widget derived from a role by blending it).
     *
     * @param x device pixels, as in the image
     * @param y device pixels, as in the image
     */
    public Optional<ThemeRole> roleAt(int x, int y) {
        return roleAt(x, y, SEARCH_RADIUS);
    }

    /**
     * The role the pointer at a pixel means, reaching out to a part near it rather than demanding the pointer sit on
     * one exactly. WHY: a glyph's stroke or a frame is one or two pixels wide, and the gaps between strokes belong to
     * the background, so an exact lookup made text and thin parts all but impossible to point at (2026-11-14).
     * <ul>
     *     <li>On a surface, or an edge no role painted: the nearest part within {@code reach}. Away from every
     *     part, the surface itself.</li>
     *     <li>On a part: the smallest part within half the {@code reach}, so text on a button wins over the button's
     *     fill near the text, while the fill stays reachable in the padding around it.</li>
     * </ul>
     *
     * @param x     device pixels, as in the image
     * @param y     device pixels, as in the image
     * @param reach how far to look for a part, in device pixels
     */
    public Optional<ThemeRole> roleAt(int x, int y, int reach) {
        if (!contains(x, y)) return Optional.empty();
        ThemeRole exact = ROLE_BY_CODE.get(rgb(x, y));
        if (exact != null && !surfaces.contains(exact)) return Optional.of(smallestPartAround(x, y, reach / 2, exact));
        Optional<ThemeRole> part = nearestPartAround(x, y, reach);
        if (part.isPresent()) return part;
        if (exact != null) return Optional.of(exact);
        return mostCommonRoleAround(x, y);
    }

    /**
     * A see-through layer, the size of the image, that lays {@code veil} over every pixel <em>not</em> painted by one
     * of {@code roles}: laid over the preview, it leaves where those roles are used standing out. WHY a veil over the
     * rest rather than a tint over the roles: a tint over thin text barely shows, and changes the very colour the
     * commander is looking at.
     */
    public BufferedImage spotlight(Collection<ThemeRole> roles, Color veil) {
        BufferedImage layer = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        int argb = veil.getRGB();
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                ThemeRole role = ROLE_BY_CODE.get(rgb(x, y));
                if (role == null || !roles.contains(role)) layer.setRGB(x, y, argb);
            }
        }
        return layer;
    }

    /**
     * An opaque layer, the size of the image, that hides every pixel <em>not</em> painted by one of {@code roles}:
     * laid over the preview, only those roles' parts are left, backgrounds included. For a swatch, where the
     * commander asks "what does this colour paint?" and a dimmed rest still drew the eye away from thin parts.
     *
     * @param color the colour those roles share: the cover is made dark or light against it, so the parts it leaves
     *              never vanish into it
     */
    public BufferedImage isolate(Collection<ThemeRole> roles, Color color) {
        int cover = coverAgainst(color).getRGB();
        BufferedImage layer = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                ThemeRole role = ROLE_BY_CODE.get(rgb(x, y));
                if (role == null || !roles.contains(role)) layer.setRGB(x, y, cover);
            }
        }
        return layer;
    }

    /** Near-black behind a light colour, mid-light grey behind a dark one. */
    static Color coverAgainst(Color color) {
        double luminance = (0.2126 * color.getRed() + 0.7152 * color.getGreen() + 0.0722 * color.getBlue()) / 255;
        return luminance > 0.4 ? COVER_DARK : COVER_LIGHT;
    }

    private ThemeRole smallestPartAround(int x, int y, int radius, ThemeRole exact) {
        ThemeRole smallest = exact;
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                if (!contains(x + dx, y + dy)) continue;
                ThemeRole role = ROLE_BY_CODE.get(rgb(x + dx, y + dy));
                if (role != null && !surfaces.contains(role) && areas.get(role) < areas.get(smallest)) smallest = role;
            }
        }
        return smallest;
    }

    private Optional<ThemeRole> nearestPartAround(int x, int y, int radius) {
        ThemeRole nearest = null;
        int nearestDistance = Integer.MAX_VALUE;
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                int distance = dx * dx + dy * dy;
                if (distance >= nearestDistance || distance > radius * radius || !contains(x + dx, y + dy)) continue;
                ThemeRole role = ROLE_BY_CODE.get(rgb(x + dx, y + dy));
                if (role != null && !surfaces.contains(role)) {
                    nearest = role;
                    nearestDistance = distance;
                }
            }
        }
        return Optional.ofNullable(nearest);
    }

    private Optional<ThemeRole> mostCommonRoleAround(int x, int y) {
        Map<ThemeRole, Integer> counts = new EnumMap<>(ThemeRole.class);
        for (int dy = -SEARCH_RADIUS; dy <= SEARCH_RADIUS; dy++) {
            for (int dx = -SEARCH_RADIUS; dx <= SEARCH_RADIUS; dx++) {
                if (!contains(x + dx, y + dy)) continue;
                ThemeRole role = ROLE_BY_CODE.get(rgb(x + dx, y + dy));
                if (role != null) counts.merge(role, 1, Integer::sum);
            }
        }
        return counts.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey);
    }

    private static Map<ThemeRole, Integer> areas(BufferedImage image) {
        Map<ThemeRole, Integer> counts = new EnumMap<>(ThemeRole.class);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                ThemeRole role = ROLE_BY_CODE.get(image.getRGB(x, y) & 0xFFFFFF);
                if (role != null) counts.merge(role, 1, Integer::sum);
            }
        }
        return counts;
    }

    private static Set<ThemeRole> surfaces(Map<ThemeRole, Integer> counts, BufferedImage image) {
        double threshold = SURFACE_SHARE * image.getWidth() * image.getHeight();
        Set<ThemeRole> surfaces = EnumSet.noneOf(ThemeRole.class);
        counts.forEach((role, count) -> {
            if (count >= threshold) surfaces.add(role);
        });
        return surfaces;
    }

    private boolean contains(int x, int y) {
        return x >= 0 && y >= 0 && x < image.getWidth() && y < image.getHeight();
    }

    private int rgb(int x, int y) {
        return image.getRGB(x, y) & 0xFFFFFF;
    }

    /**
     * WHY ordinal + 1: the darkest grid colour is skipped, being the nearest to the app's own near-black backgrounds.
     */
    private static Map<ThemeRole, Color> assignCodes() {
        int available = LEVELS.length * LEVELS.length * LEVELS.length - 1;
        if (ThemeRole.values().length > available) {
            throw new IllegalStateException("More theme roles than role map codes: " + ThemeRole.values().length);
        }
        Map<ThemeRole, Color> codes = new EnumMap<>(ThemeRole.class);
        for (ThemeRole role : ThemeRole.values()) {
            int index = role.ordinal() + 1;
            int r = LEVELS[index / (LEVELS.length * LEVELS.length)];
            int g = LEVELS[index / LEVELS.length % LEVELS.length];
            int b = LEVELS[index % LEVELS.length];
            codes.put(role, new Color(r, g, b));
        }
        return codes;
    }

    private static Map<Integer, ThemeRole> invert(Map<ThemeRole, Color> codes) {
        Map<Integer, ThemeRole> byCode = new HashMap<>();
        codes.forEach((role, color) -> byCode.put(color.getRGB() & 0xFFFFFF, role));
        return byCode;
    }
}
