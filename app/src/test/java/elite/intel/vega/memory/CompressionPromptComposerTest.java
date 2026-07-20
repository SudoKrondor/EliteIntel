package elite.intel.vega.memory;

import elite.intel.ai.brain.commons.AiResponseLanguagePolicy;
import elite.intel.vega.model.llm.LlmMessage;
import elite.intel.vega.model.llm.LlmMessageRole;
import elite.intel.vega.model.memory.MemoryKind;
import elite.intel.vega.model.memory.MemoryRecord;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;
import org.junit.jupiter.api.Test;

import java.time.Instant;
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
    void buildsKindSpecificPromptFromCompletedRecords() {
        MemoryRecord first = MemoryRecord.event(Instant.EPOCH, "found platinum");
        MemoryRecord second = MemoryRecord.event(Instant.ofEpochSecond(1), "jumped to Sol");

        List<LlmMessage> messages = composer.compose(
                MemoryKind.EVENT, "known so far", List.of(first, second));

        assertEquals(2, messages.size());
        assertEquals(LlmMessageRole.SYSTEM, messages.get(0).role());
        assertTrue(messages.get(0).content().contains(String.valueOf(CompanionMemoryPolicy.summaryMaxChars())));
        assertTrue(messages.get(0).content().contains("write the summary in " + resolvedLanguageName()));

        String user = messages.get(1).content();
        assertTrue(user.contains("Memory kind: EVENT"));
        assertTrue(user.contains("known so far"));
        assertTrue(user.contains("[EVENT] found platinum"));
        assertTrue(user.contains("[EVENT] jumped to Sol"));
    }

    @Test
    void rendersNoneWhenSummaryIsEmpty() {
        List<LlmMessage> messages = composer.compose(
                MemoryKind.DIALOGUE, "", List.of(MemoryRecord.dialogue(Instant.EPOCH, "hello", "hello")));

        assertTrue(messages.get(1).content().contains("Existing summary:\n(none)"));
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
