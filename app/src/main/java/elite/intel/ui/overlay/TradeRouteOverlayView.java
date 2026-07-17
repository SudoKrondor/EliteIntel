package elite.intel.ui.overlay;

import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.theme.HudGlyphs;
import elite.intel.ui.theme.HudPalette;

import javax.swing.*;
import java.awt.*;
import java.text.NumberFormat;
import java.util.Locale;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;

/**
 * Compact in-game view of the immediate trading action, current stop, and subordinate plotted navigation.
 * All rendering updates are confined to the EDT by {@link RouteOverlayModule}.
 */
final class TradeRouteOverlayView extends JPanel {

    private final JLabel actionValue = valueLabel(SwingConstants.LEFT,
            HudPalette.HUD_COLOR_ROLE_WARNING, HudPalette.HUD_FONT_APP_TITLE);
    private final JLabel cargoValue = valueLabel(SwingConstants.LEFT,
            HudPalette.HUD_COLOR_ROLE_PRIMARY_TEXT, HudPalette.HUD_FONT_MD);
    private final JLabel profitValue = valueLabel(SwingConstants.RIGHT,
            HudPalette.HUD_COLOR_ROLE_SUCCESS, HudPalette.HUD_FONT_STAT_LG);
    private final JLabel systemValue = valueLabel(SwingConstants.LEFT,
            HudPalette.HUD_COLOR_ROLE_INFORMATION, HudPalette.HUD_FONT_MD);
    private final JLabel stationValue = valueLabel(SwingConstants.LEFT,
            HudPalette.HUD_COLOR_ROLE_PRIMARY_TEXT, HudPalette.HUD_FONT_READOUT_VALUE);
    private final JLabel stopValue = valueLabel(SwingConstants.RIGHT,
            HudPalette.HUD_COLOR_ROLE_INFORMATION, HudPalette.HUD_FONT_READOUT_VALUE);
    private final JLabel nextJumpValue = valueLabel(SwingConstants.LEFT,
            HudPalette.HUD_COLOR_ROLE_INFORMATION, HudPalette.HUD_FONT_READOUT_VALUE);
    private final JLabel jumpsValue = valueLabel(SwingConstants.LEFT,
            HudPalette.HUD_COLOR_ROLE_INFORMATION, HudPalette.HUD_FONT_READOUT_VALUE);
    private final JPanel navigationRow;

    TradeRouteOverlayView() {
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        addRow(createSummaryRow());
        add(Box.createVerticalStrut(HudPalette.HUD_GAP_TIGHT));
        addRow(createStopRow());
        add(Box.createVerticalStrut(HudPalette.HUD_GAP_TIGHT));
        navigationRow = createNavigationRow();
        navigationRow.setVisible(false);
        addRow(navigationRow);
    }

    /** Updates the compact readout from one immutable route snapshot. Must be called on the EDT. */
    void render(TradeRouteSnapshot trade) {
        boolean buying = trade.action() == TradeRouteAction.BUY;
        actionValue.setText(upper(getText(buying ? "help.trade.row.buy" : "help.trade.row.sell")));
        actionValue.setForeground(buying
                ? HudPalette.HUD_COLOR_ROLE_WARNING
                : HudPalette.HUD_COLOR_ROLE_SUCCESS);
        cargoValue.setText(upper(valueOrUnknown(trade.cargo())));
        profitValue.setText(formatCredits(trade.projectedProfit())
                + " " + getText("ai.commander.creditsSuffix"));
        systemValue.setText(upper(valueOrUnknown(trade.system())));
        stationValue.setText(upper(valueOrUnknown(trade.station())));
        stopValue.setText(upper(getText("overlay.trade.stop", trade.pointNumber(), trade.pointCount())));

        NavigationRouteSnapshot navigation = trade.navigation();
        boolean hasNavigation = navigation != null;
        navigationRow.setVisible(hasNavigation);
        if (hasNavigation) {
            nextJumpValue.setText(upper(valueOrUnknown(navigation.nextSystem())));
            jumpsValue.setText(Integer.toString(navigation.remainingJumps()));
        }
        revalidate();
        repaint();
    }

