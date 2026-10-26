package elite.intel.ui.screen.bindings;

import elite.intel.bindforge.storage.Backup;
import elite.intel.bindforge.storage.BackupContents;
import elite.intel.bindforge.storage.FirstTimeStartup;
import elite.intel.bindforge.storage.InstallationFiles;
import elite.intel.bindforge.storage.Restore;
import elite.intel.bindforge.storage.WriteReport;
import elite.intel.bindforge.storage.Written;
import elite.intel.ui.dialog.HudConfirmDialog;
import elite.intel.util.StringUtls;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;

/**
 * RESTORE on screen (storage.md rule 10; file-manager.md, <em>Restore</em>): read the backup, refuse it if it cannot
 * be restored here, reconcile when its installations differ, one confirmation, then the write - and the report, per
 * place. Used by Player Backups (RESTORE, Browse for Backup File) and by the BindForge tab when the working copy
 * cannot be read.
 * <p>
 * <strong>One confirmation</strong> (Alan, 2026-10-25): the reconcile's summary when one is needed, otherwise RESTORE's
 * own. Either names every installation it replaces, marks any not found, and says that edits on screen are thrown
 * away. Nothing is written to a game file until the user confirms. The disk work runs off the EDT; the dialogs on it.
 */
final class RestoreFlow {

    private static final Logger log = LogManager.getLogger(RestoreFlow.class);

    private static final DateTimeFormatter TAKEN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Component parent;

    RestoreFlow(Component parent) {
        this.parent = parent;
    }

    /**
     * Restores {@code zip}. On the EDT.
     *
     * @param browsed    whether the user browsed to it - its confirmation then says to restore only their own
     * @param onFinished run on the EDT once the flow ends, however it ends
     */
    void restore(Path zip, boolean browsed, Runnable onFinished) {
        background("BindForge-RestoreRead", onFinished, () -> {
            Restore restore = Restore.stored();
            Restore.Prepared prepared;
            try {
                prepared = restore.prepare(BackupContents.read(zip));
            } catch (BackupContents.NotABackupException e) {
                log.info("Not restored: {}", e.getMessage());
                return () -> tell(getText("bindings.restore.notABackup", zip.getFileName()), onFinished);
            } catch (FirstTimeStartup.NoBindingsYetException e) {
                log.info("Not restored: {}", e.getMessage());
                return () -> tell(getText(FirstTimeStartup.NoBindingsYetException.ADVICE_KEY), onFinished);
            } catch (Restore.MissingHalfException e) {
                log.info("Not restored, {}: {}", zip, e.getMessage());
                String key = e.half() == Restore.Half.BINDINGS ? "bindings.restore.missing.bindings"
                        : "bindings.restore.missing.deviceFiles";
                return () -> tell(getText(key), onFinished);
            }
            return () -> confirm(restore, prepared, browsed, onFinished);
        });
    }

    /**
     * The working copy could not be read: says so, and offers the newest backup (storage.md rule 10). With no backup
     * at all, the BindForge tab offers first-time startup instead ({@link FirstTimeStartupFlow}). On the EDT.
     *
     * @param newest the newest backup
     */
    void offerNewest(String why, Backup newest, Runnable onFinished) {
        Objects.requireNonNull(newest, "newest");
        String taken = newest.taken().format(TAKEN);
        if (!HudConfirmDialog.confirm(parent, getText("bindings.restore.title"),
                getText("bindings.restore.unreadable.text", why, taken),
                getText("bindings.restore.confirm.button"), getText("bindings.restore.notNow"))) {
            onFinished.run();
            return;
        }
        restore(newest.zip(), false, onFinished);
    }

