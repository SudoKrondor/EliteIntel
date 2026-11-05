package elite.intel.ui.inputmonitor.overlay;

import elite.intel.ui.dialog.HudColorChooserDialog;
import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.theme.HudForms;
import elite.intel.ui.widget.HudColorSwatch;
import elite.intel.ui.widget.HudModalSpec;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;

/**
 * A readout's settings form on the standard modal scaffold, with Save and Cancel in its footer.
 * <p>
 * WHY no owner: a readout floats over the game with the main window often minimized, and a dialog owned by a
 * minimized window goes with it, leaving a modal nobody can see. Always on top for the same reason a readout is,
 * or it would open behind the readout it configures.
 */
abstract class ReadoutSettingsDialog extends JDialog {

    private static final int MIN_WIDTH = 440;
    private static final int LABEL_WIDTH = 160;

    private final ReadoutWindow readout;

    ReadoutSettingsDialog(ReadoutWindow readout, String title) {
        super((Window) null, title, ModalityType.APPLICATION_MODAL);
        this.readout = readout;
        setUndecorated(true);
        setAlwaysOnTop(true);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    }

    /**
     * Writes the form back to the readout and closes.
     */
    abstract void apply();

    /**
     * Shows the dialog beside its readout, with the main window dimmed behind it.
     */
    final void open() {
        pack();
        setMinimumSize(new Dimension(MIN_WIDTH, getHeight()));
        setLocationRelativeTo(readout);
        AppTheme.runWithModalScrim(mainWindow(), () -> setVisible(true));
    }

    /**
     * Puts {@code form} on the scaffold. Call once the form is built.
     */
    final void install(JPanel form) {
        JButton save = AppTheme.makeButton(getText("button.save"));
        save.addActionListener(e -> apply());
        JButton cancel = AppTheme.makeButtonSubtle(getText("button.cancel"));
        cancel.addActionListener(e -> dispose());

        HudModalSpec spec = HudModalSpec.builder()
                .title(getTitle())
                .onClose(this::dispose)
                .body(form)
                .primary(save)
                .dismiss(cancel)
                .build();
        setContentPane(AppTheme.hudModalScaffold(spec));

        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        getRootPane().setDefaultButton(save);
    }

    static JPanel newForm() {
        return AppTheme.transparentPanel(new GridBagLayout());
    }

    static void addRow(JPanel form, String label, JComponent field, GridBagConstraints gbc) {
        addRow(form, label, field, GridBagConstraints.HORIZONTAL, gbc);
    }

    /**
     * A row whose field is a colour swatch opening the chooser. The swatch holds the colour picked so far.
     */
    final HudColorSwatch addColorRow(JPanel form, String label, String chooserTitle, Color initial,
                                     GridBagConstraints gbc) {
        HudColorSwatch swatch = new HudColorSwatch(initial);
        swatch.addActionListener(e -> {
            Color chosen = HudColorChooserDialog.choose(this, chooserTitle, swatch.getColor());
            if (chosen != null) swatch.setColor(chosen);
        });
        // Not stretched: a colour reads as a block, not as a bar across the form.
        addRow(form, label, swatch, GridBagConstraints.NONE, gbc);
        return swatch;
    }

    private static void addRow(JPanel form, String label, JComponent field, int fill, GridBagConstraints gbc) {
        gbc.gridy++;
        HudForms.addLabel(form, label, gbc, LABEL_WIDTH);
        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.fill = fill;
        form.add(field, gbc);
    }

    private static Window mainWindow() {
        for (Frame frame : Frame.getFrames()) {
            if (frame.isVisible()) return frame;
        }
        return null;
    }
}
