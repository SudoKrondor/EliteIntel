package elite.intel.ui.screen.bindings;

import elite.intel.bindforge.install.GameInstallationProvider;
import elite.intel.bindforge.install.InstallationRegistry;
import elite.intel.bindforge.storage.Backup;
import elite.intel.bindforge.storage.Backups;
import elite.intel.bindforge.storage.ConfigurationFiles;
import elite.intel.bindforge.storage.FirstSetupFiles;
import elite.intel.bindforge.storage.FirstSetupReading;
import elite.intel.bindforge.storage.FirstTimeStartup;
import elite.intel.bindforge.storage.WorkingCopy;
import elite.intel.bindforge.storage.WriteReport;
import elite.intel.bindforge.storage.Written;
import elite.intel.db.dao.BindForgeInstallationsDao.InstallationRow;
import elite.intel.session.PlayerSession;
import elite.intel.ui.dialog.HudConfirmDialog;
import elite.intel.util.StringUtls;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;

/**
 * Runs first-time startup from a click on the BindForge tab while the flag is not set (storage.md rules 1-4): the
 * installation list, the reconcile when the installations differ, a summary, then the write - and the report, per
 * place. Abandoned at any step, it is offered again at the next click (Alan, 2026-10-25). Nothing is written to a
 * game file until the user confirms the summary.
 * <p>
 * The disk work runs off the EDT; the dialogs on it. One run at a time: a click while one is under way does nothing.
 */
public final class FirstTimeStartupFlow {

    private static final Logger log = LogManager.getLogger(FirstTimeStartupFlow.class);

    private final Component parent;
    private final AtomicBoolean running = new AtomicBoolean();

    public FirstTimeStartupFlow(Component parent) {
        this.parent = parent;
    }

    /**
     * Offers first-time startup if its flag is not set; once it is, reads the working copy, and offers the newest backup
     * when it cannot be read (storage.md rule 10; Alan, 2026-10-25). On the EDT.
     */
    public void offerIfNeeded() {
        if (!running.compareAndSet(false, true)) return;
        background("BindForge-FirstTimeStartup", () -> {
            FirstTimeStartup startup = FirstTimeStartup.stored();
            if (!startup.needed()) return workingCopyCheck(startup);
            List<InstallationRow> rows = startup.installations();
            return () -> showList(startup, rows);
        });
    }

    private void showList(FirstTimeStartup startup, List<InstallationRow> rows) {
        InstallationRegistry registry = new InstallationRegistry(
                GameInstallationProvider.forThisPlatform(PlayerSession.getInstance().getBindingsDir()));
        if (!new InstallationListDialog(parent, registry, rows).showDialog()) {
            finish();
            return;
        }
        background("BindForge-FirstTimeStartupRead", () -> {
            FirstSetupReading reading;
            try {
                reading = startup.read();
            } catch (FirstTimeStartup.NoBindingsYetException e) {
                // WHY: a new player, before the training - nothing of theirs exists yet, so the flag stays unset and
                // the next click offers it again (Alan, 2026-10-25). Told in the setup check's own words.
                log.info("First-time startup waits for the bindings folder: {}", e.getMessage());
                return () -> tell(getText(FirstTimeStartup.NoBindingsYetException.ADVICE_KEY));
            } catch (FirstTimeStartup.NoInstallationReachableException e) {
                log.info("First-time startup waits for an installation: {}", e.getMessage());
                String which = e.unreachable().stream()
                        .map(row -> getText("bindings.firstTimeStartup.place.installation", row.storefront(),
                                row.rootPath()))
                        .collect(Collectors.joining(", "));
                return () -> tell(getText("bindings.firstTimeStartup.noneReachable", which));
            } catch (IOException e) {
                // WHY: a file that is there but cannot be read stops first-time startup, and is named: it could hold the
                // one entry the working copy must not lose.
                log.warn("First-time startup could not read the files", e);
                return () -> tell(getText("bindings.aliasDesigner.firstSetup.readFailed", StringUtls.messageOf(e)));
            } catch (RuntimeException e) {
                // WHY: broad on purpose - a thread boundary. A bug, not an unreadable file, so not blamed on one
                // (review, 2026-10-25).
                log.error("First-time startup stopped while reading the files", e);
                return () -> tell(getText("bindings.firstTimeStartup.stopped", StringUtls.messageOf(e)));
            }
            Map<String, String> names = labelsFor(startup.installations());
            return () -> reconcile(startup, reading, names);
        });
    }

