package elite.intel.ai.brain.vega.memory;

import elite.intel.ai.brain.commons.AiResponseLanguagePolicy;
import elite.intel.ai.brain.vega.model.llm.LlmMessage;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompressionPromptComposerTest {

    private final CompressionPromptComposer composer = new CompressionPromptComposer();

    private static String resolvedLanguageName() {
        Language language = AiResponseLanguagePolicy.resolveEffectiveAiResponseLanguage(SystemSession.getInstance());
        return language.displayName();
    }

    @Test
    void lineCompressionUsesProvenSingleSentenceContract() {
        String source = "First leg A to B. Second leg B to C.";
        List<LlmMessage> messages = composer.composeLineCompression(source);

        assertEquals(2, messages.size());
        String system = messages.get(0).content();
        assertTrue(system.contains("ONE short sentence (about 15 words)"));
        assertTrue(system.contains("single most important point"));
        assertTrue(system.contains("never invent, change"));
        assertTrue(system.contains("Write numbers as digits"));
        assertTrue(system.contains("Call speak exactly once"));
        assertTrue(system.contains("speak.text"));
        assertTrue(system.contains("do not return free text"));
        assertTrue(system.contains("Lembava"));
        assertTrue(system.contains("write the summary in " + resolvedLanguageName()));
        assertEquals(source, messages.get(1).content());
    }
}
