package elite.intel.ui.screen.bindings;

import elite.intel.bindforge.devices.DeviceNames;
import elite.intel.bindforge.devices.MyDevice;
import elite.intel.bindforge.devices.Onboarding;
import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.theme.HudPalette;
import elite.intel.ui.widget.HudModalSpec;
import elite.intel.ui.widget.HudTextField;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.theme.AppTheme.*;

/**
 * Onboarding: a name for each controller that has none, one page each (spec, alias-designer.md, <em>What it asks:
 * just a name</em>; Alan, 2026-10-14, A6b).
 * <p>
 * Each page is prefilled with the default name. <strong>Skip</strong> takes it, <strong>Next</strong> takes what is
 * typed once the rule accepts it, and on the last page <strong>Finish</strong> hands every name to the screen,
 * which writes them all in one pass. <strong>BACK, the close glyph and Esc leave having written nothing</strong>, and
 * ask nothing - only typed names are lost, and the controllers are offered again by CHECK AGAIN, the next run, or the gear.
 * <p>
 * Labels are not asked for: they are generated, and renamed later in the Device Editor.
 */
final class OnboardingDialog extends JDialog {

    /**
     * The body's width. Every long line - a controller's full product string, the move notice carrying a name of up to
     * {@link DeviceNames#MAX_LENGTH} characters - wraps to it instead of widening the dialog past the screen or being
     * cut off.
     */
    private static final int BODY_WIDTH = 600;

    private final List<Onboarding.Page> pages;
    private final Collection<String> taken;
    private final Consumer<List<String>> onFinish;
    /** The name each page holds - its default until changed - in page order. */
    private final List<String> chosen = new ArrayList<>();
    private int page;

    private JLabel progress;
    private JTextArea device;
    private JLabel hardware;
    private HudTextField name;
    private JTextArea problem;
    private JTextArea moves;
    private JButton next;

    /**
     * @param taken    every name already in use, outside this dialog - the other pages' names are checked as well,
     *                 when a name is judged
     * @param onFinish given every page's name, in page order, once the last page is done - on the EDT, after the
     *                 dialog has closed
     */
    OnboardingDialog(Component parent, List<Onboarding.Page> pages, Collection<String> taken,
                     Consumer<List<String>> onFinish) {
        super(SwingUtilities.getWindowAncestor(parent), ModalityType.APPLICATION_MODAL);
        if (pages.isEmpty()) throw new IllegalArgumentException("Onboarding needs at least one controller to name");
        setUndecorated(true);
        this.pages = List.copyOf(pages);
        this.taken = List.copyOf(taken);
        this.onFinish = onFinish;
        pages.forEach(p -> chosen.add(p.defaultName()));
        buildUi();
        showPage();
    }

    void showDialog() {
        AppTheme.runWithModalScrim(getOwner(), () -> setVisible(true));
    }

    private void buildUi() {
        JPanel body = transparentPanel(new GridBagLayout());
        int row = 0;
        row = addRow(body, row, paragraph(getText(pages.size() == 1
                        ? "bindings.aliasDesigner.onboarding.intro.one"
                        : "bindings.aliasDesigner.onboarding.intro.many", pages.size()),
                HudPalette.HUD_COLOR_ROLE_PRIMARY_TEXT));
        progress = AppTheme.hudReadoutValue(" ", HudPalette.HUD_COLOR_ROLE_SECONDARY_TEXT);
        row = addRow(body, row, progress);
        // WHY: on rows of their own. A product string runs to 60 characters and more, and beside the hardware IDs it
        // pushed both columns past the body and clipped them.
        device = paragraph(" ", HudPalette.HUD_COLOR_ROLE_PRIMARY_TEXT);
        row = addRow(body, row, device);
        hardware = AppTheme.hudReadoutValue(" ", HudPalette.HUD_COLOR_ROLE_SECONDARY_TEXT);
        row = addRow(body, row, hardware);

        body.add(AppTheme.hudReadoutLabel(getText("bindings.aliasDesigner.onboarding.field.name")), cell(0, row));
        name = new HudTextField();
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
        GridBagConstraints field = cell(1, row++);
        field.fill = GridBagConstraints.HORIZONTAL;
        field.weightx = 1;
        field.insets.right = 0;
        body.add(name, field);
        problem = paragraph(" ", HudPalette.HUD_COLOR_ROLE_WARNING);
        row = addRow(body, row, problem);
        moves = paragraph(" ", HudPalette.HUD_COLOR_ROLE_WARNING);
        row = addRow(body, row, moves);
        addRow(body, row, paragraph(getText("bindings.aliasDesigner.onboarding.labels"),
                HudPalette.HUD_COLOR_ROLE_SECONDARY_TEXT));

        next = makeButton("");
        next.addActionListener(e -> onNext(false));
        JButton skip = makeButtonSubtle(getText("bindings.aliasDesigner.onboarding.skip"));
        skip.addActionListener(e -> onNext(true));
        JButton back = makeButtonSubtle(getText("button.back"));
        back.addActionListener(e -> dispose());

        HudModalSpec spec = HudModalSpec.builder()
                .title(getText("bindings.aliasDesigner.onboarding.title"))
                .onClose(this::dispose)
                .body(body)
                .scrollBody(false)
                .dismiss(back)
                .extra(skip)
                .primary(next)
                .build();
        setContentPane(AppTheme.hudModalScaffold(spec));
        getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        // WHY: Enter in the name box is Next - the one field on the page, and Next refuses a name the rule does not.
        getRootPane().setDefaultButton(next);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    }

