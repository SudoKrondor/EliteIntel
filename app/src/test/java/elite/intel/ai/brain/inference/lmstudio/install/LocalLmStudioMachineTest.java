package elite.intel.ai.brain.inference.lmstudio.install;

import org.junit.jupiter.api.Test;

import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
