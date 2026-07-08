package elite.intel.companion.prompt;

/**
 * One inlined answer fact for the prompt's {@code <facts>} block: the recalled text plus its provenance
 * ({@code source}), so the prompt can tag each fact with where it came from - {@code "event"} (a past ship/game
 * occurrence), {@code "commander"} (something the commander stated), or a pluggable fact source's own id (e.g. a
 * live ship/system fact). The provenance lets the model tell a remembered past fact from live state and answer
 * from it instead of re-querying (OpenAI long-context guidance: metadata in attributes).
 */
public record Fact(String text, String source) {
}
