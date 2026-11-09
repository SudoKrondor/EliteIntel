package elite.intel.ai.brain.inference.lmstudio.install;

/**
 * How an install ended.
 */
public sealed interface InstallOutcome {

    /**
     * Every step finished: LM Studio serves the model from the GPU and the fields are saved.
     */
    record Completed() implements InstallOutcome {
    }

    /**
     * {@code step} could not finish, for {@code reason}. What was done before it is kept, so the next attempt
     * continues from there.
     */
    record Failed(InstallStep step, String reason) implements InstallOutcome {
    }

    /**
     * The commander stopped it. As with a failure, what was done is kept.
     */
    record Cancelled() implements InstallOutcome {
    }
}
