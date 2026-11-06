package elite.intel.ui.inputmonitor.overlay;

import elite.intel.ui.theme.HudForms;
import elite.intel.ui.widget.HudColorSwatch;
import elite.intel.ui.widget.HudSpinner;

import javax.swing.*;
import java.awt.*;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;

final class KeyboardSettingsDialog extends ReadoutSettingsDialog {

    private final KeyboardReadout readout;

    private HudColorSwatch backgroundSwatch;
    private HudColorSwatch textSwatch;
    private JSpinner widthSpinner;
    private JSpinner heightSpinner;

    KeyboardSettingsDialog(KeyboardReadout readout) {
        super(readout, getText("inputMonitor.keyboard.settings.title"));
        this.readout = readout;
        buildUi();
    }

    private void buildUi() {
        JPanel form = newForm();
        GridBagConstraints gbc = HudForms.baseGbc();

        backgroundSwatch = addColorRow(form, getText("inputMonitor.keyboard.backgroundColor"),
                getText("inputMonitor.keyboard.chooseBackgroundColor"), readout.getBackgroundColor(), gbc);

        textSwatch = addColorRow(form, getText("inputMonitor.keyboard.textColor"),
                getText("inputMonitor.keyboard.chooseTextColor"), readout.getTextColor(), gbc);

        widthSpinner = new HudSpinner(readout.getWidth(), 80, 600, 10);
        addRow(form, getText("inputMonitor.keyboard.width"), widthSpinner, gbc);

        heightSpinner = new HudSpinner(readout.getHeight(), 80, 600, 10);
        addRow(form, getText("inputMonitor.keyboard.height"), heightSpinner, gbc);

        install(form);
    }

    @Override
    void apply() {
        readout.configure(backgroundSwatch.getColor(), textSwatch.getColor());

        int w = (int) widthSpinner.getValue();
        int h = (int) heightSpinner.getValue();
        readout.setSize(w, h);

        dispose();
    }
}
