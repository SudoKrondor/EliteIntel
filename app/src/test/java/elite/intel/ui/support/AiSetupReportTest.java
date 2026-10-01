package elite.intel.ui.support;

import elite.intel.ai.ProviderEnum;
import elite.intel.ai.brain.inference.mistral.MistralClient;
import elite.intel.ai.mouth.TtsProvider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The AI setup is the first thing a report about a wrong or odd answer is triaged on, so each way the commander
 * can be set up must read differently - and none of them may carry a key.
 */
class AiSetupReportTest {

    @Test
    void namesTheCloudProviderAndTheModelItServes() {
        String report = AiSetupReport.describe(false, false, ProviderEnum.MISTRAL, null, null, TtsProvider.KOKORO);

        assertTrue(report.contains("LLM: cloud - MISTRAL, model " + MistralClient.MODEL), report);
        assertTrue(report.contains("Text-to-speech: KOKORO (local)"), report);
    }

    @Test
    void saysSoWhenNoCloudProviderIsSelected() {
        String report = AiSetupReport.describe(false, false, null, null, null, TtsProvider.EDGE);

        assertTrue(report.contains("LLM: cloud - no provider selected"), report);
    }

    @Test
    void namesTheLocalAddressAndModel() {
        String report = AiSetupReport.describe(true, true, ProviderEnum.MISTRAL,
                "http://localhost:1234", " gemma-4-e4b ", TtsProvider.KOKORO);

        assertTrue(report.contains("LLM: local - LM Studio at http://localhost:1234, model gemma-4-e4b"), report);
        // The stored cloud provider is not what answers a local setup, so it is not named.
        assertFalse(report.contains("MISTRAL"), report);
    }

    /**
     * A fresh install ships with the model empty, and that is the setup that most needs a bundle.
     */
    @Test
    void saysSoWhenNoLocalModelIsConfigured() {
        String report = AiSetupReport.describe(true, true, null, "http://localhost:1234", "", TtsProvider.KOKORO);

        assertTrue(report.contains("no model configured"), report);
    }

    @Test
    void reportsTheQueriesLlmOnlyWhenItDiffers() {
        String same = AiSetupReport.describe(false, false, ProviderEnum.OPENAI, null, null, TtsProvider.KOKORO);
        String split = AiSetupReport.describe(false, true, ProviderEnum.OPENAI,
                "http://localhost:1234", "gemma-4-e4b", TtsProvider.KOKORO);

        assertFalse(same.contains("Queries LLM"), same);
        assertTrue(split.contains("Queries LLM: local - LM Studio"), split);
    }
}
