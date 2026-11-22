package elite.intel.ui.screen.bindings;

import elite.intel.bindforge.install.InstallationRegistry;
import elite.intel.db.dao.BindForgeInstallationsDao.InstallationRow;
import elite.intel.ui.dialog.HudConfirmDialog;
import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.widget.HudBanner;
import elite.intel.ui.widget.HudModalSpec;
import elite.intel.ui.widget.HudTable;
import elite.intel.ui.widget.StatusBadge;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.nio.file.Path;
import java.util.List;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.theme.AppTheme.makeButton;
import static elite.intel.ui.theme.AppTheme.makeButtonSubtle;
import static elite.intel.ui.theme.AppTheme.transparentPanel;

/**
 * First-time startup's first step (storage.md rule 1): every installation Elite-Intel knows about, with
 * <strong>Confirm</strong> and <strong>Add Missing</strong>. Always shown first, so a missed installation can be caught
 * (alias-designer.md, <em>Activation</em>). Add Missing is checked for the game's files before it is listed; the list
 * is the stored one, which Game Install Locations shows too. Writes nothing to a game file.
 */
final class InstallationListDialog extends JDialog {

    private final InstallationRegistry registry;
    private DefaultTableModel model;
    private JTable table;
    private boolean confirmed;

    InstallationListDialog(Component parent, InstallationRegistry registry, List<InstallationRow> rows) {
        super(SwingUtilities.getWindowAncestor(parent), ModalityType.APPLICATION_MODAL);
        setUndecorated(true);
        this.registry = registry;
        buildUi(rows);
    }

    /** @return whether the user confirmed the list */
    boolean showDialog() {
        AppTheme.runWithModalScrim(getOwner(), () -> setVisible(true));
        return confirmed;
    }

    private void buildUi(List<InstallationRow> rows) {
        JPanel body = transparentPanel(new BorderLayout(0, 8));
        body.add(HudBanner.multiline(getText("bindings.firstTimeStartup.installations.intro"), StatusBadge.State.INFO),
                BorderLayout.NORTH);

        model = new DefaultTableModel(new Object[]{
                getText("bindings.installLocations.column.storefront"),
                getText("bindings.installLocations.column.location"),
                getText("bindings.installLocations.column.status")}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(model);
        HudTable.style(table);
        body.add(HudTable.dataPlaneScrollPane(table), BorderLayout.CENTER);
        show(rows);

        JButton confirm = makeButton(getText("bindings.firstTimeStartup.installations.confirm"));
        confirm.addActionListener(e -> {
            confirmed = true;
            dispose();
        });
        JButton addMissing = makeButtonSubtle(getText("bindings.installLocations.button.addMissing"));
        addMissing.addActionListener(e -> addMissing());
        JButton cancel = makeButtonSubtle(getText("button.cancel"));
        cancel.addActionListener(e -> dispose());

        HudModalSpec spec = HudModalSpec.builder()
                .title(getText("bindings.firstTimeStartup.title"))
                .onClose(this::dispose)
                .body(body)
                .scrollBody(false)
                .primary(confirm)
                .extra(addMissing)
                .dismiss(cancel)
                .build();
        setContentPane(AppTheme.hudModalScaffold(spec));
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        pack();
        setSize(Math.max(getWidth(), 720), Math.max(getHeight(), 360));
        setLocationRelativeTo(getOwner());
    }

    private void show(List<InstallationRow> rows) {
        model.setRowCount(0);
        for (InstallationRow row : rows) {
            model.addRow(new Object[]{row.storefront(), row.rootPath(), getText(row.missing()
                    ? "bindings.installLocations.state.missing"
                    : "bindings.installLocations.state.found")});
        }
        if (rows.isEmpty()) {
            model.addRow(new Object[]{getText("bindings.firstTimeStartup.installations.none"), "", ""});
        }
        HudTable.fitColumnsToContent(table, 420);
    }

    /** Browses to an installation's root folder, checked for the game's files, and adds it to the list. */
    private void addMissing() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(getText("bindings.installLocations.add.chooserTitle"));
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Path chosen = chooser.getSelectedFile().toPath();
        try {
            registry.addByHand(chosen);
        } catch (IllegalArgumentException e) {
            // WHY: the folder is the user's choice and refusing it is an ordinary outcome, not a fault - they are told
            // which folder and what was expected.
            HudConfirmDialog.info(this, getText("bindings.installLocations.add.rejected.title"),
                    getText("bindings.installLocations.add.rejected.text", chosen.toString()), getText("button.ok"));
            return;
        }
        show(registry.current());
    }
}
