package elite.intel.ai;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProviderEnumTest {

    @Test
    void aStoredNameReadsBackAsItsProvider() {
        for (ProviderEnum provider : ProviderEnum.values()) {
            assertEquals(Optional.of(provider), ProviderEnum.fromStored(provider.name()));
        }
        assertEquals(Optional.of(ProviderEnum.GEMINI), ProviderEnum.fromStored(" gemini "));
    }

    /**
     * Nothing stored, or a value this build does not know - written by a newer version, or a provider since
     * dropped - is "not selected", so the commander is asked rather than routed to a guess.
     */
    @Test
    void anythingElseIsNotSelected() {
        assertEquals(Optional.empty(), ProviderEnum.fromStored(null));
        assertEquals(Optional.empty(), ProviderEnum.fromStored(""));
        assertEquals(Optional.empty(), ProviderEnum.fromStored("AWS_LLM"));
    }
}
