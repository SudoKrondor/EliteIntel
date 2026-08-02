package elite.intel.ui.overlay;

import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.theme.HudForms;

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
 */
public class HudOverlaySettingsDialog extends JDialog {

    private static final int ALPHA_MIN = 0;
    private static final int ALPHA_MAX = 100;
    private static final int SCALE_MIN = 75;
    private static final int SCALE_MAX = 200;

    private final NativeHudOverlay overlay;

    private JComboBox<HudDisplayMode> displayCombo;
    private JSlider alphaSlider;
    private JSlider scaleSlider;

    public HudOverlaySettingsDialog(NativeHudOverlay overlay) {
        super((Frame) null, getText("overlay.settings.title"), false);
        this.overlay = overlay;
        setSize(420, 260);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        buildUi();
    }

    private void buildUi() {
        JPanel root = new JPanel(new GridBagLayout());
        root.setBorder(new EmptyBorder(12, 16, 12, 16));
        setContentPane(root);

        GridBagConstraints gbc = HudForms.baseGbc();

        // First, because it decides which screen the two settings below are being
        // judged against.
        displayCombo = new JComboBox<>(HudDisplayMode.values());
        displayCombo.setSelectedItem(overlay.getDisplayMode());
        displayCombo.setToolTipText(getText("overlay.settings.display.tooltip"));
        displayCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index, boolean selected, boolean focused) {
                super.getListCellRendererComponent(list, value, index, selected, focused);
                if (value instanceof HudDisplayMode mode) setText(label(mode));
                return this;
            }
        });
        // Applied immediately, like the sliders: choosing VR restarts the overlay
        // children, which is the only way to see whether it worked.
        displayCombo.addActionListener(e ->
                overlay.setDisplayMode((HudDisplayMode) displayCombo.getSelectedItem()));
        gbc.gridy++;
        HudForms.addLabel(root, getText("overlay.settings.display"), gbc);
        HudForms.addField(root, displayCombo, gbc, 1, 1.0);

        alphaSlider = new JSlider(ALPHA_MIN, ALPHA_MAX,
                (int) Math.round(overlay.getBackgroundAlpha() * 100));
        // Live preview: the commander judges this against the game behind it,
        // so the overlay updates while the slider is still being dragged.
        alphaSlider.addChangeListener(e ->
                overlay.setBackgroundAlpha(alphaSlider.getValue() / 100d));
        gbc.gridy++;
        HudForms.addLabel(root, getText("overlay.settings.background"), gbc);
        HudForms.addField(root, alphaSlider, gbc, 1, 1.0);

        scaleSlider = new JSlider(SCALE_MIN, SCALE_MAX,
                (int) Math.round(overlay.getFontScale() * 100));
        scaleSlider.addChangeListener(e -> {
            // Resizes the window, so only commit on release.
            if (!scaleSlider.getValueIsAdjusting()) {
                overlay.setFontScale(scaleSlider.getValue() / 100d);
            }
        });
        gbc.gridy++;
        HudForms.addLabel(root, getText("overlay.settings.textSize"), gbc);
        HudForms.addField(root, scaleSlider, gbc, 1, 1.0);

        JButton close = AppTheme.makeButtonSubtle(getText("button.close"));
        close.addActionListener(e -> dispose());
        gbc.gridy++;
        HudForms.addField(root, close, gbc, 1, 1.0);
    }

    private static String label(HudDisplayMode mode) {
        return switch (mode) {
            case DESKTOP -> getText("overlay.settings.display.desktop");
            case VR -> getText("overlay.settings.display.vr");
            case BOTH -> getText("overlay.settings.display.both");
        };
    }
}
