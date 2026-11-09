package elite.intel.ui.screen.bindings;

import elite.intel.ui.dialog.HudConfirmDialog;
import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.widget.HudBanner;
import elite.intel.ui.widget.HudModalSpec;
import elite.intel.ui.widget.StatusBadge;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.theme.AppTheme.makeButton;
import static elite.intel.ui.theme.AppTheme.makeButtonSubtle;
import static elite.intel.ui.theme.AppTheme.transparentPanel;

/**
 * The check's warning (storage.md rule 14; Storage S8a): what differs, and the three choices - put the working copy
 * back, merge, or take the game's files. <strong>Nothing is selected and Enter chooses nothing</strong>; Esc and the
 * close glyph are Not now, which writes nothing (Alan, 2026-10-30). Putting the working copy back is the first
 * button, as a reset asks.
 */
final class CheckWarningDialog extends JDialog {

    /** What the user chose. */
    enum Choice {
        /** Overwrite the live copy with the working copy - or, offered on its own, move the files that do not belong. */
        KEEP_MINE,
        MERGE,
        /** Overwrite the working copy with the live copy. */
        TAKE_THE_GAMES,
        NOT_NOW
    }

    /** Which choices the warning offers. */
    enum Offer {
        /** Put mine back, Merge and Use the game's files. */
        EVERY_CHOICE,
        /** Put mine back alone - when only a missing or unparseable device file differs (Storage S9a). */
        PUT_MINE_BACK,
        /**
         * Tidy up and Not now - when only files to move are found (Alan, 2026-11-07, Storage S13b Q2; S13d Q3). Tidy
         * up is {@link Choice#KEEP_MINE}: with nothing else differing, putting the working copy back only tidies up.
         */
        TIDY_UP
    }

    private Choice choice = Choice.NOT_NOW;

    private CheckWarningDialog(Component parent, String message, Offer offer) {
        super(SwingUtilities.getWindowAncestor(parent), ModalityType.APPLICATION_MODAL);
        setUndecorated(true);
        JPanel body = transparentPanel(new BorderLayout());
        body.add(HudBanner.multiline(message, StatusBadge.State.STANDBY), BorderLayout.CENTER);

        HudModalSpec.Builder builder = HudModalSpec.builder()
                .title(getText("bindings.check.title"))
                .onClose(() -> finish(Choice.NOT_NOW))
                .body(body);
        if (offer == Offer.TIDY_UP) {
            JButton tidyUp = makeButton(getText("bindings.check.tidyUp"));
            tidyUp.addActionListener(e -> finish(Choice.KEEP_MINE));
            JButton notNow = makeButtonSubtle(getText("bindings.restore.notNow"));
            notNow.addActionListener(e -> finish(Choice.NOT_NOW));
            builder.primary(tidyUp).dismiss(notNow);
        } else {
            JButton keepMine = makeButton(getText("bindings.check.keepMine"));
            keepMine.addActionListener(e -> finish(Choice.KEEP_MINE));
            builder.primary(keepMine);
            if (offer == Offer.EVERY_CHOICE) {
                JButton merge = makeButtonSubtle(getText("bindings.check.merge"));
                merge.addActionListener(e -> finish(Choice.MERGE));
                JButton takeTheGames = makeButtonSubtle(getText("bindings.check.takeTheGames"));
                takeTheGames.addActionListener(e -> finish(Choice.TAKE_THE_GAMES));
                builder.middle(merge).extra(takeTheGames);
            }
        }
        HudModalSpec spec = builder.build();
        setContentPane(AppTheme.hudModalScaffold(spec));
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        getRootPane().registerKeyboardAction(e -> finish(Choice.NOT_NOW), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        // WHY: nothing selected, so the user clicks one (Alan, 2026-10-30), as the leave prompt does.
        HudConfirmDialog.chooseByClickOnly(this, body);
        pack();
        setSize(Math.max(getWidth(), 620), getHeight());
        setLocationRelativeTo(getOwner());
    }

    /** Shows the warning and waits. On the EDT. */
    static Choice show(Component parent, String message, Offer offer) {
        CheckWarningDialog dialog = new CheckWarningDialog(parent, message, offer);
        AppTheme.runWithModalScrim(dialog.getOwner(), () -> dialog.setVisible(true));
        return dialog.choice;
    }

    private void finish(Choice chosen) {
        choice = chosen;
        dispose();
    }
}
