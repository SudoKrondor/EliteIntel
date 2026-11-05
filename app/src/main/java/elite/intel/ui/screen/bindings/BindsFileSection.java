package elite.intel.ui.screen.bindings;

import com.google.common.eventbus.Subscribe;
import elite.intel.bindforge.storage.BindsEdits;
import elite.intel.bindforge.storage.ConfigurationFiles;
import elite.intel.bindforge.storage.Edits;
import elite.intel.eventbus.UiBus;
import elite.intel.ui.event.BindForgeEditsChangedEvent;
import elite.intel.ui.widget.HudComboBox;
import elite.intel.ui.widget.HudPanel;
import elite.intel.ui.widget.HudSection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.util.List;
import java.util.function.Function;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.theme.AppTheme.makeButtonSubtle;
import static elite.intel.ui.theme.AppTheme.makeTextField;

/**
 * File Manager's <em>Bindings File</em> section, between the bindings folder and the installations: the binds files in the working copy, with the edits not applied on them,
 * and LOAD, which opens the chosen one in the Bind Editor (file-manager.md, <em>Game Install Locations</em>; Storage S15,
 * Alan, 2026-11-04). It moved here from the Bind Editor's shell, because only File Manager shows folders and files
 * (principle 12).
 * <p>
 * The file selected is the one the Bind Editor shows - which file that is stays the Bind Editor's rule. While CREATE has
 * the editors greyed the dropdown and LOAD are disabled too; there is no binds file of the user's to choose.
 */
public final class BindsFileSection extends HudSection {

    private static final Logger log = LogManager.getLogger(BindsFileSection.class);

    /** The Bind Editor, as this section sees it. */
    public interface BindEditor {
        /** The binds file it shows, by file name - {@code null} for none. */
        String shown();

        /** Opens {@code file} in it, and brings it on screen. On the EDT. */
        void load(String file);
    }

    private final HudComboBox<String> fileBox = new HudComboBox<>(new DefaultComboBoxModel<>());
    private final JButton loadButton = makeButtonSubtle(getText("bindings.bindsFile.load"));
    /** The new name for the chosen binds file. */
    private final JTextField nameField = makeTextField();
    private final JButton renameButton = makeButtonSubtle(getText("bindings.bindsFile.rename"));
    private final RefreshCoalescer refreshes = new RefreshCoalescer();

    private BindEditor bindEditor;
    /** The editors are usable - false while CREATE has them greyed. EDT only. */
    private boolean editorsUsable = true;
    /** The last read found a file to choose. EDT only. */
    private boolean anyFile;

    BindsFileSection() {
        super(getText("bindings.bindsFile"), new GridLayout(1, 2, 14, 0), HudPanel.Variant.FLAT, 6);
        fileBox.setToolTipText(getText("bindings.bindsFile.tooltip"));
        loadButton.setToolTipText(getText("bindings.bindsFile.load.tooltip"));
        loadButton.addActionListener(e -> loadSelected());
        nameField.setToolTipText(getText("bindings.bindsFile.newName.tooltip"));
        renameButton.setToolTipText(getText("bindings.bindsFile.rename.tooltip"));
        // WHY two equal halves: choosing a file on the left, renaming it on the right (Alan, 2026-11-04).
        body().add(half(fileBox, loadButton));
        body().add(half(nameField, renameButton));
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
        String shown = bindEditor.shown();
        Function<String, Runnable> empty = reason -> () -> shown(List.of(), null);
        FlowSteps.inBackground("BindForge-BindsFileList", log, "The Bindings File list's read", () -> {
            ConfigurationFiles current = Edits.stored().snapshot().current();
            List<String> files = BindsEdits.bindsFiles(current);
            String selected = files.isEmpty() ? null
                    : shown != null && files.contains(shown)
                    ? shown
                    : BindsEdits.generalFile(current).orElse(files.getFirst());
            return () -> shown(files, selected);
        }, empty, empty, FlowSteps.NOTHING);
    }

    /** Shows what {@link #refresh} read, then the read asked for while it ran, if any. On the EDT. */
    private void shown(List<String> files, String selected) {
        fileBox.setModel(new DefaultComboBoxModel<>(files.toArray(String[]::new)));
        fileBox.setSelectedItem(selected);
        anyFile = selected != null;
        updateEnabled();
        if (refreshes.finish()) refresh();
    }

    private void updateEnabled() {
        boolean usable = editorsUsable && anyFile;
        fileBox.setEnabled(usable);
        loadButton.setEnabled(usable);
        nameField.setEnabled(usable);
        // WHY always off: what RENAME does is still to be settled with Alan (2026-11-04) - the box and button are laid
        // out first.
        renameButton.setEnabled(false);
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
        Object chosen = fileBox.getSelectedItem();
        if (chosen == null || bindEditor == null) return;
        bindEditor.load(chosen.toString());
    }
}
