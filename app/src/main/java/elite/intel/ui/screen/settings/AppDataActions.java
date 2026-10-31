package elite.intel.ui.screen.settings;

import elite.intel.appdata.AppDataBackup;
import elite.intel.appdata.AppDataRestore;
import elite.intel.eventbus.UiBus;
import elite.intel.session.SystemSession;
import elite.intel.ui.dialog.AppDataPasswordDialog;
import elite.intel.ui.dialog.HudConfirmDialog;
import elite.intel.ui.event.SystemShutDownEvent;
import elite.intel.util.AppPaths;
import elite.intel.util.AppRelauncher;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;

/**
 * The Settings footer's Backup Settings and Data and Restore buttons: the dialogs around {@link AppDataBackup} and
 * {@link AppDataRestore}.
 * <p>
 * Choosing files and passwords happens on the EDT; the disk work does not, because a database snapshot runs to
 * hundreds of megabytes and stretching a password takes a noticeable moment.
 */
public final class AppDataActions {

    private static final Logger log = LogManager.getLogger(AppDataActions.class);

    private AppDataActions() {
    }

    /**
     * Asks for a password and a file, then writes the backup off the EDT.
     */
    public static void backUp(Component parent) {
        AppDataPasswordDialog.Choice choice = AppDataPasswordDialog.choose(parent);
        if (choice instanceof AppDataPasswordDialog.Choice.Cancelled) return;
        char[] password = choice instanceof AppDataPasswordDialog.Choice.Password p ? p.password() : null;

        Path target = chooseBackupFile(parent);
        if (target == null) {
            clear(password);
            return;
        }
        String version = SystemSession.getInstance().readVersionFromResources();
        Thread.ofVirtual().name("app-data-backup").start(() -> writeBackup(parent, target, password, version));
    }

    /**
     * Asks for a backup, then checks, unlocks and stages it off the EDT, and restarts to finish.
     */
    public static void restore(Component parent) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(getText("settings.appData.restore.title"));
        chooser.setFileFilter(new FileNameExtensionFilter("Zip (*.zip)", "zip"));
        if (chooser.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION) return;
        Path zip = chooser.getSelectedFile().toPath();
        if (!HudConfirmDialog.confirm(parent, getText("settings.appData.restore.title"),
                getText("settings.appData.restore.confirm"),
                getText("settings.appData.restore.button"), getText("button.cancel"))) {
            return;
        }
        Thread.ofVirtual().name("app-data-restore").start(() -> stageRestore(parent, zip));
    }

    private static Path chooseBackupFile(Component parent) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(getText("settings.appData.backup.title"));
        chooser.setSelectedFile(new File("elite_intel_backup_"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".zip"));
        chooser.setFileFilter(new FileNameExtensionFilter("Zip (*.zip)", "zip"));
        if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) return null;
        File chosen = chooser.getSelectedFile();
        if (!chosen.getName().toLowerCase(Locale.ROOT).endsWith(".zip")) {
            chosen = new File(chosen.getAbsolutePath() + ".zip");
        }
        return chosen.toPath();
    }

    /**
     * Runs on the backup thread, so it reports every outcome and lets nothing escape.
     * <p>
     * WHY the broad catch: this is a thread boundary, and an exception leaving it would close the file chooser on
     * a commander who never learns whether their backup exists.
     */
    private static void writeBackup(Component parent, Path target, char[] password, String version) {
        try {
            AppDataBackup.write(AppPaths.getAppDataDir(), Path.of(AppPaths.getSecretKeyFile()), target, password, version);
            info(parent, "settings.appData.backup.title", getText(password == null
                    ? "settings.appData.backup.doneWithoutKeys"
                    : "settings.appData.backup.done", target.getFileName().toString()));
        } catch (IOException | RuntimeException e) {
            log.error("Backup Settings and Data failed: {}", target, e);
            info(parent, "settings.appData.backup.title", getText("settings.appData.backup.failed", String.valueOf(e.getMessage())));
        } finally {
            clear(password);
        }
    }

    /**
     * Runs on the restore thread - see {@link #writeBackup} on why it lets nothing escape.
     */
    private static void stageRestore(Component parent, Path zip) {
        AppDataRestore.Staged staged = null;
        try {
            staged = AppDataRestore.stage(zip, AppPaths.getAppDataDir());
            if (staged.hasLockedKey() && !unlockKey(parent, staged)) {
                staged.discard();
                return;
            }
            staged.commit(Path.of(AppPaths.getSecretKeyFile()));
            staged = null;
            restart(parent);
        } catch (AppDataRestore.RefusedException e) {
            log.warn("Restore refused {}: {}", zip, e.getMessage());
            info(parent, "settings.appData.restore.title", getText(switch (e.refusal()) {
                case NOT_A_BACKUP -> "settings.appData.restore.notABackup";
                case NEWER_VERSION -> "settings.appData.restore.newerVersion";
            }));
        } catch (IOException | RuntimeException e) {
            log.error("Restore failed: {}", zip, e);
            info(parent, "settings.appData.restore.title", getText("settings.appData.restore.stageFailed", String.valueOf(e.getMessage())));
            discardQuietly(staged);
        }
    }

    /**
     * Asks for the key's password until it opens the key or the commander goes on without it.
     *
     * @return false when the commander cancelled the restore
     */
    private static boolean unlockKey(Component parent, AppDataRestore.Staged staged) throws IOException {
        String errorText = null;
        while (true) {
            String shownError = errorText;
            AppDataPasswordDialog.Choice choice = onEdt(() -> AppDataPasswordDialog.enter(parent, shownError));
            switch (choice) {
                case AppDataPasswordDialog.Choice.Cancelled c -> {
                    return false;
                }
                case AppDataPasswordDialog.Choice.WithoutKeys w -> {
                    return true;
                }
                case AppDataPasswordDialog.Choice.Password p -> {
                    boolean opened = staged.unlock(p.password());
                    clear(p.password());
                    if (opened) return true;
                    errorText = getText("settings.appData.password.wrong");
                }
            }
        }
    }

    private static void restart(Component parent) {
        if (AppRelauncher.relaunch()) {
            UiBus.publish(new SystemShutDownEvent());
            return;
        }
        info(parent, "settings.appData.restore.title", getText("settings.appData.restore.restartByHand"));
    }

    private static void discardQuietly(AppDataRestore.Staged staged) {
        if (staged == null) return;
        try {
            staged.discard();
        } catch (IOException e) {
            log.warn("Could not remove the staged restore", e);
        }
    }

    private static void info(Component parent, String titleKey, String message) {
        onEdt(() -> {
            HudConfirmDialog.info(parent, getText(titleKey), message, getText("button.ok"));
            return null;
        });
    }

    private static <T> T onEdt(Supplier<T> work) {
        AtomicReference<T> result = new AtomicReference<>();
        try {
            SwingUtilities.invokeAndWait(() -> result.set(work.get()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted waiting for the commander", e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException("A dialog failed", e.getCause());
        }
        return result.get();
    }

    private static void clear(char[] password) {
        if (password != null) Arrays.fill(password, '\0');
    }
}
