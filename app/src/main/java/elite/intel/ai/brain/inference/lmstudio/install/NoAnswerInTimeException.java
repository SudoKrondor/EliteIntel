package elite.intel.ai.brain.inference.lmstudio.install;

/**
 * LM Studio's server took the request but sent nothing back before its time ran out - slow, not refusing.
 * <p>
 * WHY its own type: the setup test tries again on this one alone. Right after a new install, the first answers can
 * take minutes, then come in seconds: a Windows tester's first test timed out at 2 minutes and the next passed in
 * 21 s (2026-11-10). A wrong answer or an error status is a real failure and is not retried.
 */
final class NoAnswerInTimeException extends InstallException {

    NoAnswerInTimeException(String message, Throwable cause) {
        super(message, cause);
    }
}
