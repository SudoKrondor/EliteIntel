package elite.intel.ai.brain.health;

import elite.intel.ai.ProviderEnum;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MistralFundingRefusalTest {

    private static final Optional<ProviderEnum> MISTRAL = Optional.of(ProviderEnum.MISTRAL);

    @Test
    void mistralAnswering401Or402IsAskingForFunds() {
        assertTrue(MistralFundingRefusal.matches(MISTRAL, false, 402));
        assertTrue(MistralFundingRefusal.matches(MISTRAL, false, 401));
    }

    @Test
    void otherMistralRefusalsSayNothingAboutMoney() {
        // A 429 from Mistral has meant a week-long outage before; it is never read as a billing problem.
        assertFalse(MistralFundingRefusal.matches(MISTRAL, false, 429));
        assertFalse(MistralFundingRefusal.matches(MISTRAL, false, 403));
        assertFalse(MistralFundingRefusal.matches(MISTRAL, false, null));
    }

    @Test
    void onlyMistralInTheCloudCounts() {
        assertFalse(MistralFundingRefusal.matches(Optional.of(ProviderEnum.OPENAI), false, 402));
        assertFalse(MistralFundingRefusal.matches(Optional.empty(), false, 402));
        assertFalse(MistralFundingRefusal.matches(MISTRAL, true, 402), "the refusal came from LM Studio");
    }
}
