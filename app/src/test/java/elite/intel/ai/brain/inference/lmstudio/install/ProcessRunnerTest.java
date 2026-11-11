package elite.intel.ai.brain.inference.lmstudio.install;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// WHY real processes: the quiet limit is about how a running command's output arrives over time, which a fake would
// only restate. The commands are short shell lines, so this runs where bash does.
@DisabledOnOs(OS.WINDOWS)
class ProcessRunnerTest {

    private final ProcessRunner runner = new ProcessRunner(line -> {
    });

    @Test
    void aCommandThatKeepsReportingProgressRunsPastItsQuietLimit() throws InstallException {
        List<String> output = runner.run(
                List.of("bash", "-c", "for i in 1 2 3 4 5 6; do echo \"Loading $i\"; sleep 0.3; done"),
                Duration.ofSeconds(1), Duration.ofSeconds(30));

        assertEquals(6, output.size());
        assertEquals("Loading 6", output.getLast());
    }

    @Test
    void aCommandThatFallsSilentIsStoppedAtItsQuietLimit() {
        InstallException e = assertThrows(InstallException.class, () -> runner.run(List.of("bash", "-c", "echo Loading 98; sleep 30"),
                Duration.ofSeconds(1), Duration.ofSeconds(30)));
        assertTrue(e.getMessage().contains("reported no progress for 1 s"), e.getMessage());
    }

    @Test
    void aRepeatedLineIsNotProgress() {
        // A spinner redraws the same words; that says nothing about whether the work moves.
        InstallException e = assertThrows(InstallException.class, () -> runner.run(
                List.of("bash", "-c", "for i in $(seq 1 40); do echo Loading 98; sleep 0.1; done"),
                Duration.ofSeconds(1), Duration.ofSeconds(30)));
        assertTrue(e.getMessage().contains("reported no progress"), e.getMessage());
    }

    @Test
    void aCommandThatKeepsTalkingIsStillStoppedAtItsOverallLimit() {
        InstallException e = assertThrows(InstallException.class, () -> runner.run(
                List.of("bash", "-c", "i=0; while true; do i=$((i+1)); echo \"Loading $i\"; sleep 0.2; done"),
                Duration.ofSeconds(1), Duration.ofSeconds(2)));
        assertTrue(e.getMessage().contains("did not finish within 2 s"), e.getMessage());
    }

    @Test
    void aFailingCommandStillReportsItsExitCode() {
        InstallException e = assertThrows(InstallException.class, () -> runner.run(List.of("bash", "-c", "exit 3"), Duration.ofSeconds(5)));
        assertTrue(e.getMessage().contains("exit code 3"), e.getMessage());
    }
}
