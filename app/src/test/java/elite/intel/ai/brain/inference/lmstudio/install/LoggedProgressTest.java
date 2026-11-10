package elite.intel.ai.brain.inference.lmstudio.install;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Logging an install must not change what the dialog is told: every report reaches it, the download included,
 * however rarely the log takes a download line.
 */
class LoggedProgressTest {

    @Test
    void everyReportIsPassedOn() {
        List<String> seen = new ArrayList<>();
        InstallProgress dialog = new InstallProgress() {
            @Override
            public void stepStarted(InstallStep step) {
                seen.add("started " + step);
            }

            @Override
            public void stepSkipped(InstallStep step) {
                seen.add("skipped " + step);
            }

            @Override
            public void stepFinished(InstallStep step) {
                seen.add("finished " + step);
            }

            @Override
            public void log(String line) {
                seen.add(line);
            }

            @Override
            public void download(long downloadedBytes, long totalBytes, double bytesPerSecond) {
                seen.add("download " + downloadedBytes);
            }
        };
        LoggedProgress progress = new LoggedProgress(dialog);

        progress.stepSkipped(InstallStep.START);
        progress.stepStarted(InstallStep.DOWNLOAD);
        progress.download(1, 100, 1);
        progress.download(2, 100, 1);
        progress.download(0, 0, 0);
        progress.stepFinished(InstallStep.DOWNLOAD);
        progress.log("Graphics memory in use rose by 6100 MiB");

        assertEquals(List.of("skipped START", "started DOWNLOAD", "download 1", "download 2", "download 0",
                "finished DOWNLOAD", "Graphics memory in use rose by 6100 MiB"), seen);
    }
}
