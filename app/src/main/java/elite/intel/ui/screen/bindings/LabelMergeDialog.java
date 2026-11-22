package elite.intel.ui.screen.bindings;

import elite.intel.bindforge.devicefiles.DeviceEntry.HardwareId;
import elite.intel.bindforge.devicefiles.LabelMerge;
import elite.intel.bindforge.devicefiles.LabelMerge.Collision;
import elite.intel.ui.screen.bindings.ReconcileWidgets.Chip;
import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.theme.HudPalette;
import elite.intel.ui.widget.HudModalSpec;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.screen.bindings.ReconcileWidgets.badge;
import static elite.intel.ui.screen.bindings.ReconcileWidgets.line;
import static elite.intel.ui.screen.bindings.ReconcileWidgets.wrapped;
import static elite.intel.ui.theme.AppTheme.makeButton;
import static elite.intel.ui.theme.AppTheme.makeButtonSubtle;
import static elite.intel.ui.theme.AppTheme.transparentPanel;

/**
 * Button names, button by button, for one controller (multi-install-reconcile.md, <em>Button by button</em>): only the
 * inputs the copies name differently, one row each, named in words - <em>Button 1</em>, <em>Hat 2, up</em> - with the
 * code small beneath it. Each copy's name is a radio chip; a copy's <em>use all</em> link answers every row its way.
 * <strong>Pressing a button on the controller</strong> lights its row and scrolls to it, so the player need not know
 * which number a button is.
 * <p>
 * It edits a copy of the answers, returned on Done, so Cancel changes nothing. A partial answer is returned as it is -
 * the caller says how many are left.
 */
final class LabelMergeDialog extends JDialog {

    private final LabelMerge merge;
    private final List<String> sides;
    private final List<Collision> collisions;
    private final Map<String, String> sideNames;
    private final Map<String, String> answers;
    private final LiveControllers live;
    private final HardwareId hardware;
    private final Map<String, JPanel> rowsByInput = new LinkedHashMap<>();
    private final Map<String, JLabel> pressedMarks = new LinkedHashMap<>();
    private final Map<String, ButtonGroup> groups = new LinkedHashMap<>();
    private final Map<String, Map<String, Chip>> chips = new LinkedHashMap<>();
    private final JLabel count = new JLabel();
    private JScrollPane scroll;
    private Map<String, String> result;

    /**
     * @param controller what the controller is called, for the title
     * @param sideNames  what each side is called, by side
     * @param answers    the answers given so far, input to label
     * @param live       the controllers plugged in, whose presses find a row; {@code null} for none
     * @param hardware   the controller whose presses count; {@code null} when it is not known
     */
    LabelMergeDialog(Component parent, String controller, LabelMerge merge, Map<String, String> sideNames,
                     Map<String, String> answers, LiveControllers live, HardwareId hardware) {
        super(SwingUtilities.getWindowAncestor(parent), ModalityType.APPLICATION_MODAL);
        setUndecorated(true);
        this.merge = merge;
        this.sides = merge.sides();
        this.collisions = merge.collisions().stream()
                .sorted(Comparator.comparing(Collision::input, InputNames.ORDER)).toList();
        this.sideNames = sideNames;
        this.answers = new LinkedHashMap<>(answers);
        this.live = live;
        this.hardware = hardware;
        buildUi(controller);
    }

    /** The answers when Done was pressed, input to label; empty when the dialog was cancelled. */
    Optional<Map<String, String>> showDialog() {
        if (live != null) {
            live.listen(new LiveControllers.Listener() {
                @Override
                public void changed() {
                }

                @Override
                public void pressed(HardwareId pressed, String token) {
                    if (pressed.equals(hardware)) light(token);
                }
            });
        }
        AppTheme.runWithModalScrim(getOwner(), () -> setVisible(true));
        return Optional.ofNullable(result);
    }

