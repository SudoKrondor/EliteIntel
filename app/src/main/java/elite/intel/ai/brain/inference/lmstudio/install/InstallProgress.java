package elite.intel.ai.brain.inference.lmstudio.install;

/**
 * What an install reports as it goes. Called on the install's own thread, never the event thread, so a Swing
 * listener hands each call over with {@code invokeLater}.
 */
public interface InstallProgress {

    void stepStarted(InstallStep step);

    /**
     * Already done - by an earlier, interrupted install or by the commander - so not run again.
     */
    void stepSkipped(InstallStep step);

    void stepFinished(InstallStep step);

    /**
     * One line of what the step did or what its command printed.
     */
    void log(String line);

    /**
     * Called about once a second while the model downloads.
     */
    void download(long downloadedBytes, long totalBytes, double bytesPerSecond);
}
