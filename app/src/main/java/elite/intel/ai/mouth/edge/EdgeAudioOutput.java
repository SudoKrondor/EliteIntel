package elite.intel.ai.mouth.edge;

import java.util.function.BooleanSupplier;

/** Playback seam so queue and interruption behaviour can be tested without audio hardware. */
interface EdgeAudioOutput {
    void open() throws Exception;

    boolean play(byte[] pcm, BooleanSupplier interrupted) throws Exception;

    void interrupt();

    void close();
}
