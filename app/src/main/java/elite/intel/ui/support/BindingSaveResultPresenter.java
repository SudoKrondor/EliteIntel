package elite.intel.ui.support;


import elite.intel.bindforge.io.BindingSaveResult;
import elite.intel.ui.dialog.HudConfirmDialog;
import elite.intel.ui.dialog.QuietableNotice;

import java.awt.*;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;

public class BindingSaveResultPresenter {
    private final Component parent;

    public BindingSaveResultPresenter(Component parent) {
        this.parent = parent;
    }

    public void show(BindingSaveResult result) {
        QuietableNotice notice = quietableFor(result);
        if (notice != null) {
            HudConfirmDialog.infoQuietable(parent, notice, getText("bindings.assign.dialogTitle"), messageFor(result),
                    getText("button.close"));
            return;
        }
        if (result == BindingSaveResult.NO_CHANGE) {
            showInfo(messageFor(result));
            return;
        }
        showError(messageFor(result));
    }

    /**
     * The notice that quiets {@code result}'s dialog, or {@code null} when it always shows. Only <em>"Saved."</em> can
     * be quieted (Alan, 2026-11-14): it tells the user what the table already shows. <em>"No changes."</em> says the
     * assignment did nothing, and a refusal is a failure, which always shows (storage.md rule 13).
     */
    static QuietableNotice quietableFor(BindingSaveResult result) {
        return result == BindingSaveResult.SAVED ? QuietableNotice.BINDFORGE_ASSIGN_SAVED : null;
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
