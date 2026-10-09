package elite.intel.ui.screen.bindings;

import elite.intel.bindforge.devicefiles.LabelMerge;
import elite.intel.bindforge.devicefiles.LabelMerge.Collision;
import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.theme.HudPalette;
import elite.intel.ui.widget.HudBanner;
import elite.intel.ui.widget.HudModalSpec;
import elite.intel.ui.widget.HudTable;
import elite.intel.ui.widget.StatusBadge;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.theme.AppTheme.makeButton;
import static elite.intel.ui.theme.AppTheme.makeButtonSubtle;
import static elite.intel.ui.theme.AppTheme.transparentPanel;

/**
 * The {@code .buttonMap} label merge for one device: only the inputs the sides label differently, one row each,
 * one click to choose (alias-designer.md, <em>{@code .buttonMap} files merge at the label</em>). Clicking a side's
 * label keeps it; the buttons below take one side's label for every input at once.
 * <p>
 * It edits a copy of the answers, returned on DONE, so cancelling changes nothing. A partial answer is returned
 * as it is - the caller says how many are left.
 */
final class LabelMergeDialog extends JDialog {

    private static final int MAX_COLUMN_WIDTH = 260;

    private final LabelMerge merge;
    private final List<String> sides;
    private final List<Collision> collisions;
    private final Map<String, String> sideNames;
    private final Map<String, String> answers;

    private DefaultTableModel model;
    private JTable table;
    private Map<String, String> result;

    /**
     * @param sideNames what each side is called, by side
     * @param answers   the answers given so far, input to label
     */
    LabelMergeDialog(Component parent, String device, LabelMerge merge, Map<String, String> sideNames,
                     Map<String, String> answers) {
        super(SwingUtilities.getWindowAncestor(parent), ModalityType.APPLICATION_MODAL);
        setUndecorated(true);
        this.merge = merge;
        this.sides = merge.sides();
        this.collisions = merge.collisions();
        this.sideNames = sideNames;
        this.answers = new LinkedHashMap<>(answers);
        buildUi(device);
    }

    /** The answers when DONE was pressed, input to label; empty when the dialog was cancelled. */
    Optional<Map<String, String>> showDialog() {
        AppTheme.runWithModalScrim(getOwner(), () -> setVisible(true));
        return Optional.ofNullable(result);
    }

    private void buildUi(String device) {
        JPanel body = transparentPanel(new BorderLayout(0, 8));
        body.add(HudBanner.multiline(getText("bindings.aliasDesigner.labelMerge.intro"), StatusBadge.State.INFO),
                BorderLayout.NORTH);
        body.add(HudTable.dataPlaneScrollPane(buildTable()), BorderLayout.CENTER);
        body.add(wholeFileButtons(), BorderLayout.SOUTH);

        JButton done = makeButton(getText("bindings.aliasDesigner.labelMerge.done"));
        done.addActionListener(e -> {
            result = Map.copyOf(answers);
            dispose();
        });
        JButton cancel = makeButtonSubtle(getText("bindings.aliasDesigner.firstSetup.cancel"));
        cancel.addActionListener(e -> dispose());

        HudModalSpec spec = HudModalSpec.builder()
                .title(getText("bindings.aliasDesigner.labelMerge.title", device))
                .onClose(this::dispose)
                .body(body)
                .scrollBody(false)
                .primary(done)
                .dismiss(cancel)
                .build();
        setContentPane(AppTheme.hudModalScaffold(spec));
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        pack();
        setSize(Math.max(getWidth(), 640), Math.max(getHeight(), 420));
        setLocationRelativeTo(getOwner());
    }

    private JTable buildTable() {
        List<Object> columns = new ArrayList<>();
        columns.add(getText("bindings.aliasDesigner.labelMerge.column.input"));
        sides.forEach(side -> columns.add(sideNames.getOrDefault(side, side)));
        columns.add(getText("bindings.aliasDesigner.labelMerge.column.keep"));
        model = new DefaultTableModel(columns.toArray(), 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        HudTable.style(table);
        // WHY: an unanswered row is the row's text colour, never a fill or a pill - the HUD canon's rule for state.
        table.setDefaultRenderer(Object.class, new AnswerRenderer());
        table.putClientProperty(AppTheme.HUD_TABLE_STYLE_LOCKED, Boolean.TRUE);
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                choose(table.rowAtPoint(e.getPoint()), table.columnAtPoint(e.getPoint()));
            }
        });
        refreshRows();
        return table;
    }

    /** A click on a side's label keeps it. Anywhere else, or a side holding no label there, does nothing. */
    private void choose(int row, int column) {
        int sideIndex = column - 1;
        if (row < 0 || row >= collisions.size() || sideIndex < 0 || sideIndex >= sides.size()) return;
        Collision collision = collisions.get(row);
        String label = collision.bySide().get(sides.get(sideIndex));
        if (label == null) return;
        answers.put(collision.input(), label);
        refreshRows();
    }

    private void refreshRows() {
        int selected = table.getSelectedRow();
        model.setRowCount(0);
        List<String> unanswered = merge.unanswered(answers);
        for (Collision collision : collisions) {
            List<Object> cells = new ArrayList<>();
            cells.add(collision.input());
            sides.forEach(side -> cells.add(collision.bySide().getOrDefault(side,
                    getText("bindings.aliasDesigner.labelMerge.none"))));
            cells.add(unanswered.contains(collision.input())
                    ? getText("bindings.aliasDesigner.firstSetup.answer.choose")
                    : answers.get(collision.input()));
            model.addRow(cells.toArray());
        }
        HudTable.fitColumnsToContent(table, MAX_COLUMN_WIDTH);
        if (selected >= 0 && selected < collisions.size()) table.setRowSelectionInterval(selected, selected);
    }

    /** The whole-file route - <em>use Steam's labels for this device</em>. */
    private JPanel wholeFileButtons() {
        JPanel panel = transparentPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        for (String side : sides) {
            JButton button = makeButtonSubtle(getText("bindings.aliasDesigner.labelMerge.useLabels",
                    sideNames.getOrDefault(side, side)));
            button.addActionListener(e -> {
                answers.putAll(merge.answersFavouring(side));
                refreshRows();
            });
            panel.add(button);
        }
        return panel;
    }

    /** Unanswered rows in the warning colour, answered ones in the success colour; selection keeps its own. */
    private final class AnswerRenderer extends HudTable.CellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                       boolean focused, int row, int column) {
            Component cell = super.getTableCellRendererComponent(table, value, selected, focused, row, column);
            if (!selected && row < collisions.size()) {
                boolean answered = !merge.unanswered(answers).contains(collisions.get(row).input());
                cell.setForeground(answered ? HudPalette.HUD_COLOR_ROLE_SUCCESS : HudPalette.HUD_COLOR_ROLE_WARNING);
            }
            return cell;
        }
    }
}
