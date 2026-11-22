package elite.intel.ai.brain.inference.lmstudio.install;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Locale;

/**
 * Writes everything an install reports to the LM Studio log ({@code logs/lm-studio.log}), then passes it on.
 * <p>
 * WHY: the dialog's details and the in-app system log are both gone once the app restarts, which is the first thing a
 * commander tries after a failed install. The LM Studio log outlives that, and a support bundle carries it.
 * <p>
 * The download reports about once a second, so it is logged at each tenth of the way only. Called on the install's
 * own thread, one call at a time.
 */
final class LoggedProgress implements InstallProgress {

    private static final Logger log = LogManager.getLogger(LoggedProgress.class);

    private static final double BYTES_PER_GB = 1024.0 * 1024 * 1024;
    private static final double BYTES_PER_MB = 1024.0 * 1024;

    private final InstallProgress next;
    private int downloadTenthLogged = -1;

    LoggedProgress(InstallProgress next) {
        this.next = next;
    }

    @Override
    public void stepStarted(InstallStep step) {
        downloadTenthLogged = -1;
        log.info("Step {}: started", step);
        next.stepStarted(step);
    }

    @Override
    public void stepSkipped(InstallStep step) {
        log.info("Step {}: already done", step);
        next.stepSkipped(step);
    }

    @Override
    public void stepFinished(InstallStep step) {
        log.info("Step {}: done", step);
        next.stepFinished(step);
    }

    @Override
    public void log(String line) {
        log.info(line);
        next.log(line);
    }

    @Override
    public void download(long downloadedBytes, long totalBytes, double bytesPerSecond) {
        int tenth = totalBytes > 0 ? (int) Math.min(10, downloadedBytes * 10 / totalBytes) : 0;
        if (tenth != downloadTenthLogged) {
            downloadTenthLogged = tenth;
            log.info(String.format(Locale.ROOT, "Download %d%%: %.1f of %.1f GB at %.1f MB/s", tenth * 10,
                    downloadedBytes / BYTES_PER_GB, totalBytes / BYTES_PER_GB, bytesPerSecond / BYTES_PER_MB));
        }
        next.download(downloadedBytes, totalBytes, bytesPerSecond);
    }
}
