package elite.intel.ui.screen.bindings;

import elite.intel.gameapi.DataDirectoryValidator;
import elite.intel.session.PlayerSession;
import elite.intel.ui.theme.HudPalette;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.theme.AppTheme.hudReadoutLabel;
import static elite.intel.ui.theme.AppTheme.makeFieldButton;
import static elite.intel.ui.theme.HudGlyphs.verticalEllipsisIcon;
import static elite.intel.ui.theme.HudPalette.HUD_FIELD_HEIGHT;

/**
 * The User Configuration Folder - the one folder of {@code .binds} files every installation shares - and the ⋮ that
 * changes it, Elite-Intel's own bindings setting. It sits on the {@link ApplyBar}, left of DISCARD and APPLY, above
 * every BindForge tab (Alan, 2026-11-08: moved up from Game Install Locations).
 * <p>
 * The field is read-only; the folder is changed only through the ⋮. A missing folder is shown in warning colour, so the
 * state reads even when a long path is cut short - the tooltip carries all of it.
 */
public final class BindingsFolderField extends JPanel {

    /**
     * Wide enough for most of a path, narrow enough to leave the bar its status line.
     */
    private static final int MAX_FIELD_WIDTH = 420;

    private final JTextField field = new JTextField() {
        @Override
        public Dimension getPreferredSize() {
            Dimension preferred = super.getPreferredSize();
            return new Dimension(Math.min(preferred.width, MAX_FIELD_WIDTH), preferred.height);
        }
    };
    private final Color fieldForeground;
    private final Runnable folderChanged;

    /**
     * @param folderChanged run on the EDT after the user picks a usable folder - the screens that read the folder read
     *                      it again. The working copy does not change with the folder.
     */
    public BindingsFolderField(Runnable folderChanged) {
        super(new FlowLayout(FlowLayout.LEFT, HudPalette.HUD_GAP, 0));
        this.folderChanged = folderChanged;
        setOpaque(false);
        field.setEditable(false);
        fieldForeground = field.getForeground();

        JButton chooseButton = makeFieldButton(verticalEllipsisIcon(HUD_FIELD_HEIGHT), HUD_FIELD_HEIGHT);
        chooseButton.setToolTipText(getText("player.bindingsDirectory.select.tooltip"));
        chooseButton.addActionListener(e -> selectBindingsDirectory());

        add(hudReadoutLabel(getText("bindings.installLocations.section.configFolder")));
        add(field);
        add(chooseButton);
    }

    /**
     * Reads the folder and whether it is there off the EDT - a folder on a disconnected drive takes its time.
     */
    public void refresh() {
        Thread.ofPlatform().name("BindForge-BindingsFolder").daemon().start(() -> {
            Path bindingsDir = PlayerSession.getInstance().getBindingsDir();
            boolean found = bindingsDir != null && Files.isDirectory(bindingsDir);
            SwingUtilities.invokeLater(() -> show(bindingsDir, found));
        });
    }

    private void show(Path bindingsDir, boolean found) {
        String text = bindingsDir == null
                ? getText("bindings.installLocations.configFolder.unset")
                : bindingsDir + "   -   " + getText(found
                ? "bindings.installLocations.state.found"
                : "bindings.installLocations.state.missing");
        field.setText(text);
        field.setCaretPosition(0);
        field.setToolTipText(text);
        field.setForeground(found ? fieldForeground : HudPalette.HUD_COLOR_ROLE_WARNING);
        // WHY: the bar is a FlowLayout - a new path changes the field's width.
        revalidate();
    }

    /**
     * The folder chooser, with the same setting and checks the row had in Game Install Locations.
     */
    private void selectBindingsDirectory() {
        PlayerSession playerSession = PlayerSession.getInstance();
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle(getText("player.bindingsDirectory.dialog"));
        String current = playerSession.getBindingsDir().toString();
        if (!current.isBlank()) chooser.setCurrentDirectory(new File(current).getParentFile());
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        if (!playerSession.setBindingsDir(chooser.getSelectedFile().getAbsolutePath())) {
            DataDirectoryValidator.warnUnusable(DataDirectoryValidator.DirectoryKind.BINDINGS);
            return;
        }
        DataDirectoryValidator.validateAndWarn(playerSession.getBindingsDir(), DataDirectoryValidator.DirectoryKind.BINDINGS);
        refresh();
        folderChanged.run();
    }
}
