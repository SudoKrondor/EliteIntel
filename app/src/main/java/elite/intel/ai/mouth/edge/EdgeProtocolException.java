package elite.intel.ai.mouth.edge;

import java.io.IOException;

/** Signals a malformed or unexpected response from Edge's consumer Read Aloud service. */
final class EdgeProtocolException extends IOException {
    EdgeProtocolException(String message) {
        super(message);
    }

    EdgeProtocolException(String message, Throwable cause) {
        super(message, cause);
    }
}
