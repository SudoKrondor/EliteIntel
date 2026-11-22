package elite.intel.ui.screen.bindings;

import elite.intel.ui.dialog.HudConfirmDialog;
import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.theme.HudPalette;
import elite.intel.ui.widget.HudModalSpec;
import elite.intel.ui.widget.HudTable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.List;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.theme.AppTheme.makeButton;
import static elite.intel.ui.theme.AppTheme.makeButtonSubtle;
import static elite.intel.ui.theme.AppTheme.transparentPanel;

/**
 * The check's warning (storage.md rule 14; Storage S8a): what differs, and the three choices - put the working copy
 * back, merge, or take the game's files. <strong>Nothing is selected and Enter chooses nothing</strong>; Esc and the
 * close glyph are Not now, which writes nothing (Alan, 2026-10-30). Putting the working copy back is the first
 * button, as a reset asks.
 * <p>
 * What differs is a grid, one row per file or installation, with what was found and what happens to it; each long
 * explanation is said once below it (Alan, 2026-11-10, rule 14's <em>One warning for the whole set</em>).
 */
final class CheckWarningDialog extends JDialog {

    /** What the user chose. */
    enum Choice {
        /** Overwrite the live copy with the working copy - or, offered on its own, move the files that do not belong. */
        KEEP_MINE,
        MERGE,
        /** Overwrite the working copy with the live copy. */
        TAKE_THE_GAMES,
        NOT_NOW
    }

    /** Which choices the warning offers. */
    enum Offer {
        /** Put mine back, Merge and Use the game's files. */
        EVERY_CHOICE,
        /** Put mine back alone - when only a missing or unparseable device file differs (Storage S9a). */
        PUT_MINE_BACK,
        /**
         * Tidy up and Not now - when only files to move are found (Alan, 2026-11-07, Storage S13b Q2; S13d Q3). Tidy
         * up is {@link Choice#KEEP_MINE}: with nothing else differing, putting the working copy back only tidies up.
         */
        TIDY_UP
    }

    /** A row's text colour - the HUD canon: state is text colour, never a pill. */
    enum Tone { NORMAL, WARNING, DANGER }

    /** One row of the grid: the file or installation, what was found, and what happens to it. */
    record Row(String file, String found, String fate, Tone tone) {
    }

    /** What the warning says: the line above the grid, its rows, and the paragraphs below it. */
    record Warning(String intro, List<Row> rows, String after) {
        Warning {
            rows = List.copyOf(rows);
        }
    }

    /** The grid stops growing at this many rows and scrolls. */
    private static final int VISIBLE_ROWS = 8;
    /** The narrowest the text may wrap at - the dialog's old width less its padding. */
    private static final int MIN_WIDTH = 580;
    /** The widest the grid may make the dialog. */
    private static final int MAX_WIDTH = 900;
    /** The widest one column may become; a longer cell ends in an ellipsis. Every cell shows whole as its tooltip. */
    private static final int MAX_COLUMN = 380;
    private static final int GAP = 10;

    private Choice choice = Choice.NOT_NOW;

