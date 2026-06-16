package elite.intel.ui.support;


import elite.intel.ai.hands.BindingSaveResult;

import javax.swing.*;
import java.awt.*;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;

public class BindingSaveResultPresenter {
    private final Component parent;

    public BindingSaveResultPresenter(Component parent) {
        this.parent = parent;
    }

    public void show(BindingSaveResult result) {
        if (result == BindingSaveResult.SAVED || result == BindingSaveResult.NO_CHANGE) {
            showInfo(messageFor(result));
            return;
        }
        showError(messageFor(result));
    }

    public void showWriteFailed() {
        showError(getText("bindings.assign.writeFailed"));
    }

    private void showInfo(String message) {
        JOptionPane.showMessageDialog(
                parent,
                message,
                getText("bindings.assign.dialogTitle"),
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(
                parent,
                message,
                getText("bindings.assign.dialogTitle"),
                JOptionPane.ERROR_MESSAGE
        );
    }

    private String messageFor(BindingSaveResult result) {
        return switch (result) {
            case SAVED -> getText("bindings.assign.success");
            case NO_CHANGE -> getText("bindings.assign.noChange");
            case STALE_FILE -> getText("bindings.assign.staleFile");
            case KEY_OCCUPIED -> getText("bindings.assign.keyOccupied");
            case UNKNOWN_KEY -> getText("bindings.assign.unknownKey");
            case BINDING_NOT_FOUND -> getText("bindings.assign.bindingNotFound");
            case UNSUPPORTED_XML -> getText("bindings.assign.unsupportedXml");
            case BACKUP_FAILED -> getText("bindings.assign.backupFailed");
            case WRITE_FAILED -> getText("bindings.assign.writeFailed");
        };
    }
}
