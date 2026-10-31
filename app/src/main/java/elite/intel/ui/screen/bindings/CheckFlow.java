package elite.intel.ui.screen.bindings;

import elite.intel.bindforge.storage.BindingsCheck;
import elite.intel.bindforge.storage.BindingsComparison.Differences;
import elite.intel.bindforge.storage.BindingsComparison.FileDifference;
import elite.intel.bindforge.storage.BindingsComparison.LineItem;
import elite.intel.bindforge.storage.BindingsComparison.Side;
import elite.intel.bindforge.storage.DeviceCheck;
import elite.intel.bindforge.storage.Edits;
import elite.intel.bindforge.storage.FirstSetupFiles;
import elite.intel.bindforge.storage.FirstSetupReading;
import elite.intel.bindforge.storage.WriteReport;
import elite.intel.bindforge.storage.Written;
import elite.intel.db.dao.BindForgeInstallationsDao.InstallationRow;
import elite.intel.session.PlayerSession;
import elite.intel.ui.dialog.HudConfirmDialog;
import elite.intel.util.AppPaths;
import elite.intel.util.StringUtls;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;

/**
 * The check, on screen (storage.md rules 5 and 14): if anything differs, one warning for the whole set, then the three
 * choices - put the working copy back, merge, or take the game's files - and the write, backed up first. It runs at two
 * moments:
 * <ul>
 *   <li><strong>The first click on the BindForge tab in a session</strong> (Storage S8a): a session flag, set when the
 *       check starts, stops it running again until Elite-Intel restarts; Not now leaves it set (Alan, 2026-10-30). A
 *       read that fails is told, and not tried again this session - APPLY still runs the check (Storage S8R, the
 *       S7-S8b review). Edits on screen put it off to the next click, since its write would throw them away.
 *       <strong>The device files are checked here too</strong> (Storage S9a): every installation that differs is named
 *       in the same warning; taking the game's files takes the one installation that differs, or opens first setup's
 *       reconcile, with the working copy as one side, when several do; merge answers the binds rows, then opens the
 *       reconcile. One backup and one write for everything.</li>
 *   <li><strong>APPLY</strong> (Storage S8b; {@link ApplyBar}): after the confirmation; the write is APPLY's, the edits
 *       on top. The rows the edits change are not asked about, and the warning says how many. Not now cancels the
 *       APPLY, leaving the edits on screen (Alan, 2026-10-30). The device files are not judged there until Storage
 *       S9b.</li>
 * </ul>
 * The disk work runs off the EDT; the dialogs on it.
 */
final class CheckFlow {

    private static final Logger log = LogManager.getLogger(CheckFlow.class);

    /** Whether the check has run in this Elite-Intel session (storage.md rule 5). */
    private static final AtomicBoolean CHECKED_THIS_SESSION = new AtomicBoolean();

    /** The write once the user has answered - the check's own, or APPLY's - and how it is told. Off the EDT. */
    interface Writing {
        /** The title of the report box. */
        String title();

        /**
         * Writes what the user settled on.
         *
         * @param devices the device files settled on - {@link DeviceCheck.Answer#WORKING_COPY} when none were asked
         * @return the report - what was written, or why nothing was
         * @throws BindingsCheck.ChangedMeanwhileException if the game's files changed since {@code found} - nothing
         *                                                 was written
         * @throws IOException                             if nothing was written, told with {@link #failed}
         */
        String write(BindingsCheck.Found found, Map<String, Side> answers, DeviceCheck.Answer devices)
                throws IOException;

        /** Nothing was written, for {@code reason}. */
        String failed(String reason);

        /** It stopped part way, for {@code reason} - a bug. */
        String stopped(String reason);
    }

    /** The first click's write: the check's own, with no edits on screen. */
    private static final Writing SETTLE = new Writing() {
        @Override
        public String title() {
            return getText("bindings.check.title");
        }

        @Override
        public String write(BindingsCheck.Found found, Map<String, Side> answers, DeviceCheck.Answer devices)
                throws IOException {
            try {
                StringBuilder report = new StringBuilder(reportText(BindingsCheck.stored().settle(found, answers,
                        devices)));
                devices.moves().forEach((from, to) -> report.append('\n')
                        .append(getText("bindings.aliasDesigner.firstSetup.summary.moves", from, to)));
                return report.toString();
            } catch (BindingsCheck.EditsNotAppliedException e) {
                log.info("The check wrote nothing: {}", e.getMessage());
                return getText("bindings.check.editsNotApplied");
            } catch (Edits.WriteRunningException e) {
                log.info("The check wrote nothing: {}", e.getMessage());
                return getText("bindings.writeRunning");
            }
        }

        @Override
        public String failed(String reason) {
            return getText("bindings.check.failed", reason);
        }

        @Override
        public String stopped(String reason) {
            return getText("bindings.check.stopped", reason);
        }
    };

