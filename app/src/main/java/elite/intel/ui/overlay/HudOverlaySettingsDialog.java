package elite.intel.ui.overlay;

import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.theme.HudForms;
import elite.intel.ui.theme.HudPalette;
import elite.intel.ui.widget.HudComboBox;
import elite.intel.ui.widget.HudSlider;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;

/**
 * Settings for the HUD overlay: where it is drawn, background transparency and
 * text size.
 * <p>
 * Background alpha and font scale are separate on purpose. A single "opacity"
 * control would dim the text along with the backdrop, which is what makes a
 * low-opacity overlay unreadable over a bright planet surface.
 * <p>
 * Built from the shared HUD widgets ({@link HudComboBox}, {@link HudSlider},
 * {@link AppTheme#makeButtonSubtle}) rather than plain Swing components: this
 * dialog is opened from the overlay toggle in the main window and sits beside
 * it, so a stock combo box here reads as a different application.
 */
public class HudOverlaySettingsDialog extends JDialog {

    private static final int ALPHA_MIN = 0;
    private static final int ALPHA_MAX = 100;
    private static final int SCALE_MIN = 75;
    private static final int SCALE_MAX = 200;

    /**
     * Fixed width, because the form's own preferred width is not a usable one:
     * the shared row builder gives every field a preferred width of zero and
     * lets the layout stretch it, so packing to preferred size collapses the
     * combo to its arrow and clips the longest mode name.
     */
    private static final int DIALOG_WIDTH = 560;

    /**
     * Text width for the wrapped VR hint, inside the dialog's 16px side borders
     * and the row insets.
     */
    private static final int HINT_TEXT_WIDTH = DIALOG_WIDTH - 90;

    /**
     * Font scale resizes the overlay, so a drag is coalesced into one write once
     * the commander stops moving. {@link HudSlider} reports every step and has no
     * "still adjusting" flag to test, so the quiet period is the seam.
     */
    private static final int SCALE_COMMIT_MS = 200;

    private final NativeHudOverlay overlay;

    private HudComboBox<HudDisplayMode> displayCombo;
    private HudComboBox<HudVrPosition> vrPositionCombo;
    private JLabel vrPositionLabel;
    private JTextArea vrHint;
    private HudSlider alphaSlider;
    private HudSlider scaleSlider;
    private Timer scaleCommit;

    public HudOverlaySettingsDialog(NativeHudOverlay overlay) {
        super((Frame) null, getText("overlay.settings.title"), false);
        this.overlay = overlay;
        AppTheme.applyAppIcon(this);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        buildUi();
        showVrRows(overlay.getDisplayMode());
        fitToContent();
        setLocationRelativeTo(null);
    }

    private void buildUi() {
        JPanel root = new JPanel(new GridBagLayout());
        root.setBorder(new EmptyBorder(12, 16, 12, 16));
        setContentPane(root);

        GridBagConstraints gbc = HudForms.baseGbc();

        // First, because it decides which screen the two settings below are being
        // judged against.
        displayCombo = new HudComboBox<>(HudDisplayMode.values(), HudOverlaySettingsDialog::label);
        displayCombo.setSelectedItem(overlay.getDisplayMode());
        displayCombo.setToolTipText(getText("overlay.settings.display.tooltip"));
        // Applied immediately, like the sliders: choosing VR restarts the overlay
        // children, which is the only way to see whether it worked.
        displayCombo.addActionListener(e -> {
            HudDisplayMode mode = (HudDisplayMode) displayCombo.getSelectedItem();
            overlay.setDisplayMode(mode);
            showVrRows(mode);
            fitToContent();
        });
        gbc.gridy++;
        HudForms.addLabel(root, getText("overlay.settings.display"), gbc);
        HudForms.addField(root, displayCombo, gbc, 1, 1.0);

        // In the headset there is no window to drag, so this is the only way to
        // move the card. Applied live: the commander is wearing the headset while
        // they pick, and can see each direction land.
        vrPositionCombo = new HudComboBox<>(HudVrPosition.values(), HudOverlaySettingsDialog::label);
        vrPositionCombo.setSelectedItem(overlay.getVrPosition());
        vrPositionCombo.addActionListener(e ->
                overlay.setVrPosition((HudVrPosition) vrPositionCombo.getSelectedItem()));
        gbc.gridy++;
        vrPositionLabel = HudForms.addLabel(root, getText("overlay.settings.vr.position"), gbc);
        HudForms.addField(root, vrPositionCombo, gbc, 1, 1.0);

        // Which way is "ahead" is not the app's to set, and that is not
        // discoverable: the answer is SteamVR's seated origin, and the control
        // that moves it cannot be offered from a desktop dialog the commander
        // cannot see with the headset on. So it is said here.
        vrHint = wrappedNote(getText("overlay.settings.vr.placement"));
        gbc.gridy++;
        HudForms.addSpanComponent(root, vrHint, gbc);

        alphaSlider = new HudSlider(ALPHA_MIN, ALPHA_MAX, 1,
                (int) Math.round(overlay.getBackgroundAlpha() * 100));
        // Live preview: the commander judges this against the game behind it,
        // so the overlay updates while the slider is still being dragged.
        alphaSlider.addChangeListener(e ->
                overlay.setBackgroundAlpha(alphaSlider.getValue() / 100d));
        gbc.gridy++;
        HudForms.addLabel(root, getText("overlay.settings.background"), gbc);
        HudForms.addField(root, alphaSlider, gbc, 1, 1.0);

        scaleCommit = new Timer(SCALE_COMMIT_MS,
                e -> overlay.setFontScale(scaleSlider.getValue() / 100d));
        scaleCommit.setRepeats(false);
        scaleSlider = new HudSlider(SCALE_MIN, SCALE_MAX, 5,
                (int) Math.round(overlay.getFontScale() * 100));
        scaleSlider.addChangeListener(e -> scaleCommit.restart());
        gbc.gridy++;
        HudForms.addLabel(root, getText("overlay.settings.textSize"), gbc);
        HudForms.addField(root, scaleSlider, gbc, 1, 1.0);

        JButton close = AppTheme.makeButtonSubtle(getText("button.close"));
        close.addActionListener(e -> dispose());
        gbc.gridy++;
        HudForms.addField(root, close, gbc, 1, 1.0);
    }

