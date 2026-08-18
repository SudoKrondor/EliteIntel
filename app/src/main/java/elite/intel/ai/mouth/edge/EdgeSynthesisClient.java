package elite.intel.ai.mouth.edge;

import java.io.IOException;
import java.util.List;

/** Transport seam for deterministic tests and the native Java Edge Read Aloud client. */
interface EdgeSynthesisClient {
    List<EdgeVoice> listVoices() throws IOException, InterruptedException;

    byte[] synthesize(EdgeSynthesisRequest request) throws IOException, InterruptedException;

    default void cancel(String requestId) {
    }

    default void cancelAll() {
    }
}
