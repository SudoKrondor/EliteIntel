package elite.intel.ui.theme;

import java.awt.*;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * A theme being edited in the Color Themes tab: the saved theme, and the colours changed since.
 * <p>
 * Several roles share a colour - the accent paints about a dozen - so a change can reach one role or every role
 * that shows the same colour right now ({@link #changeEveryRoleColoredLike}). The grouping is by the current
 * colour, not by a fixed base colour, so it follows whatever the commander has already split apart.
 * <p>
 * A picked colour keeps the alpha of the colour it replaces: the colour picker has no alpha, and a role drawn
 * see-through (the accent is) would otherwise turn solid behind the commander's back.
 */
public final class ThemeDraft {

    private static final int MAX_FIX_PASSES = 4;
    private static final int BALANCE_STEPS = 100;
    /** A balance must raise the worst ratio by at least this much to be worth a change. */
    private static final double BALANCE_GAIN = 0.01;

    private ColorTheme saved;
    private final Map<ThemeRole, Color> colors = new EnumMap<>(ThemeRole.class);

    public ThemeDraft(ColorTheme saved) {
        this.saved = saved;
        revert();
    }

    /** The theme as edited so far. */
    public ColorTheme current() {
        return new ColorTheme(saved.name(), saved.author(), colors);
    }

    /** Whether anything changed since the theme was last saved. */
    public boolean isDirty() {
        return !current().equals(saved);
    }

    /** The colour a role shows in the draft. */
    public Color color(ThemeRole role) {
        return colors.get(role);
    }

    /** Gives one role a new colour, keeping its alpha. */
    public void changeRole(ThemeRole role, Color picked) {
        colors.put(role, withAlphaOf(colors.get(role), picked));
    }

    /** What a fix achieved. */
    public enum Fix {
        /** The pair reads at the minimum now, and nothing else got harder to read. */
        FIXED,
        /** The pair reads better, but not at the minimum: these colours allow no more without hurting other text. */
        IMPROVED,
        /** Nothing could be changed without making other text harder to read; nothing was changed. */
        NOT_POSSIBLE
    }

    /**
     * Makes one hard-to-read pair readable without making anything else worse, trying in turn:
     * <ol>
     *     <li>the text, moved further the way it already leans (light text lighter, dark text darker);</li>
     *     <li>the background, moved away from the text;</li>
     *     <li>a balance, when the colours cannot all reach the minimum together: the move that most raises the
     *         worst ratio among the pairs these two roles are part of.</li>
     * </ol>
     * A step is taken only if no other pair becomes hard to read. WHY so careful: a fix that flipped light text to
     * dark once made a selected tab unreadable while curing the warning it was asked to cure. The changed colour is
     * opaque.
     */
    public Fix makeReadable(ThemeRole text, ThemeRole background) {
        ColorTheme now = current();
        ThemeContrast.Pair pair = new ThemeContrast.Pair(text, background);
        Set<ThemeContrast.Pair> hardNow = hardToRead(now);
        Color backgroundColor = ThemeContrast.shownBackground(now, background);
        Color textColor = ThemeContrast.opaque(colors.get(text), backgroundColor);

        Optional<Color> textMoved = ThemeContrast.apart(textColor, backgroundColor);
        if (textMoved.isPresent() && cleanFix(text, textMoved.get(), pair, hardNow)) return Fix.FIXED;
        Optional<Color> backgroundMoved = ThemeContrast.apart(backgroundColor, textColor);
        if (backgroundMoved.isPresent() && cleanFix(background, backgroundMoved.get(), pair, hardNow)) return Fix.FIXED;
        return balance(text, textColor, background, backgroundColor, hardNow) ? Fix.IMPROVED : Fix.NOT_POSSIBLE;
    }

    /**
     * Fixes every hard-to-read pair it can, by the same rules as {@link #makeReadable}. WHY passes: a role can be
     * text and background at once - the accent is both text and the fill under selected text - so one fix can make
     * room for another. It stops as soon as a pass changes nothing.
     */
    public void makeAllReadable() {
        for (int pass = 0; pass < MAX_FIX_PASSES; pass++) {
            ColorTheme before = current();
            for (ThemeContrast.Warning warning : ThemeContrast.check(before)) {
                makeReadable(warning.text(), warning.background());
            }
            if (current().equals(before)) return;
        }
    }

    private boolean cleanFix(ThemeRole role, Color color, ThemeContrast.Pair pair, Set<ThemeContrast.Pair> hardNow) {
        Set<ThemeContrast.Pair> hardAfter = hardToRead(with(role, color));
        if (hardAfter.contains(pair) || !hardNow.containsAll(hardAfter)) return false;
        colors.put(role, color);
        return true;
    }

    private boolean balance(ThemeRole text, Color textColor, ThemeRole background, Color backgroundColor,
                            Set<ThemeContrast.Pair> hardNow) {
        Set<ThemeRole> involved = Set.of(text, background);
        double best = worstRatioAround(current(), involved);
        ThemeRole bestRole = null;
        Color bestColor = null;
        for (int step = 1; step <= BALANCE_STEPS; step++) {
            double share = step / (double) BALANCE_STEPS;
            for (ThemeRole role : List.of(text, background)) {
                Color moved = role == text
                        ? ThemeContrast.away(textColor, backgroundColor, share)
                        : ThemeContrast.away(backgroundColor, textColor, share);
                ColorTheme trial = with(role, moved);
                double worst = worstRatioAround(trial, involved);
                if (worst > best + BALANCE_GAIN && onlyAround(hardToRead(trial), hardNow, involved)) {
                    best = worst;
                    bestRole = role;
                    bestColor = moved;
                }
            }
        }
        if (bestRole == null) return false;
        colors.put(bestRole, bestColor);
        return true;
    }

    private static double worstRatioAround(ColorTheme theme, Set<ThemeRole> roles) {
        double worst = Double.MAX_VALUE;
        for (ThemeContrast.Pair pair : ThemeContrast.PAIRS) {
            if (roles.contains(pair.text()) || roles.contains(pair.background())) {
                worst = Math.min(worst, ThemeContrast.ratio(theme, pair));
            }
        }
        return worst;
    }

    /** No pair away from {@code roles} became hard to read. */
    private static boolean onlyAround(Set<ThemeContrast.Pair> hardAfter, Set<ThemeContrast.Pair> hardNow, Set<ThemeRole> roles) {
        for (ThemeContrast.Pair pair : hardAfter) {
            boolean around = roles.contains(pair.text()) || roles.contains(pair.background());
            if (!around && !hardNow.contains(pair)) return false;
        }
        return true;
    }

    private static Set<ThemeContrast.Pair> hardToRead(ColorTheme theme) {
        Set<ThemeContrast.Pair> pairs = new HashSet<>();
        for (ThemeContrast.Warning warning : ThemeContrast.check(theme)) pairs.add(warning.pair());
        return pairs;
    }

    private ColorTheme with(ThemeRole role, Color color) {
        Map<ThemeRole, Color> trial = new EnumMap<>(colors);
        trial.put(role, color);
        return new ColorTheme(saved.name(), saved.author(), trial);
    }

    /** Gives every role that shows the same colour as {@code role} right now the new colour, each keeping its alpha. */
    public void changeEveryRoleColoredLike(ThemeRole role, Color picked) {
        for (ThemeRole sharing : rolesColored(colors.get(role))) changeRole(sharing, picked);
    }

    /** Each colour the draft uses, with the roles that show it, in role order. */
    public Map<Color, List<ThemeRole>> colorsInUse() {
        Map<Color, List<ThemeRole>> inUse = new LinkedHashMap<>();
        for (ThemeRole role : ThemeRole.values()) {
            inUse.computeIfAbsent(colors.get(role), color -> new ArrayList<>()).add(role);
        }
        return inUse;
    }

    /** The roles that show {@code color}. */
    public List<ThemeRole> rolesColored(Color color) {
        List<ThemeRole> roles = new ArrayList<>();
        for (ThemeRole role : ThemeRole.values()) {
            if (colors.get(role).equals(color)) roles.add(role);
        }
        return roles;
    }

    /** Throws away the changes since the last save. */
    public void revert() {
        colors.clear();
        for (ThemeRole role : ThemeRole.values()) colors.put(role, saved.color(role));
    }

    /** Records that the draft as it stands has been saved, under the theme's possibly new name. */
    public void markSaved(ColorTheme savedAs) {
        saved = savedAs;
    }

    private static Color withAlphaOf(Color previous, Color picked) {
        return new Color(picked.getRed(), picked.getGreen(), picked.getBlue(), previous.getAlpha());
    }
}
