package elite.intel.ai.brain.inference.lmstudio.install;

import elite.intel.ai.brain.inference.lmstudio.LmStudioDefaults;
import elite.intel.db.managers.LmStudioInstallManager.Install;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The app's own LM Studio runs while the services do, against a {@link FakeLmStudioMachine}.
 */
class LmStudioHostTest {

    @TempDir
    Path home;

    private final List<Thread> exitHooks = new ArrayList<>();
    private final LmStudioExitStop exitStop = new LmStudioExitStop(exitHooks::add, exitHooks::remove);

    @Test
    void startingBringsUpTheServerAndLoadsTheModel() {
        FakeLmStudioMachine machine = new FakeLmStudioMachine(home);
        LmStudioHost host = new LmStudioHost(home.resolve("bin/lms"), machine, exitStop);

        host.start();

        assertEquals(List.of("daemon up", "server start", "load"), machine.calls);
        host.stop();
    }

    @Test
    void whatIsAlreadyRunningIsLeftAsItIs() {
        FakeLmStudioMachine machine = new FakeLmStudioMachine(home);
        machine.serverAnswering = true;
        machine.loaded = true;
        LmStudioHost host = new LmStudioHost(home.resolve("bin/lms"), machine, exitStop);

        host.start();

        assertTrue(machine.calls.isEmpty(), "a model already loaded with VEGA's window is not loaded twice");
        host.stop();
    }

    @Test
    void aModelLoadedWithASmallerWindowIsReloadedWithVegas() {
        assertReloadedFrom(4096);
    }

    @Test
    void aModelLoadedWithALargerWindowIsReloadedWithVegas() {
        assertReloadedFrom(16384);
    }

    private void assertReloadedFrom(int loadedContext) {
        FakeLmStudioMachine machine = new FakeLmStudioMachine(home);
        machine.serverAnswering = true;
        machine.loaded = true;
        machine.loadedContext = loadedContext;
        LmStudioHost host = new LmStudioHost(home.resolve("bin/lms"), machine, exitStop);

        host.start();

        assertEquals(List.of("unload", "load"), machine.calls);
        assertEquals(LocalLmStudioMachine.CONTEXT_LENGTH, machine.loadedContext);
        host.stop();
    }

    @Test
    void stoppingUnloadsTheModelAndStopsTheServerAndTheDaemon() {
        FakeLmStudioMachine machine = new FakeLmStudioMachine(home);
        LmStudioHost host = new LmStudioHost(home.resolve("bin/lms"), machine, exitStop);
        host.start();
        machine.calls.clear();

        host.stop();

        assertEquals(List.of("unload all", "server stop", "daemon down"), machine.calls);
    }

    @Test
    void aStartedHostIsStoppedAtExitUntilItIsStoppedItself() {
        FakeLmStudioMachine machine = new FakeLmStudioMachine(home);
        LmStudioHost host = new LmStudioHost(home.resolve("bin/lms"), machine, exitStop);

        host.start();
        assertTrue(exitStop.isArmed());

        host.stop();
        assertFalse(exitStop.isArmed(), "stopped by the services, so nothing is left for the exit to stop");
        assertTrue(exitHooks.isEmpty());
    }

    @Test
    void aStopWhileTheAppIsExitingLeavesTheStopToTheExitHook() {
        FakeLmStudioMachine machine = new FakeLmStudioMachine(home);
        LmStudioExitStop exiting = new LmStudioExitStop(exitHooks::add, hook -> {
            throw new IllegalStateException("Shutdown in progress");
        });
        LmStudioHost host = new LmStudioHost(home.resolve("bin/lms"), machine, exiting);
        host.start();
        machine.calls.clear();

        host.stop();

        assertTrue(machine.calls.isEmpty(), "the exit hook stops it, once");
    }

    @Test
    void aFailedStartStopsNothingElseAndIsNotStoppedLater() {
        FakeLmStudioMachine machine = new FakeLmStudioMachine(home);
        machine.cancel(); // its daemon commands now fail, as a broken install's would
        LmStudioHost host = new LmStudioHost(home.resolve("bin/lms"), machine, exitStop);

        assertDoesNotThrow(host::start, "the other services carry on without LM Studio");
        host.stop();

        assertFalse(machine.calls.contains("unload all"), "nothing was started, so nothing is stopped");
        assertFalse(exitStop.isArmed());
    }

    @Test
    void onlyAFinishedOwnInstallRunsAndOnlyForALocalBrainOnThisPc() {
        String local = LmStudioDefaults.ADDRESS;

        assertTrue(LmStudioHost.shouldRun(Optional.of(install(true)), true, local));
        assertFalse(LmStudioHost.shouldRun(Optional.empty(), true, local), "an LM Studio the app did not install");
        assertFalse(LmStudioHost.shouldRun(Optional.of(install(false)), true, local), "an unfinished install");
        assertFalse(LmStudioHost.shouldRun(Optional.of(install(true)), false, local), "the brain is in the cloud");
        assertFalse(LmStudioHost.shouldRun(Optional.of(install(true)), true,
                "http://192.168.1.20:1234/v1/chat/completions"), "the brain is on another machine");
    }

    private Install install(boolean setupComplete) {
        return new Install(UUID.randomUUID(), home, false, setupComplete, Instant.now());
    }
}
