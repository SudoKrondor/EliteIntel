package elite.intel.ai.brain.inference.lmstudio.install;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Stops the app's own LM Studio when the app exits, whichever part of the app has it running: the services
 * ({@link LmStudioHost}), or an install run - finished or not - while the services were stopped.
 * <p>
 * WHY one hook for both: an install that stops part-way leaves LM Studio's daemon running with no service to stop it
 * (a Windows tester's did, 2026-11-10), and a finished one hands it to the host, which must not stop it a second time
 * at exit. Only a normal exit runs it; a process killed from the task manager stops nothing.
 */
final class LmStudioExitStop {

    static final LmStudioExitStop INSTANCE = new LmStudioExitStop(
            Runtime.getRuntime()::addShutdownHook, Runtime.getRuntime()::removeShutdownHook);

    private final Consumer<Thread> addHook;
    private final Predicate<Thread> removeHook;
    private Thread hook;

    /**
     * @param addHook    registers a thread to run at exit; throws {@link IllegalStateException} once the app is exiting
     * @param removeHook takes a registered thread back; throws {@link IllegalStateException} once the app is exiting
     */
    LmStudioExitStop(Consumer<Thread> addHook, Predicate<Thread> removeHook) {
        this.addHook = addHook;
        this.removeHook = removeHook;
    }

    /**
     * Has {@code stop} run at exit, unless something is already armed: the first to arm keeps it, and they all stop
     * the same LM Studio. Skipped at exit when {@code lms} is gone - an uninstall already stopped and deleted it.
     */
    synchronized void arm(Path lms, Runnable stop) {
        if (hook != null) return;
        Thread next = new Thread(() -> {
            if (Files.exists(lms)) stop.run();
        }, "lm-studio-shutdown");
        try {
            addHook.accept(next);
        } catch (IllegalStateException exiting) {
            // WHY nothing to do: the app is already exiting, and an LM Studio started this late is not this run's.
            return;
        }
        hook = next;
    }

    /**
     * Takes the exit stop back, for a caller about to stop LM Studio itself.
     *
     * @return false when the app is already exiting, so the hook is stopping LM Studio and the caller must not
     */
    synchronized boolean disarm() {
        if (hook == null) return true;
        try {
            removeHook.test(hook);
        } catch (IllegalStateException exiting) {
            return false;
        }
        hook = null;
        return true;
    }

    synchronized boolean isArmed() {
        return hook != null;
    }
}
