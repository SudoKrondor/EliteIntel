package elite.intel.ui.screen.settings;

import elite.intel.ui.dialog.CommandDetailsDialog;
import elite.intel.ui.widget.HudCheckBox;
import elite.intel.ui.widget.HudSection;
import elite.intel.ui.widget.HudTextField;

import elite.intel.ai.brain.actions.catalog.CommandCatalog;
import elite.intel.db.dao.TradeProfileDao;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.theme.AppTheme.*;
import static elite.intel.ui.theme.HudPalette.*;
import static elite.intel.ui.theme.HudForms.*;

/**
 * Builds {@link SettingRow} instances for the trade profile section of the ship settings popup.
 * Each call to {@link #buildRows} returns rows bound to the supplied {@link TradeProfileDao.TradeProfile};
 * changes are written to the profile object immediately and persisted when the popup closes.
 */
public class TradeProfileSettingsPanel {

    private static final int LABEL_WIDTH = 200;
    private static final int FIELD_WIDTH = 130;

    private TradeProfileSettingsPanel() {}

    /** Returns trade-profile setting rows: a single flat section with a two-column body. */
    public static List<SettingRow> buildRows(TradeProfileDao.TradeProfile profile) {
        List<SettingRow> rows = new ArrayList<>();
        rows.add(twoColumnBody(profile));
        return rows;
    }

    /**
     * Two-column layout wrapped in a FLAT HudSection: left column has int fields
     * (max LS, max jumps, starting capital), right column has checkboxes
     * (planetary, prohibited, permit, fleet carrier, stronghold).
     */
    private static SettingRow twoColumnBody(TradeProfileDao.TradeProfile profile) {
        return () -> {
            HudSection section = HudSection.flat(getText("trade.profile.section"), new GridBagLayout());
            JPanel root = section.body();
            GridBagConstraints gbc = baseGbc();
            gbc.anchor = GridBagConstraints.NORTHWEST;
            gbc.insets = new Insets(2, 6, 2, 6);

            // Left column — labels (col 0) + fields (col 1), rows 0-2
            gbc.gridy = 0;
            addLabel(root, getText("trade.profile.maxLsFromArrival"), gbc);
            gbc.gridx = 1;
            gbc.fill = GridBagConstraints.NONE;
            root.add(buildField(profile::getMaxDistanceLs, profile::setMaxDistanceLs,
                    "trade_profile_set_max_distance"), gbc);

            nextRow(gbc);
            addLabel(root, getText("trade.profile.maxJumps"), gbc);
            gbc.gridx = 1;
            gbc.fill = GridBagConstraints.NONE;
            root.add(buildField(profile::getMaxJumps, profile::setMaxJumps,
                    "trade_profile_set_max_stops"), gbc);

            nextRow(gbc);
            addLabel(root, getText("trade.profile.startingCapital"), gbc);
            gbc.gridx = 1;
            gbc.fill = GridBagConstraints.NONE;
            root.add(buildField(profile::getStartingBudget, profile::setStartingBudget,
                    "trade_profile_set_budget"), gbc);

            // Right column — checkboxes in a nested panel so their vertical rhythm is
            // independent of the taller HudTextField rows in the left column.
            HudCheckBox cbPlanetary    = buildCheck(getText("trade.profile.allowPlanetary"),    profile::isAllowPlanetary,    profile::setAllowPlanetary,    "trade_profile_toggle_planetary_ports");
            HudCheckBox cbProhibited   = buildCheck(getText("trade.profile.allowProhibited"),   profile::isAllowProhibited,   profile::setAllowProhibited,   "trade_profile_toggle_prohibited_cargo");
            HudCheckBox cbPermit       = buildCheck(getText("trade.profile.allowPermit"),       profile::isAllowPermit,       profile::setAllowPermit,       "trade_profile_toggle_permit_systems");
            HudCheckBox cbFleetCarrier = buildCheck(getText("trade.profile.allowFleetCarrier"), profile::isAllowFleetCarrier, profile::setAllowFleetCarrier, null);
            HudCheckBox cbStrongHold   = buildCheck(getText("trade.profile.allowStrongHold"),   profile::isAllowStrongHold,   profile::setAllowStrongHold,   "trade_profile_toggle_strongholds");

            JPanel checks = transparentPanel(new GridBagLayout());
            GridBagConstraints cg = baseGbc();
            cg.gridx = 0;
            cg.weightx = 1.0;
            cg.weighty = 0.0;
            cg.fill = GridBagConstraints.HORIZONTAL;
            cg.anchor = GridBagConstraints.NORTHWEST;
            cg.insets = new Insets(0, 0, 0, 0);
            cg.gridy = 0; checks.add(cbPlanetary, cg);
            cg.insets = new Insets(HUD_GAP, 0, 0, 0);
            cg.gridy = 1; checks.add(cbProhibited, cg);
            cg.gridy = 2; checks.add(cbPermit, cg);
            cg.gridy = 3; checks.add(cbFleetCarrier, cg);
            cg.gridy = 4; checks.add(cbStrongHold, cg);

            gbc.gridx = 2;
            gbc.gridy = 0;
            gbc.gridheight = GridBagConstraints.REMAINDER;
            gbc.weightx = 0.0;
            gbc.weighty = 0.0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.anchor = GridBagConstraints.NORTHWEST;
            gbc.insets = new Insets(2, HUD_GAP, 2, 6);
            root.add(checks, gbc);
            gbc.gridheight = 1;

            return section;
        };
    }

