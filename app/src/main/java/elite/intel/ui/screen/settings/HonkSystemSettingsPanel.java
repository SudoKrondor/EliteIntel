package elite.intel.ui.screen.settings;

import elite.intel.ui.widget.HudCheckBox;
import elite.intel.ui.widget.HudComboBox;


import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static elite.intel.ui.theme.AppTheme.*;
import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.theme.HudPalette.*;

public class HonkSystemSettingsPanel implements SettingRow {

    private final String checkLabel;
    private final BooleanSupplier checkGetter;
    private final Consumer<Boolean> checkSetter;

    private final List<String> fireGroupOptions;
    private final Supplier<String> fireGroupGetter;
    private final Consumer<String> fireGroupSetter;

    private final List<Integer> triggerOptions;
    private final Supplier<Integer> triggerGetter;
    private final Consumer<Integer> triggerSetter;

    public HonkSystemSettingsPanel(String checkLabel,
                                   BooleanSupplier checkGetter,
                                   Consumer<Boolean> checkSetter,

                                   List<String> fireGroupOptions,
                                   Supplier<String> fireGroupGetter,
                                   Consumer<String> fireGroupSetter,

                                   List<Integer> triggerOptions,
                                   Supplier<Integer> triggerGetter,
                                   Consumer<Integer> triggerSetter
    ) {
        this.checkLabel = checkLabel;
        this.checkGetter = checkGetter;
        this.checkSetter = checkSetter;

        this.fireGroupOptions = fireGroupOptions;
        this.fireGroupGetter = fireGroupGetter;
        this.fireGroupSetter = fireGroupSetter;

        this.triggerOptions = triggerOptions;
        this.triggerGetter = triggerGetter;
        this.triggerSetter = triggerSetter;
    }

    @Override
    public JPanel build() {
        JPanel row = transparentPanel(new FlowLayout(FlowLayout.LEFT, HUD_GAP, 4));

        boolean checked = checkGetter.getAsBoolean();
        HudCheckBox cb = new HudCheckBox(checkLabel, checked);

        HudComboBox<String> fireGroupComboBox = new HudComboBox<>(fireGroupOptions.toArray(new String[0]));
        HudComboBox<Integer> triggerGroupComboBox = new HudComboBox<>(triggerOptions.toArray(new Integer[0]));

        String currentFireGroup = fireGroupGetter.get();
        if (currentFireGroup != null) fireGroupComboBox.setSelectedItem(currentFireGroup);

        Integer currentTrigger = triggerGetter.get();
        if (currentTrigger != null) triggerGroupComboBox.setSelectedItem(currentTrigger);

        JLabel fgLabel  = hudReadoutLabel(getText("automation.fireGroup"));
        JLabel trgLabel = hudReadoutLabel(getText("automation.trigger"));

        Runnable applyEnabled = () -> {
            boolean on = cb.isSelected();
            fireGroupComboBox.setEnabled(on);
            triggerGroupComboBox.setEnabled(on);
            Color c = on ? HUD_COLOR_ROLE_SECONDARY_TEXT : HUD_COLOR_ROLE_DISABLED;
            fgLabel.setForeground(c);
            trgLabel.setForeground(c);
        };
        applyEnabled.run();

        cb.addActionListener(e -> { checkSetter.accept(cb.isSelected()); applyEnabled.run(); });

        fireGroupComboBox.addActionListener(e -> {
            if (fireGroupComboBox.isEnabled()) {
                String val = (String) fireGroupComboBox.getSelectedItem();
                if (val != null) fireGroupSetter.accept(val);
            }
        });

        triggerGroupComboBox.addActionListener(e -> {
            if (triggerGroupComboBox.isEnabled()) {
                Integer val = (Integer) triggerGroupComboBox.getSelectedItem();
                if (val != null) triggerSetter.accept(val);
            }
        });

        row.add(cb);
        row.add(fgLabel);
        row.add(fireGroupComboBox);
        row.add(trgLabel);
        row.add(triggerGroupComboBox);

        return row;
    }
}