    /**
     * Nothing to do when the working copy reads; otherwise the step that says so and offers the newest backup - or,
     * with no backup at all, first-time startup again from the live files (Alan, 2026-10-25, from the S3 review), the
     * one way out once Run first-time startup again is gone. A working copy that is missing while first-time startup is
     * complete counts as one that cannot be read.
     */
    private Runnable workingCopyCheck(FirstTimeStartup startup) {
        try {
            WorkingCopy.stored().read();
            return null;
        } catch (IOException e) {
            log.error("The working copy could not be read", e);
            String why = e instanceof NoSuchFileException ? getText("bindings.restore.unreadable.missing")
                    : StringUtls.messageOf(e);
            Backup newest = newestBackup();
            if (newest == null) return () -> offerSetUpAgain(startup, why);
            return () -> new RestoreFlow(parent).offerNewest(why, newest, this::finish);
        }
    }

    /** No backup to restore the working copy from: first-time startup again, from the live files, if the user agrees. */
    private void offerSetUpAgain(FirstTimeStartup startup, String why) {
        if (!HudConfirmDialog.confirm(parent, getText("bindings.restore.title"),
                getText("bindings.restore.unreadable.noBackup", why),
                getText("bindings.restore.unreadable.setUpAgain"), getText("bindings.restore.notNow"))) {
            finish();
            return;
        }
        background("BindForge-FirstTimeStartupAgain", () -> {
            // WHY: the flag is cleared first, so abandoning the list below leaves first-time startup offered at the next
            // click - as for any first-time startup not finished.
            startup.runAgain();
            List<InstallationRow> rows = startup.installations();
            return () -> showList(startup, rows);
        });
    }

    /** The newest backup of any kind, or {@code null} when there is none or the backup folder cannot be listed. */
    private static Backup newestBackup() {
        try {
            List<Backup> backups = Backups.stored().list();
            return backups.isEmpty() ? null : backups.getFirst();
        } catch (IOException e) {
            log.warn("Could not list the backups to offer one", e);
            return null;
        }
    }

    /** Identical installations ask nothing but the summary; any difference opens the reconcile (rule 2). */
    private void reconcile(FirstTimeStartup startup, FirstSetupReading reading, Map<String, String> names) {
        Optional<FirstSetupDialog.Choice> choice;
        if (reading.identical()) {
            ConfigurationFiles files = FirstSetupFiles.identical(reading);
            String summary = getText("bindings.firstTimeStartup.summary.identical", files.buttonMaps().size())
                    + "\n\n" + getText("bindings.firstTimeStartup.summary.installations",
                    reading.installations().size());
            choice = HudConfirmDialog.confirm(parent, getText("bindings.firstTimeStartup.summary.title"), summary,
                    getText("bindings.firstTimeStartup.summary.create"),
                    getText("bindings.aliasDesigner.firstSetup.cancel"))
                    ? Optional.of(new FirstSetupDialog.Choice(files, 0))
                    : Optional.empty();
        } else {
            choice = new FirstSetupDialog(parent, reading, names).showDialog();
        }
        if (choice.isEmpty()) {
            finish();
            return;
        }
        FirstSetupDialog.Choice chosen = choice.get();
        background("BindForge-FirstTimeStartupWrite", () -> {
            String message;
            try {
                Written written = startup.write(chosen.files());
                message = writtenText(written, chosen.moved());
            } catch (Backups.NothingToBackUpException e) {
                log.warn("First-time startup wrote nothing: there was nothing to back up");
                message = getText(Backups.NothingToBackUpException.ADVICE_KEY);
            } catch (IOException | RuntimeException e) {
                // WHY: broad on purpose - a thread boundary. Nothing was written to a game file, and the flag is unset.
                log.error("First-time startup stopped before writing the game files", e);
                message = getText("bindings.firstTimeStartup.result.failed", StringUtls.messageOf(e));
            }
            String told = message;
            return () -> tell(told);
        });
    }

    /** The report, per place: <em>"updated 2 of 2"</em>, or which one failed and why (alias-designer.md, step 5). */
    private static String writtenText(Written written, int moved) {
        WriteReport report = written.report();
        StringBuilder text = new StringBuilder(getText("bindings.firstTimeStartup.result.written",
                written.backup().zip().toString(), report.installationsUpdated(), report.installations().size()));
        if (moved > 0) text.append('\n').append(getText("bindings.firstTimeStartup.result.moved", moved));
        return text.append(placeFailures(report)).toString();
    }

