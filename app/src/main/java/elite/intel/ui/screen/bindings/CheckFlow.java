package elite.intel.ui.screen.bindings;

import elite.intel.bindforge.storage.BindingsCheck;
import elite.intel.bindforge.storage.BindingsComparison.Differences;
import elite.intel.bindforge.storage.BindingsComparison.FileDifference;
import elite.intel.bindforge.storage.BindingsComparison.LineItem;
import elite.intel.bindforge.storage.BindingsComparison.NewerVersion;
import elite.intel.bindforge.storage.BindingsComparison.Side;
import elite.intel.bindforge.storage.DeviceCheck;
import elite.intel.bindforge.storage.Edits;
import elite.intel.bindforge.storage.FirstSetupFiles;
import elite.intel.bindforge.storage.FirstSetupReading;
import elite.intel.bindforge.storage.WriteReport;
import elite.intel.bindforge.storage.Written;
import elite.intel.db.dao.BindForgeInstallationsDao.InstallationRow;
import elite.intel.ui.dialog.HudConfirmDialog;
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
import java.util.function.Consumer;
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
 *   <li><strong>APPLY</strong> (Storage S8b, S9b; {@link ApplyBar}): after the confirmation; the write is APPLY's, the
 *       edits on top. The rows the edits change are not asked about, and the warning says how many. Not now cancels the
 *       APPLY, leaving the edits on screen (Alan, 2026-10-30). The device files are judged as at the first click, and
 *       the reconcile's own side is the user's files with the edits not applied (Alan, 2026-10-31). An installation
 *       that cannot be read at all asks nothing: APPLY writes, leaves it alone, and the report names it.</li>
 * </ul>
 * <strong>A newer version of one of the user's binds files is asked first</strong>, at either moment (Storage S10): on
 * its own, before the warning - Approve when every binding carried over, else Reconcile, its lines one by one - and
 * written in the same backup and write as the rest. Put mine back is never offered for it. Not now ends the check, or
 * cancels the APPLY, as the warning's does.
 * <p>
 * The reconcile's reading is built once the binds rows are answered, from the binds files they settle (Storage S9b), so
 * a device only the game's side uses counts. The disk work runs off the EDT; the dialogs on it.
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
         * @param devices the device files settled on - {@link DeviceCheck.Answer#PUT_MINE_BACK} when none were asked
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
                report.append(newerReport(found));
                devices.moves().forEach((from, to) -> report.append('\n')
                        .append(getText("bindings.aliasDesigner.firstSetup.summary.moves", from, to)));
                return report.toString();
            } catch (BindingsCheck.EditsNotAppliedException e) {
                log.info("The check wrote nothing: {}", e.getMessage());
                return getText("bindings.check.editsNotApplied");
            } catch (Edits.RefusedException e) {
                log.info("The check wrote nothing: {}", e.getMessage());
                return getText(e.messageKey());
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
     * What one check found, ready for the screen.
     *
     * @param names what each side of the device files' reconcile is called, keyed by its id as text - BindForge's own
     *              side first
     * @param newer the answers to the newer versions' rows, once they are asked (Storage S10) - every write carries them
     */
    record Checked(BindingsCheck.Found found, Map<String, String> names, Map<String, Side> newer) {

        Checked withNewer(Map<String, Side> answers) {
            return new Checked(found, names, Map.copyOf(answers));
        }

        /** {@code answers} to the warning's rows, with the newer versions'. */
        Map<String, Side> with(Map<String, Side> answers) {
            Map<String, Side> all = new HashMap<>(newer);
            all.putAll(answers);
            return all;
        }

        /** BindForge's own side's name. */
        String mine() {
            return names.get(String.valueOf(DeviceCheck.WORKING_COPY_SIDE));
        }

        /** Whether any installation's files can be a side of the reconcile. */
        boolean anySide() {
            return !found.devices().sides().isEmpty();
        }
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
            checked = prepared(BindingsCheck.stored().find());
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
        return () -> begin(checked, Map.of(), false, then);
    }

    /**
     * APPLY's check, once {@code found} is read (Storage S8b, S9b): with nothing to ask, the write runs here and now -
     * an installation that cannot be read at all is left alone, and the report names it (Alan, 2026-10-31); else the
     * warning, handed back for the EDT. Off the EDT.
     *
     * @param then run on the EDT once it is over - written, cancelled or refused
     * @throws IOException if nothing to ask and the write fails - nothing was written
     */
    Runnable atApply(BindingsCheck.Found found, Runnable then) throws IOException {
        if (found.edited() > 0) {
            log.info("APPLY's check: {} rows that differ are under the edits, which go on top", found.edited());
        }
        if (!found.anythingToAsk()) return written(found, Map.of(), DeviceCheck.Answer.PUT_MINE_BACK, false, then);
        Checked checked = prepared(found);
        return () -> begin(checked, Map.of(), false, then);
    }

    /**
     * {@code found}, with what each side of the device files' reconcile is called. Off the EDT: it reads the stored
     * installations' names. The reconcile itself is read once the binds rows are answered ({@link #withReading}).
     */
    private Checked prepared(BindingsCheck.Found found) {
        // WHY: at APPLY the user's side holds the edits not applied too (Alan, 2026-10-31, Storage S9b).
        String mine = getText(atApply ? "bindings.check.devices.workingCopyWithEdits"
                : "bindings.check.devices.workingCopy");
        List<InstallationRow> rows = found.devices().sides().stream().map(DeviceCheck.Judged::row).toList();
        Map<String, String> names = new LinkedHashMap<>();
        names.put(String.valueOf(DeviceCheck.WORKING_COPY_SIDE), mine);
        names.putAll(FirstTimeStartupFlow.labelsFor(rows));
        return new Checked(found, names, Map.of());
    }

    /**
     * Each newer version first, on its own (Alan, 2026-10-31, Storage S10 D1): approved when every binding carried
     * over, else reconciled line by line; Not now on any ends the check - at APPLY, cancels it. Then the warning, or the
     * merge when it is opened again, for what else differs; with nothing else to ask, the write.
     *
     * @param given   answers already given - kept when the files changed meanwhile
     * @param merging whether to go on to the merge rather than the warning
     */
    private void begin(Checked checked, Map<String, Side> given, boolean merging, Runnable then) {
        BindingsCheck.Found found = checked.found();
        Map<String, Side> newer = new HashMap<>();
        for (NewerVersion version : found.differences().newer()) {
            Optional<Map<String, Side>> answered = askNewer(version, given);
            if (answered.isEmpty()) {
                log.info(atApply ? "APPLY was cancelled at a newer version: Not now"
                        : "The check was put off at a newer version: Not now");
                then.run();
                return;
            }
            newer.putAll(answered.get());
        }
        Checked asked = checked.withNewer(newer);
        if (found.differences().files().isEmpty() && found.devices().asked().isEmpty()) {
            write(found, asked.with(Map.of()), DeviceCheck.Answer.PUT_MINE_BACK, false, then);
        } else if (merging) {
            merge(asked, given, then);
        } else {
            warn(asked, then);
        }
    }

    /**
     * One newer version (storage.md rule 14): <em>Approve</em> when every binding carried over; else <em>Reconcile</em>,
     * the merge for its rows, with <em>"Are you sure?"</em> before a reset file's line is taken. Cancelling the merge goes
     * back to the question. Nothing is selected and Enter chooses nothing. Empty for Not now.
     */
    private Optional<Map<String, Side>> askNewer(NewerVersion version, Map<String, Side> given) {
        Map<String, Side> kept = given;
        while (true) {
            String action = getText(version.carriedOver() ? "bindings.check.newer.approve"
                    : "bindings.check.newer.reconcile");
            HudConfirmDialog.Result chosen = HudConfirmDialog.showWithoutDefault(parent,
                    getText("bindings.check.newer.title"), newerText(version, atApply), action, null,
                    getText("bindings.restore.notNow"));
            if (chosen != HudConfirmDialog.Result.PRIMARY) return Optional.empty();
            if (version.carriedOver()) return Optional.of(Map.of());
            Optional<Map<String, Side>> answered = new CheckMergeDialog(parent, version.items(), kept).showDialog();
            if (answered.isEmpty()) continue;
            kept = answered.get();
            Map<String, Side> answers = kept;
            boolean takesTheGames = version.items().stream().anyMatch(item -> answers.get(item.id()) == Side.LIVE);
            if (version.reset() && takesTheGames && !sure()) continue;
            return answered;
        }
    }

    private void warn(Checked checked, Runnable then) {
        BindingsCheck.Found found = checked.found();
        // WHY: with no binds row and no installation's files to take - only a missing or unparseable file - putting
        // the working copy back is all there is to offer (Alan, 2026-10-31).
        boolean offerMergeAndTake = !found.differences().files().isEmpty() || checked.anySide();
        switch (CheckWarningDialog.show(parent, warningText(found, atApply), offerMergeAndTake)) {
            case KEEP_MINE -> write(found, checked.with(everyRow(found, Side.WORKING_COPY)),
                    DeviceCheck.Answer.PUT_MINE_BACK, false, then);
            case TAKE_THE_GAMES -> {
                if (found.differences().anyReset() && !sure()) {
                    warn(checked, then);
                    return;
                }
                Map<String, Side> answers = checked.with(everyRow(found, Side.LIVE));
                if (!checked.anySide()) {
                    // WHY: only a missing or unparseable file - never a side; the working copy goes back there.
                    write(found, answers, DeviceCheck.Answer.PUT_MINE_BACK, false, then);
                    return;
                }
                withReading(checked, answers, then, reading -> theGames(checked, reading).ifPresentOrElse(
                        devices -> write(found, answers, devices, false, then), () -> warn(checked, then)));
            }
            case MERGE -> merge(checked, Map.of(), then);
            case NOT_NOW -> {
                log.info(atApply ? "APPLY was cancelled at the check: Not now" : "The check was put off: Not now");
                then.run();
            }
        }
    }

    /**
     * The merge: the binds rows, then the device files' reconcile. Cancelling the binds rows goes back to the warning;
     * cancelling the reconcile goes back to the binds rows, the answers kept (Alan, 2026-10-31, Storage S9R) - or to
     * the warning when there were no binds rows to answer.
     */
    private void merge(Checked checked, Map<String, Side> given, Runnable then) {
        BindingsCheck.Found found = checked.found();
        Map<String, Side> answered = Map.of();
        if (!found.differences().files().isEmpty()) {
            answered = new CheckMergeDialog(parent, found.differences().items(), given).showDialog().orElse(null);
            if (answered == null) {
                warn(checked, then);
                return;
            }
            if (takesAReset(found.differences(), answered) && !sure()) {
                merge(checked, answered, then);
                return;
            }
        }
        Map<String, Side> answers = checked.with(answered);
        if (!checked.anySide()) {
            // WHY: only a missing or unparseable file - never a side; the working copy goes back there.
            write(found, answers, DeviceCheck.Answer.PUT_MINE_BACK, true, then);
            return;
        }
        withReading(checked, answers, then, reading -> reconciled(checked, reading).ifPresentOrElse(
                devices -> write(found, answers, devices, true, then),
                // WHY: with no binds rows, reopening the merge would open the reconcile again at once.
                () -> {
                    if (found.differences().files().isEmpty()) warn(checked, then);
                    else merge(checked, answers, then);
                }));
    }

    /**
     * The device files' reconcile, read off the EDT from the binds files {@code answers} settle - with the edits on top
     * at APPLY (Storage S9b, from the S9a review) - then {@code next} on the EDT. Only when an installation's files can be
     * a side. A reading that fails is told, and the flow ends: nothing was written.
     */
    private void withReading(Checked checked, Map<String, Side> answers, Runnable then,
                             Consumer<FirstSetupReading> next) {
        FlowSteps.inBackground(atApply ? "BindForge-Apply" : "BindForge-Check", log, "The check's reconcile", () -> {
                    FirstSetupReading reading = BindingsCheck.stored().reading(checked.found(), answers, checked.mine())
                            .orElseThrow(() -> new IllegalStateException("no installation's files can be a side"));
                    return () -> next.accept(reading);
                }, reason -> () -> tell(writing.failed(reason), then),
                reason -> () -> tell(writing.stopped(reason), then), then);
    }

    /**
     * The game's device files (Alan, 2026-10-31): the one installation that differs, taken whole after "use one
     * installation for all"'s confirmation; the reconcile when several do. Empty when cancelled.
     */
    private Optional<DeviceCheck.Answer> theGames(Checked checked, FirstSetupReading reading) {
        List<DeviceCheck.Judged> sides = checked.found().devices().sides();
        if (sides.size() > 1) return reconciled(checked, reading);
        DeviceCheck.Judged only = sides.getFirst();
        long id = only.row().id();
        String name = checked.names().getOrDefault(String.valueOf(id), only.row().storefront());
        if (!FirstSetupDialog.confirmUseOne(parent, reading, id, name, wording(checked))) {
            return Optional.empty();
        }
        if (only.state().reset() && !deviceSure(name)) return Optional.empty();
        return Optional.of(DeviceCheck.Answer.reconciled(FirstSetupFiles.useOne(reading, id), Map.of()));
    }

    /**
     * First setup's reconcile, with the user's own files as one side (storage.md rule 14) - the working copy, with the
     * edits not applied at APPLY. Empty when cancelled.
     */
    private Optional<DeviceCheck.Answer> reconciled(Checked checked, FirstSetupReading reading) {
        while (true) {
            Optional<FirstSetupDialog.Choice> choice = new FirstSetupDialog(parent, reading,
                    checked.names(), wording(checked)).showDialog();
            if (choice.isEmpty()) return Optional.empty();
            FirstSetupDialog.Choice chosen = choice.get();
            Long usedOne = chosen.usedOne();
            if (usedOne != null && usedOne == DeviceCheck.WORKING_COPY_SIDE) {
                return Optional.of(DeviceCheck.Answer.PUT_MINE_BACK);
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
            if (atApply && !now.anythingToAsk()) {
                // WHY: at APPLY the edits are still to be written.
                return written(now, Map.of(), DeviceCheck.Answer.PUT_MINE_BACK, false, then);
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
                // WHY: a newer version is asked again too - its answers kept on the rows that did not change.
                begin(again, kept, merging, then);
            };
        }
    }

    /** The answers to rows that are still exactly as they were - a newer version's included. */
    static Map<String, Side> kept(BindingsCheck.Found before, BindingsCheck.Found now, Map<String, Side> answers) {
        Set<LineItem> unchanged = new HashSet<>(now.differences().rows());
        Map<String, Side> kept = new HashMap<>();
        for (LineItem item : before.differences().rows()) {
            Side answer = answers.get(item.id());
            if (answer != null && unchanged.contains(item)) kept.put(item.id(), answer);
        }
        return kept;
    }

    private void tell(String message, Runnable then) {
        FlowSteps.tell(parent, writing.title(), message, then);
    }

    /**
     * One newer version's question (Storage S10): what it is, its new slots, and - when lines differ - how many and what
     * Reconcile does; a reset named; at APPLY, that closing cancels the APPLY.
     */
    static String newerText(NewerVersion version, boolean atApply) {
        String text = version.carriedOver()
                ? getText("bindings.check.newer.carriedOver", version.newer(), version.older(),
                version.newSlots().size(), newSlots(version))
                : getText("bindings.check.newer.changed", version.newer(), version.older(), version.items().size(),
                version.newSlots().size(), newSlots(version));
        if (version.reset()) text += "\n\n" + getText("bindings.check.resetAdvice");
        if (atApply) text += "\n\n" + getText("bindings.check.atApply");
        return text;
    }

    /** The new slots, by the game's own element names (Alan, 2026-10-31, Storage S10 D6). */
    private static String newSlots(NewerVersion version) {
        return version.newSlots().isEmpty() ? getText("bindings.check.newer.noNewSlots")
                : String.join(", ", version.newSlots());
    }

    /** The report's lines for each newer version written: what replaced what, and the new slots to bind. */
    static String newerReport(BindingsCheck.Found found) {
        StringBuilder text = new StringBuilder();
        for (NewerVersion version : found.differences().newer()) {
            text.append('\n').append(getText("bindings.check.newer.result", version.newer(), version.older(),
                    newSlots(version)));
        }
        return text.toString();
    }

    /**
     * The warning: every file and installation that differs, each one that looks reset, any newer version left as it is
     * - and, at APPLY, how many rows the edits change and that closing cancels the APPLY (Storage S8b). A newer version
     * that can be read was asked on its own before it.
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
            text += "\n\n" + getText("bindings.check.newer.leftOut", String.join(", ", differences.leftOut()));
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
