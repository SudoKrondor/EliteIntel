package elite.intel.ai;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The one-time carry-over from key-format detection to a stored provider: it may only ever fill a gap, never
 * overwrite what the commander picked.
 */
class LlmProviderUpgradeTest {

    private static final String MISTRAL_KEY = "mstrl_aB3dE6gH9jK2mN5pQ8sT1vW4yZ7bC0eF_x9Yz-Q1";

    @Test
    void aKeyWithNoProviderGetsTheProviderItsFormatNames() {
        assertEquals(Optional.of(ProviderEnum.MISTRAL), LlmProviderUpgrade.adopt(Optional.empty(), MISTRAL_KEY));
    }

    /**
     * A commander who picked a provider by hand is never second-guessed by the key's shape - even when the key
     * looks like another vendor's.
     */
    @Test
    void aStoredProviderIsNeverOverwritten() {
        assertEquals(Optional.empty(), LlmProviderUpgrade.adopt(Optional.of(ProviderEnum.OPENAI), MISTRAL_KEY));
    }

    @Test
    void noKeyMeansNothingToAdopt() {
        assertEquals(Optional.empty(), LlmProviderUpgrade.adopt(Optional.empty(), null));
        assertEquals(Optional.empty(), LlmProviderUpgrade.adopt(Optional.empty(), ""));
        assertEquals(Optional.empty(), LlmProviderUpgrade.adopt(Optional.empty(), "   "));
    }

    /**
     * An unrecognised key stays without a provider, for the setup check to ask about - not a guess.
     */
    @Test
    void anUnrecognisedKeyIsLeftForTheCommanderToResolve() {
        assertEquals(Optional.empty(), LlmProviderUpgrade.adopt(Optional.empty(), "some-future-key-format"));
    }
}
