package elite.intel.ui.screen.bindings;

import com.google.common.eventbus.Subscribe;
import elite.intel.bindforge.devices.DeviceNames;
import elite.intel.bindforge.storage.BindsEdits;
import elite.intel.bindforge.storage.ConfigurationFiles;
import elite.intel.bindforge.storage.Edits;
import elite.intel.bindforge.storage.Rename;
import elite.intel.eventbus.UiBus;
import elite.intel.ui.event.BindForgeEditsChangedEvent;
import elite.intel.ui.widget.HudComboBox;
import elite.intel.ui.widget.HudPanel;
import elite.intel.ui.widget.HudSection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import static elite.intel.ui.dialog.HudConfirmDialog.info;
import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.theme.AppTheme.hudReadoutLabel;
import static elite.intel.ui.theme.AppTheme.makeButtonSubtle;
import static elite.intel.ui.theme.AppTheme.makeTextField;

/**
 * File Manager's <em>Bindings File</em> section, between the bindings folder and the installations: the binds files in
 * the working copy, with the edits not applied on them, and LOAD, which opens the chosen one in the Bind Editor
 * (file-manager.md, <em>Game Install Locations</em>; Storage S15, Alan, 2026-11-04). It moved here from the Bind
 * Editor's shell, because only File Manager shows folders and files (principle 12).
 * <p>
 * The file selected is the one the Bind Editor shows - which file that is stays the Bind Editor's rule. While CREATE has
 * the editors greyed the section is disabled; there is no binds file of the user's to choose.
 * <p>
 * <strong>RENAME</strong> renames the file selected, loaded or not, as one edit not applied ({@link Rename}; Storage
 * S16, R1-R4 and Q1-Q4, Alan, 2026-11-04). A name the rules refuse is said in a dialog and nothing changes. After a
 * rename the dropdown, and the Bind Editor if it showed the file, follow it to its new name.
 */
public final class BindsFileSection extends HudSection {

    private static final Logger log = LogManager.getLogger(BindsFileSection.class);

    /** The Bind Editor, as this section sees it. */
    public interface BindEditor {
        /** The binds file it shows, by file name - {@code null} for none. */
        String shown();

        /** Opens {@code file} in it, and brings it on screen. On the EDT. */
        void load(String file);

        /** Shows {@code file} at its next read, without bringing it on screen - a file it showed, renamed. On the EDT. */
        void showRenamed(String file);
    }

    private final HudComboBox<String> fileBox = new HudComboBox<>(new DefaultComboBoxModel<>());
    private final JButton loadButton = makeButtonSubtle(getText("bindings.bindsFile.load"));
    /** The new name for the chosen binds file - its BindName, editable (R2). */
    private final JTextField nameField = makeTextField();
    /** The chosen file's version and extension, fixed text after the name box (R2). */
    private final JLabel nameSuffix = hudReadoutLabel("");
    private final JButton renameButton = makeButtonSubtle(getText("bindings.bindsFile.rename"));
    private final RefreshCoalescer refreshes = new RefreshCoalescer();

    private BindEditor bindEditor;
    /** The editors are usable - false while CREATE has them greyed. EDT only. */
    private boolean editorsUsable = true;
    /** The last read found a file to choose. EDT only. */
    private boolean anyFile;
    /** A rename is running; a second waits for it. EDT only. */
    private boolean renameRunning;
    /** The file whose name the box holds, so a refresh that keeps the selection keeps what is typed. EDT only. */
    private String namedFile;
    /** The file to select at the next read - a file just renamed (Q1). EDT only. */
    private String selectNext;

