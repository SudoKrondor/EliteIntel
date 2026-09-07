package elite.intel.ui.overlay;

import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.theme.HudForms;
import elite.intel.ui.theme.HudPalette;
import elite.intel.ui.widget.HudComboBox;
import elite.intel.ui.widget.HudSlider;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.EnumMap;
import java.util.Map;

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

    /**
     * Colour swatch size. Wide enough to judge a tone rather than a speck, and
     * short enough that two columns of four still fit the dialog's own width.
     */
    private static final int SWATCH_WIDTH = 46;
    private static final int SWATCH_HEIGHT = 18;

    /**
     * Colour roles laid out in two columns, so eight of them cost four rows.
     */
    private static final int COLOR_COLUMNS = 2;

    private final NativeHudOverlay overlay;

    private HudComboBox<HudDisplayMode> displayCombo;
    private HudComboBox<HudVrPosition> vrPositionCombo;
    private JLabel vrPositionLabel;
    private JTextArea vrHint;
    private JTextArea captureHint;
    private HudSlider alphaSlider;
    private HudSlider scaleSlider;
    private Timer scaleCommit;
    private final Map<HudOverlayColor, Swatch> swatches = new EnumMap<>(HudOverlayColor.class);
    private JButton resetColors;

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

        // Capture mode needs the commander to do something outside this app, and
        // nothing on screen would tell them: the window simply appears on the
        // desktop and nothing arrives in the headset until a capture tool pins
        // it. Said here rather than left to the release notes.
        captureHint = wrappedNote(getText("overlay.settings.display.captureWindow.hint"));
        gbc.gridy++;
        HudForms.addSpanComponent(root, captureHint, gbc);

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

        // The colours themselves are applied live, like the sliders above, so this
        // heading carries the one control that is not: a way back out of a palette
        // the commander cannot read.
        resetColors = AppTheme.makeButtonSubtle(getText("overlay.settings.color.reset"));
        resetColors.setToolTipText(getText("overlay.settings.color.reset.tooltip"));
        resetColors.addActionListener(e -> resetColors());
        gbc.gridy++;
        HudForms.addLabel(root, getText("overlay.settings.color"), gbc);
        HudForms.addField(root, resetColors, gbc, 1, 1.0);

        gbc.gridy++;
        HudForms.addSpanComponent(root, colorGrid(), gbc);
        showResetState();

        JButton close = AppTheme.makeButtonSubtle(getText("button.close"));
        close.addActionListener(e -> dispose());
        gbc.gridy++;
        HudForms.addField(root, close, gbc, 1, 1.0);
    }

    /**
     * One swatch per colour role, in two columns.
     * <p>
     * Every role is offered, including the three conversation lanes, because the
     * point of the whole control is a commander who cannot tell two of them apart
     * - and which two that is, is not ours to guess.
     */
    private JPanel colorGrid() {
        HudOverlayColor[] roles = HudOverlayColor.values();
        JPanel grid = new JPanel(new GridLayout(
                (roles.length + COLOR_COLUMNS - 1) / COLOR_COLUMNS, COLOR_COLUMNS,
                HudPalette.HUD_GAP, HudPalette.HUD_GAP_TIGHT));
        grid.setOpaque(false);
        for (HudOverlayColor role : roles) grid.add(colorRow(role));
        return grid;
    }

    private JPanel colorRow(HudOverlayColor role) {
        Swatch swatch = new Swatch(overlay.getColor(role));
        swatch.addActionListener(e -> chooseColor(role, swatch));
        swatches.put(role, swatch);

        // The shared field-label treatment (upper case, section 5.1), so these read
        // as the same kind of label as the rows above them.
        JLabel label = AppTheme.hudReadoutLabel(getText(role.labelKey()));

        JPanel row = new JPanel(new BorderLayout(HudPalette.HUD_GAP_TIGHT, 0));
        row.setOpaque(false);
        row.add(swatch, BorderLayout.WEST);
        row.add(label, BorderLayout.CENTER);
        return row;
    }

    /**
     * Applied on OK and not on every step of the chooser: unlike transparency,
     * which is judged against the game behind it, a colour is judged against the
     * swatch in front of the commander, and repainting the card for each drag of
     * a hue slider is a write per pixel of travel.
     */
    private void chooseColor(HudOverlayColor role, Swatch swatch) {
        Color chosen = AppTheme.showColorChooser(this, getText("overlay.settings.color.choose"), swatch.getColor());
        if (chosen == null) return;
        overlay.setColor(role, chosen);
        swatch.setColor(overlay.getColor(role));
        showResetState();
    }

    /**
     * Reads every swatch back from the overlay rather than from the defaults, so
     * this row says what is actually being drawn even if a role could not be set.
     */
    private void resetColors() {
        overlay.resetColors();
        swatches.forEach((role, swatch) -> swatch.setColor(overlay.getColor(role)));
        showResetState();
    }

    /**
     * Greys the reset control out while there is nothing to undo, so it says
     * whether this palette is the shipped one - which the swatches alone cannot,
     * a commander having no way to know a colour is the default by looking at it.
     */
    private void showResetState() {
        resetColors.setEnabled(overlay.hasCustomColors());
    }

    /**
     * A block of one colour, clickable.
     * <p>
     * A button so it is reachable by keyboard and announces itself as something
     * to press, but painted rather than themed: the HUD button styling would
     * cover the very thing the control exists to show.
     */
    private static final class Swatch extends JButton {

        private Color color;

        Swatch(Color color) {
            this.color = color;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setPreferredSize(new Dimension(SWATCH_WIDTH, SWATCH_HEIGHT));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        Color getColor() {
            return color;
        }

        void setColor(Color color) {
            this.color = color;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(color);
            g2.fillRect(1, 1, getWidth() - 2, getHeight() - 2);
            // Framed, because a swatch set to the dialog's own background would
            // otherwise look like a missing control rather than a dark colour.
            g2.setColor(hasFocus()
                    ? HudPalette.HUD_COLOR_ROLE_INPUT_FOCUS
                    : HudPalette.HUD_COLOR_ROLE_FRAME_BORDER);
            g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
            g2.dispose();
        }
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
        // Placement is the capture tool's to offer in capture mode, not ours, so
        // the compass rows stay hidden and this takes their place.
        captureHint.setVisible(mode == HudDisplayMode.CAPTURE_WINDOW);
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
            case CAPTURE_WINDOW -> getText("overlay.settings.display.captureWindow");
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
