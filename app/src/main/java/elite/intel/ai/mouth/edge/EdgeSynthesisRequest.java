package elite.intel.ai.mouth.edge;

/** Provider-specific synthesis request after voice and prosody resolution. */
record EdgeSynthesisRequest(
        String requestId,
        String text,
        EdgeVoice voice,
        String rate,
        String volume,
        String pitch
) {
    EdgeSynthesisRequest {
        if (requestId == null || requestId.isBlank()) {
            throw new IllegalArgumentException("Edge synthesis request id must not be blank");
        }
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Edge synthesis text must not be blank");
        }
        if (voice == null) {
            throw new IllegalArgumentException("Edge synthesis voice must not be null");
        }
    }
}
