package elite.intel.ui.screen.bindings;

import elite.intel.bindforge.model.BindingDisplayNames;
import elite.intel.bindforge.storage.BindingsComparison.LineItem;
import elite.intel.bindforge.storage.BindingsComparison.Side;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.theme.AppTheme.makeButton;
import static elite.intel.ui.theme.AppTheme.makeButtonSubtle;
import static elite.intel.ui.theme.AppTheme.transparentPanel;

/**
 * The check's merge (overview.md, <em>Reconciling BindForge Edits With In-Game Rebinds</em>; Storage S8a): one row per
 * line item that differs, grouped by file, the working copy's value and the game's side by side. Clicking one keeps
 * it. <strong>Nothing is pre-selected</strong>, and Merge stays dimmed until every row has an answer, with a count of
 * how many are left; Keep all mine and Keep all in-game answer every row at once. Cancel resolves nothing.
 * <p>
 * It edits a copy of the answers, returned on Merge, so cancelling changes nothing.
 */
final class CheckMergeDialog extends JDialog {

    private static final int MAX_COLUMN_WIDTH = 300;
    private static final int MINE = 2;
    private static final int IN_GAME = 3;

    private final List<LineItem> items;
    private final Map<String, Side> answers;
    private final JButton merge = makeButton(getText("bindings.check.merge.merge"));
    private final JLabel left = AppTheme.hudReadoutValue(" ", HudPalette.HUD_COLOR_ROLE_WARNING);

    private DefaultTableModel model;
    private JTable table;
    private Map<String, Side> result;

    /**
     * @param items   every row, file by file
     * @param answers answers already given - a merge opened again after the files changed keeps them
     */
    CheckMergeDialog(Component parent, List<LineItem> items, Map<String, Side> answers) {
        super(SwingUtilities.getWindowAncestor(parent), ModalityType.APPLICATION_MODAL);
        setUndecorated(true);
        this.items = List.copyOf(items);
        this.answers = new LinkedHashMap<>(answers);
        buildUi();
    }

    /** The answers when Merge was pressed, by row id; empty when the dialog was cancelled. */
    Optional<Map<String, Side>> showDialog() {
        AppTheme.runWithModalScrim(getOwner(), () -> setVisible(true));
        return Optional.ofNullable(result);
    }

    private void buildUi() {
        JPanel body = transparentPanel(new BorderLayout(0, 8));
        body.add(HudBanner.multiline(getText("bindings.check.merge.intro"), StatusBadge.State.INFO),
                BorderLayout.NORTH);
        body.add(HudTable.dataPlaneScrollPane(buildTable()), BorderLayout.CENTER);
        JPanel south = transparentPanel(new BorderLayout());
        south.add(allButtons(), BorderLayout.WEST);
        south.add(left, BorderLayout.EAST);
        body.add(south, BorderLayout.SOUTH);

        merge.addActionListener(e -> {
            result = Map.copyOf(answers);
            dispose();
        });
        JButton cancel = makeButtonSubtle(getText("button.cancel"));
        cancel.addActionListener(e -> dispose());

        HudModalSpec spec = HudModalSpec.builder()
                .title(getText("bindings.check.merge.title"))
                .onClose(this::dispose)
                .body(body)
                .scrollBody(false)
                .primary(merge)
                .dismiss(cancel)
                .build();
        setContentPane(AppTheme.hudModalScaffold(spec));
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        pack();
        setSize(Math.max(getWidth(), 820), Math.max(getHeight(), 480));
        setLocationRelativeTo(getOwner());
        refreshRows();
    }

