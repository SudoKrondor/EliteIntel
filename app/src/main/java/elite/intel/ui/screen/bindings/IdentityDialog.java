package elite.intel.ui.screen.bindings;

import elite.intel.bindforge.devices.IdentityQuestions.Answer;
import elite.intel.bindforge.devices.IdentityQuestions.Question;
import elite.intel.bindforge.devices.MyDevice;
import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.theme.HudPalette;
import elite.intel.ui.widget.HudModalSpec;
import elite.intel.ui.widget.HudTable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.Objects;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.theme.AppTheme.*;

/**
 * The identity question for one missing controller: which attached controller is it (spec, alias-designer.md, <em>Ask
 * about one missing device at a time</em>; Alan, 2026-10-15, A12).
 * <p>
 * It gives evidence, not a verdict: the name and VID/PID that are missing, how many bindings use it, and every unnamed
 * controller attached, marked where the vendor is the same. One row per candidate and a last row, <strong>None of
 * these</strong>. <strong>OK</strong> takes the selected row; <strong>Not now</strong>, the close glyph and Esc
 * defer - the question comes back when the hardware changes, or from CHECK AGAIN. Nothing is written here.
 */
final class IdentityDialog extends JDialog {

    /** The body's width - the lines above the list wrap to it. */
    private static final int BODY_WIDTH = 560;

    /**
     * What the user answered: a controller picked, or one of the two remembered answers.
     *
     * @param picked the controller it is, or {@code null} when {@code answer} says otherwise
     */
    record Choice(MyDevice picked, Answer answer) {
        Choice {
            if ((picked == null) == (answer == null)) {
                throw new IllegalArgumentException("A choice is a controller or an answer, never both or neither");
            }
        }
    }

    private final Question question;
    private Choice choice;

    private JTable table;
    private JButton ok;

    IdentityDialog(Component parent, Question question) {
        super(SwingUtilities.getWindowAncestor(parent), ModalityType.APPLICATION_MODAL);
        setUndecorated(true);
        this.question = Objects.requireNonNull(question, "question");
        this.choice = new Choice(null, Answer.NOT_NOW);
        buildUi();
        pack();
        setLocationRelativeTo(getOwner());
    }

    /** Shows the question and returns the answer - Not now when the user left it without one. */
    Choice ask() {
        AppTheme.runWithModalScrim(getOwner(), () -> setVisible(true));
        return choice;
    }

    private void buildUi() {
        JPanel body = transparentPanel(new BorderLayout(0, HudPalette.HUD_GAP));
        JPanel lines = transparentPanel(new GridLayout(0, 1, 0, HudPalette.HUD_GAP / 2));
        lines.add(paragraph(getText("bindings.aliasDesigner.identity.missing", question.deviceName(),
                question.was().vid(), question.was().pid()), HudPalette.HUD_COLOR_ROLE_PRIMARY_TEXT));
        lines.add(paragraph(getText("bindings.aliasDesigner.identity.bindings", question.bindings()),
                HudPalette.HUD_COLOR_ROLE_WARNING));
        lines.add(paragraph(getText("bindings.aliasDesigner.identity.which"), HudPalette.HUD_COLOR_ROLE_PRIMARY_TEXT));
        body.add(lines, BorderLayout.NORTH);
        body.add(HudTable.dataPlaneScrollPane(buildTable()), BorderLayout.CENTER);

        ok = makeButton(getText("bindings.aliasDesigner.identity.ok"));
        ok.setEnabled(false);
        ok.addActionListener(e -> onOk());
        JButton notNow = makeButtonSubtle(getText("bindings.aliasDesigner.identity.notNow"));
        notNow.addActionListener(e -> dispose());

        HudModalSpec spec = HudModalSpec.builder()
                .title(getText("bindings.aliasDesigner.identity.title"))
                .onClose(this::dispose)
                .body(body)
                .scrollBody(false)
                .dismiss(notNow)
                .primary(ok)
                .build();
        setContentPane(AppTheme.hudModalScaffold(spec));
        getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    }

    /** One row per candidate, then None of these. Nothing is selected until the user picks. */
    private JTable buildTable() {
        DefaultTableModel model = new DefaultTableModel(new Object[]{
                getText("bindings.aliasDesigner.column.device"),
                getText("bindings.aliasDesigner.column.vidPid"),
                getText("bindings.aliasDesigner.identity.column.evidence")}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        for (MyDevice candidate : question.candidates()) {
            model.addRow(new Object[]{candidate.label(), candidate.vid() + ":" + candidate.pid(),
                    question.sameVendor(candidate) ? getText("bindings.aliasDesigner.identity.sameVendor") : ""});
        }
        model.addRow(new Object[]{getText("bindings.aliasDesigner.identity.noneOfThese"), "", ""});
        table = new JTable(model);
        HudTable.style(table);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(e -> ok.setEnabled(table.getSelectedRow() >= 0));
        HudTable.fitColumnsToContent(table, BODY_WIDTH);
        table.setPreferredScrollableViewportSize(new Dimension(BODY_WIDTH,
                table.getRowHeight() * Math.min(model.getRowCount(), 6)));
        return table;
    }

    private void onOk() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        choice = row < question.candidates().size()
                ? new Choice(question.candidates().get(row), null)
                : new Choice(null, Answer.NONE_OF_THESE);
        dispose();
    }

    /** Text that wraps to the body's width, as {@code OnboardingDialog}'s does. */
    private static JTextArea paragraph(String text, Color color) {
        JTextArea area = new JTextArea(text);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setEditable(false);
        area.setFocusable(false);
        area.setOpaque(false);
        area.setBorder(null);
        area.setForeground(color);
        area.setFont(UIManager.getFont("Label.font").deriveFont(HudPalette.HUD_FONT_READOUT_VALUE));
        area.putClientProperty(AppTheme.HUD_LOCKED_FOREGROUND, Boolean.TRUE);
        area.setSize(BODY_WIDTH, Short.MAX_VALUE);
        area.setPreferredSize(new Dimension(BODY_WIDTH, area.getPreferredSize().height));
        return area;
    }
}