    /**
     * Each place a write did not fully update, one line each - empty when every place took everything. RESTORE reports
     * the same way. <strong>Only the bindings folder is promised to the next APPLY</strong> (Alan, 2026-10-27): an
     * installation a write missed, or one that kept a read-only file, no longer matches the working copy, so APPLY
     * leaves it as it is until the check reconciles it (storage.md, <em>How the files are written</em>; Storage S9).
     */
    static String placeFailures(WriteReport report) {
        StringBuilder text = new StringBuilder();
        if (!report.bindings().updated()) {
            text.append('\n').append(getText("bindings.firstTimeStartup.result.place",
                    getText("bindings.firstTimeStartup.place.bindingsFolder"),
                    String.join("; ", report.bindings().failures())));
            text.append('\n').append(getText("bindings.firstTimeStartup.result.nextApply"));
        }
        for (WriteReport.Place place : report.installations()) {
            if (place.updated()) continue;
            String name = getText("bindings.firstTimeStartup.place.installation", place.storefront(), place.folder());
            if (place.differs()) {
                text.append('\n').append(getText("bindings.applyBar.result.differs", name));
                continue;
            }
            if (!place.failures().isEmpty()) {
                text.append('\n').append(getText("bindings.firstTimeStartup.result.place", name,
                        String.join("; ", place.failures())));
            }
            // WHY: worded here, in the user's language - it is the one line that tells them what to do (review,
            // 2026-10-25). The failure lines are the system's own words, for support.
            if (!place.keptReadOnly().isEmpty()) {
                text.append('\n').append(getText("bindings.firstTimeStartup.result.keptReadOnly", name,
                        String.join(", ", place.keptReadOnly())));
            }
            text.append('\n').append(getText("bindings.firstTimeStartup.result.leftAsItIs", name));
        }
        return text.toString();
    }

    /**
     * What each installation is called in the reconcile, keyed by its id: the storefront alone where it identifies one
     * installation, and the storefront with its folder where it does not - so two Frontier copies read as two different
     * things. <em>Moved here from the Alias Designer's reader, which no longer names installations (Storage S4a).</em>
     */
    static Map<String, String> labelsFor(List<InstallationRow> rows) {
        Map<String, Long> countByStorefront = rows.stream()
                .collect(Collectors.groupingBy(InstallationRow::storefront, Collectors.counting()));
        Map<String, String> labels = new LinkedHashMap<>();
        for (InstallationRow row : rows) {
            boolean storefrontIsEnough = countByStorefront.getOrDefault(row.storefront(), 0L) == 1;
            labels.put(String.valueOf(row.id()), storefrontIsEnough
                    ? row.storefront()
                    : row.storefront() + " (" + row.rootPath() + ")");
        }
        return labels;
    }

    private void tell(String message) {
        HudConfirmDialog.info(parent, getText("bindings.firstTimeStartup.title"), message,
                getText("bindings.aliasDesigner.firstSetup.close"));
        finish();
    }

    private void finish() {
        running.set(false);
    }

    /** Disk work that hands back the next step for the EDT, or {@code null} for none. */
    @FunctionalInterface
    private interface Step {
        Runnable run() throws IOException;
    }

    private void background(String name, Step step) {
        new Thread(() -> {
            Runnable next;
            try {
                next = step.run();
            } catch (IOException e) {
                log.warn("First-time startup stopped", e);
                String reason = StringUtls.messageOf(e);
                next = () -> tell(getText("bindings.firstTimeStartup.result.failed", reason));
            } catch (RuntimeException e) {
                // WHY: broad on purpose - a thread boundary; escaping, it would leave the flow marked running, and
                // first-time startup would never be offered again this run. A RuntimeException here is a bug, so it is
                // logged as one and said to have stopped unexpectedly (review, 2026-10-25).
                log.error("First-time startup stopped", e);
                String reason = StringUtls.messageOf(e);
                next = () -> tell(getText("bindings.firstTimeStartup.stopped", reason));
            }
            Runnable then = next;
            SwingUtilities.invokeLater(() -> {
                if (then == null) {
                    finish();
                } else {
                    then.run();
                }
            });
        }, name).start();
    }
}
