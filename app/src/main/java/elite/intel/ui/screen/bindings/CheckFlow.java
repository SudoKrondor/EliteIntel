package elite.intel.ui.screen.bindings;

import elite.intel.bindforge.storage.BindingsCheck;
import elite.intel.bindforge.storage.BindingsComparison.Differences;
import elite.intel.bindforge.storage.BindingsComparison.FileDifference;
import elite.intel.bindforge.storage.BindingsComparison.LineItem;
import elite.intel.bindforge.storage.BindingsComparison.Side;
import elite.intel.bindforge.storage.Edits;
import elite.intel.bindforge.storage.WriteReport;
import elite.intel.bindforge.storage.Written;
import elite.intel.ui.dialog.HudConfirmDialog;
import elite.intel.util.StringUtls;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;

/**
 * The check for the binds and preset files, on screen (storage.md rules 5 and 14): if anything differs, one warning for
 * the whole set, then the three choices - put the working copy back, merge, or take the game's files - and the write,
 * backed up first. It runs at two moments:
 * <ul>
 *   <li><strong>The first click on the BindForge tab in a session</strong> (Storage S8a): a session flag, set once the
 *       check has read the files, stops it running again until Elite-Intel restarts; Not now leaves it set (Alan,
 *       2026-10-30). Edits on screen put it off to the next click, since its write would throw them away.</li>
 *   <li><strong>APPLY</strong> (Storage S8b; {@link ApplyBar}): after the confirmation; the write is APPLY's, the edits
 *       on top. The rows the edits change are not asked about, and the warning says how many. Not now cancels the
 *       APPLY, leaving the edits on screen (Alan, 2026-10-30).</li>
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
         * @return the report - what was written, or why nothing was
         * @throws BindingsCheck.ChangedMeanwhileException if the game's files changed since {@code found} - nothing
         *                                                 was written
         * @throws IOException                             if nothing was written, told with {@link #failed}
         */
        String write(BindingsCheck.Found found, Map<String, Side> answers) throws IOException;

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
        public String write(BindingsCheck.Found found, Map<String, Side> answers) throws IOException {
            try {
                return reportText(BindingsCheck.stored().settle(found, answers));
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
     * The check, once a session: reads and compares, and hands back the warning for the EDT, or {@code null} when there
     * is nothing to say. Off the EDT.
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
        BindingsCheck.Found found;
        try {
            found = BindingsCheck.stored().find();
        } catch (IOException e) {
            log.warn("The check could not compare your control files", e);
            return () -> tell(getText("bindings.check.readFailed", StringUtls.messageOf(e)), then);
        }
        if (found.differences().none()) {
            log.info("The check found your control files as BindForge last wrote them");
            return null;
        }
        return () -> warn(found, then);
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
        if (found.differences().none()) return written(found, Map.of(), false, then);
        return () -> warn(found, then);
    }

    private void warn(BindingsCheck.Found found, Runnable then) {
        switch (CheckWarningDialog.show(parent, warningText(found, atApply))) {
            case KEEP_MINE -> write(found, everyRow(found, Side.WORKING_COPY), false, then);
            case TAKE_THE_GAMES -> {
                if (found.differences().anyReset() && !sure()) {
                    warn(found, then);
                    return;
                }
                write(found, everyRow(found, Side.LIVE), false, then);
            }
            case MERGE -> merge(found, Map.of(), then);
            case NOT_NOW -> {
                log.info(atApply ? "APPLY was cancelled at the check: Not now" : "The check was put off: Not now");
                then.run();
            }
        }
    }

    /** The merge; Cancel goes back to the warning. */
    private void merge(BindingsCheck.Found found, Map<String, Side> given, Runnable then) {
        Map<String, Side> answers = new CheckMergeDialog(parent, found.differences().items(), given).showDialog()
                .orElse(null);
        if (answers == null) {
            warn(found, then);
            return;
        }
        if (takesAReset(found.differences(), answers) && !sure()) {
            merge(found, answers, then);
            return;
        }
        write(found, answers, true, then);
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
                getText("bindings.check.takeTheGames"), getText("bindings.aliasDesigner.firstSetup.cancel"));
    }

    private static Map<String, Side> everyRow(BindingsCheck.Found found, Side side) {
        Map<String, Side> answers = new HashMap<>();
        found.differences().items().forEach(item -> answers.put(item.id(), side));
        return answers;
    }

    /** The write, on its own thread, then the report. */
    private void write(BindingsCheck.Found found, Map<String, Side> answers, boolean merging, Runnable then) {
        FlowSteps.inBackground(atApply ? "BindForge-Apply" : "BindForge-Check", log, atApply ? "APPLY" : "The check",
                () -> written(found, answers, merging, then),
                reason -> () -> tell(writing.failed(reason), then),
                reason -> () -> tell(writing.stopped(reason), then), then);
    }

    /**
     * The write, off the EDT; the next step for the EDT. If the files changed meanwhile, the user is told and the
     * warning, or the merge, opens again, keeping the answers to every row that did not change (Alan, 2026-10-30) - at
     * APPLY, with nothing left to ask, it writes straight away.
     */
    private Runnable written(BindingsCheck.Found found, Map<String, Side> answers, boolean merging, Runnable then)
            throws IOException {
        try {
            String report = writing.write(found, answers);
            return () -> tell(report, then);
        } catch (BindingsCheck.ChangedMeanwhileException e) {
            log.info("The check found the files changed while it was open");
            BindingsCheck.Found now = e.now();
            if (now.differences().none()) {
                // WHY: at APPLY the edits are still to be written; at the first click there is nothing left to do.
                if (atApply) return written(now, Map.of(), false, then);
                return () -> tell(getText("bindings.check.changed.none"), then);
            }
            Map<String, Side> kept = kept(found, now, answers);
            return () -> {
                HudConfirmDialog.info(parent, getText("bindings.check.title"), getText("bindings.check.changed"),
                        getText("button.close"));
                if (merging) merge(now, kept, then);
                else warn(now, then);
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
     * The warning: every file that differs, each one that looks reset, any left for a newer version - and, at APPLY,
     * how many rows the edits change and that closing cancels the APPLY (Storage S8b).
     */
    static String warningText(BindingsCheck.Found found, boolean atApply) {
        Differences differences = found.differences();
        String files = differences.files().stream().map(CheckFlow::fileLine).collect(Collectors.joining("\n"));
        String text = getText("bindings.check.intro", files);
        if (differences.anyReset()) text += "\n\n" + getText("bindings.check.resetAdvice");
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
