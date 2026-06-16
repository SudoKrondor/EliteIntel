package elite.intel.ui.widget;

import elite.intel.ui.theme.AppTheme;

import javax.swing.*;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.*;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * HUD-styled tabbed pane for cockpit-dashboard navigation and data-panel switching.
 * <p>
 * Use {@link Level} to select the visual density appropriate for the context.
 * Prefer this class over {@code AppTheme.style*TabbedPane} methods for new code.
 */
public class HudTabbedPane extends JTabbedPane {

    /** Client property key marking a flat/compact tab pane that should skip the content border. */
    static final String HUD_FLAT_TABS = "eliteIntel.hud.flatTabs";

    /** Visual density and framing level for a HUD tabbed pane. */
    public enum Level {
        /** Application-level navigation bar: compact, bold, icon-ready. */
        MAIN_NAV,
        /** Sub-navigation within a screen section: compact, normal weight, no content border. */
        SECTION,
        /** Data-panel tabs for dense diagnostic views: compact, bold, no content border. */
        COMPACT,
        /** Settings-style tabs with a visible content frame and larger tab insets. */
        STANDARD
    }

    /** Creates a HUD-styled tabbed pane at the given visual level. */
    public HudTabbedPane(Level level) {
        applyStyle(this, level);
    }

    /**
     * Applies HUD tab styling to an existing {@code JTabbedPane} instance.
     * Prefer constructing a {@link HudTabbedPane} directly for new code.
     */
    public static void applyStyle(JTabbedPane tp, Level level) {
        boolean flatContent = level != Level.STANDARD;
        boolean compact = level != Level.STANDARD;
        boolean mainNavigation = level == Level.MAIN_NAV;
        boolean section = level == Level.SECTION;

        tp.putClientProperty(HUD_FLAT_TABS, flatContent);
        tp.setOpaque(true);
        tp.setBackground(mainNavigation ? AppTheme.HUD_SHELL_BACKGROUND : AppTheme.HUD_CONTENT_BACKGROUND);
        tp.setForeground(AppTheme.FG);
        // MAIN_NAV uses WRAP so that calculateTabWidth can distribute width evenly in a single run.
        tp.setTabLayoutPolicy(mainNavigation ? JTabbedPane.WRAP_TAB_LAYOUT : JTabbedPane.SCROLL_TAB_LAYOUT);

        if (mainNavigation) {
            tp.setFont(tp.getFont().deriveFont(Font.BOLD, AppTheme.HUD_FONT_TAB_MAIN));
        } else if (level == Level.COMPACT) {
            tp.setFont(tp.getFont().deriveFont(Font.BOLD, AppTheme.HUD_FONT_TAB_COMPACT));
        } else if (level == Level.SECTION) {
            tp.setFont(tp.getFont().deriveFont(Font.PLAIN, AppTheme.HUD_FONT_TAB_SECTION));
        }

        tp.setUI(new HudTabbedPaneUi(flatContent, compact, mainNavigation, section));
    }

    private static class HudTabbedPaneUi extends BasicTabbedPaneUI {

        private static final int TINT_ACTIVE   = 0;
        private static final int TINT_INACTIVE = 1;
        private static final int TINT_DISABLED = 2;

        private final boolean flatContent;
        private final boolean compact;
        private final boolean mainNavigation;
        private final boolean section;

        /** Cache: original Icon → [active tint, inactive tint, disabled tint]. Keyed by identity. */
        private final Map<Icon, Icon[]> tintCache = new IdentityHashMap<>();

        HudTabbedPaneUi(boolean flatContent, boolean compact, boolean mainNavigation, boolean section) {
            this.flatContent = flatContent;
            this.compact = compact;
            this.mainNavigation = mainNavigation;
            this.section = section;
        }

        @Override
        protected void installDefaults() {
            super.installDefaults();
            contentBorderInsets = flatContent ? new Insets(0, 0, 0, 0) : new Insets(1, 1, 1, 1);
            if (mainNavigation) {
                // Top/bottom flush; left/right inset by SHELL_GAP so the first/last tab box lines up
                // with the inner panels' content inset from the window edge (hudScreenBorder). The
                // rail still spans full width, so the boxes read as inset from the line.
                tabAreaInsets = new Insets(0, AppTheme.SHELL_GAP, 0, AppTheme.SHELL_GAP);
                tabInsets = new Insets(12, 14, 12, 14);
            } else if (section) {
                // Small left indent so the first tab label is offset from the rail's start. The extra
                // bottom inset makes the row 5px taller so a transparent gap opens between the active
                // box and the bottom rail while the box keeps its height. Roomier horizontal tab insets.
                tabAreaInsets = new Insets(0, AppTheme.HUD_GAP, 0, 0);
                tabInsets = new Insets(6, 14, 11, 14);
            } else if (compact) {
                tabInsets = new Insets(3, 9, 3, 9);
            } else {
                tabInsets = new Insets(8, 14, 8, 14);
            }
            selectedTabPadInsets = new Insets(1, 1, 1, 1);
            focus = AppTheme.HUD_CYAN;
        }

