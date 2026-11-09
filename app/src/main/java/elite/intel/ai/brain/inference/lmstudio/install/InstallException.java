package elite.intel.ai.brain.inference.lmstudio.install;

/**
 * An install step that could not finish: a command that failed, LM Studio answering wrongly, or a check that did not
 * pass. The message is for the install log, which a support bundle carries, so it says what was tried and what came
 * back.
 */
public class InstallException extends Exception {

    public InstallException(String message) {
        super(message);
    }

    public InstallException(String message, Throwable cause) {
        super(message, cause);
    }
}
