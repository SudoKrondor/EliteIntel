package elite.intel.ai.brain.inference.lmstudio.install;

import elite.intel.ai.brain.inference.lmstudio.install.LmStudioServer.DownloadStart;
import elite.intel.ai.brain.inference.lmstudio.install.LmStudioServer.DownloadStatus;

import java.nio.file.Path;
import java.util.OptionalInt;
import java.util.OptionalLong;

/**
 * Everything the install does to the PC: LM Studio's script, its {@code lms} command, its server, and the GPU.
 * <p>
 * WHY an interface: {@link LmStudioInstaller} decides which steps run, which are skipped and what a failure means,
 * and that is what needs testing - without installing LM Studio on the build server. {@link LocalLmStudioMachine}
 * is the real one.
 */
interface LmStudioMachine {

    /**
     * Points LM Studio's home at {@code home} through {@code ~/.lmstudio-home-pointer}.
     */
    void writePointer(Path home) throws InstallException;

    /**
     * Runs LM Studio's official install script, which installs into its home - the pointer's folder when one is set.
     */
    void runInstallScript() throws InstallException;

    void daemonUp(Path lms) throws InstallException;

    void daemonDown(Path lms) throws InstallException;

    /**
     * Starts the server on this PC only, and returns once it answers.
     */
    void startServer(Path lms) throws InstallException;

    boolean isServerAnswering();

    void stopServer(Path lms) throws InstallException;

    DownloadStart startDownload(String model) throws InstallException;

    DownloadStatus downloadStatus(String jobId) throws InstallException;

    /**
     * Whether LM Studio lists {@code model} as downloaded, so {@code lms load} can find it.
     */
    boolean isListed(String model) throws InstallException;

    boolean isLoaded(String model) throws InstallException;

    /**
     * The context length {@code model} is loaded with, or empty when it is not loaded.
     */
    OptionalInt loadedContextLength(String model) throws InstallException;

    void load(Path lms, String model) throws InstallException;

    void unload(Path lms, String model) throws InstallException;

    void unloadAll(Path lms) throws InstallException;

    OptionalLong gpuMemoryUsedMib();

    /**
     * Sends VEGA's kind of request and reports whether a tool call came back.
     *
     * @throws NoAnswerInTimeException when the server took the request but did not answer in time
     */
    boolean answersWithToolCall(String model) throws InstallException;

    /**
     * Stops the command running now, and every one after it.
     */
    void cancel();
}
