package elite.intel.ui.dialog;

import elite.intel.ai.hands.BindingSlotType;
import elite.intel.ai.hands.KeyBindingsParser;
import elite.intel.ui.support.BindingSlotDisplayFormatter;
import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.widget.HudBanner;
import elite.intel.ui.widget.HudModalSpec;
import elite.intel.ui.widget.HudSection;
import elite.intel.ui.widget.StatusBadge;

import javax.swing.*;
import java.awt.*;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.theme.AppTheme.*;
import static elite.intel.ui.theme.HudForms.*;
import static elite.intel.ui.theme.HudPalette.HUD_COLOR_ROLE_PRIMARY_TEXT;

/**
 * Modal that clears the <em>keyboard</em> binding of one control's Primary and/or Secondary slot
 * while leaving any Controller/HOTAS binding on the other slot untouched.
 * <p>
 * Each "Clear Primary" / "Clear Secondary" button is enabled only while that slot actually holds a
 * keyboard binding. Clearing a slot re-reads the row and, if the other slot still has a keyboard
 * binding to clear, keeps the dialog open; once nothing keyboard-bound remains, the dialog closes.
 * The actual XML edit and file reload live in the caller's {@link SlotClearer}; this dialog only
 * drives the choice and reflects the result.
 */
public class ClearKeyboardBindingDialog extends JDialog {

    /**
     * Clears one slot's keyboard binding and reports the row's state after the attempt.
     */
    public interface SlotClearer {
        ClearOutcome clearSlot(BindingSlotType slotType);
    }

    /**
     * Whether the clear succeeded, plus the row's slots as they stand after the attempt.
     */
    public record ClearOutcome(boolean success, KeyBindingsParser.ReadOnlyBindingSlots slots) {
    }

    private final BindingSlotDisplayFormatter slotFormatter = new BindingSlotDisplayFormatter();
    private final String bindingId;
    private final SlotClearer clearer;

    private final JButton clearPrimaryButton;
    private final JButton clearSecondaryButton;
    private final JLabel primaryValue;
    private final JLabel secondaryValue;

    public ClearKeyboardBindingDialog(
            Component parent,
            String bindingId,
            KeyBindingsParser.ReadOnlyBindingSlots slots,
            SlotClearer clearer
    ) {
        super(SwingUtilities.getWindowAncestor(parent),
                getText("bindings.clear.dialogTitle"), ModalityType.APPLICATION_MODAL);
        setUndecorated(true);
        this.bindingId = bindingId;
        this.clearer = clearer;
        this.clearPrimaryButton = makeButton(getText("bindings.clear.primary"));
        this.clearSecondaryButton = makeButton(getText("bindings.clear.secondary"));
        this.primaryValue = hudReadoutValue("", HUD_COLOR_ROLE_PRIMARY_TEXT);
        this.secondaryValue = hudReadoutValue("", HUD_COLOR_ROLE_PRIMARY_TEXT);
        buildUi();
        updateState(slots);
    }

    public void showDialog() {
        setVisible(true);
    }

    private void buildUi() {
        JPanel content = transparentPanel(new GridBagLayout());
        GridBagConstraints gbc = baseGbc();
        gbc.insets = new Insets(3, 6, 3, 6);

        addLabel(content, getText("bindings.assign.selectedBinding"), gbc);
        addValue(content, bindingId, gbc);

        nextRow(gbc);
        addLabel(content, getText("bindings.column.primary"), gbc);
        addValueComponent(content, primaryValue, gbc);

        nextRow(gbc);
        addLabel(content, getText("bindings.column.secondary"), gbc);
        addValueComponent(content, secondaryValue, gbc);

        nextRow(gbc);
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 6, 3, 6);
        content.add(HudBanner.multiline(getText("bindings.clear.description"), StatusBadge.State.INFO), gbc);
        gbc.gridwidth = 1;
        gbc.insets = new Insets(3, 6, 3, 6);

        HudSection section = HudSection.flat(getText("bindings.clear.section"), new BorderLayout());
        section.body().add(content, BorderLayout.CENTER);

        JButton closeButton = makeButtonSubtle(getText("bindings.clear.close"));
        closeButton.addActionListener(e -> dispose());
        clearPrimaryButton.addActionListener(e -> onClear(BindingSlotType.PRIMARY));
        clearSecondaryButton.addActionListener(e -> onClear(BindingSlotType.SECONDARY));

        HudModalSpec spec = HudModalSpec.builder()
                .title(getText("bindings.clear.dialogTitle"))
                .onClose(this::dispose)
                .body(section)
                .scrollBody(false)
                .extra(clearPrimaryButton)
                .extra(clearSecondaryButton)
                .dismiss(closeButton)
                .build();

        setContentPane(AppTheme.hudModalScaffold(spec));
        getRootPane().setDefaultButton(closeButton);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        pack();
        if (getWidth() < 520) {
            setSize(520, getHeight());
        }
        setMinimumSize(new Dimension(520, getHeight()));
        setLocationRelativeTo(getOwner());
        setResizable(false);
    }

    private void onClear(BindingSlotType slotType) {
        ClearOutcome outcome = clearer.clearSlot(slotType);
        if (!outcome.success()) {
            JOptionPane.showMessageDialog(
                    this,
                    getText("bindings.clear.failed"),
                    getText("bindings.clear.dialogTitle"),
                    JOptionPane.ERROR_MESSAGE);
        }
        updateState(outcome.slots());
        if (!clearPrimaryButton.isEnabled() && !clearSecondaryButton.isEnabled()) {
            // Nothing keyboard-bound remains to clear on this row.
            dispose();
        }
    }

    /**
     * Reflects the current row: shows each slot's value and enables its Clear button only while that
     * slot holds a keyboard binding. Does not close the dialog - {@link #onClear} owns that decision so
     * the initial build never disposes a dialog that has not been shown yet.
     */
    private void updateState(KeyBindingsParser.ReadOnlyBindingSlots slots) {
        KeyBindingsParser.ReadOnlyBindingSlot primary = slots == null ? null : slots.primary();
        KeyBindingsParser.ReadOnlyBindingSlot secondary = slots == null ? null : slots.secondary();
        primaryValue.setText(slotFormatter.formatSlot(primary));
        secondaryValue.setText(slotFormatter.formatSlot(secondary));
        clearPrimaryButton.setEnabled(isKeyboardSlot(primary));
        clearSecondaryButton.setEnabled(isKeyboardSlot(secondary));
    }

    /**
     * True when the slot carries a real keyboard key (not empty, not a non-keyboard device).
     */
    private boolean isKeyboardSlot(KeyBindingsParser.ReadOnlyBindingSlot slot) {
        return slot != null
                && "Keyboard".equals(slot.device())
                && slot.key() != null
                && !slot.key().isBlank()
                && !"Key_".equals(slot.key());
    }

    private void addValue(JPanel panel, String value, GridBagConstraints gbc) {
        addValueComponent(panel, hudReadoutValue(value, HUD_COLOR_ROLE_PRIMARY_TEXT), gbc);
    }

    private void addValueComponent(JPanel panel, JComponent value, GridBagConstraints gbc) {
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(value, gbc);
    }
}