        @Override
        protected void paintTabArea(Graphics g, int tabPlacement, int selectedIndex) {
            int tabAreaHeight = calculateTabAreaHeight(tabPlacement, runCount, maxTabHeight);
            g.setColor(mainNavigation ? AppTheme.HUD_SHELL_BACKGROUND : AppTheme.HUD_CONTENT_BACKGROUND);
            g.fillRect(0, 0, tabPane.getWidth(), tabAreaHeight);
            super.paintTabArea(g, tabPlacement, selectedIndex);
            if (mainNavigation) {
                int width = tabPane.getWidth();
                // top rail — muted warm, 2px
                g.setColor(AppTheme.HUD_ORANGE_SOFT);
                g.fillRect(0, 0, width, 2);
                // bottom rail — same fill as the active tab box, 3px; flush to tab-area bottom
                g.setColor(AppTheme.HUD_TAB_MAIN_FILL);
                g.fillRect(0, tabAreaHeight - 3, width, 3);
            } else if (section) {
                // Underline rail under the section tab row (§11): warm red-orange, paired with the
                // active box fill so the active tab reads as live (not the old muted/dim box).
                g.setColor(AppTheme.HUD_TAB_SECTION_RAIL);
                g.fillRect(0, tabAreaHeight - 2, tabPane.getWidth(), 2);
            }
        }

        @Override
        protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex,
                                          int x, int y, int w, int h, boolean isSelected) {
            if (mainNavigation) {
                if (isSelected) {
                    int gap = 8;
                    int bottomRail = 3;
                    int bottomGap = gap - 2; // box extends 2px lower, closing the gap to the rail (nav height unchanged)
                    int fillH = h - gap - bottomGap - bottomRail;
                    if (fillH < 1) fillH = 1;
                    g.setColor(AppTheme.HUD_TAB_MAIN_FILL);
                    g.fillRect(x, y + gap, w, fillH);
                }
                // unselected: no fill — paintTabArea already painted HUD_SHELL_BACKGROUND
                return;
            }
            if (section) {
                // Active = filled box (inversion, §11) in a bright red-shifted orange so the active
                // tab reads as live; the warmer hue separates it from the MAIN_NAV accent box.
                if (isSelected) {
                    int gap = 4;
                    int bottomRail = 2;
                    int separator = 5;   // transparent gap between the box and the bottom rail
                    int fillH = h - gap - separator - bottomRail;
                    if (fillH < 1) fillH = 1;
                    g.setColor(AppTheme.HUD_TAB_SECTION_FILL);
                    g.fillRect(x, y + gap, w, fillH);
                }
                // unselected: leave the HUD_CONTENT_BACKGROUND painted by paintTabArea
                return;
            }
            Color background = compact ? AppTheme.HUD_SHELL_BACKGROUND : AppTheme.HUD_CONTENT_BACKGROUND;
            g.setColor(compact ? background : isSelected ? AppTheme.HUD_PANEL_BG_ALT : background);
            g.fillRect(x, y, w, h);
        }

        @Override
        protected void paintIcon(Graphics g, int tabPlacement, int tabIndex,
                                 Icon icon, Rectangle iconRect, boolean isSelected) {
            if (!mainNavigation || !(icon instanceof ImageIcon imageIcon)) {
                super.paintIcon(g, tabPlacement, tabIndex, icon, iconRect, isSelected);
                return;
            }
            Color tintColor;
            if (!tabPane.isEnabled() || !tabPane.isEnabledAt(tabIndex)) {
                tintColor = AppTheme.HUD_DISABLED;
            } else {
                tintColor = isSelected ? AppTheme.SEL_FG : AppTheme.HUD_ORANGE_SOFT;
            }
            int slot = tintColor == AppTheme.SEL_FG ? TINT_ACTIVE
                     : tintColor == AppTheme.HUD_ORANGE_SOFT ? TINT_INACTIVE
                     : TINT_DISABLED;
            int w = iconRect.width  > 0 ? iconRect.width  : AppTheme.HUD_ICON_NAV;
            int h = iconRect.height > 0 ? iconRect.height : AppTheme.HUD_ICON_NAV;
            Icon tinted = cachedTint(imageIcon, w, h, slot, tintColor);
            tinted.paintIcon(tabPane, g, iconRect.x, iconRect.y);
        }

        private Icon cachedTint(ImageIcon original, int w, int h, int slot, Color color) {
            Icon[] slots = tintCache.computeIfAbsent(original, k -> new Icon[3]);
            if (slots[slot] == null) {
                slots[slot] = AppTheme.tintIcon(original, w, h, color);
            }
            return slots[slot];
        }

