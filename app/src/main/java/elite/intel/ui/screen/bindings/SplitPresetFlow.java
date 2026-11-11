package elite.intel.ui.screen.bindings;

import elite.intel.bindforge.io.BindingsLoader;
import elite.intel.bindforge.io.BindingsLoader.SplitChoice;
import elite.intel.bindforge.io.BindingsMonitor;
import elite.intel.bindforge.io.SplitPresetChoice;
import elite.intel.bindforge.storage.Edits;
import elite.intel.bindforge.storage.PresetEdits;
import elite.intel.bindforge.storage.ReadOnlyMode;
import elite.intel.bindforge.storage.WorkingCopy;
import elite.intel.session.PlayerSession;
import elite.intel.ui.dialog.HudConfirmDialog;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;

/**
 * Asks which binds file Elite-Intel reads when the four lines of the game's preset file name different ones, then
 * whether that file should be the only one the game uses (Storage S13f; storage.md, <em>Settled with Storage S13f's
 * sources</em>). A dialog, never VEGA: it can come up before an LLM is set up (Krondor, 2026-11-10).
 * <ul>
 *   <li><strong>When:</strong> at the first BindForge click of a run with the lines split, after first-time startup and
 *   the check - with BindForge's other questions, not at Elite-Intel start, where it would come without context
 *   (Krondor, 2026-11-10). Asked once a run, answered or not, and again the next run while the lines still differ.
 *   Nothing is stored.</li>
 *   <li><strong>The file chosen</strong> is what the assistant reads this run ({@link SplitPresetChoice}).</li>
 *   <li><strong>Yes</strong> sets all four lines to it as the Preset Editor's SET ALL TO SAME does, and applies it at
 *   once - the answer is the user asking (principle 4), and APPLY's check and backup run.</li>
 *   <li><strong>No</strong> keeps the lines; the question says what that costs, and the log says it in ERROR, for the
 *   support bundle.</li>
 *   <li><strong>Only which file is asked</strong> while read only, before first-time startup has made a working copy
 *   (nothing to write through), and while edits not applied are on the bar (APPLY would write them too).</li>
 * </ul>
 */
public final class SplitPresetFlow {

    private static final Logger log = LogManager.getLogger(SplitPresetFlow.class);

    /** Asked this run - set when the question is shown, so a run whose lines were not split yet asks once they are. */
    private static final AtomicBoolean ASKED_THIS_RUN = new AtomicBoolean();

    private final Component parent;
    private final ApplyBar applyBar;
    private final AtomicBoolean running = new AtomicBoolean();

    public SplitPresetFlow(Component parent, ApplyBar applyBar) {
        this.parent = parent;
        this.applyBar = applyBar;
    }

    /** Asks, if the lines are split and it has not been asked this run. On the EDT; the disk work off it. */
    public void askIfNeeded() {
        if (ASKED_THIS_RUN.get() || !running.compareAndSet(false, true)) return;
        background(() -> {
            Path folder = PlayerSession.getInstance().getBindingsDir();
            if (folder == null) return null;
            List<SplitChoice> choices = new BindingsLoader().splitChoices(folder);
            if (choices.isEmpty() || !ASKED_THIS_RUN.compareAndSet(false, true)) return null;
            boolean canSetAll = canSetAll(ReadOnlyMode.stored().active(), WorkingCopy.stored().exists(),
                    Edits.stored().hasEdits());
            log.info("The preset file's lines name different binds files: {}; asking which to read", choices);
            return () -> ask(choices, canSetAll);
        });
    }

    /**
     * Whether the second question - should the game use only this file - is offered: not while read only, not before
     * first-time startup has made a working copy (nothing to write through), and not while edits not applied are on
     * the bar (APPLY would write them too). Otherwise only which file is asked.
     */
    static boolean canSetAll(boolean readOnly, boolean workingCopyExists, boolean hasEdits) {
        return !readOnly && workingCopyExists && !hasEdits;
    }

    private void ask(List<SplitChoice> choices, boolean canSetAll) {
        var chosen = new SplitPresetDialog(parent, choices).showDialog();
        if (chosen.isEmpty()) {
            log.warn("Not answered which binds file to read for the split preset file; the first line's file is read");
            finish();
            return;
        }
        String bindName = chosen.get();
        background(() -> {
            SplitPresetChoice.choose(bindName);
            BindingsMonitor.getInstance().reread();
            return canSetAll ? () -> askOnlyOne(bindName, choices) : () -> {
                kept(bindName, choices);
                finish();
            };
        });
    }

    private void askOnlyOne(String bindName, List<SplitChoice> choices) {
        HudConfirmDialog.Result result = HudConfirmDialog.showWithoutDefault(parent,
                getText("bindings.splitPreset.onlyOne.title"),
                getText("bindings.splitPreset.onlyOne.question", bindName),
                getText("bindings.splitPreset.onlyOne.yes"), null, getText("bindings.splitPreset.onlyOne.no"));
        if (result != HudConfirmDialog.Result.PRIMARY) {
            kept(bindName, choices);
            finish();
            return;
        }
        String title = getText("bindings.splitPreset.onlyOne.title");
        background(() -> {
            try {
                Edits.stored().change((current, applied) ->
                        PresetEdits.change(current, applied, PresetEditorPanel.allTo(bindName)));
            } catch (Edits.RefusedException e) {
                log.info("The preset file's four lines not set to {}: {}", bindName, e.getMessage());
                return () -> FlowSteps.tell(parent, title, getText(e.messageKey()), this::finish);
            }
            log.info("Setting all four preset lines to {}, then APPLY", bindName);
            return () -> applyBar.applyAsked(this::finish);
        });
    }

    /** The lines stay split: said in ERROR, so the support bundle shows why some keys VEGA presses do nothing. */
    private static void kept(String bindName, List<SplitChoice> choices) {
        String others = choices.stream().filter(choice -> !choice.bindName().equals(bindName))
                .map(choice -> choice.sections() + " on " + choice.bindName())
                .collect(Collectors.joining(", "));
        log.error("The preset file stays split: the assistant reads {}, while {} - their keys may not match the game",
                bindName, others);
    }

    private void finish() {
        running.set(false);
    }

    private void background(FlowSteps.Step step) {
        String title = getText("bindings.splitPreset.title");
        FlowSteps.inBackground("BindForge-SplitPreset", log, "The split preset file's question", step,
                reason -> () -> FlowSteps.tell(parent, title, getText("bindings.splitPreset.failed", reason),
                        this::finish),
                reason -> () -> FlowSteps.tell(parent, title, getText("bindings.splitPreset.stopped", reason),
                        this::finish),
                this::finish);
    }
}
