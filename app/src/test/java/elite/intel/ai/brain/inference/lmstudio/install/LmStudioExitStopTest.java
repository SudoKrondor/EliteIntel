package elite.intel.ai.brain.inference.lmstudio.install;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The one exit hook that stops the app's own LM Studio, with the hook registry faked.
 */
class LmStudioExitStopTest {

    @TempDir
    Path home;

    private final List<Thread> hooks = new ArrayList<>();
    private final LmStudioExitStop exitStop = new LmStudioExitStop(hooks::add, hooks::remove);

    @Test
    void theFirstToArmIsWhatRunsAtExit() throws IOException {
        Path lms = Files.createFile(home.resolve("lms"));
        List<String> stopped = new ArrayList<>();

        exitStop.arm(lms, () -> stopped.add("install"));
        exitStop.arm(lms, () -> stopped.add("host"));
        runHooks();

        assertEquals(1, hooks.size());
        assertEquals(List.of("install"), stopped, "one LM Studio, stopped once");
    }

    @Test
    void disarmingTakesTheHookBack() throws IOException {
        Path lms = Files.createFile(home.resolve("lms"));
        exitStop.arm(lms, () -> fail("disarmed"));

        assertTrue(exitStop.disarm());

        assertTrue(hooks.isEmpty());
        assertFalse(exitStop.isArmed());
        assertTrue(exitStop.disarm(), "nothing armed is nothing to take back");
    }

    @Test
    void anLmStudioDeletedSinceIsNotStopped() {
        List<String> stopped = new ArrayList<>();

        exitStop.arm(home.resolve("uninstalled").resolve("lms"), () -> stopped.add("stop"));
        runHooks();

        assertTrue(stopped.isEmpty(), "an uninstall already stopped it and took its lms away");
    }

    @Test
    void disarmingWhileTheAppExitsSaysSo() throws IOException {
        LmStudioExitStop exiting = new LmStudioExitStop(hooks::add, hook -> {
            throw new IllegalStateException("Shutdown in progress");
        });
        exiting.arm(Files.createFile(home.resolve("lms")), () -> {
        });

        assertFalse(exiting.disarm(), "the hook is stopping LM Studio, so the caller must not");
    }

    @Test
    void armingWhileTheAppExitsArmsNothing() throws IOException {
        LmStudioExitStop exiting = new LmStudioExitStop(hook -> {
            throw new IllegalStateException("Shutdown in progress");
        }, hooks::remove);

        exiting.arm(Files.createFile(home.resolve("lms")), () -> {
        });

        assertFalse(exiting.isArmed());
    }

    private void runHooks() {
        for (Thread hook : hooks) {
            hook.run();
        }
    }
}
