package elite.intel.ai.brain.actions.customcommand;

/**
 * Strategy for executing a SPEAK step in a customCommand.
 * <p>
 * The implementation must block the calling thread until speech playback is complete
 * (or until a timeout or failure), so the next custom command step does not start while TTS is still speaking.
 *
 * @see SynchronousCustomCommandSpeech
 */
@FunctionalInterface
interface CustomCommandSpeakExecutor {

    /**
     * Speaks {@code text} and blocks until TTS playback finishes.
     *
     * @throws InterruptedException if the custom command execution thread is interrupted while waiting
     */
    void speak(String text) throws InterruptedException;
}