    private CheckWarningDialog(Component parent, Warning warning, Offer offer) {
        super(SwingUtilities.getWindowAncestor(parent), ModalityType.APPLICATION_MODAL);
        setUndecorated(true);
        JTable grid = grid(warning.rows());
        int width = Math.clamp(grid.getPreferredSize().width, MIN_WIDTH, MAX_WIDTH);
        JPanel body = transparentPanel(new BorderLayout(0, GAP));
        body.add(text(warning.intro(), HudPalette.HUD_COLOR_ROLE_WARNING, true, width), BorderLayout.NORTH);
        if (!warning.rows().isEmpty()) body.add(scrolling(grid, width), BorderLayout.CENTER);
        if (!warning.after().isEmpty()) {
            body.add(text(warning.after(), HudPalette.HUD_COLOR_ROLE_PRIMARY_TEXT, false, width),
                    BorderLayout.SOUTH);
        }

        HudModalSpec.Builder builder = HudModalSpec.builder()
                .title(getText("bindings.check.title"))
                .onClose(() -> finish(Choice.NOT_NOW))
                .body(body);
        if (offer == Offer.TIDY_UP) {
            JButton tidyUp = makeButton(getText("bindings.check.tidyUp"));
            tidyUp.addActionListener(e -> finish(Choice.KEEP_MINE));
            JButton notNow = makeButtonSubtle(getText("bindings.restore.notNow"));
            notNow.addActionListener(e -> finish(Choice.NOT_NOW));
            builder.primary(tidyUp).dismiss(notNow);
        } else {
            JButton keepMine = makeButton(getText("bindings.check.keepMine"));
            keepMine.addActionListener(e -> finish(Choice.KEEP_MINE));
            builder.primary(keepMine);
            if (offer == Offer.EVERY_CHOICE) {
                JButton merge = makeButtonSubtle(getText("bindings.check.merge"));
                merge.addActionListener(e -> finish(Choice.MERGE));
                JButton takeTheGames = makeButtonSubtle(getText("bindings.check.takeTheGames"));
                takeTheGames.addActionListener(e -> finish(Choice.TAKE_THE_GAMES));
                builder.middle(merge).extra(takeTheGames);
            }
        }
        HudModalSpec spec = builder.build();
        setContentPane(AppTheme.hudModalScaffold(spec));
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        getRootPane().registerKeyboardAction(e -> finish(Choice.NOT_NOW), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        // WHY: nothing selected, so the user clicks one (Alan, 2026-10-30), as the leave prompt does.
        HudConfirmDialog.chooseByClickOnly(this, body);
        pack();
        setLocationRelativeTo(getOwner());
    }

    /** Shows the warning and waits. On the EDT. */
    static Choice show(Component parent, Warning warning, Offer offer) {
        CheckWarningDialog dialog = new CheckWarningDialog(parent, warning, offer);
        AppTheme.runWithModalScrim(dialog.getOwner(), () -> dialog.setVisible(true));
        return dialog.choice;
    }

    /** A read-only grid, each column as wide as its widest cell up to {@link #MAX_COLUMN}, each row in its tone. */
    private static JTable grid(List<Row> rows) {
        DefaultTableModel model = new DefaultTableModel(new Object[]{getText("bindings.check.column.file"),
                getText("bindings.check.column.found"), getText("bindings.check.column.fate")}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        rows.forEach(row -> model.addRow(new Object[]{row.file(), row.found(), row.fate()}));
        JTable table = new JTable(model);
        HudTable.styleCompact(table);
        table.setDefaultRenderer(Object.class, new ToneRenderer(rows));
        table.setFocusable(false);
        table.setRowSelectionAllowed(false);
        table.getTableHeader().setReorderingAllowed(false);
        HudTable.fitColumnsToContent(table, MAX_COLUMN);
        return table;
    }

    /** The grid in its data-plane frame, as wide as the text and at most {@link #VISIBLE_ROWS} rows high. */
    private static JScrollPane scrolling(JTable table, int width) {
        int visible = Math.min(table.getRowCount(), VISIBLE_ROWS);
        table.setPreferredScrollableViewportSize(new Dimension(width, visible * table.getRowHeight()));
        JScrollPane pane = HudTable.dataPlaneScrollPane(table);
        pane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        return pane;
    }

    /**
     * Wrapped text at {@code width}. A line-wrapping JTextArea reports a one-line height until it has a width, so
     * pack() would clip it - the reason this warning was cut off before the grid (2026-11-10).
     */
    private static JTextArea text(String text, Color colour, boolean bold, int width) {
        JTextArea area = new JTextArea(text);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setEditable(false);
        area.setFocusable(false);
        area.setOpaque(false);
        area.setBorder(null);
        area.setForeground(colour);
        Font base = UIManager.getFont("Label.font");
        if (base == null) base = area.getFont();
        area.setFont(base.deriveFont(bold ? Font.BOLD : Font.PLAIN, HudPalette.HUD_FONT_FIELD_VALUE));
        area.putClientProperty(AppTheme.HUD_LOCKED_FOREGROUND, Boolean.TRUE);
        area.setSize(width, Short.MAX_VALUE);
        area.setPreferredSize(new Dimension(width, area.getPreferredSize().height));
        return area;
    }

    private void finish(Choice chosen) {
        choice = chosen;
        dispose();
    }

    /** Each row in its tone's text colour, and every cell's whole text as its tooltip, cut short by its column or not. */
    private static final class ToneRenderer extends HudTable.CellRenderer {

        private final List<Row> rows;

        ToneRenderer(List<Row> rows) {
            super(2);
            this.rows = rows;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, false, false, row, column);
            label.setForeground(switch (rows.get(table.convertRowIndexToModel(row)).tone()) {
                case NORMAL -> HudPalette.HUD_COLOR_ROLE_VALUE_TEXT;
                case WARNING -> HudPalette.HUD_COLOR_ROLE_WARNING;
                case DANGER -> HudPalette.HUD_COLOR_ROLE_DANGER;
            });
            label.setToolTipText(value == null ? null : value.toString());
            return label;
        }
    }
}
