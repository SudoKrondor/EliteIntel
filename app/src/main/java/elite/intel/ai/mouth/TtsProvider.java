package elite.intel.ai.mouth;

/**
 * The engine that voices the companion. Exactly one is active, and the choice is a stored setting in its own
 * right ({@code game_session.ttsProvider}) - it is never inferred from the shape of the cloud API key.
 * <p>
 * {@link #KOKORO} runs locally and needs nothing configured, which is why it is both the shipped default and
 * the fallback for an unreadable stored value: a commander with no cloud account still has a voice. {@link #GOOGLE}
 * is the only engine that needs an API key. {@link #EDGE} is Microsoft's online Read Aloud service, which is
 * keyless but not local - it still talks to Microsoft over the network.
 */
public enum TtsProvider {
    KOKORO,
    GOOGLE,
    EDGE;

    /**
     * Resolves a stored setting value, falling back to {@link #KOKORO} for anything this build does not
     * recognise (null, blank, or a provider written by a newer version).
     */
    public static TtsProvider fromStored(String stored) {
        if (stored == null || stored.isBlank()) {
            return KOKORO;
        }
        for (TtsProvider provider : values()) {
            if (provider.name().equalsIgnoreCase(stored.trim())) {
                return provider;
            }
        }
        return KOKORO;
    }

    /**
     * Whether the engine synthesises on this machine, with no network call and no account.
     */
    public boolean isLocal() {
        return this == KOKORO;
    }
}
