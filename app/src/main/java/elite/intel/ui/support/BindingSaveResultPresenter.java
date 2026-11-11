package elite.intel.ui.support;


import elite.intel.bindforge.io.BindingSaveResult;
import elite.intel.ui.dialog.HudConfirmDialog;

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

    private void showInfo(String message) {
        HudConfirmDialog.info(parent, getText("bindings.assign.dialogTitle"),
                message, getText("button.close"));
    }

    private void showError(String message) {
        HudConfirmDialog.info(parent, getText("bindings.assign.dialogTitle"),
                message, getText("button.close"));
    }

    private String messageFor(BindingSaveResult result) {
        return switch (result) {
            case SAVED -> getText("bindings.assign.success");
            case NO_CHANGE -> getText("bindings.assign.noChange");
            case KEY_OCCUPIED -> getText("bindings.assign.keyOccupied");
            case UNKNOWN_KEY -> getText("bindings.assign.unknownKey");
            case BINDING_NOT_FOUND -> getText("bindings.assign.bindingNotFound");
            case UNSUPPORTED_XML -> getText("bindings.assign.unsupportedXml");
        };
    }
}