    private static HudCheckBox buildCheck(String label, BooleanSupplier getter, Consumer<Boolean> setter, String commandKey) {
        HudCheckBox cb = new HudCheckBox(label, getter.getAsBoolean());
        cb.addActionListener(e -> setter.accept(cb.isSelected()));
        if (commandKey != null) cb.setInfoAction(commandInfo(cb, commandKey));
        return cb;
    }

    private static HudTextField buildField(Supplier<Integer> getter, Consumer<Integer> setter, String commandKey) {
        HudTextField field = new HudTextField();
        field.setText(String.valueOf(getter.get()));
        field.setPreferredSize(new Dimension(FIELD_WIDTH, field.getPreferredSize().height));
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) { commitField(field, getter, setter); }
        });
        field.addActionListener(e -> commitField(field, getter, setter));
        if (commandKey != null) field.setInfoAction(commandInfo(field, commandKey));
        return field;
    }

    private static SettingRow checkRow(String label, BooleanSupplier getter, Consumer<Boolean> setter, String commandKey) {
        return () -> {
            JPanel row = transparentPanel(new FlowLayout(FlowLayout.LEFT, HUD_GAP, 4));
            row.add(buildCheck(label, getter, setter, commandKey));
            return row;
        };
    }

    private static SettingRow intRow(String label, Supplier<Integer> getter, Consumer<Integer> setter, String commandKey) {
        return () -> {
            JPanel row = transparentPanel(new FlowLayout(FlowLayout.LEFT, HUD_GAP, 4));
            JLabel lbl = hudReadoutLabel(label);
            lbl.setPreferredSize(new Dimension(LABEL_WIDTH, lbl.getPreferredSize().height));
            row.add(lbl);
            row.add(buildField(getter, setter, commandKey));
            return row;
        };
    }

    /** Returns a Runnable that opens {@link CommandDetailsDialog} for the given command, parented to {@code parentRef}. */
    private static Runnable commandInfo(Component parentRef, String commandKey) {
        return () -> new CommandCatalog().findById(commandKey).ifPresent(entry ->
                runWithModalScrim(
                        SwingUtilities.getWindowAncestor(parentRef),
                        () -> new CommandDetailsDialog(parentRef, entry).setVisible(true)));
    }

    private static void commitField(JTextField field, Supplier<Integer> getter, Consumer<Integer> setter) {
        try {
            setter.accept(Integer.parseInt(field.getText().trim()));
        } catch (NumberFormatException ex) {
            field.setText(String.valueOf(getter.get()));
        }
    }
}
