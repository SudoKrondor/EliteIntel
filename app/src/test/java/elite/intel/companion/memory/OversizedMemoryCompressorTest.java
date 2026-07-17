package elite.intel.companion.memory;

import com.google.gson.JsonObject;
import elite.intel.companion.CompanionRuntimeGeneration;
import elite.intel.companion.diag.CompanionMemoryDump;
import elite.intel.companion.llm.LlmGateway;
import elite.intel.companion.model.llm.LlmRequest;
import elite.intel.companion.model.llm.LlmResult;
import elite.intel.companion.model.llm.LlmToolInvocation;
import elite.intel.companion.model.llm.PromptCacheProfile;
import elite.intel.companion.model.memory.MemoryKind;
import elite.intel.companion.model.memory.MemoryRecord;
import elite.intel.companion.tools.SpeakFunction;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OversizedMemoryCompressorTest {

    @Test
    void compressesLongQueryAnswerAndRewritesOneCompletePair() {
        SessionMemoryGateway memory = new SessionMemoryGateway(text -> 0);
        FakeLlm llm = new FakeLlm();
        llm.response = CompletableFuture.completedFuture(speakResult(
                "Route: Kharan/Yang Enterprise to Sinufee/Leonid Progress;\nthen to Cubeo/Medupe City."));
        OversizedMemoryCompressor compressor = new OversizedMemoryCompressor(
                memory, llm, new CompanionRuntimeGeneration(), Runnable::run);
        memory.setOversizedMemoryListener(compressor);
        String fullAnswer = "The first route leg contains many spoken details. "
                + "The second route leg contains the remaining destination details. ".repeat(8);

        memory.write(MemoryRecord.query(Instant.EPOCH, "current trade route", fullAnswer));

        assertEquals(1, llm.calls);
        assertEquals(PromptCacheProfile.COMPRESSION, llm.lastRequest.profile());
        assertTrue(llm.lastRequest.messages().get(0).content().contains("single most important point"));
        assertEquals(1, llm.lastRequest.tools().size());
        assertEquals(SpeakFunction.ID, llm.lastRequest.tools().getFirst().name());
        assertEquals(SpeakFunction.PARAM_TEXT,
                llm.lastRequest.tools().getFirst().parameters().getFirst().getName());
        assertEquals(1, memory.readRecentHistory().size());
        MemoryRecord stored = memory.readRecentHistory().getFirst();
        assertEquals(MemoryKind.QUERY, stored.kind());
        assertEquals(2, stored.entryCount());
        assertEquals("current trade route", stored.commanderText());
        assertEquals("Route: Kharan/Yang Enterprise to Sinufee/Leonid Progress; then to Cubeo/Medupe City.",
                stored.companionText());
        assertFalse(stored.companionText().endsWith("..."));
        String dump = CompanionMemoryDump.toJson(memory.snapshot());
        assertTrue(dump.contains("Route: Kharan/Yang Enterprise"));
        assertFalse(dump.contains("The first route leg contains many spoken details"));
    }

    @Test
    void providerFailureFallsBackToAWholeBoundedRecord() {
        SessionMemoryGateway memory = new SessionMemoryGateway(text -> 0);
        FakeLlm llm = new FakeLlm();
        llm.response = CompletableFuture.failedFuture(new IllegalStateException("offline"));
        OversizedMemoryCompressor compressor = new OversizedMemoryCompressor(
                memory, llm, new CompanionRuntimeGeneration(), Runnable::run);
        memory.setOversizedMemoryListener(compressor);

        memory.write(MemoryRecord.dialogue(
                Instant.EPOCH, "tell me", "complete words for the fallback path ".repeat(20)));

        MemoryRecord stored = memory.readRecentHistory().getFirst();
        assertEquals(MemoryKind.DIALOGUE, stored.kind());
        assertEquals(2, stored.entryCount());
        assertTrue(stored.companionText().length() <= CompanionMemoryPolicy.entryMaxChars());
        assertTrue(stored.companionText().endsWith("..."));
        assertFalse(stored.companionText().endsWith("pa..."));
    }

    @Test
    void ignoresDroppedReasoningAndStoresOnlySpeakText() {
        SessionMemoryGateway memory = new SessionMemoryGateway(text -> 0);
        FakeLlm llm = new FakeLlm();
        llm.response = CompletableFuture.completedFuture(speakResult(
                "Route continues from Kharan through Sinufee to Cubeo.",
                "The user wants a concise memory. Analysis and draft follow. ".repeat(8)));
        OversizedMemoryCompressor compressor = new OversizedMemoryCompressor(
                memory, llm, new CompanionRuntimeGeneration(), Runnable::run);
        memory.setOversizedMemoryListener(compressor);

        memory.write(MemoryRecord.event(Instant.EPOCH,
                "event detail ".repeat(CompanionMemoryPolicy.entryMaxChars())));

        assertEquals("Route continues from Kharan through Sinufee to Cubeo.",
                memory.readRecentHistory().getFirst().eventFact());
    }

    @Test
    void oversizedGistIsBoundedBeforeAtomicRewrite() {
        SessionMemoryGateway memory = new SessionMemoryGateway(text -> 0);
        FakeLlm llm = new FakeLlm();
        llm.response = CompletableFuture.completedFuture(speakResult(
                "summary word ".repeat(CompanionMemoryPolicy.entryMaxChars())));
        OversizedMemoryCompressor compressor = new OversizedMemoryCompressor(
                memory, llm, new CompanionRuntimeGeneration(), Runnable::run);
        memory.setOversizedMemoryListener(compressor);

        memory.write(MemoryRecord.event(Instant.EPOCH,
                "event detail ".repeat(CompanionMemoryPolicy.entryMaxChars())));

        MemoryRecord stored = memory.readRecentHistory().getFirst();
        assertEquals(MemoryKind.EVENT, stored.kind());
        assertTrue(stored.eventFact().length() <= CompanionMemoryPolicy.entryMaxChars());
        assertTrue(stored.eventFact().endsWith("..."));
    }

    @Test
    void closeInterruptsPendingCompressionAndPreventsLateRewrite() throws Exception {
        SessionMemoryGateway memory = new SessionMemoryGateway(text -> 0);
        BlockingLlm llm = new BlockingLlm();
        CompanionRuntimeGeneration generation = new CompanionRuntimeGeneration();
        ExecutorService worker = Executors.newSingleThreadExecutor();
        OversizedMemoryCompressor compressor = new OversizedMemoryCompressor(
                memory, llm, generation, worker);
        memory.setOversizedMemoryListener(compressor);

        memory.write(MemoryRecord.event(Instant.EPOCH,
                "event detail ".repeat(CompanionMemoryPolicy.entryMaxChars())));
        assertTrue(llm.started.await(1, TimeUnit.SECONDS));

        compressor.close();
        llm.result.complete(speakResult("late gist"));

        assertTrue(worker.awaitTermination(1, TimeUnit.SECONDS));
        assertTrue(memory.readRecentHistory().isEmpty());
        assertFalse(compressor.onOversized(MemoryRecord.event(
                Instant.ofEpochSecond(1), "x".repeat(CompanionMemoryPolicy.entryMaxChars() + 1))));
    }

    private static LlmResult speakResult(String text) {
        return speakResult(text, null);
    }

    private static LlmResult speakResult(String text, String droppedText) {
        JsonObject arguments = new JsonObject();
        arguments.addProperty(SpeakFunction.PARAM_TEXT, text);
        LlmToolInvocation speak = new LlmToolInvocation("gist-call", SpeakFunction.ID, arguments);
        return new LlmResult(LlmResult.Status.OK, java.util.List.of(speak), "tool_calls", droppedText);
    }

    private static final class FakeLlm implements LlmGateway {
        private CompletableFuture<LlmResult> response;
        private LlmRequest lastRequest;
        private int calls;

        @Override
        public CompletableFuture<LlmResult> submit(LlmRequest request) {
            calls++;
            lastRequest = request;
            return response;
        }

        @Override
        public CompletableFuture<String> compressMidTermMemory(LlmRequest request) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class BlockingLlm implements LlmGateway {
        private final CountDownLatch started = new CountDownLatch(1);
        private final CompletableFuture<LlmResult> result = new CompletableFuture<>();

        @Override
        public CompletableFuture<LlmResult> submit(LlmRequest request) {
            started.countDown();
            return result;
        }

        @Override
        public CompletableFuture<String> compressMidTermMemory(LlmRequest request) {
            throw new UnsupportedOperationException();
        }
    }
}