    /**
     * What one check found, ready for the screen: the reconcile of the device files, worked out off the EDT, when an
     * installation's files can be a side of it.
     *
     * @param reconcile first setup's reading with the working copy as one side - {@code null} when no installation's
     *                  files can be a side
     * @param names     what each side is called, keyed by its id as text
     */
    record Checked(BindingsCheck.Found found, FirstSetupReading reconcile, Map<String, String> names) {
    }

    private final Component parent;
    private final Writing writing;
    private final boolean atApply;

    /** The check at the first click, over {@code parent}. */
    CheckFlow(Component parent) {
        this(parent, SETTLE, false);
    }

    /**
     * @param writing the write once the user has answered
     * @param atApply whether this is APPLY's check - nothing differing then still writes, and the warning says that
     *                closing it cancels the APPLY
     */
    CheckFlow(Component parent, Writing writing, boolean atApply) {
        this.parent = parent;
        this.writing = writing;
        this.atApply = atApply;
    }

    /**
     * The check, once a session: reads and compares the binds and preset files and every installation's device files,
     * and hands back the warning for the EDT, or {@code null} when there is nothing to say. Off the EDT.
     *
     * @param then run on the EDT once the check is over, whatever the user chose
     */
    Runnable firstClick(Runnable then) {
        if (CHECKED_THIS_SESSION.get()) return null;
        if (Edits.stored().hasEdits()) {
            log.info("The check waits for the next BindForge click: there are edits not applied");
            return null;
        }
        if (!CHECKED_THIS_SESSION.compareAndSet(false, true)) return null;
        Checked checked;
        try {
            checked = prepared(BindingsCheck.stored().findWithDeviceFiles());
        } catch (IOException e) {
            log.warn("The check could not compare your control files", e);
            return () -> tell(getText("bindings.check.readFailed", StringUtls.messageOf(e)), then);
        }
        BindingsCheck.Found found = checked.found();
        if (found.none()) {
            log.info("The check found your control files as BindForge last wrote them");
            return null;
        }
        if (!found.anythingToAsk()) {
            // WHY: only installations it could not read at all - named, and left as they are (Alan, 2026-10-31).
            return () -> tell(getText("bindings.check.unreadableOnly", installationLines(found)), then);
        }
        return () -> warn(checked, then);
    }

    /**
     * APPLY's check, once {@code found} is read (Storage S8b): with nothing to ask, the write runs here and now; else the
     * warning, handed back for the EDT. Off the EDT.
     *
     * @param then run on the EDT once it is over - written, cancelled or refused
     * @throws IOException if nothing to ask and the write fails - nothing was written
     */
    Runnable atApply(BindingsCheck.Found found, Runnable then) throws IOException {
        if (found.edited() > 0) {
            log.info("APPLY's check: {} rows that differ are under the edits, which go on top", found.edited());
        }
        if (found.differences().none()) return written(found, Map.of(), DeviceCheck.Answer.WORKING_COPY, false, then);
        return () -> warn(new Checked(found, null, Map.of()), then);
    }

    /**
     * {@code found}, with the device files' reconcile worked out when an installation's files can be a side of it. Off
     * the EDT: it parses the device files and finds the working copy's folder.
     *
     * @throws IOException if a side's files cannot be read for the reconcile, or the working copy's folder found
     */
    private static Checked prepared(BindingsCheck.Found found) throws IOException {
        DeviceCheck.Found devices = found.devices();
        if (devices.sides().isEmpty()) return new Checked(found, null, Map.of());
        String workingCopy = getText("bindings.check.devices.workingCopy");
        FirstSetupReading reading = devices.reading(found.workingCopy(), PlayerSession.getInstance().getBindingsDir(),
                AppPaths.getWorkingCopyDir(), workingCopy);
        List<InstallationRow> rows = devices.sides().stream().map(DeviceCheck.Judged::row).toList();
        Map<String, String> names = new LinkedHashMap<>();
        names.put(String.valueOf(DeviceCheck.WORKING_COPY_SIDE), workingCopy);
        names.putAll(FirstTimeStartupFlow.labelsFor(rows));
        return new Checked(found, reading, names);
    }

