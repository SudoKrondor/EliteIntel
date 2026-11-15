package elite.intel.ui.screen.colorthemes;

import elite.intel.eventbus.UiBus;
import elite.intel.session.SystemSession;
import elite.intel.ui.dialog.HudColorChooserDialog;
import elite.intel.ui.dialog.HudConfirmDialog;
import elite.intel.ui.event.SystemShutDownEvent;
import elite.intel.ui.i18n.MultiLingualTextProvider;
import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.theme.ColorTheme;
import elite.intel.ui.theme.HudPalette;
import elite.intel.ui.theme.ThemeDraft;
import elite.intel.ui.widget.HudSegmentedControl;
import elite.intel.ui.theme.preview.ColorVisionSimulation;
import elite.intel.ui.theme.ThemeContrast;
import elite.intel.ui.theme.ThemeFile;
import elite.intel.ui.theme.ThemeFileException;
import elite.intel.ui.theme.ThemeLibrary;
import elite.intel.ui.theme.ThemeLibrary.Entry;
import elite.intel.ui.theme.ThemeRole;
import elite.intel.ui.theme.ThemeStore;
import elite.intel.ui.widget.HudColorSwatch;
import elite.intel.ui.widget.HudFooter;
import elite.intel.ui.widget.HudPanel;
import elite.intel.ui.widget.HudSection;
import elite.intel.util.AppRelauncher;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.theme.AppTheme.makeButton;
import static elite.intel.ui.theme.AppTheme.makeButtonSubtle;
import static elite.intel.ui.theme.AppTheme.transparentPanel;

/**
 * Commander → Color Themes: the commander's colour themes for the app window. The list on the left, the theme drawn
 * in the middle (click any part to change its colour), its colours on the right, and Revert, Save and Apply below.
 * <p>
 * The built-in themes cannot be changed: the first change to one goes into a copy, which the list then selects.
 * Applying a theme stores it as the one in use and offers to restart, because the app reads its colours once, at
 * startup ({@code ActiveTheme}). See {@code docs/COLOR_THEMES.md}.
 */
public final class ColorThemesTabPanel extends JPanel {

    private static final Logger log = LogManager.getLogger(ColorThemesTabPanel.class);

    private static final int GAP = HudPalette.HUD_GAP;
    private static final int LIST_WIDTH = 240;
    private static final int COLORS_WIDTH = 200;
    private static final int SWATCH_COLUMNS = 3;
    /** In HTML px, which Swing draws wider than screen pixels at the UI font: found by eye to fit {@link #COLORS_WIDTH}. */
    private static final int COLORS_HINT_WRAP = 135;
    private static final String FILE_EXTENSION = "json";

    private final ThemeLibrary library;
    private final SystemSession session = SystemSession.getInstance();
    /** WHY kept: the app draws in the theme it started with until it restarts, whatever is chosen since. */
    private final String startedWithId;

    private final DefaultListModel<Entry> listModel = new DefaultListModel<>();
    private final JList<Entry> list = new JList<>(listModel);
    private final ThemePreviewPane preview;
    private final JPanel swatches = transparentPanel(new GridLayout(0, SWATCH_COLUMNS, GAP / 2, GAP / 2));
    private final JPanel contrastWarnings = transparentPanel(new GridLayout(0, 1, 0, GAP / 2));
    private final JLabel status = new JLabel();

    private final JButton renameButton = makeButtonSubtle(getText("themes.button.rename"));
    private final JButton deleteButton = makeButtonSubtle(getText("themes.button.delete"));
    private final JButton revertButton = makeButtonSubtle(getText("themes.button.revert"));
    private final JButton saveButton = makeButtonSubtle(getText("themes.button.save"));
    private final JButton applyButton = makeButton(getText("themes.button.apply"));

    /** The theme in use as stored, read again whenever it may have changed: the list marks it on every repaint. */
    private String activeId;
    private Entry selected;
    private ThemeDraft draft;
    private boolean selecting;

