package elite.intel.ui.view.settings;

import elite.intel.ai.brain.actions.catalog.CommandCatalog;
import elite.intel.db.dao.TradeProfileDao;
import elite.intel.ui.view.CommandDetailsDialog;

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
import static elite.intel.ui.view.AppTheme.*;

/**
 * Builds {@link SettingRow} instances for the trade profile section of the ship settings popup.
 * Each call to {@link #buildRows} returns rows bound to the supplied {@link TradeProfileDao.TradeProfile};
 * changes are written to the profile object immediately and persisted when the popup closes.
 */
public class TradeProfileSettingsPanel {

    private static final int LABEL_WIDTH = 200;
    private static final int FIELD_WIDTH = 130;

    private TradeProfileSettingsPanel() {}

    /** Returns all trade-profile setting rows, starting with a section header. */
    public static List<SettingRow> buildRows(TradeProfileDao.TradeProfile profile) {
        List<SettingRow> rows = new ArrayList<>();
        rows.add(sectionHeader(getText("trade.profile.section")));
        rows.add(checkRow(getText("trade.profile.allowPlanetary"),    profile::isAllowPlanetary,    profile::setAllowPlanetary,
                "trade_profile_toggle_planetary_ports"));
        rows.add(checkRow(getText("trade.profile.allowProhibited"),   profile::isAllowProhibited,   profile::setAllowProhibited,
                "trade_profile_toggle_prohibited_cargo"));
        rows.add(checkRow(getText("trade.profile.allowPermit"),       profile::isAllowPermit,       profile::setAllowPermit,
                "trade_profile_toggle_permit_systems"));
        rows.add(checkRow(getText("trade.profile.allowFleetCarrier"), profile::isAllowFleetCarrier, profile::setAllowFleetCarrier,
                null));
        rows.add(checkRow(getText("trade.profile.allowStrongHold"),   profile::isAllowStrongHold,   profile::setAllowStrongHold,
                "trade_profile_toggle_strongholds"));
        rows.add(intRow(getText("trade.profile.maxLsFromArrival"), profile::getMaxDistanceLs, profile::setMaxDistanceLs,
                "trade_profile_set_max_distance"));
        rows.add(intRow(getText("trade.profile.maxJumps"),         profile::getMaxJumps,       profile::setMaxJumps,
                "trade_profile_set_max_stops"));
        rows.add(intRow(getText("trade.profile.startingCapital"),  profile::getStartingBudget, profile::setStartingBudget,
                "trade_profile_set_budget"));
        return rows;
    }

    private static SettingRow sectionHeader(String title) {
        return () -> {
            JPanel row = transparentPanel(new FlowLayout(FlowLayout.LEFT, HUD_GAP, 6));
            JLabel lbl = hudSectionLabel(title.toUpperCase());
            row.add(lbl);
            return row;
        };
    }

    private static SettingRow checkRow(String label, BooleanSupplier getter, Consumer<Boolean> setter, String commandKey) {
        return () -> {
            JPanel row = transparentPanel(new FlowLayout(FlowLayout.LEFT, HUD_GAP, 4));
            JCheckBox cb = makeCheckBox(label, getter.getAsBoolean());
            cb.addActionListener(e -> setter.accept(cb.isSelected()));
            row.add(cb);
            if (commandKey != null) {
                row.add(commandLink(commandKey));
            }
            return row;
        };
    }

    private static SettingRow intRow(String label, Supplier<Integer> getter, Consumer<Integer> setter, String commandKey) {
        return () -> {
            JPanel row = transparentPanel(new FlowLayout(FlowLayout.LEFT, HUD_GAP, 4));

            JLabel lbl = new JLabel(label);
            lbl.setForeground(FG);
            lbl.setPreferredSize(new Dimension(LABEL_WIDTH, lbl.getPreferredSize().height));

            JTextField field = makeTextField();
            field.setText(String.valueOf(getter.get()));
            field.setPreferredSize(new Dimension(FIELD_WIDTH, field.getPreferredSize().height));
            field.addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent e) {
                    commitField(field, getter, setter);
                }
            });
            field.addActionListener(e -> commitField(field, getter, setter));

            row.add(lbl);
            row.add(field);
            if (commandKey != null) {
                row.add(commandLink(commandKey));
            }
            return row;
        };
    }

    /** Returns a hyperlink-styled button that opens {@link CommandDetailsDialog} for the given command key. */
    private static JButton commandLink(String commandKey) {
        String name = getText("command." + commandKey + ".name");
        JButton link = new JButton("<html><u>" + name + "</u></html>");
        link.setContentAreaFilled(false);
        link.setBorderPainted(false);
        link.setFocusPainted(false);
        link.setOpaque(false);
        link.setForeground(BUTTON_BG);
        link.setFont(link.getFont().deriveFont(Font.ITALIC, 11f));
        link.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        link.setMargin(new Insets(0, 0, 0, 0));
        link.addActionListener(e ->
                new CommandCatalog().findById(commandKey).ifPresent(entry ->
                        new CommandDetailsDialog(link, entry).setVisible(true)
                )
        );
        return link;
    }

    private static void commitField(JTextField field, Supplier<Integer> getter, Consumer<Integer> setter) {
        try {
            setter.accept(Integer.parseInt(field.getText().trim()));
        } catch (NumberFormatException ex) {
            field.setText(String.valueOf(getter.get()));
        }
    }
}
