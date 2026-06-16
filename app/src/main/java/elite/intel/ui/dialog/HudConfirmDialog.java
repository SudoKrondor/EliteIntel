package elite.intel.ui.dialog;

import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.widget.HudModalSpec;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

/**
 * Reusable HUD modal confirm dialog (§10/§10.1) — the canon replacement for raw
 * {@code JOptionPane} confirms. Shows a title, a wrapped message and 2–3 buttons
 * (primary right, optional extra left of primary, dismiss left) and reports which the user chose.
 * Built on {@link AppTheme#hudModalScaffold(HudModalSpec)}; ESC and the close glyph map to DISMISS.
 */
public final class HudConfirmDialog extends JDialog {

    /** Which button closed the dialog. */
    public enum Result { PRIMARY, EXTRA, DISMISS }

    private Result result = Result.DISMISS;

    private HudConfirmDialog(Component parent, String title, String message,
                             String primaryLabel, String extraLabel, String dismissLabel) {
        super(SwingUtilities.getWindowAncestor(parent), ModalityType.APPLICATION_MODAL);
        buildUi(title, message, primaryLabel, extraLabel, dismissLabel);
    }

    private void buildUi(String title, String message, String primaryLabel,
                         String extraLabel, String dismissLabel) {
        setUndecorated(true);

        JTextArea msg = new JTextArea(message == null ? "" : message);
        msg.setLineWrap(true);
        msg.setWrapStyleWord(true);
        msg.setEditable(false);
        msg.setFocusable(false);
        msg.setOpaque(false);
        msg.setColumns(40);
        msg.setForeground(AppTheme.FG);
        // Proportional Label font (not the monospaced JTextArea default).
        Font base = UIManager.getFont("Label.font");
        if (base == null) base = msg.getFont();
        msg.setFont(base.deriveFont(AppTheme.HUD_FONT_FIELD_VALUE));
        msg.putClientProperty(AppTheme.HUD_LOCKED_FOREGROUND, Boolean.TRUE);
        msg.setBorder(null);

        JPanel body = AppTheme.transparentPanel(new BorderLayout());
        body.add(msg, BorderLayout.CENTER);

        JButton primary = AppTheme.makeButton(primaryLabel);
        primary.addActionListener(e -> finish(Result.PRIMARY));
        JButton dismiss = AppTheme.makeButtonSubtle(dismissLabel);
        dismiss.addActionListener(e -> finish(Result.DISMISS));

        HudModalSpec.Builder b = HudModalSpec.builder()
                .title(title)
                .onClose(() -> finish(Result.DISMISS))
                .body(body)
                .primary(primary)
                .dismiss(dismiss);
        if (extraLabel != null) {
            JButton extra = AppTheme.makeButtonSubtle(extraLabel);
            extra.addActionListener(e -> finish(Result.EXTRA));
            b.extra(extra);
        }

        setContentPane(AppTheme.hudModalScaffold(b.build()));

        getRootPane().registerKeyboardAction(
                e -> finish(Result.DISMISS),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        getRootPane().setDefaultButton(primary);
        pack();
        setLocationRelativeTo(getOwner());
    }

    private void finish(Result r) {
        result = r;
        dispose();
    }

    /**
     * Shows a modal confirm with up to three buttons and returns the user's choice.
     *
     * @param extraLabel optional middle action label; pass {@code null} for a plain two-button confirm
     */
    public static Result show(Component parent, String title, String message,
                              String primaryLabel, String extraLabel, String dismissLabel) {
        HudConfirmDialog dlg = new HudConfirmDialog(parent, title, message,
                primaryLabel, extraLabel, dismissLabel);
        dlg.setVisible(true);
        return dlg.result;
    }

    /** Two-button yes/no style confirm; returns {@code true} when the primary action was chosen. */
    public static boolean confirm(Component parent, String title, String message,
                                  String primaryLabel, String dismissLabel) {
        return show(parent, title, message, primaryLabel, null, dismissLabel) == Result.PRIMARY;
    }
}