    private JTable buildTable() {
        Object[] columns = {getText("bindings.check.merge.column.file"), getText("bindings.check.merge.column.control"),
                getText("bindings.check.merge.column.mine"), getText("bindings.check.merge.column.inGame"),
                getText("bindings.check.merge.column.keep")};
        model = new DefaultTableModel(columns, 0) {
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
        return table;
    }

    /** A click on a side's value keeps it; anywhere else does nothing. */
    private void choose(int row, int column) {
        if (row < 0 || row >= items.size()) return;
        if (column == MINE) answers.put(items.get(row).id(), Side.WORKING_COPY);
        else if (column == IN_GAME) answers.put(items.get(row).id(), Side.LIVE);
        else return;
        refreshRows();
    }

    private void refreshRows() {
        int selected = table.getSelectedRow();
        model.setRowCount(0);
        String previousFile = null;
        for (LineItem item : items) {
            // WHY: the file is named on its first row only, so the rows read as grouped by file.
            String file = item.file().equals(previousFile) ? "" : item.file();
            previousFile = item.file();
            Side answer = answers.get(item.id());
            String keep = answer == null ? getText("bindings.check.merge.choose")
                    : answer == Side.WORKING_COPY ? getText("bindings.check.merge.column.mine")
                    : getText("bindings.check.merge.column.inGame");
            model.addRow(new Object[]{file, controlOf(item), value(item.workingCopy()), value(item.live()), keep});
        }
        HudTable.fitColumnsToContent(table, MAX_COLUMN_WIDTH);
        if (selected >= 0 && selected < items.size()) table.setRowSelectionInterval(selected, selected);
        int unanswered = unanswered();
        merge.setEnabled(unanswered == 0);
        left.setText(unanswered == 0 ? " " : getText("bindings.check.merge.left", unanswered));
    }

    private int unanswered() {
        return (int) items.stream().filter(item -> !answers.containsKey(item.id())).count();
    }

    /** <em>Ship › Flight Rotation / Yaw Left › Primary</em>, a preset line's section, or what the row is. */
    static String controlOf(LineItem item) {
        return switch (item.kind()) {
            case FILE -> getText("bindings.check.row.file");
            case DETAILS -> getText("bindings.check.row.details");
            case PRESET_LINE -> getText(item.section().getLabelKey());
            case ELEMENT -> controlName(item.element());
            case SLOT -> controlName(item.element()) + " › " + partName(item.part());
        };
    }

    private static String controlName(String element) {
        BindingDisplayNames.ControlName name = BindingDisplayNames.lookup(element);
        return getText(name.section().getLabelKey()) + " › " + name.label();
    }

    private static String partName(String part) {
        return switch (part) {
            case "Primary" -> getText("bindings.check.part.primary");
            case "Secondary" -> getText("bindings.check.part.secondary");
            case "ToggleOn" -> getText("bindings.check.part.toggle");
            default -> part;
        };
    }

    /** A side's value: <em>(not there)</em> when it holds nothing, <em>(unbound)</em> when it is empty. */
    static String value(String value) {
        if (value == null) return getText("bindings.check.value.none");
        if (value.isEmpty()) return getText("bindings.check.value.unbound");
        return value;
    }

    private JPanel allButtons() {
        JPanel panel = transparentPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        JButton mine = makeButtonSubtle(getText("bindings.check.merge.keepAllMine"));
        mine.addActionListener(e -> answerAll(Side.WORKING_COPY));
        JButton inGame = makeButtonSubtle(getText("bindings.check.merge.keepAllInGame"));
        inGame.addActionListener(e -> answerAll(Side.LIVE));
        panel.add(mine);
        panel.add(inGame);
        return panel;
    }

    private void answerAll(Side side) {
        items.forEach(item -> answers.put(item.id(), side));
        refreshRows();
    }

    /** Unanswered rows in the warning colour, answered ones in the success colour; selection keeps its own. */
    private final class AnswerRenderer extends HudTable.CellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                       boolean focused, int row, int column) {
            Component cell = super.getTableCellRendererComponent(table, value, selected, focused, row, column);
            if (!selected && row < items.size()) {
                boolean answered = answers.containsKey(items.get(row).id());
                cell.setForeground(answered ? HudPalette.HUD_COLOR_ROLE_SUCCESS : HudPalette.HUD_COLOR_ROLE_WARNING);
            }
            return cell;
        }
    }
}