    /**
     * A paragraph of explanation that wraps to the dialog, in the muted
     * field-label voice - it is an aside about the row above it, and at the
     * dialog's default font it reads as the loudest thing on screen.
     * <p>
     * A text area rather than an HTML label: a JLabel lays its HTML out at the
     * text's own preferred width and paints it clipped when the dialog is
     * narrower, and the CSS width that is supposed to prevent that is not
     * honoured here. This wraps to whatever width the row gives it, in every
     * language, which is the whole requirement.
     */
    private static JTextArea wrappedNote(String text) {
        JTextArea note = new JTextArea(text);
        note.setLineWrap(true);
        note.setWrapStyleWord(true);
        note.setEditable(false);
        note.setFocusable(false);
        note.setOpaque(false);
        note.setBorder(null);
        note.setForeground(HudPalette.HUD_COLOR_ROLE_SECONDARY_TEXT);
        note.setFont(note.getFont().deriveFont(HudPalette.HUD_FONT_FIELD_LABEL));
        // Wrap once at the width it will be given, so the height it asks the
        // layout for is the height it actually needs.
        note.setSize(HINT_TEXT_WIDTH, Short.MAX_VALUE);
        note.setPreferredSize(new Dimension(HINT_TEXT_WIDTH, note.getPreferredSize().height));
        return note;
    }

    /**
     * The placement row and its explanation only apply where the HUD is drawn in
     * a headset, and read as noise on a desktop-only overlay.
     */
    private void showVrRows(HudDisplayMode mode) {
        boolean inVr = mode == HudDisplayMode.VR || mode == HudDisplayMode.BOTH;
        vrPositionLabel.setVisible(inVr);
        vrPositionCombo.setVisible(inVr);
        vrHint.setVisible(inVr);
    }

    /**
     * Height from the content, width fixed - see DIALOG_WIDTH. Re-run whenever
     * the VR rows come or go, so the dialog is not left with a band of empty
     * space where they used to be.
     */
    private void fitToContent() {
        pack();
        setSize(DIALOG_WIDTH, getHeight());
    }

    /**
     * A pending font-scale write is committed rather than dropped: the commander
     * let go of the slider and closed the dialog, and losing the size they just
     * chose is not what that looks like.
     */
    @Override
    public void dispose() {
        if (scaleCommit != null && scaleCommit.isRunning()) {
            scaleCommit.stop();
            overlay.setFontScale(scaleSlider.getValue() / 100d);
        }
        super.dispose();
    }

    private static String label(HudDisplayMode mode) {
        return switch (mode) {
            case DESKTOP -> getText("overlay.settings.display.desktop");
            case VR -> getText("overlay.settings.display.vr");
            case BOTH -> getText("overlay.settings.display.both");
        };
    }

    private static String label(HudVrPosition position) {
        return switch (position) {
            case TOP -> getText("overlay.settings.vr.position.top");
            case TOP_RIGHT -> getText("overlay.settings.vr.position.topRight");
            case RIGHT -> getText("overlay.settings.vr.position.right");
            case BOTTOM_RIGHT -> getText("overlay.settings.vr.position.bottomRight");
            case BOTTOM -> getText("overlay.settings.vr.position.bottom");
            case BOTTOM_LEFT -> getText("overlay.settings.vr.position.bottomLeft");
            case LEFT -> getText("overlay.settings.vr.position.left");
            case TOP_LEFT -> getText("overlay.settings.vr.position.topLeft");
        };
    }
}
