package elite.intel.ui.screen.bindings;

import elite.intel.ui.dialog.HudConfirmDialog;
import elite.intel.util.StringUtls;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.function.Function;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;

/**
 * What first-time startup and RESTORE share on screen (Krondor's review, 2026-10-27): disk work on its own thread that
 * hands the next step back to the EDT, and the one message box each flow ends with.
 */
final class FlowSteps {

    private FlowSteps() {
    }

    /** Disk work that hands back the next step for the EDT, or {@code null} for none. */
    @FunctionalInterface
    interface Step {
        Runnable run() throws IOException;
    }

    /**
     * Runs {@code step} on a thread of its own, then its next step on the EDT. Nothing escapes: a flow left half run
     * would stay marked busy, and never be offered again this run.
     *
     * @param what      the flow, for the log
     * @param failed    the step for an {@link IOException}, given its reason - a file the step could not use
     * @param stopped   the step for a {@link RuntimeException}, given its reason - a bug, logged as an error and said to
     *                  have stopped unexpectedly rather than blamed on a file (review, 2026-10-25)
     * @param nothingTo run on the EDT when the step hands back none
     */
    static void inBackground(String threadName, Logger log, String what, Step step,
                             Function<String, Runnable> failed, Function<String, Runnable> stopped,
                             Runnable nothingTo) {
        new Thread(() -> {
            Runnable next;
            try {
                next = step.run();
            } catch (IOException e) {
                log.warn("{} stopped", what, e);
                next = failed.apply(StringUtls.messageOf(e));
            } catch (RuntimeException e) {
                // WHY: broad on purpose - a thread boundary.
                log.error("{} stopped", what, e);
                next = stopped.apply(StringUtls.messageOf(e));
            }
            Runnable then = next == null ? nothingTo : next;
            SwingUtilities.invokeLater(then);
        }, threadName).start();
    }

    /** The message box a flow ends with, then {@code then}. On the EDT. */
    static void tell(Component parent, String title, String message, Runnable then) {
        HudConfirmDialog.info(parent, title, message, getText("bindings.aliasDesigner.firstSetup.close"));
        then.run();
    }
}
