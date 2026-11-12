package elite.intel.ai.brain.inference.lmstudio.install;

import elite.intel.ai.brain.inference.lmstudio.LmStudioDefaults;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalLmStudioMachineTest {

    @Test
    void aRunningDaemonGivesItsPid() {
        // LM Studio's documented answer to lms daemon status --json.
        assertEquals(OptionalLong.of(12345),
                LocalLmStudioMachine.runningPid("{ \"status\": \"running\", \"pid\": 12345, \"isDaemon\": true }"));
    }

    @Test
    void aStatusPrintedOverSeveralLinesIsStillRead() {
        assertEquals(OptionalLong.of(46156),
                LocalLmStudioMachine.runningPid("{\n  \"status\": \"running\",\n  \"pid\": 46156\n}"));
    }

    @Test
    void aDaemonThatIsNotRunningGivesNoPid() {
        assertEquals(OptionalLong.empty(), LocalLmStudioMachine.runningPid("{ \"status\": \"not-running\" }"));
    }

    @Test
    void anythingElseCountsAsNotRunning() {
        assertEquals(OptionalLong.empty(), LocalLmStudioMachine.runningPid(""));
        assertEquals(OptionalLong.empty(), LocalLmStudioMachine.runningPid("Error: something went wrong"));
        assertEquals(OptionalLong.empty(), LocalLmStudioMachine.runningPid("{ \"status\": \"running\" }"));
        assertEquals(OptionalLong.empty(), LocalLmStudioMachine.runningPid("{ not json }"));
    }

    // WHY the shell's false and true as lms: the load runs a real command, and these two fail and pass it without
    // LM Studio. They are where bash is, hence not on Windows.
    private static final Path FAILING_LMS = Path.of("false");
    private static final Path PASSING_LMS = Path.of("true");
    private static final String MODEL = LmStudioDefaults.MODEL;

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void aLoadTheCommandGaveUpOnCountsWhenTheServerHasTheModelLoaded() {
        // The Windows tester's load (2026-11-10): lms was stopped, and the daemon finished loading moments later.
        List<String> asked = new ArrayList<>();
        LocalLmStudioMachine machine = new LocalLmStudioMachine(line -> {
        }, model -> asked.add(model));

        assertDoesNotThrow(() -> machine.load(FAILING_LMS, MODEL));
        assertEquals(List.of(MODEL), asked);
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void aLoadTheCommandGaveUpOnFailsWhenTheServerDoesNotHaveTheModel() {
        LocalLmStudioMachine machine = new LocalLmStudioMachine(line -> {
        }, model -> false);

        assertThrows(InstallException.class, () -> machine.load(FAILING_LMS, MODEL));
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void aLoadTheCommandGaveUpOnFailsWhenTheServerDoesNotAnswer() {
        LocalLmStudioMachine machine = new LocalLmStudioMachine(line -> {
        }, model -> {
            throw new InstallException("LM Studio did not answer");
        });

        assertThrows(InstallException.class, () -> machine.load(FAILING_LMS, MODEL));
    }

    @Test
    void aCancelledLoadFailsWithoutAskingTheServer() {
        List<String> asked = new ArrayList<>();
        LocalLmStudioMachine machine = new LocalLmStudioMachine(line -> {
        }, model -> asked.add(model));
        machine.cancel();

        InstallException failed = assertThrows(InstallException.class, () -> machine.load(PASSING_LMS, MODEL));
        assertEquals("Cancelled", failed.getMessage());
        assertTrue(asked.isEmpty(), "a cancel is the user's answer, whatever the server has loaded");
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void aLoadThatSucceedsDoesNotAskTheServer() {
        List<String> asked = new ArrayList<>();
        LocalLmStudioMachine machine = new LocalLmStudioMachine(line -> {
        }, model -> asked.add(model));

        assertDoesNotThrow(() -> machine.load(PASSING_LMS, MODEL));
        assertTrue(asked.isEmpty());
    }
}