    private void warn(Checked checked, Runnable then) {
        BindingsCheck.Found found = checked.found();
        // WHY: with no binds row and no installation's files to take - only a missing or unparseable file - putting
        // the working copy back is all there is to offer (Alan, 2026-10-31).
        boolean choices = !found.differences().none() || checked.reconcile() != null;
        switch (CheckWarningDialog.show(parent, warningText(found, atApply), choices)) {
            case KEEP_MINE -> write(found, everyRow(found, Side.WORKING_COPY), DeviceCheck.Answer.WORKING_COPY,
                    false, then);
            case TAKE_THE_GAMES -> {
                if (found.differences().anyReset() && !sure()) {
                    warn(checked, then);
                    return;
                }
                Optional<DeviceCheck.Answer> devices = theGames(checked);
                if (devices.isEmpty()) {
                    warn(checked, then);
                    return;
                }
                write(found, everyRow(found, Side.LIVE), devices.get(), false, then);
            }
            case MERGE -> merge(checked, Map.of(), then);
            case NOT_NOW -> {
                log.info(atApply ? "APPLY was cancelled at the check: Not now" : "The check was put off: Not now");
                then.run();
            }
        }
    }

    /**
     * The merge: the binds rows, then the device files' reconcile. Cancel in either goes back to the warning.
     */
    private void merge(Checked checked, Map<String, Side> given, Runnable then) {
        BindingsCheck.Found found = checked.found();
        Map<String, Side> answers = Map.of();
        if (!found.differences().none()) {
            answers = new CheckMergeDialog(parent, found.differences().items(), given).showDialog().orElse(null);
            if (answers == null) {
                warn(checked, then);
                return;
            }
            if (takesAReset(found.differences(), answers) && !sure()) {
                merge(checked, answers, then);
                return;
            }
        }
        Optional<DeviceCheck.Answer> devices = reconciled(checked);
        if (devices.isEmpty()) {
            warn(checked, then);
            return;
        }
        write(found, answers, devices.get(), true, then);
    }

    /**
     * The game's device files (Alan, 2026-10-31): the one installation that differs, taken whole after "use one
     * installation for all"'s confirmation; the reconcile when several do. With none that can be a side - only a missing
     * or unreadable file - the working copy is put back, the only choice offered for those. Empty when cancelled.
     */
    private Optional<DeviceCheck.Answer> theGames(Checked checked) {
        List<DeviceCheck.Judged> sides = checked.found().devices().sides();
        if (sides.isEmpty()) return Optional.of(DeviceCheck.Answer.WORKING_COPY);
        if (sides.size() > 1) return reconciled(checked);
        DeviceCheck.Judged only = sides.getFirst();
        long id = only.row().id();
        String name = checked.names().getOrDefault(String.valueOf(id), only.row().storefront());
        if (!FirstSetupDialog.confirmUseOne(parent, checked.reconcile(), id, name, wording(checked))) {
            return Optional.empty();
        }
        if (only.state().reset() && !deviceSure(name)) return Optional.empty();
        return Optional.of(DeviceCheck.Answer.reconciled(FirstSetupFiles.useOne(checked.reconcile(), id), Map.of()));
    }

    /**
     * First setup's reconcile, with the working copy as one side (storage.md rule 14). With no installation that can be
     * a side, the working copy is put back - all a missing or unreadable file is offered. Empty when cancelled.
     */
    private Optional<DeviceCheck.Answer> reconciled(Checked checked) {
        if (checked.reconcile() == null) return Optional.of(DeviceCheck.Answer.WORKING_COPY);
        while (true) {
            Optional<FirstSetupDialog.Choice> choice = new FirstSetupDialog(parent, checked.reconcile(),
                    checked.names(), wording(checked)).showDialog();
            if (choice.isEmpty()) return Optional.empty();
            FirstSetupDialog.Choice chosen = choice.get();
            Long usedOne = chosen.usedOne();
            if (usedOne != null && usedOne == DeviceCheck.WORKING_COPY_SIDE) {
                return Optional.of(DeviceCheck.Answer.WORKING_COPY);
            }
            if (usedOne != null && resetInstallation(checked, usedOne)
                    && !deviceSure(checked.names().getOrDefault(String.valueOf(usedOne), String.valueOf(usedOne)))) {
                continue;
            }
            return Optional.of(DeviceCheck.Answer.reconciled(chosen.files(), chosen.moves()));
        }
    }