    private void confirm(Restore restore, Restore.Prepared prepared, boolean browsed, Runnable onFinished) {
        String trailer = trailer(prepared, browsed);
        Optional<FirstSetupDialog.Choice> choice;
        if (prepared.identical()) {
            choice = HudConfirmDialog.confirm(parent, getText("bindings.restore.title"), trailer,
                    getText("bindings.restore.confirm.button"), getText("button.cancel"))
                    ? Optional.of(new FirstSetupDialog.Choice(prepared.files(), 0))
                    : Optional.empty();
        } else {
            FirstSetupDialog.Wording wording = new FirstSetupDialog.Wording(getText("bindings.restore.title"),
                    getText("bindings.restore.reconcile.intro"), getText("bindings.restore.reconcile.backupBefore"),
                    getText("bindings.restore.title"), getText("bindings.restore.confirm.button"), trailer);
            choice = new FirstSetupDialog(parent, prepared.reading(), namesOf(prepared), wording).showDialog();
        }
        if (choice.isEmpty()) {
            onFinished.run();
            return;
        }
        FirstSetupDialog.Choice chosen = choice.get();
        background("BindForge-RestoreWrite", onFinished, () -> {
            String message;
            try {
                Restore.Restored restored = restore.write(prepared.contents(), chosen.files());
                message = restoredText(restored, chosen.moved());
            } catch (IOException | RuntimeException e) {
                // WHY: broad on purpose - a thread boundary. The backup or the working copy failed, so no game file
                // was written.
                log.error("RESTORE stopped before writing the game files", e);
                message = getText("bindings.restore.result.failed", StringUtls.messageOf(e));
            }
            String told = message;
            return () -> tell(told, onFinished);
        });
    }

    /** What RESTORE replaces, where, and that edits on screen are thrown away - the one confirmation's text. */
    private static String trailer(Restore.Prepared prepared, boolean browsed) {
        String installations = prepared.targets().isEmpty()
                ? getText("bindings.restore.confirm.noInstallations")
                : prepared.targets().stream().map(RestoreFlow::placeLine).collect(Collectors.joining("\n"));
        String taken = prepared.contents().taken() != null ? prepared.contents().taken()
                : String.valueOf(prepared.contents().zip().getFileName());
        String text = getText("bindings.restore.confirm.text", taken, prepared.bindingsFolder(), installations);
        return browsed ? text + "\n\n" + getText("bindings.restore.ownBackupsOnly") : text;
    }

    /** Formats only: whether the folder is there was decided off the EDT ({@link Restore.Target#found()}). */
    private static String placeLine(Restore.Target target) {
        String name = getText("bindings.firstTimeStartup.place.installation", target.installation().storefront(),
                target.installation().root());
        return target.found() ? "- " + name : "- " + getText("bindings.restore.confirm.notFound", name);
    }

    /** What the backup's installations are called in the reconcile, keyed by their number in it. */
    private static Map<String, String> namesOf(Restore.Prepared prepared) {
        Map<String, String> names = new LinkedHashMap<>();
        for (InstallationFiles installation : prepared.contents().installations()) {
            names.put(String.valueOf(installation.installId()), getText("bindings.firstTimeStartup.place.installation",
                    installation.storefront(), installation.root()));
        }
        return names;
    }

    /** The report: where the old files went, how many installations took it, bindings a merge moved, what failed. */
    private static String restoredText(Restore.Restored restored, int moved) {
        Written written = restored.written();
        WriteReport report = written.report();
        StringBuilder text = new StringBuilder(getText("bindings.restore.result.written",
                written.backup().zip().toString(), report.installationsUpdated(), report.installations().size()));
        if (moved > 0) text.append('\n').append(getText("bindings.firstTimeStartup.result.moved", moved));
        text.append(FirstTimeStartupFlow.placeFailures(report));
        if (restored.setUpAskedAgain()) text.append('\n').append(getText("bindings.restore.result.setUpAgain"));
        return text.toString();
    }

    private void tell(String message, Runnable onFinished) {
        HudConfirmDialog.info(parent, getText("bindings.restore.title"), message,
                getText("bindings.aliasDesigner.firstSetup.close"));
        onFinished.run();
    }

    /** Disk work that hands back the next step for the EDT. */
    @FunctionalInterface
    private interface Step {
        Runnable run() throws IOException;
    }

    private void background(String name, Runnable onFinished, Step step) {
        new Thread(() -> {
            Runnable next;
            try {
                next = step.run();
            } catch (IOException e) {
                log.warn("RESTORE could not read the backup", e);
                String reason = StringUtls.messageOf(e);
                next = () -> tell(getText("bindings.restore.readFailed", reason), onFinished);
            } catch (RuntimeException e) {
                // WHY: broad on purpose - a thread boundary; escaping, it would leave Player Backups busy for good. A
                // RuntimeException here is a bug, not a bad backup, so it is logged as one and not blamed on the
                // backup (review, 2026-10-25).
                log.error("RESTORE stopped", e);
                String reason = StringUtls.messageOf(e);
                next = () -> tell(getText("bindings.restore.stopped", reason), onFinished);
            }
            SwingUtilities.invokeLater(next);
        }, name).start();
    }
}
