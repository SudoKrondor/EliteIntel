package elite.intel.ui.dialog;

import elite.intel.gameapi.bookmarks.LocationBookmark;
import elite.intel.ui.overlay.LocationBookmarkCard;
import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.theme.HudPalette;
import elite.intel.ui.widget.HudModalSpec;
import elite.intel.ui.widget.HudSection;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.Optional;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;

/**
 * Everything known about one bookmark, with its Display Name - the one thing the commander can change - in an
 * editable field.
 * <p>
 * The field opens on what the overlay shows today. Saving it unchanged, or clearing it, leaves the bookmark
 * unrenamed, so it keeps following the app language for its coordinates and VEGA keeps naming the place.
 */
public final class LocationBookmarkDialog extends JDialog {

    private final LocationBookmark bookmark;
    private final JTextField nameField = AppTheme.makeTextField();
    /**
     * The name to store: null for "not renamed". Only meaningful once {@link #saved} is set.
     */
    private String result;
    private boolean saved;

    private LocationBookmarkDialog(Component parent, LocationBookmark bookmark) {
        super(SwingUtilities.getWindowAncestor(parent), getText("bookmarks.dialog.title"), ModalityType.APPLICATION_MODAL);
        this.bookmark = bookmark;
        buildUi();
    }

    /**
     * Shows the dialog and waits for it.
     *
     * @return the commander's choice; empty when they cancelled
     */
    public static Optional<Rename> edit(Component parent, LocationBookmark bookmark) {
        LocationBookmarkDialog dialog = new LocationBookmarkDialog(parent, bookmark);
        AppTheme.runWithModalScrim(SwingUtilities.getWindowAncestor(parent), () -> dialog.setVisible(true));
        return dialog.saved ? Optional.of(new Rename(dialog.result)) : Optional.empty();
    }

    /**
     * What Save asks for.
     *
     * @param displayName the new name, or null to go back to naming the place
     */
    public record Rename(String displayName) {
    }

    private void buildUi() {
        setUndecorated(true);

        HudSection section = HudSection.flat(getText("bookmarks.dialog.section"), new BorderLayout());
        section.body().add(details(), BorderLayout.CENTER);

        JButton save = AppTheme.makeButton(getText("button.save"));
        save.addActionListener(e -> save());
        JButton cancel = AppTheme.makeButtonSubtle(getText("button.cancel"));
        cancel.addActionListener(e -> dispose());

        HudModalSpec spec = HudModalSpec.builder()
                .title(getText("bookmarks.dialog.title"))
                .onClose(this::dispose)
                .body(section)
                .primary(save)
                .dismiss(cancel)
                .build();
        setContentPane(AppTheme.hudModalScaffold(spec));

        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        getRootPane().setDefaultButton(save);
        pack();
        setMinimumSize(new Dimension(560, getPreferredSize().height));
        setLocationRelativeTo(getOwner());
    }

    private JPanel details() {
        JPanel panel = AppTheme.transparentPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.insets = new Insets(5, 0, 5, HudPalette.HUD_GAP);
        gbc.anchor = GridBagConstraints.WEST;

        nameField.setText(LocationBookmarkCard.label(bookmark));
        addRow(panel, gbc, getText("bookmarks.column.displayName"), nameField);
        addValue(panel, gbc, "bookmarks.field.kind", getText("bookmarks.kind." + bookmark.kind().name()));
        addValue(panel, gbc, "bookmarks.column.starSystem", bookmark.starSystem());
        addValue(panel, gbc, "bookmarks.field.station", bookmark.stationName());
        addValue(panel, gbc, "bookmarks.field.planet", bookmark.planetName());
        if (bookmark.hasCoordinates()) {
            addValue(panel, gbc, "bookmarks.field.latitude", LocationBookmarkCard.degrees(bookmark.latitude()));
            addValue(panel, gbc, "bookmarks.field.longitude", LocationBookmarkCard.degrees(bookmark.longitude()));
        }
        return panel;
    }

    /**
     * A read-only line; a field the bookmark's kind does not fill is left out rather than shown empty.
     */
    private static void addValue(JPanel panel, GridBagConstraints gbc, String labelKey, String value) {
        if (value == null || value.isBlank()) return;
        addRow(panel, gbc, getText(labelKey), AppTheme.hudReadoutValue(value, HudPalette.HUD_COLOR_ROLE_PRIMARY_TEXT));
    }

    private static void addRow(JPanel panel, GridBagConstraints gbc, String label, JComponent value) {
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(AppTheme.hudReadoutLabel(label), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(value, gbc);
        gbc.gridy++;
    }

    private void save() {
        String typed = LocationBookmark.storedName(nameField.getText());
        String unrenamed = LocationBookmarkCard.label(bookmark.withDisplayName(null));
        result = typed == null || typed.equals(unrenamed) ? null : typed;
        saved = true;
        dispose();
    }
}