        @Override
        protected void paintTabBorder(Graphics g, int tabPlacement, int tabIndex,
                                      int x, int y, int w, int h, boolean isSelected) {
            if (!isSelected) return;
            if (mainNavigation || section) return; // selection = filled box (inversion), no underline
            // Orange underline
            g.setColor(AppTheme.ACCENT);
            g.fillRect(x, y + h - 3, w, 3);
        }

        @Override
        protected void layoutLabel(int tabPlacement, FontMetrics metrics, int tabIndex,
                                   String title, Icon icon, Rectangle tabRect, Rectangle iconRect,
                                   Rectangle textRect, boolean isSelected) {
            super.layoutLabel(tabPlacement, metrics, tabIndex,
                    title != null ? title.toUpperCase() : title,
                    icon, tabRect, iconRect, textRect, isSelected);

            if (!mainNavigation && !section) return;

            // Re-center the icon+label group vertically in the active box. The `- 3` exclusion matches
            // both MAIN_NAV (3px underline at the bottom) and SECTION (whose top gap minus bottom
            // separator+rail nets the same offset), keeping the label centered in the visible box.
            boolean hasIcon = icon != null && iconRect.width > 0;
            boolean hasText = title != null && !title.isEmpty() && textRect.width > 0;
            int groupTop, groupBottom;
            if (hasIcon && hasText) {
                groupTop = Math.min(iconRect.y, textRect.y);
                groupBottom = Math.max(iconRect.y + iconRect.height, textRect.y + textRect.height);
            } else if (hasIcon) {
                groupTop = iconRect.y;
                groupBottom = iconRect.y + iconRect.height;
            } else if (hasText) {
                groupTop = textRect.y;
                groupBottom = textRect.y + textRect.height;
            } else {
                return;
            }

            // Usable area excludes the 3px underline painted at the tab bottom.
            int usableCenterY = (tabRect.y + tabRect.y + tabRect.height - 3) / 2;
            int groupCenterY = (groupTop + groupBottom) / 2;
            int delta = usableCenterY - groupCenterY;
            if (delta != 0) {
                iconRect.y += delta;
                textRect.y += delta;
            }
        }

        @Override
        protected void paintText(Graphics g, int tabPlacement, Font font, FontMetrics metrics,
                                 int tabIndex, String title, Rectangle textRect, boolean isSelected) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
            g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            g.setFont(font);
            String upper = title != null ? title.toUpperCase() : "";
            if (!tabPane.isEnabled() || !tabPane.isEnabledAt(tabIndex)) {
                g.setColor(AppTheme.HUD_DISABLED);
            } else if (mainNavigation || section) {
                g.setColor(isSelected ? AppTheme.SEL_FG : AppTheme.FG_MUTED);
            } else {
                g.setColor(isSelected ? AppTheme.ACCENT : AppTheme.FG_MUTED);
            }
            g.drawString(upper, textRect.x, textRect.y + metrics.getAscent());
        }

        @Override
        protected int calculateTabWidth(int tabPlacement, int tabIndex, FontMetrics metrics) {
            if (mainNavigation) {
                int count = Math.max(1, tabPane.getTabCount());
                Insets pi = tabPane.getInsets();
                // Subtract tabAreaInsets too, otherwise the even-width tabs sum to the full pane
                // width and overflow past the left inset, forcing a second WRAP row.
                int available = tabPane.getWidth() - pi.left - pi.right
                        - tabAreaInsets.left - tabAreaInsets.right;
                if (available > 0) {
                    int slot = available / count;
                    // Last tab absorbs any pixel remainder from integer division.
                    return (tabIndex == count - 1) ? available - slot * (count - 1) : slot;
                }
            }
            int base = super.calculateTabWidth(tabPlacement, tabIndex, metrics);
            String title = tabPane.getTitleAt(tabIndex);
            if (title != null && !title.isEmpty()) {
                base += metrics.stringWidth(title.toUpperCase())
                        - metrics.stringWidth(title);
            }
            return base;
        }

        @Override
        protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {
            if (flatContent) {
                return;
            }
            Insets in = tabPane.getInsets();
            int top = calculateTabAreaHeight(tabPlacement, runCount, maxTabHeight);
            int x = in.left;
            int y = in.top + top;
            int w = tabPane.getWidth() - in.left - in.right;
            int h = tabPane.getHeight() - y - in.bottom;
            g.setColor(AppTheme.HUD_BORDER_DIM);
            g.drawRect(x, y, Math.max(0, w - 1), Math.max(0, h - 1));
        }

        @Override
        protected void paintFocusIndicator(Graphics g, int tabPlacement, Rectangle[] rects,
                                           int tabIndex, Rectangle iconRect,
                                           Rectangle textRect, boolean isSelected) {
            // no dotted focus ring
        }
    }
}
