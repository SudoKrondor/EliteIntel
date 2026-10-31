package elite.intel.ui.dialog;

import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.theme.HudPalette;
import elite.intel.ui.widget.HudModalSpec;
import elite.intel.ui.widget.HudPasswordField;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.Arrays;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;

/**
 * The password that locks the app's key into a Backup Settings and Data zip, or unlocks it on restore.
 * <p>
 * The commander can also go on without one: a backup then leaves the API keys out, and a restore leaves them
 * behind. Both say so in the dialog, so nobody is surprised to type their keys again.
 */
public final class AppDataPasswordDialog extends JDialog {

    /**
     * What the commander chose: a password, to go on without one, or to stop.
     */
    public sealed interface Choice {
        record Password(char[] password) implements Choice {
        }

        record WithoutKeys() implements Choice {
        }

        record Cancelled() implements Choice {
        }
    }

    private enum Mode {CHOOSE, ENTER}

    private final Mode mode;
    private final HudPasswordField password = new HudPasswordField();
    private final HudPasswordField repeat = new HudPasswordField();
    private final JLabel error = new JLabel(" ");
    private Choice choice = new Choice.Cancelled();

    private AppDataPasswordDialog(Component parent, Mode mode, String errorText) {
        super(SwingUtilities.getWindowAncestor(parent), ModalityType.APPLICATION_MODAL);
        this.mode = mode;
        error.setText(errorText == null ? " " : errorText);
        buildUi();
    }

    /**
     * Asks for a new password to lock the key into a backup.
     */
    public static Choice choose(Component parent) {
        return show(new AppDataPasswordDialog(parent, Mode.CHOOSE, null));
    }

    /**
     * Asks for the password a backup's key was locked with.
     *
     * @param errorText shown above the field, after a wrong password; null the first time
     */
    public static Choice enter(Component parent, String errorText) {
        return show(new AppDataPasswordDialog(parent, Mode.ENTER, errorText));
    }

    private static Choice show(AppDataPasswordDialog dialog) {
        AppTheme.runWithModalScrim(dialog.getOwner(), () -> dialog.setVisible(true));
        return dialog.choice;
    }

    private void buildUi() {
        setUndecorated(true);
        boolean choosing = mode == Mode.CHOOSE;

        JButton primary = AppTheme.makeButton(getText(choosing ? "settings.appData.password.lock" : "settings.appData.password.unlock"));
        primary.addActionListener(e -> accept());
        JButton without = AppTheme.makeButtonSubtle(getText("settings.appData.password.withoutKeys"));
        without.addActionListener(e -> finish(new Choice.WithoutKeys()));
        JButton cancel = AppTheme.makeButtonSubtle(getText("button.cancel"));
        cancel.addActionListener(e -> finish(new Choice.Cancelled()));

        HudModalSpec spec = HudModalSpec.builder()
                .title(getText(choosing ? "settings.appData.backup.title" : "settings.appData.restore.title"))
                .onClose(() -> finish(new Choice.Cancelled()))
                .body(body(choosing))
                .primary(primary)
                .extra(without)
                .dismiss(cancel)
                .build();
        setContentPane(AppTheme.hudModalScaffold(spec));

        getRootPane().registerKeyboardAction(e -> finish(new Choice.Cancelled()),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        getRootPane().setDefaultButton(primary);
        pack();
        setMinimumSize(new Dimension(560, getPreferredSize().height));
        setLocationRelativeTo(getOwner());
    }

    private JPanel body(boolean choosing) {
        JPanel panel = AppTheme.transparentPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 0, 5, HudPalette.HUD_GAP);
        panel.add(explanation(getText(choosing ? "settings.appData.password.chooseHint" : "settings.appData.password.enterHint")), gbc);

        gbc.gridy++;
        error.setForeground(HudPalette.HUD_COLOR_ROLE_DANGER);
        error.putClientProperty(AppTheme.HUD_LOCKED_FOREGROUND, Boolean.TRUE);
        panel.add(error, gbc);

        gbc.gridwidth = 1;
        addRow(panel, gbc, getText("settings.appData.password.label"), password);
        if (choosing) addRow(panel, gbc, getText("settings.appData.password.repeat"), repeat);
        return panel;
    }

    private static JTextArea explanation(String text) {
        JTextArea area = new JTextArea(text);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setEditable(false);
        area.setFocusable(false);
        area.setOpaque(false);
        area.setColumns(44);
        area.setForeground(HudPalette.HUD_COLOR_ROLE_PRIMARY_TEXT);
        area.putClientProperty(AppTheme.HUD_LOCKED_FOREGROUND, Boolean.TRUE);
        Font base = UIManager.getFont("Label.font");
        if (base != null) area.setFont(base.deriveFont(HudPalette.HUD_FONT_FIELD_VALUE));
        area.setBorder(null);
        return area;
    }

    private static void addRow(JPanel panel, GridBagConstraints gbc, String label, JComponent field) {
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(AppTheme.hudReadoutLabel(label), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(field, gbc);
    }

    private void accept() {
        char[] typed = password.getPassword();
        if (typed.length == 0) {
            error.setText(getText("settings.appData.password.empty"));
            return;
        }
        if (mode == Mode.CHOOSE && !Arrays.equals(typed, repeat.getPassword())) {
            error.setText(getText("settings.appData.password.mismatch"));
            return;
        }
        finish(new Choice.Password(typed));
    }

    private void finish(Choice chosen) {
        choice = chosen;
        dispose();
    }
}