    private void showPage() {
        Onboarding.Page current = pages.get(page);
        MyDevice controller = current.device();
        progress.setText(getText("bindings.aliasDesigner.onboarding.progress", page + 1, pages.size()));
        hardware.setText(getText("bindings.aliasDesigner.onboarding.hardware", controller.vid(), controller.pid()));
        setParagraph(device, controller.label());
        name.setText(chosen.get(page));
        if (!current.waitsForApply()) setParagraph(moves, " ");
        next.setText(getText(page == pages.size() - 1
                ? "bindings.aliasDesigner.onboarding.finish"
                : "bindings.aliasDesigner.onboarding.next"));
        judge();
        // WHY: packed here, not once when built. Packed before the first page was filled in, the dialog took the
        // height of empty rows, and every line of the real text was squeezed into it and cut off.
        pack();
        setLocationRelativeTo(getOwner());
        name.requestFocusInWindow();
        name.selectAll();
    }

    /** Says what is wrong with the typed name, and dims Next until it is right. */
    private void judge() {
        DeviceNames.Verdict verdict = Onboarding.check(name.getText().trim(), page, chosen, taken);
        setParagraph(problem, verdict.valid() ? " " : problemText(verdict));
        next.setEnabled(verdict.valid());
        Onboarding.Page current = pages.get(page);
        if (current.waitsForApply()) {
            String shown = verdict.valid() ? name.getText().trim() : current.defaultName();
            setParagraph(moves, getText("bindings.aliasDesigner.onboarding.movesAtApply", current.bindings(), shown));
        }
        growToFit();
    }

    /**
     * Makes the dialog tall enough for its text, which can wrap onto another line as a name is typed. It only grows:
     * shrinking back would make the buttons jump under the pointer on every keystroke.
     */
    private void growToFit() {
        if (!isDisplayable()) return;
        int wanted = getPreferredSize().height;
        if (wanted > getHeight()) setSize(getWidth(), wanted);
    }

    /** Why a typed name was refused - onboarding's words, which the Device Editor's rename shares. */
    static String problemText(DeviceNames.Verdict verdict) {
        return switch (verdict.problem()) {
            case TOO_SHORT -> getText("bindings.aliasDesigner.onboarding.problem.tooShort", DeviceNames.MIN_LENGTH);
            case TOO_LONG -> getText("bindings.aliasDesigner.onboarding.problem.tooLong", DeviceNames.MAX_LENGTH);
            case NOT_STARTING_WITH_LETTER -> getText("bindings.aliasDesigner.onboarding.problem.notStartingWithLetter");
            case INVALID_CHARACTER -> getText("bindings.aliasDesigner.onboarding.problem.invalidCharacter");
            case CLASHES -> getText("bindings.aliasDesigner.onboarding.problem.clashes", verdict.clashesWith());
        };
    }

    /**
     * Next keeps the typed name, Skip the default; either moves on, and on the last page finishes.
     *
     * @param skip whether the page takes its default name
     */
    private void onNext(boolean skip) {
        String typed = name.getText().trim();
        if (skip) {
            chosen.set(page, pages.get(page).defaultName());
        } else {
            if (!Onboarding.check(typed, page, chosen, taken).valid()) return;
            chosen.set(page, typed);
        }
        // WHY: a skipped page needs no check. Every page starts holding its default, and check() counts every other
        // page's name, so no name typed elsewhere can have taken this page's default.
        if (page < pages.size() - 1) {
            page++;
            showPage();
            return;
        }
        List<String> names = List.copyOf(chosen);
        dispose();
        onFinish.accept(names);
    }

    /**
     * Text that wraps to the body's width. A text area, not a label: a JLabel lays its text out on one line at its own
     * width and is cut off when that is wider than the dialog (see {@code HudOverlaySettingsDialog.wrappedNote}).
     */
    private static JTextArea paragraph(String text, Color color) {
        JTextArea area = new JTextArea();
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setEditable(false);
        area.setFocusable(false);
        area.setOpaque(false);
        area.setBorder(null);
        area.setForeground(color);
        area.setFont(UIManager.getFont("Label.font").deriveFont(HudPalette.HUD_FONT_READOUT_VALUE));
        area.putClientProperty(AppTheme.HUD_LOCKED_FOREGROUND, Boolean.TRUE);
        setParagraph(area, text);
        return area;
    }

    /**
     * Sets a {@link #paragraph}'s text and the height it needs, wrapped at the body's width.
     */
    private static void setParagraph(JTextArea area, String text) {
        area.setText(text);
        area.setPreferredSize(null);
        area.setSize(BODY_WIDTH, Short.MAX_VALUE);
        area.setPreferredSize(new Dimension(BODY_WIDTH, area.getPreferredSize().height));
        area.revalidate();
    }

    /** A component across both columns, on its own row. */
    private static int addRow(JPanel panel, int row, JComponent component) {
        GridBagConstraints c = cell(0, row);
        c.gridwidth = 2;
        c.insets.right = 0;
        panel.add(component, c);
        return row + 1;
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
