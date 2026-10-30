package elite.intel.ui.screen.bindings;

import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.widget.HudBanner;
import elite.intel.ui.widget.HudModalSpec;
import elite.intel.ui.widget.StatusBadge;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

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
        /** Overwrite the live copy with the working copy. */
        KEEP_MINE,
        MERGE,
        /** Overwrite the working copy with the live copy. */
        TAKE_THE_GAMES,
        NOT_NOW
    }

    private Choice choice = Choice.NOT_NOW;

    private CheckWarningDialog(Component parent, String message) {
        super(SwingUtilities.getWindowAncestor(parent), ModalityType.APPLICATION_MODAL);
        setUndecorated(true);
        JPanel body = transparentPanel(new BorderLayout());
        body.add(HudBanner.multiline(message, StatusBadge.State.STANDBY), BorderLayout.CENTER);

        JButton keepMine = makeButton(getText("bindings.check.keepMine"));
        keepMine.addActionListener(e -> finish(Choice.KEEP_MINE));
        JButton merge = makeButtonSubtle(getText("bindings.check.merge"));
        merge.addActionListener(e -> finish(Choice.MERGE));
        JButton takeTheGames = makeButtonSubtle(getText("bindings.check.takeTheGames"));
        takeTheGames.addActionListener(e -> finish(Choice.TAKE_THE_GAMES));

        HudModalSpec spec = HudModalSpec.builder()
                .title(getText("bindings.check.title"))
                .onClose(() -> finish(Choice.NOT_NOW))
                .body(body)
                .primary(keepMine)
                .middle(merge)
                .extra(takeTheGames)
                .build();
        setContentPane(AppTheme.hudModalScaffold(spec));
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        getRootPane().registerKeyboardAction(e -> finish(Choice.NOT_NOW), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        // WHY: no default button, and the focus on the message rather than a button, so neither Enter nor Space chooses
        // anything - the user clicks one (Alan, 2026-10-30), as the leave prompt does.
        getRootPane().setDefaultButton(null);
        body.setFocusable(true);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                body.requestFocusInWindow();
            }
        });
        pack();
        setSize(Math.max(getWidth(), 620), getHeight());
        setLocationRelativeTo(getOwner());
    }

    /** Shows the warning and waits. On the EDT. */
    static Choice show(Component parent, String message) {
        CheckWarningDialog dialog = new CheckWarningDialog(parent, message);
        AppTheme.runWithModalScrim(dialog.getOwner(), () -> dialog.setVisible(true));
        return dialog.choice;
    }

    private void finish(Choice chosen) {
        choice = chosen;
        dispose();
    }
}
