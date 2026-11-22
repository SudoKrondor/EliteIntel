package elite.intel.ui.screen.bindings;

import elite.intel.bindforge.storage.Edits;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

/**
 * Leaving BindForge with edits not applied - another Elite-Intel tab, or closing Elite-Intel - asks first: <strong>Go
 * back</strong>, <strong>Discard</strong> or <strong>Apply</strong> (storage.md rule 9; overview.md, <em>Unsaved Work
 * on Exit</em>; Storage S7). Nothing is selected and Enter chooses nothing; Esc and the close glyph are Go back, which
 * on closing cancels the close.
 * <p>
 * Settled with S7's sources (Alan, 2026-10-30): <strong>Apply</strong> is the bar's APPLY, its confirmation and report
 * included, and leaves only once the edits are gone - refused, failed or cancelled, the user stays with them on screen.
 * <strong>Discard</strong> asks nothing more; this question is the confirmation. <strong>While a write runs</strong>,
 * closing is refused and said so, and switching tabs is let through - the edits are already being written.
 * <p>
 * On the EDT.
 */
public final class LeavePrompt {

    private static final Logger log = LogManager.getLogger(LeavePrompt.class);

    /** The user's answer. */
    public enum Choice { GO_BACK, DISCARD, APPLY }

    /** Where the user is going. */
    public enum Leaving { TAB, CLOSE }

    /** The edits, as far as leaving needs them. */
    interface EditsState {
        /** Whether there are no edits not applied - nothing to ask about. */
        boolean noEdits();

        boolean writing();

        /** DISCARD; {@code false}, with nothing thrown away, when a write started meanwhile. */
        boolean discard();

        static EditsState of(Edits edits) {
            return new EditsState() {
                @Override
                public boolean noEdits() {
                    return !edits.hasEdits();
                }

                @Override
                public boolean writing() {
                    return edits.writing();
                }

                @Override
                public boolean discard() {
                    try {
                        edits.discard();
                        return true;
                    } catch (Edits.WriteRunningException e) {
                        log.info("Discard on leaving not done: {}", e.getMessage());
                        return false;
                    }
                }
            };
        }
    }

    /** The bar's APPLY; runs {@code finished} on the EDT once it is over, written or not. */
    @FunctionalInterface
    interface Applying {
        void apply(Runnable finished);
    }

    private final EditsState edits;
    private final Supplier<Choice> question;
    private final Applying applying;
    private final Runnable writeRunning;

    /**
     * @param question     asks Go back, Discard or Apply - one question for a tab switch and for closing
     * @param writeRunning tells the user a write is running, so they cannot leave yet
     */
    LeavePrompt(EditsState edits, Supplier<Choice> question, Applying applying, Runnable writeRunning) {
        this.edits = edits;
        this.question = question;
        this.applying = applying;
        this.writeRunning = writeRunning;
    }

    /**
     * Leaves - runs {@code leave} - now, later, or not at all, as the user chooses.
     *
     * @param leave switches the tab or closes Elite-Intel; run on the EDT, at most once
     */
    public void request(Leaving leaving, Runnable leave) {
        if (edits.writing()) {
            if (leaving == Leaving.CLOSE) {
                // WHY: closing part way through would leave installations unwritten until the check (S9).
                log.info("Close refused: BindForge is writing the control files");
                writeRunning.run();
                return;
            }
            leave.run();
            return;
        }
        if (edits.noEdits()) {
            leave.run();
            return;
        }
        Choice choice = question.get();
        log.info("Leaving BindForge ({}) with edits not applied: {}", leaving, choice);
        switch (choice) {
            case GO_BACK -> {
            }
            case DISCARD -> {
                if (!edits.discard()) {
                    // WHY: a write started while the question was open - the edits are being written, not thrown away.
                    writeRunning.run();
                    return;
                }
                leave.run();
            }
            case APPLY -> applying.apply(() -> {
                if (edits.noEdits()) leave.run();
            });
        }
    }
}
