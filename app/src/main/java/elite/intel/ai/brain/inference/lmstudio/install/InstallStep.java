package elite.intel.ai.brain.inference.lmstudio.install;

/**
 * The steps of an install, in order (docs/LM_STUDIO_INSTALL_ASSISTANCE.md, "The install"). Setting up an LM Studio
 * the app did not install runs only {@link #START} onwards.
 */
public enum InstallStep {
    /**
     * Free space on the chosen drive.
     */
    CHECK,
    /**
     * The pointer, the install record and its marker, then LM Studio's script.
     */
    INSTALL,
    /**
     * LM Studio's daemon and its server on this PC.
     */
    START,
    DOWNLOAD,
    LOAD,
    /**
     * That the model went onto the graphics card rather than the CPU.
     */
    GPU_CHECK,
    /**
     * One request shaped as VEGA's, answered with a tool call.
     */
    TEST,
    /**
     * Address and Model, saved through the AI Services panel.
     */
    SAVE
}