    /**
     * @param themesFolder the commander's themes folder ({@code AppPaths.getThemesDir()})
     */
    public ColorThemesTabPanel(Path themesFolder) {
        super(new BorderLayout(GAP, GAP));
        setOpaque(false);
        this.library = new ThemeLibrary(themesFolder);
        this.startedWithId = session.getUiTheme();
        this.activeId = startedWithId;
        this.preview = new ThemePreviewPane(session.getLanguage(), this::changeRole);
        buildUi();
        reload(startedWithId);
    }

    // -- Layout ----------------------------------------------------------------

    private void buildUi() {
        add(listSection(), BorderLayout.WEST);
        add(previewSection(), BorderLayout.CENTER);
        add(colorsSection(), BorderLayout.EAST);
        status.setForeground(HudPalette.HUD_COLOR_ROLE_SECONDARY_TEXT);
        status.putClientProperty(AppTheme.HUD_LOCKED_FOREGROUND, Boolean.TRUE);
        add(HudFooter.build(false, null, status, List.of(revertButton, saveButton, applyButton)), BorderLayout.SOUTH);
        revertButton.addActionListener(e -> revert());
        saveButton.addActionListener(e -> save());
        applyButton.addActionListener(e -> apply());
    }

    private JComponent listSection() {
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new EntryRenderer());
        list.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && !selecting) select(list.getSelectedValue());
        });

        JPanel actions = transparentPanel(new GridLayout(0, 2, GAP / 2, GAP / 2));
        actions.add(action("themes.button.new", this::createNew));
        actions.add(action("themes.button.duplicate", this::duplicate));
        renameButton.addActionListener(e -> rename());
        actions.add(renameButton);
        deleteButton.addActionListener(e -> delete());
        actions.add(deleteButton);
        actions.add(action("themes.button.import", this::importTheme));
        actions.add(action("themes.button.export", this::exportTheme));

        HudSection section = new HudSection(getText("themes.section.list"), new BorderLayout(0, GAP), HudPanel.Variant.FRAMED);
        section.body().add(AppTheme.hudScrollPane(list), BorderLayout.CENTER);
        section.body().add(actions, BorderLayout.SOUTH);
        section.setPreferredSize(new Dimension(LIST_WIDTH, 0));
        return section;
    }

    private JComponent previewSection() {
        HudSection section = new HudSection(getText("themes.section.preview"), new BorderLayout(0, GAP), HudPanel.Variant.FRAMED);
        // WHY in the header: a row of its own for one line of help took height from the preview.
        section.setHeaderNote(getText("themes.preview.hint"));
        JPanel top = transparentPanel(new BorderLayout());
        top.add(viewAs(), BorderLayout.EAST);
        section.body().add(top, BorderLayout.NORTH);
        section.body().add(AppTheme.hudScrollPane(preview), BorderLayout.CENTER);
        return section;
    }

    private JComponent colorsSection() {
        HudSection section = new HudSection(getText("themes.section.colors"), new BorderLayout(0, GAP), HudPanel.Variant.FRAMED);
        section.body().add(hint(getText("themes.colors.hint"), COLORS_HINT_WRAP), BorderLayout.NORTH);
        JPanel content = transparentPanel(new BorderLayout(0, GAP * 2));
        content.add(swatches, BorderLayout.NORTH);
        JPanel contrast = transparentPanel(new BorderLayout(0, GAP));
        contrast.add(hint(getText("themes.contrast.title"), 0), BorderLayout.NORTH);
        contrast.add(contrastWarnings, BorderLayout.CENTER);
        content.add(contrast, BorderLayout.CENTER);
        JPanel top = transparentPanel(new BorderLayout());
        top.add(content, BorderLayout.NORTH);
        section.body().add(AppTheme.hudScrollPane(top), BorderLayout.CENTER);
        section.setPreferredSize(new Dimension(COLORS_WIDTH, 0));
        return section;
    }

    private JButton action(String key, Runnable onClick) {
        JButton button = makeButtonSubtle(getText(key));
        button.addActionListener(e -> onClick.run());
        return button;
    }

    // -- Choosing a theme ------------------------------------------------------

    private void select(Entry entry) {
        if (entry == null || entry.equals(selected)) return;
        if (!confirmDiscard()) {
            showSelected();
            return;
        }
        selected = entry;
        draft = new ThemeDraft(entry.theme());
        refresh();
    }

    /** Reads the list again and selects the theme with {@code id}, or Default when it is gone. */
    private void reload(String id) {
        List<Entry> entries;
        try {
            entries = library.entries();
        } catch (IOException e) {
            log.error("Could not read the colour themes folder", e);
            error("themes.error.read", e);
            entries = List.of(new Entry(ThemeStore.DEFAULT_ID, ColorTheme.DEFAULT, true));
        }
        selecting = true;
        listModel.clear();
        entries.forEach(listModel::addElement);
        selecting = false;
        Entry target = entries.stream().filter(entry -> entry.id().equals(id)).findFirst().orElse(entries.getFirst());
        selected = target;
        draft = new ThemeDraft(target.theme());
        showSelected();
        refresh();
    }

    private void showSelected() {
        selecting = true;
        list.setSelectedValue(selected, true);
        selecting = false;
    }

    private boolean confirmDiscard() {
        if (draft == null || !draft.isDirty()) return true;
        return HudConfirmDialog.confirm(this, getText("themes.unsaved.title"),
                getText("themes.unsaved.message", displayName(selected)),
                getText("themes.unsaved.discard"), getText("button.cancel"));
    }

    // -- Editing ---------------------------------------------------------------

    /** A click on the preview: the part's colour, for that part only or every part showing the same colour. */
    private void changeRole(ThemeRole role) {
        if (!editable()) return;
        Color picked = HudColorChooserDialog.choose(this, getText("themes.change.title"), draft.color(role));
        if (picked == null) return;
        int sharing = draft.rolesColored(draft.color(role)).size();
        if (sharing > 1) {
            HudConfirmDialog.Result scope = HudConfirmDialog.show(this, getText("themes.change.title"),
                    getText("themes.change.message", sharing), getText("themes.change.every"),
                    getText("themes.change.only"), getText("button.cancel"));
            if (scope == HudConfirmDialog.Result.DISMISS) return;
            if (scope == HudConfirmDialog.Result.PRIMARY) draft.changeEveryRoleColoredLike(role, picked);
            else draft.changeRole(role, picked);
        } else {
            draft.changeRole(role, picked);
        }
        refresh();
    }

    /** A click on a swatch: that colour, everywhere it is used. */
    private void changeColor(List<ThemeRole> roles) {
        if (!editable()) return;
        ThemeRole first = roles.getFirst();
        Color picked = HudColorChooserDialog.choose(this, getText("themes.change.title"), draft.color(first));
        if (picked == null) return;
        draft.changeEveryRoleColoredLike(first, picked);
        refresh();
    }

    /**
     * Whether the selected theme can take a change. A built-in one cannot: it is copied first, the copy selected,
     * and the commander told where their change went.
     */
    private boolean editable() {
        if (!selected.builtIn()) return true;
        try {
            Entry copy = library.create(copyOf(selected, displayName(selected)));
            reload(copy.id());
            HudConfirmDialog.info(this, getText("themes.change.title"),
                    getText("themes.builtInCopied", copy.theme().name()), getText("button.ok"));
            return true;
        } catch (IOException e) {
            log.error("Could not copy the built-in colour theme {}", selected.id(), e);
            error("themes.error.save", e);
            return false;
        }
    }

    private void revert() {
        draft.revert();
        refresh();
    }

    private boolean save() {
        if (selected.builtIn() || !draft.isDirty()) return true;
        try {
            selected = library.save(selected, draft.current());
            draft.markSaved(selected.theme());
            reload(selected.id());
            return true;
        } catch (IOException e) {
            log.error("Could not save colour theme {}", selected.id(), e);
            error("themes.error.save", e);
            return false;
        }
    }

    /** Saves the theme, makes it the one in use, and offers to restart: the app reads its colours at startup. */
    private void apply() {
        if (!save()) return;
        setActive(selected.id());
        reload(selected.id());
        String chosen = getText("themes.apply.message", displayName(selected));
        if (!AppRelauncher.canRelaunch()) {
            HudConfirmDialog.info(this, getText("themes.apply.title"),
                    chosen + "\n\n" + getText("themes.apply.restartByHand"), getText("button.ok"));
            return;
        }
        HudConfirmDialog.Result answer = HudConfirmDialog.show(this, getText("themes.apply.title"), chosen,
                getText("themes.apply.restartNow"), null, getText("themes.apply.later"));
        if (answer == HudConfirmDialog.Result.PRIMARY) restart();
    }

    private void restart() {
        if (AppRelauncher.relaunch()) {
            UiBus.publish(new SystemShutDownEvent());
            return;
        }
        HudConfirmDialog.info(this, getText("themes.apply.title"), getText("themes.apply.restartByHand"),
                getText("button.ok"));
    }

    // -- The list's actions ----------------------------------------------------

    private void createNew() {
        if (!confirmDiscard()) return;
        askName(getText("themes.name.newTitle"), getText("themes.name.newDefault"))
                .ifPresent(name -> createAndSelect(new ColorTheme(name, "", Map.of())));
    }

    private void duplicate() {
        if (!confirmDiscard()) return;
        askName(getText("themes.button.duplicate"), getText("themes.copyOf", displayName(selected)))
                .ifPresent(name -> createAndSelect(new ColorTheme(name, selected.theme().author(), selected.theme().overrides())));
    }

    private void rename() {
        if (selected.builtIn()) return;
        askName(getText("themes.button.rename"), selected.theme().name()).ifPresent(name -> {
            try {
                Entry renamed = library.rename(selected, name);
                selected = renamed;
                draft.markSaved(renamed.theme());
                ThemeDraft kept = draft;
                reload(renamed.id());
                draft = kept;
                refresh();
            } catch (IOException e) {
                log.error("Could not rename colour theme {}", selected.id(), e);
                error("themes.error.save", e);
            }
        });
    }

    private void delete() {
        if (selected.builtIn()) return;
        String message = getText("themes.delete.message", displayName(selected));
        boolean inUse = selected.id().equals(activeId);
        if (inUse) message += "\n\n" + getText("themes.delete.activeNote");
        if (!HudConfirmDialog.confirm(this, getText("themes.delete.title"), message,
                getText("themes.button.delete"), getText("button.cancel"))) {
            return;
        }
        try {
            library.delete(selected);
            if (inUse) setActive(ThemeStore.DEFAULT_ID);
            draft = null;
            reload(ThemeStore.DEFAULT_ID);
        } catch (IOException e) {
            log.error("Could not delete colour theme {}", selected.id(), e);
            error("themes.error.save", e);
        }
    }

    private void importTheme() {
        if (!confirmDiscard()) return;
        JFileChooser chooser = themeFileChooser(getText("themes.import.title"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            ColorTheme imported = ThemeFile.read(chooser.getSelectedFile().toPath());
            Optional<Entry> sameName = library.findByName(imported.name());
            if (sameName.isEmpty()) {
                createAndSelect(imported);
                return;
            }
            HudConfirmDialog.Result answer = HudConfirmDialog.show(this, getText("themes.import.title"),
                    getText("themes.import.nameTaken", imported.name()), getText("themes.import.replace"),
                    getText("themes.import.keepBoth"), getText("button.cancel"));
            if (answer == HudConfirmDialog.Result.PRIMARY) {
                Entry replaced = library.save(sameName.get(), imported);
                draft = null;
                reload(replaced.id());
            } else if (answer == HudConfirmDialog.Result.EXTRA) {
                createAndSelect(imported);
            }
        } catch (IOException | ThemeFileException e) {
            log.error("Could not import colour theme {}", chooser.getSelectedFile(), e);
            error("themes.import.failed", e);
        }
    }

    /** Exports the theme as it looks now, unsaved changes included: what the commander sees is what they share. */
    private void exportTheme() {
        JFileChooser chooser = themeFileChooser(getText("themes.export.title"));
        chooser.setSelectedFile(new File(displayName(selected) + "." + FILE_EXTENSION));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File target = chooser.getSelectedFile();
        if (!target.getName().toLowerCase(Locale.ROOT).endsWith("." + FILE_EXTENSION)) {
            target = new File(target.getAbsolutePath() + "." + FILE_EXTENSION);
        }
        ColorTheme shared = new ColorTheme(displayName(selected), draft.current().author(), draft.current().overrides());
        try {
            library.export(new Entry(selected.id(), shared, selected.builtIn()), target.toPath());
        } catch (IOException e) {
            log.error("Could not export colour theme to {}", target, e);
            error("themes.error.save", e);
        }
    }

    private void createAndSelect(ColorTheme theme) {
        try {
            Entry created = library.create(theme);
            draft = null;
            reload(created.id());
        } catch (IOException e) {
            log.error("Could not create colour theme {}", theme.name(), e);
            error("themes.error.save", e);
        }
    }

    // -- Showing the draft -----------------------------------------------------

    private void refresh() {
        preview.show(draft.current());
        rebuildSwatches();
        rebuildContrastWarnings();
        boolean own = !selected.builtIn();
        boolean dirty = draft.isDirty();
        renameButton.setEnabled(own);
        deleteButton.setEnabled(own);
        revertButton.setEnabled(dirty);
        saveButton.setEnabled(own && dirty);
        applyButton.setEnabled(dirty || !selected.id().equals(activeId));
        status.setText(startedWithId.equals(activeId) ? "" : getText("themes.status.pendingRestart"));
        list.repaint();
    }

    private void rebuildSwatches() {
        swatches.removeAll();
        draft.colorsInUse().forEach((color, roles) -> swatches.add(swatch(color, roles)));
        swatches.revalidate();
        swatches.repaint();
    }

    private HudColorSwatch swatch(Color color, List<ThemeRole> roles) {
        HudColorSwatch swatch = new HudColorSwatch(color);
        swatch.setToolTipText(getText("themes.colors.parts", roles.size()));
        swatch.addActionListener(e -> changeColor(roles));
        swatch.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                preview.isolate(roles, color);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                preview.highlight(List.of());
            }
        });
        return swatch;
    }

    /** The text the theme makes hard to read, lowest contrast first, or a line saying there is none. */
    private void rebuildContrastWarnings() {
        contrastWarnings.removeAll();
        List<ThemeContrast.Warning> warnings = ThemeContrast.check(draft.current());
        if (warnings.isEmpty()) contrastWarnings.add(hint(getText("themes.contrast.none"), COLORS_HINT_WRAP));
        if (warnings.size() > 1) contrastWarnings.add(action("themes.contrast.fixAll", this::fixAllContrast));
        for (ThemeContrast.Warning warning : warnings) contrastWarnings.add(contrastWarning(warning));
        contrastWarnings.revalidate();
        contrastWarnings.repaint();
    }

    private JComponent contrastWarning(ThemeContrast.Warning warning) {
        JPanel row = transparentPanel(new FlowLayout(FlowLayout.LEFT, GAP, 0));
        row.add(new ContrastSample(warning.textColor(), warning.backgroundColor()));
        JLabel ratio = new JLabel(getText("themes.contrast.ratio", warning.ratio()));
        ratio.setForeground(HudPalette.HUD_COLOR_ROLE_WARNING);
        ratio.putClientProperty(AppTheme.HUD_LOCKED_FOREGROUND, Boolean.TRUE);
        row.add(ratio);
        row.setToolTipText(getText("themes.contrast.tooltip", warning.ratio()));
        row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        row.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                fixContrast(warning);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                preview.highlight(List.of(warning.text()));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                preview.highlight(List.of());
            }
        });
        return row;
    }

    /**
     * Makes one hard-to-read text readable, without asking for a colour: the commander sees the text is hard to read
     * but cannot be expected to know which way to move the colour, or how far ({@link ThemeDraft#makeReadable}).
     */
    private void fixContrast(ThemeContrast.Warning warning) {
        if (!editable()) return;
        ThemeDraft.Fix fix = draft.makeReadable(warning.text(), warning.background());
        preview.highlight(List.of());
        refresh();
        if (fix != ThemeDraft.Fix.FIXED) explainContrastLimit();
    }

    private void fixAllContrast() {
        if (!editable()) return;
        draft.makeAllReadable();
        refresh();
        if (!ThemeContrast.check(draft.current()).isEmpty()) explainContrastLimit();
    }

    /**
     * Says why a warning is still there. WHY it can be: a colour can be text in one place and the background of other
     * text in another, and with some colours no choice lets both read at the minimum - the fix then stops at the
     * best balance rather than making other text worse.
     */
    private void explainContrastLimit() {
        HudConfirmDialog.info(this, getText("themes.contrast.title"), getText("themes.contrast.limit"),
                getText("button.ok"));
    }

    private JComponent viewAs() {
        ColorVisionSimulation[] visions = ColorVisionSimulation.values();
        String[] labels = new String[visions.length];
        for (int i = 0; i < visions.length; i++) {
            labels[i] = getText("themes.viewAs." + visions[i].name().toLowerCase(Locale.ROOT));
        }
        HudSegmentedControl control = new HudSegmentedControl(labels, 0);
        control.addChangeListener(e -> preview.simulate(visions[control.getSelectedIndex()]));
        control.setToolTipText(getText("themes.viewAs"));
        return control;
    }

    // -- Helpers ---------------------------------------------------------------

    private void setActive(String id) {
        session.setUiTheme(id);
        activeId = id;
    }

    private Optional<String> askName(String title, String initial) {
        return ThemeNameDialog.ask(this, title, initial);
    }

    private ColorTheme copyOf(Entry entry, String name) {
        return new ColorTheme(getText("themes.copyOf", name), entry.theme().author(), draft.current().overrides());
    }

    private static String displayName(Entry entry) {
        if (!entry.builtIn()) return entry.theme().name();
        if (entry.id().equals(ThemeStore.HIGH_CONTRAST_ID)) return getText("themes.list.highContrast");
        if (entry.id().equals(ThemeStore.CLOCKWORK_ORANGE_ID)) return getText("themes.list.clockworkOrange");
        return getText("themes.list.default");
    }

    private static JFileChooser themeFileChooser(String title) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(title);
        chooser.setFileFilter(new FileNameExtensionFilter(getText("themes.fileFilter"), FILE_EXTENSION));
        return chooser;
    }

    private void error(String key, Exception cause) {
        HudConfirmDialog.info(this, getText("themes.error.title"), getText(key, cause.getMessage()), getText("button.ok"));
    }

    /**
     * A line of help above a section. WHY a label: the app's palette pass dresses every text area as an input field.
     *
     * @param wrapWidth the width to wrap at, or 0 for one line
     */
    private static JComponent hint(String text, int wrapWidth) {
        String escaped = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        JLabel label = new JLabel(wrapWidth <= 0 ? text
                : "<html><div style='width:" + wrapWidth + "px'>" + escaped + "</div></html>");
        label.setForeground(HudPalette.HUD_COLOR_ROLE_SECONDARY_TEXT);
        label.putClientProperty(AppTheme.HUD_LOCKED_FOREGROUND, Boolean.TRUE);
        return label;
    }

    /**
     * A theme's name, and a mark on the one the app is drawn in ("in use") and on one applied since, which the app
     * draws from its next start ("after restart").
     */
    private final class EntryRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                                                      boolean cellHasFocus) {
            Entry entry = (Entry) value;
            String text = displayName(entry);
            if (entry.id().equals(startedWithId)) {
                text += "  ·  " + MultiLingualTextProvider.getText("themes.list.inUse");
            } else if (entry.id().equals(activeId)) {
                text += "  ·  " + MultiLingualTextProvider.getText("themes.list.afterRestart");
            }
            return super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
        }
    }

    /** "Aa" in a text colour on its background: the colours of a contrast warning, seen rather than named. */
    private static final class ContrastSample extends JComponent {
        private static final Dimension SIZE = new Dimension(46, 24);
        private final Color text;
        private final Color background;

        ContrastSample(Color text, Color background) {
            this.text = text;
            this.background = background;
            setPreferredSize(SIZE);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setColor(background);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(HudPalette.HUD_COLOR_ROLE_CONTROL_DECORATION);
                g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
                g2.setColor(text);
                FontMetrics metrics = g2.getFontMetrics();
                String sample = "Aa";
                g2.drawString(sample, (getWidth() - metrics.stringWidth(sample)) / 2,
                        (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent());
            } finally {
                g2.dispose();
            }
        }
    }
}
