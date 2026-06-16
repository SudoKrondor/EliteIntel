package elite.intel.ui.widget;

import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.theme.HudPalette;

import javax.swing.*;
import java.awt.*;

/**
 * Compact HUD toolbar row for screen-level filtering with a reusable {@link HudSearchField}.
 */
public class HudSearchToolbar extends JPanel {

    private static final int DEFAULT_SEARCH_WIDTH = 360;

    private final HudSearchField searchField;

    /**
     * Creates a compact search/filter row with an optional muted inline label.
     *
     * @param label small label shown before the search field, or blank for no label
     * @param placeholder localized placeholder shown by the search field
     * @param clearTooltip localized tooltip for the embedded clear button
     */
    public HudSearchToolbar(String label, String placeholder, String clearTooltip) {
        this(label, placeholder, clearTooltip, HudSearchField.Variant.STANDARD, false, false);
    }

    /**
     * Creates a full-width table filter bar that visually belongs to a data table.
     *
     * @param placeholder localized placeholder shown by the search field
     * @param clearTooltip localized tooltip for the embedded clear button
     */
    public static HudSearchToolbar tableFilter(String placeholder, String clearTooltip) {
        return new HudSearchToolbar("", placeholder, clearTooltip, HudSearchField.Variant.TABLE_FILTER, true, false);
    }

    /**
     * Creates a full-width HUD filter bar that connects directly to the data table below it.
     * The filter bar has no bottom gap and uses {@link HudSearchField.Variant#TABLE_FILTER_CONNECTED}
     * so that side borders align with a {@code hudConnectedScrollPaneBorder()} table beneath it.
     *
     * @param placeholder  localized placeholder shown by the search field
     * @param clearTooltip localized tooltip for the embedded clear button
     */
    public static HudSearchToolbar connectedTableFilter(String placeholder, String clearTooltip) {
        return new HudSearchToolbar("", placeholder, clearTooltip, HudSearchField.Variant.TABLE_FILTER_CONNECTED, true, true);
    }

    private HudSearchToolbar(
            String label,
            String placeholder,
            String clearTooltip,
            HudSearchField.Variant variant,
            boolean fullWidth,
            boolean connected
    ) {
        super();
        setOpaque(false);
        setLayout(fullWidth ? new BorderLayout() : new BoxLayout(this, BoxLayout.X_AXIS));
        int bottomPad = connected ? 0 : (fullWidth ? HudPalette.HUD_PADDING_SMALL : HudPalette.HUD_GAP);
        setBorder(BorderFactory.createEmptyBorder(0, 0, bottomPad, 0));

        if (label != null && !label.isBlank()) {
            JLabel inlineLabel = new JLabel(label.toUpperCase());
            inlineLabel.setForeground(HudPalette.FG_MUTED);
            inlineLabel.setFont(inlineLabel.getFont().deriveFont(Font.BOLD, HudPalette.HUD_FONT_SM));
            add(inlineLabel);
            add(Box.createRigidArea(new Dimension(HudPalette.HUD_GAP, 0)));
        }

        searchField = new HudSearchField(placeholder, clearTooltip, variant);
        int height = fullWidth ? HudPalette.HUD_BUTTON_HEIGHT_COMPACT : HudPalette.HUD_FIELD_HEIGHT;
        searchField.setPreferredSize(new Dimension(DEFAULT_SEARCH_WIDTH, height));
        if (fullWidth) {
            add(searchField, BorderLayout.CENTER);
        } else {
            searchField.setMaximumSize(new Dimension(DEFAULT_SEARCH_WIDTH, height));
            add(searchField);
            add(Box.createHorizontalGlue());
        }
    }

    /**
     * Returns the text field so screens can attach filtering listeners without owning toolbar layout.
     */
    public JTextField textField() {
        return searchField.textField();
    }
}
