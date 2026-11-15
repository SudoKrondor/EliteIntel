package elite.intel.ui.screen.colorthemes;

import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.widget.HudModalSpec;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.Optional;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.theme.AppTheme.makeButton;
import static elite.intel.ui.theme.AppTheme.makeButtonSubtle;
import static elite.intel.ui.theme.AppTheme.transparentPanel;

/** Asks for a theme's name: for a new theme, a copy, or a rename. OK stays off while the name is blank. */
final class ThemeNameDialog extends JDialog {

    private static final int MIN_WIDTH = 460;

    private final JTextField nameField = AppTheme.makeTextField();
    private final JButton ok = makeButton(getText("button.ok"));
    private String answer;

    private ThemeNameDialog(Component parent, String title, String initialName) {
        super(SwingUtilities.getWindowAncestor(parent), ModalityType.APPLICATION_MODAL);
        setUndecorated(true);
        buildUi(title, initialName);
    }

    /** @return the name typed, stripped, or empty when the commander cancelled */
    static Optional<String> ask(Component parent, String title, String initialName) {
        ThemeNameDialog dialog = new ThemeNameDialog(parent, title, initialName);
        AppTheme.runWithModalScrim(dialog.getOwner(), () -> dialog.setVisible(true));
        return Optional.ofNullable(dialog.answer);
    }

    private void buildUi(String title, String initialName) {
        JPanel body = transparentPanel(new BorderLayout(0, 8));
        body.add(new JLabel(getText("themes.name.prompt")), BorderLayout.NORTH);
        nameField.setText(initialName);
        nameField.selectAll();
        nameField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { updateOk(); }
            @Override public void removeUpdate(DocumentEvent e) { updateOk(); }
            @Override public void changedUpdate(DocumentEvent e) { updateOk(); }
        });
        nameField.addActionListener(e -> accept());
        body.add(nameField, BorderLayout.CENTER);

        ok.addActionListener(e -> accept());
        JButton cancel = makeButtonSubtle(getText("button.cancel"));
        cancel.addActionListener(e -> dispose());

        setContentPane(AppTheme.hudModalScaffold(HudModalSpec.builder()
                .title(title)
                .onClose(this::dispose)
                .body(body)
                .primary(ok)
                .dismiss(cancel)
                .build()));
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        updateOk();
        pack();
        setSize(Math.max(getWidth(), MIN_WIDTH), getHeight());
        setLocationRelativeTo(getOwner());
    }

    private void accept() {
        if (!ok.isEnabled()) return;
        answer = nameField.getText().strip();
        dispose();
    }

    private void updateOk() {
        ok.setEnabled(!nameField.getText().isBlank());
    }
}
