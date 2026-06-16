package elite.intel.ui.screen.settings;


import javax.swing.*;
import java.awt.*;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import static elite.intel.ui.theme.HudPalette.HUD_GAP;
import static elite.intel.ui.theme.AppTheme.makeCheckBox;
import static elite.intel.ui.theme.AppTheme.transparentPanel;
import static elite.intel.ui.theme.HudPalette.*;

public class CheckboxRow implements SettingRow {

    private final String label;
    private final BooleanSupplier getter;
    private final Consumer<Boolean> setter;

    public CheckboxRow(String label, BooleanSupplier getter, Consumer<Boolean> setter) {
        this.label = label;
        this.getter = getter;
        this.setter = setter;
    }

    @Override
    public JPanel build() {
        JPanel row = transparentPanel(new FlowLayout(FlowLayout.LEFT, HUD_GAP, 4));

        JCheckBox cb = makeCheckBox(label, getter.getAsBoolean());
        cb.addActionListener(e -> setter.accept(cb.isSelected()));

        row.add(cb);
        return row;
    }
}
