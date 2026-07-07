package elite.intel.ui.dialog;

import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.theme.HudPalette;
import elite.intel.ui.widget.HudModalSpec;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

/**
 * Reusable HUD modal confirm dialog (section 10/section 10.1) - the canon replacement for raw
 * {@code JOptionPane} confirms and message popups. Shows a title, a wrapped message and 1-3 buttons
 * (primary right, optional extra left of primary, optional dismiss left) and reports which the user
 * chose; {@link #info} is the single-button message variant.
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
        msg.setForeground(HudPalette.HUD_COLOR_ROLE_PRIMARY_TEXT);
        // Proportional Label font (not the monospaced JTextArea default).
        Font base = UIManager.getFont("Label.font");
        if (base == null) base = msg.getFont();
        msg.setFont(base.deriveFont(HudPalette.HUD_FONT_FIELD_VALUE));
        msg.putClientProperty(AppTheme.HUD_LOCKED_FOREGROUND, Boolean.TRUE);
        msg.setBorder(null);
        // A line-wrapping JTextArea reports a single-line preferred height until it has a width, so
        // pack() would clip a long message. Fix the width to the columns, then let it report the real
        // wrapped height at that width.
        int wrapWidth = msg.getPreferredSize().width;
        msg.setSize(wrapWidth, Short.MAX_VALUE);
        msg.setPreferredSize(new Dimension(wrapWidth, msg.getPreferredSize().height));

        JPanel body = AppTheme.transparentPanel(new BorderLayout());
        body.add(msg, BorderLayout.CENTER);

        JButton primary = AppTheme.makeButton(primaryLabel);
        primary.addActionListener(e -> finish(Result.PRIMARY));

        HudModalSpec.Builder b = HudModalSpec.builder()
                .title(title)
                .onClose(() -> finish(Result.DISMISS))
                .body(body)
                .primary(primary);
        // A single-button info popup passes no dismiss label; skip the BACK button then.
        if (dismissLabel != null) {
            JButton dismiss = AppTheme.makeButtonSubtle(dismissLabel);
            dismiss.addActionListener(e -> finish(Result.DISMISS));
            b.dismiss(dismiss);
        }
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
        // Dim the parent for the modal's lifetime (canon 10 / 10.1); the scrim is removed on close.
        AppTheme.runWithModalScrim(SwingUtilities.getWindowAncestor(parent), () -> dlg.setVisible(true));
        return dlg.result;
    }

    /** Two-button yes/no style confirm; returns {@code true} when the primary action was chosen. */
    public static boolean confirm(Component parent, String title, String message,
                                  String primaryLabel, String dismissLabel) {
        return show(parent, title, message, primaryLabel, null, dismissLabel) == Result.PRIMARY;
    }

    /**
     * Shows a modal, single-button information popup (HUD section 10) - the canon replacement for
     * {@code JOptionPane.showMessageDialog}. The one button and ESC/close glyph just dismiss it.
     */
    public static void info(Component parent, String title, String message, String buttonLabel) {
        show(parent, title, message, buttonLabel, null, null);
    }
}
