package elite.intel.ai.brain.inference.lmstudio;

/**
 * The LM Studio setup the app is built around: the model it supports and LM Studio's own local address. Restore
 * Defaults on the AI Services panel puts these back, and the LM Studio install assistance fills them in.
 */
public final class LmStudioDefaults {

    /**
     * The supported local model, as LM Studio names it. V1.1 and V1.2 are built around it because the VEGA pipeline
     * needs tool calling.
     */
    public static final String MODEL = "google/gemma-4-e4b";

    /**
     * The port LM Studio's server listens on out of the box.
     */
    public static final int PORT = 1234;

    /**
     * LM Studio's server root on this PC, under which both its OpenAI-compatible {@code /v1} and its own
     * {@code /api/v1} live.
     */
    public static final String SERVER_ROOT = "http://localhost:" + PORT;

    /**
     * LM Studio's out-of-the-box OpenAI-compatible endpoint; the only local host this app talks to.
     */
    public static final String ADDRESS = SERVER_ROOT + "/v1/chat/completions";

    private LmStudioDefaults() {
    }
}