    BindsFileSection() {
        super(getText("bindings.bindsFile"), new GridLayout(1, 2, 14, 0), HudPanel.Variant.FLAT, 6);
        fileBox.setToolTipText(getText("bindings.bindsFile.tooltip"));
        fileBox.addActionListener(e -> showName(false));
        loadButton.setToolTipText(getText("bindings.bindsFile.load.tooltip"));
        loadButton.addActionListener(e -> loadSelected());
        nameField.setToolTipText(getText("bindings.bindsFile.newName.tooltip"));
        // WHY: Enter renames and Esc puts the file's own name back, as S12's Name box did (Q3, Alan, 2026-11-04).
        nameField.addActionListener(e -> renameSelected());
        nameField.getInputMap(JComponent.WHEN_FOCUSED)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "bindforge.bindsFile.name.revert");
        nameField.getActionMap().put("bindforge.bindsFile.name.revert", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showName(true);
            }
        });
        renameButton.setToolTipText(getText("bindings.bindsFile.rename.tooltip"));
        renameButton.addActionListener(e -> renameSelected());
        // WHY two equal halves: choosing a file on the left, renaming it on the right (Alan, 2026-11-04).
        body().add(half(fileBox, loadButton));
        body().add(half(nameAndSuffix(), renameButton));
        updateEnabled();
        // WHY: LOAD changes the file the Bind Editor shows while this section is off screen, and nothing announces it -
        // coming back is when it is looked at, so that is when it is read again.
        addHierarchyListener(event -> {
            if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) == 0 || !isShowing()) return;
            refresh();
        });
        UiBus.register(this);
    }

    void dispose() {
        UiBus.unregister(this);
    }

    /** Wired by the BindForge tab, which holds both screens. */
    void setBindEditor(BindEditor bindEditor) {
        this.bindEditor = bindEditor;
        refresh();
    }

    /** CREATE greyed the editors, or brought them back. On the EDT. */
    void setEditorsUsable(boolean usable) {
        editorsUsable = usable;
        updateEnabled();
    }

    /**
     * The edits changed - DISCARD, APPLY, RESTORE, CREATE or an edit - so the binds files may have.
     */
    @Subscribe
    public void onEditsChanged(BindForgeEditsChangedEvent event) {
        SwingUtilities.invokeLater(this::refresh);
    }

    /**
     * Reads the binds files off the EDT, then shows them. A read already in flight is left to finish, and one more
     * follows it. A read that fails shows an empty, disabled list - the log says why, and before first-time startup
     * there is no working copy to read.
     */
    void refresh() {
        if (bindEditor == null || !refreshes.tryStart()) return;
        String wanted = selectNext != null ? selectNext : bindEditor.shown();
        Function<String, Runnable> empty = reason -> () -> shown(List.of(), null);
        FlowSteps.inBackground("BindForge-BindsFileList", log, "The Bindings File list's read", () -> {
            ConfigurationFiles current = Edits.stored().snapshot().current();
            List<String> files = BindsEdits.bindsFiles(current);
            String selected = files.isEmpty() ? null
                    : wanted != null && files.contains(wanted)
                    ? wanted
                    : BindsEdits.generalFile(current).orElse(files.getFirst());
            return () -> shown(files, selected);
        }, empty, empty, FlowSteps.NOTHING);
    }

    /** Shows what {@link #refresh} read, then the read asked for while it ran, if any. On the EDT. */
    private void shown(List<String> files, String selected) {
        if (Objects.equals(selected, selectNext)) selectNext = null;
        fileBox.setModel(new DefaultComboBoxModel<>(files.toArray(String[]::new)));
        fileBox.setSelectedItem(selected);
        anyFile = selected != null;
        showName(false);
        updateEnabled();
        if (refreshes.finish()) refresh();
    }

    private void updateEnabled() {
        boolean usable = editorsUsable && anyFile;
        fileBox.setEnabled(usable);
        loadButton.setEnabled(usable);
        nameField.setEnabled(usable && !renameRunning);
        nameSuffix.setEnabled(usable);
        // WHY lit for any name: a name the rules refuse is said when RENAME is pressed (Q4, Alan, 2026-11-04).
        renameButton.setEnabled(usable && !renameRunning);
    }

    /**
     * The selected file's BindName in the box and its version after it (R2, R3). Kept as typed when the selection is
     * still the file named, unless {@code always}.
     */
    private void showName(boolean always) {
        String file = selectedFile();
        if (!always && Objects.equals(file, namedFile)) return;
        namedFile = file;
        nameField.setText(file == null ? "" : Rename.bindName(file).orElse(""));
        nameSuffix.setText(file == null ? "" : Rename.bindName(file).map(name -> Rename.suffix(file)).orElse(""));
    }

    private String selectedFile() {
        Object chosen = fileBox.getSelectedItem();
        return chosen == null ? null : chosen.toString();
    }

    /** The name box, the version and extension after it as fixed text. */
    private JPanel nameAndSuffix() {
        JPanel row = new JPanel(new BorderLayout(4, 0));
        row.setOpaque(false);
        row.add(nameField, BorderLayout.CENTER);
        row.add(nameSuffix, BorderLayout.EAST);
        return row;
    }

    /** One half of the row: a field taking the width, its button after it. */
    private static JPanel half(JComponent field, JButton button) {
        JPanel half = new JPanel(new BorderLayout(7, 0));
        half.setOpaque(false);
        half.add(field, BorderLayout.CENTER);
        half.add(button, BorderLayout.EAST);
        return half;
    }

    /** LOAD: the file chosen, in the Bind Editor (D1, Alan, 2026-11-04). */
    private void loadSelected() {
        String chosen = selectedFile();
        if (chosen == null || bindEditor == null) return;
        bindEditor.load(chosen);
    }

    /**
     * RENAME, or Enter in the box: the file selected renamed to the name typed, as an edit not applied, off the EDT
     * (R1, R3). A refused name is said and nothing changes (R4); the file's own name does nothing (Q4).
     */
    private void renameSelected() {
        String file = selectedFile();
        if (file == null || renameRunning || !renameButton.isEnabled()) return;
        String typed = nameField.getText().strip();
        if (Rename.bindName(file).isEmpty() || typed.equals(Rename.bindName(file).get())) return;
        String renamed = typed + Rename.suffix(file);
        String shown = bindEditor == null ? null : bindEditor.shown();
        String title = getText("bindings.bindsFile.rename");
        renameRunning = true;
        updateEnabled();
        FlowSteps.inBackground("BindForge-BindsFileRename", log, "The binds file's rename", () -> {
                    Optional<DeviceNames.Verdict> refusal = refusal(file, typed, Rename.names(Edits.stored().snapshot()));
                    if (refusal.isPresent()) {
                        return () -> renameEnded(title, OnboardingDialog.problemText(refusal.get()));
                    }
                    try {
                        Edits.stored().change((current, applied) -> Rename.rename(current, applied, file, typed));
                    } catch (Edits.WriteRunningException e) {
                        // WHY: a write is running - said in the user's language, not the exception's.
                        log.info("The binds file's rename not done: {}", e.getMessage());
                        return () -> renameEnded(title, getText("bindings.writeRunning"));
                    }
                    return () -> {
                        // WHY: the old name is gone from the edits, so without this both would fall back to the
                        // General line's file (Q1, Alan, 2026-11-04).
                        selectNext = renamed;
                        if (file.equals(shown) && bindEditor != null) bindEditor.showRenamed(renamed);
                        renameEnded(title, null);
                        refresh();
                    };
                }, reason -> () -> renameEnded(title, getText("bindings.change.failed")),
                reason -> () -> renameEnded(title, getText("bindings.change.stopped", reason)),
                FlowSteps.NOTHING);
    }

    /** After {@link #renameSelected}: {@code failure} shown, if any, the typed name left to correct. On the EDT. */
    private void renameEnded(String title, String failure) {
        renameRunning = false;
        updateEnabled();
        if (failure != null) info(this, title, failure, getText("button.close"));
    }

    /**
     * Why {@code typed} may not rename {@code file}, as S12's Name box judged it (bind-editor.md, <em>Shell</em>; R4):
     * the controller name's rule against the set's own BindNames, loosely, the file's own included, and a factory
     * BindName refused whatever its case. Empty when it may be used, or is the file's own name.
     */
    static Optional<DeviceNames.Verdict> refusal(String file, String typed, Rename.Names names) {
        if (typed.equals(Rename.bindName(file).orElse(null))) return Optional.empty();
        DeviceNames.Verdict verdict = DeviceNames.validate(typed, names.taken(typed));
        if (!verdict.valid()) return Optional.of(verdict);
        return Rename.factoryClash(typed, names.factory())
                .map(factory -> new DeviceNames.Verdict(DeviceNames.Problem.CLASHES, factory));
    }
}
