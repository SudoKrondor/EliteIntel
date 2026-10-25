package elite.intel.ui.screen.bindings;

import elite.intel.bindforge.install.GameInstallationProvider;
import elite.intel.bindforge.install.InstallationRegistry;
import elite.intel.bindforge.storage.Backups;
import elite.intel.bindforge.storage.ConfigurationFiles;
import elite.intel.bindforge.storage.FirstSetupFiles;
import elite.intel.bindforge.storage.FirstSetupReading;
import elite.intel.bindforge.storage.FirstTimeStartup;
import elite.intel.bindforge.storage.WriteReport;
import elite.intel.db.dao.BindForgeInstallationsDao.InstallationRow;
import elite.intel.session.PlayerSession;
import elite.intel.ui.dialog.HudConfirmDialog;
import elite.intel.util.StringUtls;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

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

    /** Offers first-time startup if its flag is not set. On the EDT. */
    public void offerIfNeeded() {
        if (!running.compareAndSet(false, true)) return;
        background("BindForge-FirstTimeStartup", () -> {
            FirstTimeStartup startup = FirstTimeStartup.stored();
            if (!startup.needed()) return null;
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
            } catch (IOException | RuntimeException e) {
                // WHY: broad on purpose - a thread boundary. A file that is there but cannot be read stops first-time
                // startup, and is named: it could hold the one entry the working copy must not lose.
                log.warn("First-time startup could not read the files", e);
                return () -> tell(getText("bindings.aliasDesigner.firstSetup.readFailed", StringUtls.messageOf(e)));
            }
            Map<String, String> names = AliasDesignerReader.labelsFor(startup.installations());
            return () -> reconcile(startup, reading, names);
        });
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
                FirstTimeStartup.Written written = startup.write(chosen.files());
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
    private static String writtenText(FirstTimeStartup.Written written, int moved) {
        WriteReport report = written.report();
        StringBuilder text = new StringBuilder(getText("bindings.firstTimeStartup.result.written",
                written.backup().zip().toString(), report.installationsUpdated(), report.installations().size()));
        if (moved > 0) text.append('\n').append(getText("bindings.firstTimeStartup.result.moved", moved));
        if (!report.bindings().updated()) {
            text.append('\n').append(getText("bindings.firstTimeStartup.result.place",
                    getText("bindings.firstTimeStartup.place.bindingsFolder"),
                    String.join("; ", report.bindings().failures())));
        }
        for (WriteReport.Place place : report.installations()) {
            if (place.updated()) continue;
            text.append('\n').append(getText("bindings.firstTimeStartup.result.place",
                    getText("bindings.firstTimeStartup.place.installation", place.storefront(), place.folder()),
                    String.join("; ", place.failures())));
        }
        if (!report.complete()) text.append('\n').append(getText("bindings.firstTimeStartup.result.nextApply"));
        return text.toString();
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
            } catch (IOException | RuntimeException e) {
                // WHY: broad on purpose - a thread boundary. Escaping, it would leave the flow marked running, and
                // first-time startup would never be offered again this run.
                log.warn("First-time startup stopped", e);
                String reason = StringUtls.messageOf(e);
                next = () -> tell(getText("bindings.firstTimeStartup.result.failed", reason));
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
