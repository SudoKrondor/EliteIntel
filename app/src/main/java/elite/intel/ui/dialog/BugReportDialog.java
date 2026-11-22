package elite.intel.ui.dialog;

import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.theme.HudPalette;
import elite.intel.ui.widget.HudModalSpec;
import elite.intel.ui.widget.HudScrollPane;
import elite.intel.ui.widget.HudTextArea;
import elite.intel.ui.widget.HudTextField;
import elite.intel.util.BrowserUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.regex.Pattern;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;

/**
 * Asks the commander what went wrong before a bug report goes to the team, and how to answer them if they want
 * an answer.
 * <p>
 * The description is required: a Support Bundle with no word on what to look for is the hardest report to act
 * on. The email is optional, because sending a report must never need an account or an address. The commander
 * can also save the bundle to a file instead, for anyone who would rather post it themselves.
 */
public final class BugReportDialog extends JDialog {

    /**
     * What the commander chose: to send the report, to save the bundle to a file, or to stop.
     */
    public sealed interface Choice {
        /** @param email blank when the commander wants no answer */
        record Send(String description, String email) implements Choice {
        }

        record SaveToFile() implements Choice {
        }

        record Cancelled() implements Choice {
        }
    }

    /**
     * Catches a typo like a missing {@code @}, nothing more: the only proof an address works is an answer reaching it.
     */
    private static final Pattern EMAIL = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");

    private final HudTextArea description = new HudTextArea(7, 44);
    private final HudTextField email = new HudTextField();
    private final JLabel error = new JLabel(" ");
    private Choice choice = new Choice.Cancelled();

    private BugReportDialog(Component parent) {
        super(SwingUtilities.getWindowAncestor(parent), ModalityType.APPLICATION_MODAL);
        buildUi();
    }

    /**
     * Shows the dialog and returns once the commander has chosen.
     */
    public static Choice show(Component parent) {
        BugReportDialog dialog = new BugReportDialog(parent);
        AppTheme.runWithModalScrim(dialog.getOwner(), () -> dialog.setVisible(true));
        return dialog.choice;
    }

    private void buildUi() {
        setUndecorated(true);

        JButton send = AppTheme.makeButton(getText("support.report.send"));
        send.addActionListener(e -> accept());
        JButton save = AppTheme.makeButtonSubtle(getText("support.report.saveToFile"));
        save.addActionListener(e -> finish(new Choice.SaveToFile()));
        JButton cancel = AppTheme.makeButtonSubtle(getText("button.cancel"));
        cancel.addActionListener(e -> finish(new Choice.Cancelled()));

        HudModalSpec spec = HudModalSpec.builder()
                .title(getText("support.report.title"))
                .onClose(() -> finish(new Choice.Cancelled()))
                .body(body())
                .primary(send)
                .extra(save)
                .dismiss(cancel)
                .build();
        setContentPane(AppTheme.hudModalScaffold(spec));

        getRootPane().registerKeyboardAction(e -> finish(new Choice.Cancelled()),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        // WHY: no default button. Enter in the description is a new line, and must not send a half-written report.
        pack();
        setMinimumSize(new Dimension(620, getPreferredSize().height));
        setLocationRelativeTo(getOwner());
    }

    private JPanel body() {
        JPanel panel = AppTheme.transparentPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 0, 5, HudPalette.HUD_GAP);

        panel.add(explanation(getText("support.report.explanation")), gbc);

        gbc.gridy++;
        panel.add(AppTheme.hudReadoutLabel(getText("support.report.description")), gbc);
        gbc.gridy++;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(new HudScrollPane(description), gbc);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridy++;
        panel.add(AppTheme.hudReadoutLabel(getText("support.report.email")), gbc);
        gbc.gridy++;
        panel.add(email, gbc);

        gbc.gridy++;
        error.setForeground(HudPalette.HUD_COLOR_ROLE_DANGER);
        error.putClientProperty(AppTheme.HUD_LOCKED_FOREGROUND, Boolean.TRUE);
        panel.add(error, gbc);

        gbc.gridy++;
        panel.add(matrixRow(), gbc);
        return panel;
    }

    /**
     * The invitation to the community's Matrix rooms, for a commander who would rather talk to the team.
     */
    private JPanel matrixRow() {
        JPanel row = AppTheme.transparentPanel(new BorderLayout(HudPalette.HUD_GAP, 0));
        row.add(explanation(getText("support.report.matrix.hint")), BorderLayout.CENTER);
        JButton open = AppTheme.makeButtonSubtle(getText("support.report.matrix.open"));
        open.addActionListener(e -> BrowserUtil.openUrl(getText("support.report.matrix.url")));
        row.add(open, BorderLayout.EAST);
        return row;
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

    private void accept() {
        String what = description.getText().strip();
        String address = email.getText().strip();
        if (what.isEmpty()) {
            error.setText(getText("support.report.descriptionRequired"));
            description.requestFocusInWindow();
            return;
        }
        if (!address.isEmpty() && !EMAIL.matcher(address).matches()) {
            error.setText(getText("support.report.emailInvalid"));
            email.requestFocusInWindow();
            return;
        }
        finish(new Choice.Send(what, address));
    }

    private void finish(Choice chosen) {
        choice = chosen;
        dispose();
    }
}