    private void buildUi(String controller) {
        JPanel body = transparentPanel(new BorderLayout(0, 10));
        JPanel top = ReconcileWidgets.column(6);
        String intro = getText("bindings.reconcile.byButton.intro", collisions.size())
                + (hardware != null && live != null && live.isConnected(hardware)
                ? " " + getText("bindings.reconcile.byButton.press") : "");
        ReconcileWidgets.stack(top, wrapped(intro, HudPalette.HUD_COLOR_ROLE_SECONDARY_TEXT, HudPalette.HUD_FONT_MD, 760));
        JPanel countRow = transparentPanel(new BorderLayout());
        countRow.add(line(getText("bindings.reconcile.byButton.hidden"), HudPalette.HUD_COLOR_ROLE_READOUT_LABEL,
                HudPalette.HUD_FONT_SM, Font.PLAIN), BorderLayout.WEST);
        count.setForeground(HudPalette.HUD_COLOR_ROLE_PRIMARY_ACTION);
        count.setFont(count.getFont().deriveFont(HudPalette.HUD_FONT_SM));
        countRow.add(count, BorderLayout.EAST);
        ReconcileWidgets.stack(top, countRow);
        body.add(top, BorderLayout.NORTH);

        scroll = AppTheme.hudScrollPane(grid());
        scroll.getViewport().setBackground(HudPalette.HUD_COLOR_ROLE_DIALOG_BODY_BACKGROUND);
        scroll.setBorder(BorderFactory.createLineBorder(HudPalette.HUD_COLOR_ROLE_SECONDARY_BORDER));
        scroll.putClientProperty(AppTheme.HUD_SCROLL_STYLE_LOCKED, Boolean.TRUE);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        body.add(scroll, BorderLayout.CENTER);

        JButton done = makeButton(getText("bindings.aliasDesigner.labelMerge.done"));
        done.addActionListener(e -> {
            result = Map.copyOf(answers);
            dispose();
        });
        JButton cancel = makeButtonSubtle(getText("button.cancel"));
        cancel.addActionListener(e -> dispose());

        HudModalSpec spec = HudModalSpec.builder()
                .title(getText("bindings.reconcile.byButton.title", controller))
                .onClose(this::dispose)
                .body(body)
                .scrollBody(false)
                .primary(done)
                .dismiss(cancel)
                .build();
        setContentPane(AppTheme.hudModalScaffold(spec));
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(860, Math.min(720, 260 + collisions.size() * 52));
        setLocationRelativeTo(getOwner());
        refresh();
    }

    /** The header - Button, then each copy with its <em>use all</em> - and one row per input. */
    private JPanel grid() {
        JPanel grid = transparentPanel(new GridBagLayout());
        GridBagConstraints at = new GridBagConstraints();
        at.anchor = GridBagConstraints.WEST;
        at.fill = GridBagConstraints.BOTH;
        at.insets = new Insets(0, 0, 0, 0);
        at.gridy = 0;

        at.gridx = 0;
        grid.add(header(line(getText("bindings.reconcile.byButton.column.button").toUpperCase(),
                HudPalette.HUD_COLOR_ROLE_READOUT_LABEL, HudPalette.HUD_FONT_XS, Font.BOLD)), at);
        for (int i = 0; i < sides.size(); i++) {
            String side = sides.get(i);
            JPanel head = transparentPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            head.add(badge(sideNames.getOrDefault(side, side), false));
            head.add(Box.createHorizontalStrut(8));
            JButton useAll = ReconcileWidgets.link(getText("bindings.reconcile.byButton.useAll"));
            useAll.addActionListener(e -> {
                answers.putAll(merge.answersFavouring(side));
                refresh();
            });
            head.add(useAll);
            at.gridx = i + 1;
            grid.add(header(head), at);
        }
        at.gridx = sides.size() + 1;
        at.weightx = 1;
        grid.add(header(new JLabel()), at);
        at.weightx = 0;

        for (Collision collision : collisions) {
            at.gridy++;
            String input = collision.input();
            JPanel name = ReconcileWidgets.column(2);
            ReconcileWidgets.stack(name, line(InputNames.spoken(input), HudPalette.HUD_COLOR_ROLE_PRIMARY_TEXT,
                    HudPalette.HUD_FONT_SM, Font.BOLD));
            ReconcileWidgets.stack(name, ReconcileWidgets.alias(input, HudPalette.HUD_COLOR_ROLE_READOUT_LABEL,
                    HudPalette.HUD_FONT_XS));
            JPanel first = cell(name, input);
            at.gridx = 0;
            grid.add(first, at);

            ButtonGroup group = new ButtonGroup();
            groups.put(input, group);
            Map<String, Chip> bySide = new LinkedHashMap<>();
            for (int i = 0; i < sides.size(); i++) {
                String label = collision.bySide().get(sides.get(i));
                JComponent content;
                if (label == null) {
                    content = line("—", HudPalette.HUD_COLOR_ROLE_DISABLED, HudPalette.HUD_FONT_SM, Font.PLAIN);
                } else {
                    Chip chip = new Chip(label);
                    group.add(chip);
                    chip.addActionListener(e -> {
                        answers.put(input, label);
                        refresh();
                    });
                    bySide.put(sides.get(i), chip);
                    content = chip;
                }
                at.gridx = i + 1;
                grid.add(cell(content, input), at);
            }
            chips.put(input, bySide);
            JLabel pressed = line("", HudPalette.HUD_COLOR_ROLE_READOUT_LABEL, HudPalette.HUD_FONT_SM, Font.PLAIN);
            pressedMarks.put(input, pressed);
            at.gridx = sides.size() + 1;
            at.weightx = 1;
            grid.add(cell(pressed, input), at);
            at.weightx = 0;
        }
        at.gridy++;
        at.weighty = 1;
        at.gridx = 0;
        grid.add(Box.createGlue(), at);
        return grid;
    }

