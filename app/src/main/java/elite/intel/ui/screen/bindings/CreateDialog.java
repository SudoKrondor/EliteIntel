package elite.intel.ui.screen.bindings;

import elite.intel.bindforge.devices.DeviceNames;
import elite.intel.bindforge.storage.Create;
import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.theme.HudPalette;
import elite.intel.ui.widget.HudComboBox;
import elite.intel.ui.widget.HudModalSpec;
import elite.intel.ui.widget.HudTextField;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.Optional;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.theme.AppTheme.makeButton;
import static elite.intel.ui.theme.AppTheme.makeButtonSubtle;
import static elite.intel.ui.theme.AppTheme.transparentPanel;

/**
 * CREATE's two questions (overview.md, <em>Create</em>; Storage S11): which of Frontier's factory binds files to start
 * from - {@value Create#FIRST_FACTORY} first and selected - and a name, pre-filled {@value Create#DEFAULT_NAME}, held to
 * the controller name's rule (bind-editor.md, <em>Shell</em>) and refused while it is too close to a binds file's.
 * Writes nothing: the answer becomes an edit not applied.
 */
final class CreateDialog extends JDialog {

    /** What the user chose. */
    record Answer(Create.Factory factory, String name) {
    }

    private final Create.Choices choices;
    private HudComboBox<Create.Factory> factory;
    private HudTextField name;
    private JLabel problem;
    private JButton create;
    private Answer answer;

    CreateDialog(Component parent, Create.Choices choices) {
        super(SwingUtilities.getWindowAncestor(parent), ModalityType.APPLICATION_MODAL);
        setUndecorated(true);
        this.choices = choices;
        buildUi();
        judge();
    }

    /** @return the answer, or empty when the user cancelled */
    Optional<Answer> showDialog() {
        AppTheme.runWithModalScrim(getOwner(), () -> setVisible(true));
        return Optional.ofNullable(answer);
    }

    private void buildUi() {
        JPanel body = transparentPanel(new GridBagLayout());
        int row = 0;
        JLabel intro = AppTheme.hudReadoutValue(getText("bindings.create.intro"),
                HudPalette.HUD_COLOR_ROLE_PRIMARY_TEXT);
        body.add(intro, span(row++));

        factory = new HudComboBox<>(choices.factories().toArray(Create.Factory[]::new), Create.Factory::bindName);
        factory.setSelectedIndex(0);
        row = field(body, row, "bindings.create.field.factory", factory);

        name = new HudTextField();
        name.setText(Create.DEFAULT_NAME);
        name.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                judge();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                judge();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                judge();
            }
        });
        row = field(body, row, "bindings.create.field.name", name);
        problem = AppTheme.hudReadoutValue(" ", HudPalette.HUD_COLOR_ROLE_WARNING);
        body.add(problem, span(row));

        create = makeButton(getText("bindings.create.button"));
        create.addActionListener(e -> {
            answer = new Answer((Create.Factory) factory.getSelectedItem(), typed());
            dispose();
        });
        JButton cancel = makeButtonSubtle(getText("button.cancel"));
        cancel.addActionListener(e -> dispose());

        HudModalSpec spec = HudModalSpec.builder()
                .title(getText("bindings.create.title"))
                .onClose(this::dispose)
                .body(body)
                .scrollBody(false)
                .primary(create)
                .dismiss(cancel)
                .build();
        setContentPane(AppTheme.hudModalScaffold(spec));
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        pack();
        setSize(Math.max(getWidth(), 560), getHeight());
        setLocationRelativeTo(getOwner());
    }

    private String typed() {
        return name.getText().strip();
    }

    /** CREATE is offered only for a name the rule accepts; the reason shows below the box otherwise. */
    private void judge() {
        DeviceNames.Verdict verdict = DeviceNames.validate(typed(), choices.taken());
        problem.setText(verdict.valid() ? " " : OnboardingDialog.problemText(verdict));
        create.setEnabled(verdict.valid());
    }

    private static int field(JPanel body, int row, String labelKey, JComponent control) {
        body.add(AppTheme.hudReadoutLabel(getText(labelKey)), cell(0, row));
        GridBagConstraints c = cell(1, row);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.insets.right = 0;
        body.add(control, c);
        return row + 1;
    }

    private static GridBagConstraints span(int row) {
        GridBagConstraints c = cell(0, row);
        c.gridwidth = 2;
        c.insets.right = 0;
        return c;
    }

    private static GridBagConstraints cell(int x, int y) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = x;
        c.gridy = y;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(HudPalette.HUD_GAP / 2, 0, HudPalette.HUD_GAP / 2, HudPalette.HUD_GAP);
        return c;
    }
}