    private static boolean resetInstallation(Checked checked, long id) {
        return checked.found().devices().sides().stream()
                .anyMatch(judged -> judged.row().id() == id && judged.state().reset());
    }

    /** The reconcile's words, for the check: the result goes to every installation the check read. */
    private static FirstSetupDialog.Wording wording(Checked checked) {
        long installations = checked.found().devices().installations().stream()
                .filter(judged -> judged.state() != DeviceCheck.State.UNREADABLE).count();
        return new FirstSetupDialog.Wording(getText("bindings.check.title"), getText("bindings.check.devices.intro"),
                getText("bindings.aliasDesigner.firstSetup.backupBefore"), getText("bindings.check.title"),
                getText("bindings.check.devices.confirm"), getText("bindings.check.devices.trailer", installations));
    }

    /** Whether any row of a file that looks reset keeps the game's side. */
    private static boolean takesAReset(Differences differences, Map<String, Side> answers) {
        return differences.files().stream().filter(FileDifference::reset)
                .flatMap(file -> file.items().stream())
                .anyMatch(item -> answers.get(item.id()) == Side.LIVE);
    }

    /** <em>"Are you sure?"</em> before a reset file replaces the working copy (storage.md rule 14). */
    private boolean sure() {
        return HudConfirmDialog.confirm(parent, getText("bindings.check.title"), getText("bindings.check.resetSure"),
                getText("bindings.check.takeTheGames"), getText("button.cancel"));
    }

    /** <em>"Are you sure?"</em> before a reset installation's device files replace the working copy's. */
    private boolean deviceSure(String installation) {
        return HudConfirmDialog.confirm(parent, getText("bindings.check.title"),
                getText("bindings.check.devices.resetSure", installation), getText("bindings.check.takeTheGames"),
                getText("button.cancel"));
    }

    private static Map<String, Side> everyRow(BindingsCheck.Found found, Side side) {
        Map<String, Side> answers = new HashMap<>();
        found.differences().items().forEach(item -> answers.put(item.id(), side));
        return answers;
    }

    /** The write, on its own thread, then the report. */
    private void write(BindingsCheck.Found found, Map<String, Side> answers, DeviceCheck.Answer devices,
                       boolean merging, Runnable then) {
        FlowSteps.inBackground(atApply ? "BindForge-Apply" : "BindForge-Check", log, atApply ? "APPLY" : "The check",
                () -> written(found, answers, devices, merging, then),
                reason -> () -> tell(writing.failed(reason), then),
                reason -> () -> tell(writing.stopped(reason), then), then);
    }

    /**
     * The write, off the EDT; the next step for the EDT. If the files changed meanwhile, the user is told and the
     * warning, or the merge, opens again, keeping the answers to every binds row that did not change (Alan,
     * 2026-10-30) - at APPLY, with nothing left to ask, it writes straight away. The device files are asked again.
     */
    private Runnable written(BindingsCheck.Found found, Map<String, Side> answers, DeviceCheck.Answer devices,
                             boolean merging, Runnable then) throws IOException {
        try {
            String report = writing.write(found, answers, devices);
            return () -> tell(report, then);
        } catch (BindingsCheck.ChangedMeanwhileException e) {
            log.info("The check found the files changed while it was open");
            BindingsCheck.Found now = e.now();
            if (atApply && now.differences().none()) {
                // WHY: at APPLY the edits are still to be written.
                return written(now, Map.of(), DeviceCheck.Answer.WORKING_COPY, false, then);
            }
            if (now.none()) return () -> tell(getText("bindings.check.changed.none"), then);
            if (!now.anythingToAsk()) {
                return () -> tell(getText("bindings.check.unreadableOnly", installationLines(now)), then);
            }
            Checked again = prepared(now);
            Map<String, Side> kept = kept(found, now, answers);
            return () -> {
                HudConfirmDialog.info(parent, getText("bindings.check.title"), getText("bindings.check.changed"),
                        getText("button.close"));
                if (merging) merge(again, kept, then);
                else warn(again, then);
            };
        }
    }