    private static JComponent header(JComponent content) {
        JPanel cell = transparentPanel(new BorderLayout());
        cell.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, HudPalette.HUD_COLOR_ROLE_FRAME_BORDER),
                new EmptyBorder(8, 12, 8, 12)));
        cell.add(content, BorderLayout.WEST);
        return cell;
    }

    /** One cell of an input's row; the row's cells light together. */
    private JPanel cell(JComponent content, String input) {
        JPanel cell = transparentPanel(new BorderLayout());
        cell.setOpaque(true);
        cell.setBackground(HudPalette.HUD_COLOR_ROLE_TABLE_CELL_BACKGROUND);
        cell.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, HudPalette.HUD_COLOR_ROLE_SECONDARY_BORDER),
                new EmptyBorder(8, 12, 8, 12)));
        cell.add(content, BorderLayout.WEST);
        cell.putClientProperty("input", input);
        rowsByInput.merge(input, cell, (first, ignored) -> first);
        return cell;
    }

    /** Lights the row of the input pressed on the controller, and scrolls it into view. */
    private void light(String token) {
        if (!chips.containsKey(token)) return;
        pressedMarks.forEach((input, mark) -> mark.setText(input.equals(token)
                ? "← " + getText("bindings.reconcile.byButton.pressed") : ""));
        Container grid = rowsByInput.get(token).getParent();
        for (Component child : grid.getComponents()) {
            if (child instanceof JPanel cell && cell.getClientProperty("input") != null) {
                boolean lit = token.equals(cell.getClientProperty("input"));
                cell.setBackground(lit ? HudPalette.HUD_COLOR_ROLE_SELECTION_BACKGROUND
                        : HudPalette.HUD_COLOR_ROLE_TABLE_CELL_BACKGROUND);
            }
        }
        JPanel row = rowsByInput.get(token);
        row.scrollRectToVisible(new Rectangle(0, 0, row.getWidth(), row.getHeight()));
        grid.repaint();
    }

    /** Each chip as the answers say, and the count. */
    private void refresh() {
        for (Collision collision : collisions) {
            String chosen = answers.get(collision.input());
            Map<String, Chip> bySide = chips.get(collision.input());
            if (bySide == null) continue;
            ButtonGroup group = groups.get(collision.input());
            group.clearSelection();
            bySide.values().stream().filter(chip -> chip.getText().equals(chosen)).findFirst()
                    .ifPresent(chip -> chip.setSelected(true));
        }
        int left = merge.unanswered(answers).size();
        count.setText(getText("bindings.aliasDesigner.labelMerge.chosen", collisions.size() - left,
                collisions.size()));
        repaint();
    }
}