    private JPanel createSummaryRow() {
        JPanel summary = AppTheme.transparentPanel(new BorderLayout(HudPalette.HUD_GAP, 0));
        JLabel profitKey = AppTheme.hudReadoutLabel(getText("overlay.trade.profit"));
        profitKey.setHorizontalAlignment(SwingConstants.LEFT);

        summary.add(createRoutePair(actionValue, cargoValue), BorderLayout.CENTER);
        summary.add(createRoutePair(profitKey, profitValue), BorderLayout.EAST);
        return summary;
    }

    /** Builds one compact source → target pair for action/cargo and profit/value readouts. */
    private static JPanel createRoutePair(JComponent source, JComponent target) {
        JPanel pair = AppTheme.transparentPanel(null);
        pair.setLayout(new BoxLayout(pair, BoxLayout.X_AXIS));
        source.setAlignmentY(Component.CENTER_ALIGNMENT);
        target.setAlignmentY(Component.CENTER_ALIGNMENT);
        pair.add(source);
        pair.add(Box.createHorizontalStrut(HudPalette.HUD_GAP_TIGHT));
        pair.add(new RouteArrow());
        pair.add(Box.createHorizontalStrut(HudPalette.HUD_GAP_TIGHT));
        pair.add(target);
        return pair;
    }

    private JPanel createStopRow() {
        JPanel row = AppTheme.transparentPanel(new BorderLayout(HudPalette.HUD_GAP, 0));
        JPanel destination = AppTheme.transparentPanel(null);
        destination.setLayout(new BoxLayout(destination, BoxLayout.X_AXIS));
        systemValue.setAlignmentY(Component.CENTER_ALIGNMENT);
        stationValue.setAlignmentY(Component.CENTER_ALIGNMENT);
        destination.add(systemValue);
        destination.add(Box.createHorizontalStrut(HudPalette.HUD_GAP_TIGHT));
        destination.add(new RouteArrow());
        destination.add(Box.createHorizontalStrut(HudPalette.HUD_GAP_TIGHT));
        destination.add(stationValue);

        row.add(destination, BorderLayout.CENTER);
        row.add(stopValue, BorderLayout.EAST);
        return row;
    }

    private JPanel createNavigationRow() {
        JPanel row = AppTheme.transparentPanel(null);
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.add(AppTheme.hudReadoutLabel(getText("overlay.navigation.next")));
        row.add(Box.createHorizontalStrut(HudPalette.HUD_GAP_TIGHT));
        row.add(nextJumpValue);
        row.add(Box.createHorizontalStrut(HudPalette.HUD_GAP * 2));
        row.add(AppTheme.hudReadoutLabel(getText("overlay.navigation.jumps")));
        row.add(Box.createHorizontalStrut(HudPalette.HUD_GAP_TIGHT));
        row.add(jumpsValue);
        return row;
    }

    private void addRow(JComponent row) {
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(row);
    }

    private static JLabel valueLabel(int alignment, Color color, float fontSize) {
        JLabel label = AppTheme.hudReadoutValue("", color);
        label.setHorizontalAlignment(alignment);
        label.setFont(label.getFont().deriveFont(Font.BOLD, fontSize));
        return label;
    }

    private static String valueOrUnknown(String value) {
        return value == null || value.isBlank() ? getText("help.trade.value.unknown") : value;
    }

    private static String formatCredits(long value) {
        return NumberFormat.getIntegerInstance(Locale.US).format(value);
    }

    private static String upper(String value) {
        return value == null ? "" : value.toUpperCase(Locale.ROOT);
    }

    /** Directional glyph between the active system and station, rendered through the shared HUD primitive. */
    private static final class RouteArrow extends JComponent {

        private RouteArrow() {
            Dimension size = new Dimension(HudPalette.HUD_ICON_TABLE, HudPalette.HUD_ICON_TABLE);
            setOpaque(false);
            setMinimumSize(size);
            setPreferredSize(size);
            setMaximumSize(size);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            try {
                int iconSize = Math.min(HudPalette.HUD_ICON_TABLE, Math.min(getWidth(), getHeight()));
                int x = (getWidth() - iconSize) / 2;
                int y = (getHeight() - iconSize) / 2;
                HudGlyphs.paintHudArrowRight(g2, x, y, iconSize, iconSize,
                        HudPalette.HUD_COLOR_ROLE_CONTROL_DECORATION);
            } finally {
                g2.dispose();
            }
        }
    }
}
