package elite.intel.ui.support;

import elite.intel.ai.hands.BindingsApplyException;
import elite.intel.ai.mouth.subscribers.events.AiVoxResponseEvent;
import elite.intel.eventbus.GameEventBus;
import elite.intel.util.StringUtls;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;

/**
 * Shared success/error feedback for {@code BindingsApplyService.apply()} - used by both a
 * normal Apply (from the Binding Profile tab) and a restore-to-live (from Binding Management),
 * since both end at the same safe-apply pipeline and should give identical feedback.
 */
public class BindingApplyResultPresenter {
    private final Component parent;

    public BindingApplyResultPresenter(Component parent) {
        this.parent = parent;
    }

    /**
     * Shows the apply-succeeded dialog and speaks the reload reminder - Elite only re-reads the
     * .binds file when its Controls screen is opened, so users need the nudge to actually load
     * what was just written to the game file.
     */
    public void showSuccess(Path backupPath) {
        String successMsg = (backupPath != null
                ? getText("bindings.apply.success", backupPath.getFileName())
                : getText("bindings.apply.success.noBackup"))
                + System.lineSeparator() + System.lineSeparator()
                + getText("bindings.apply.reloadReminder");
        // Also speak the reload reminder — users reflexively dismiss dialogs without reading.
        GameEventBus.publish(new AiVoxResponseEvent(StringUtls.localizedSpeech("speech.bindingsAppliedReload")));
        JOptionPane.showMessageDialog(
                parent,
                successMsg,
                getText("bindings.apply.dialogTitle"),
                JOptionPane.INFORMATION_MESSAGE);
    }

    public void showError(BindingsApplyException e) {
        String errorMessage = e.localizationKey() == null ? e.getMessage() : getText(e.localizationKey());
        JOptionPane.showMessageDialog(
                parent,
                getText("bindings.apply.error", errorMessage),
                getText("bindings.apply.dialogTitle"),
                JOptionPane.ERROR_MESSAGE);
    }
}