    /** The answers to rows that are still exactly as they were. */
    static Map<String, Side> kept(BindingsCheck.Found before, BindingsCheck.Found now, Map<String, Side> answers) {
        Set<LineItem> unchanged = new HashSet<>(now.differences().items());
        Map<String, Side> kept = new HashMap<>();
        for (LineItem item : before.differences().items()) {
            Side answer = answers.get(item.id());
            if (answer != null && unchanged.contains(item)) kept.put(item.id(), answer);
        }
        return kept;
    }

    private void tell(String message, Runnable then) {
        FlowSteps.tell(parent, writing.title(), message, then);
    }

    /**
     * The warning: every file and installation that differs, each one that looks reset, any left for a newer version -
     * and, at APPLY, how many rows the edits change and that closing cancels the APPLY (Storage S8b).
     */
    static String warningText(BindingsCheck.Found found, boolean atApply) {
        Differences differences = found.differences();
        List<String> lines = new ArrayList<>();
        differences.files().forEach(file -> lines.add(fileLine(file)));
        String installations = installationLines(found);
        if (!installations.isEmpty()) lines.add(installations);
        String text = getText("bindings.check.intro", String.join("\n", lines));
        if (differences.anyReset() || found.devices().anyReset()) text += "\n\n" + getText("bindings.check.resetAdvice");
        if (!differences.leftOut().isEmpty()) {
            text += "\n\n" + getText("bindings.check.leftOut", String.join(", ", differences.leftOut()));
        }
        if (found.edited() > 0) text += "\n\n" + getText("bindings.check.edited", found.edited());
        if (atApply) text += "\n\n" + getText("bindings.check.atApply");
        return text;
    }

    private static String fileLine(FileDifference file) {
        String line = switch (file.presence()) {
            case LIVE_ONLY -> getText("bindings.check.file.liveOnly", file.file());
            case WORKING_COPY_ONLY -> getText("bindings.check.file.workingCopyOnly", file.file());
            case BOTH -> getText("bindings.check.file.differs", file.file(), file.items().size());
        };
        return "- " + line + (file.reset() ? getText("bindings.check.file.reset") : "");
    }

    /** One line for each installation that does not match, in the stored list's order (Storage S9a). */
    static String installationLines(BindingsCheck.Found found) {
        return found.devices().differing().stream().map(CheckFlow::installationLine)
                .collect(Collectors.joining("\n"));
    }

    private static String installationLine(DeviceCheck.Judged judged) {
        String name = getText("bindings.firstTimeStartup.place.installation", judged.row().storefront(),
                judged.row().rootPath());
        String line = switch (judged.state()) {
            case DIFFERS -> getText("bindings.check.install.differs", name);
            case RESET_FACTORY -> getText("bindings.check.install.resetFactory", name);
            case RESET_MISSING -> getText("bindings.check.install.resetMissing", name);
            case UNPARSEABLE -> getText("bindings.check.install.unparseable", name, judged.problem());
            case UNREADABLE -> getText("bindings.check.install.unreadable", name, judged.problem());
            case MATCHES -> throw new IllegalArgumentException("an installation that matches has no line");
        };
        return "- " + line;
    }

    /** What the check's write did: the backup, and each place that did not take everything. */
    static String reportText(Written written) {
        WriteReport report = written.report();
        StringBuilder text = new StringBuilder(getText("bindings.check.result", written.backup().zip().toString()));
        WriteReport.Place bindings = report.bindings();
        if (!bindings.failures().isEmpty()) {
            text.append('\n').append(getText("bindings.firstTimeStartup.result.place",
                    getText("bindings.firstTimeStartup.place.bindingsFolder"), String.join("; ", bindings.failures())));
        }
        text.append(keptReadOnly(report));
        text.append(FirstTimeStartupFlow.installationFailures(report));
        return text.toString();
    }

    /**
     * A live-only file the check would have removed but kept, being read-only - its own line, shared with APPLY's report
     * since S8b removes such files too.
     */
    static String keptReadOnly(WriteReport report) {
        List<String> kept = report.bindings().keptReadOnly();
        if (kept.isEmpty()) return "";
        return "\n" + getText("bindings.check.result.keptReadOnly", String.join(", ", kept));
    }
}
