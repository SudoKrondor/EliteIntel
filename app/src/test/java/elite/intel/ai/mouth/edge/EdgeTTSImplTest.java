package elite.intel.ai.mouth.edge;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.mouth.subscribers.events.AiVoxResponseEvent;
import elite.intel.ai.mouth.subscribers.events.TTSInterruptEvent;
import elite.intel.ai.mouth.subscribers.events.VocalisationRequestEvent;
import elite.intel.ai.mouth.subscribers.events.VocalisationSuccessfulEvent;
import elite.intel.eventbus.GameEventBus;
import elite.intel.i18n.Language;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;

class EdgeTTSImplTest {
    private final List<EdgeTTSImpl> mouths = new ArrayList<>();

    @AfterEach
    void stopMouths() {
        mouths.forEach(EdgeTTSImpl::stop);
    }

    @Test
    void synthesizesAheadWhilePlayingInOrderAndSettlesOnce() throws Exception {
        FakeClient client = new FakeClient();
        FakeOutput output = new FakeOutput();
        output.blockPlayback();
        EdgeTTSImpl mouth = mouth(client, output, 0.25f, 50);
        SuccessRecorder successes = new SuccessRecorder();
        GameEventBus.register(successes);
        try {
            CompletableFuture<Void> first = new CompletableFuture<>();
            CompletableFuture<Void> second = new CompletableFuture<>();
            AtomicInteger firstCompletions = completionCount(first);
            AtomicInteger secondCompletions = completionCount(second);
            mouth.start();

            mouth.onVoiceProcessEvent(event("first", "one", first));
            mouth.onVoiceProcessEvent(event("second", "two", second));

            assertTrue(output.playStarted.await(2, TimeUnit.SECONDS));
            assertTrue(client.twoSyntheses.await(2, TimeUnit.SECONDS),
                    "the synthesis worker should prepare sentence two while sentence one plays");
            assertFalse(first.isDone());
            output.releasePlayback();
            await(first);
            await(second);

            assertEquals(List.of("One", "Two"), client.requests.stream().map(EdgeSynthesisRequest::text).toList());
            assertEquals(List.of("+25%", "+25%"), client.requests.stream().map(EdgeSynthesisRequest::rate).toList());
            assertEquals(2, output.played.size());
            assertEquals(1, firstCompletions.get());
            assertEquals(1, secondCompletions.get());
            assertEquals(2, successes.count.get());

            // Sample 160 is outside the 6 ms fade. It proves decoded PCM gets application volume locally.
            assertEquals(('O' * 100) / 2, sample(output.played.getFirst(), 160));
            assertEquals(('T' * 100) / 2, sample(output.played.get(1), 160));
        } finally {
            GameEventBus.unregister(successes);
        }
    }

    @Test
    void boundsTheActualPostPreprocessingTextSentToEdge() throws Exception {
        FakeClient client = new FakeClient();
        FakeOutput output = new FakeOutput();
        EdgeTTSImpl mouth = mouth(client, output, 0f, 100);
        mouth.start();
        CompletableFuture<Void> completion = new CompletableFuture<>();

        mouth.onVoiceProcessEvent(event("expanded-text", "present ".repeat(600), completion));

        await(completion);
        assertTrue(client.requests.size() > 1);
        assertTrue(client.requests.stream().noneMatch(request -> request.text().contains("present")));
        assertTrue(client.requests.stream().allMatch(request -> EdgeSsml.escape(request.text())
                .getBytes(StandardCharsets.UTF_8).length <= EdgeSentenceSplitter.MAX_ESCAPED_TEXT_BYTES));
    }

    @Test
    void synthesisAndPlaybackFailuresCompleteExceptionallyWithoutKillingWorkers() throws Exception {
        FakeClient client = new FakeClient();
        FakeOutput output = new FakeOutput();
        EdgeTTSImpl mouth = mouth(client, output, 0f, 100);
        mouth.start();

        client.failText = "network";
        CompletableFuture<Void> network = new CompletableFuture<>();
        mouth.onVoiceProcessEvent(event("network-id", "network", network));
        assertInstanceOf(IOException.class, awaitFailure(network));

        output.failNext.set(true);
        CompletableFuture<Void> device = new CompletableFuture<>();
        mouth.onVoiceProcessEvent(event("device-id", "device", device));
        assertInstanceOf(IOException.class, awaitFailure(device));

        CompletableFuture<Void> recovery = new CompletableFuture<>();
        mouth.onVoiceProcessEvent(event("recovery-id", "recovery", recovery));
        await(recovery);
        assertFalse(mouth.workersStopped());
    }

    @Test
    void voiceEnumerationFailureUsesFallbackWithoutRetryingEverySentence() throws Exception {
        FakeClient client = new FakeClient();
        client.failVoiceList = true;
        FakeOutput output = new FakeOutput();
        EdgeTTSImpl mouth = mouth(client, output, 0f, 100);
        mouth.start();
        CompletableFuture<Void> first = new CompletableFuture<>();

        mouth.onVoiceProcessEvent(event("fallback", "one. two.", first));

        await(first);
        assertEquals(1, client.voiceListAttempts.get());
        assertEquals(List.of(
                        EdgeVoices.DEFAULT_FEMALE.defaultShortName(),
                        EdgeVoices.DEFAULT_FEMALE.defaultShortName()),
                client.requests.stream().map(request -> request.voice().shortName()).toList());

        mouth.stop();
        mouth.start();
        CompletableFuture<Void> afterRestart = new CompletableFuture<>();
        mouth.onVoiceProcessEvent(event("fallback-restart", "three", afterRestart));
        await(afterRestart);
        assertEquals(2, client.voiceListAttempts.get(), "a new service run may retry enumeration once");
    }

    @Test
    void interruptionDuringDecodeDoesNotQueueStalePcmOrLeakANetworkCancellation() throws Exception {
        FakeClient client = new FakeClient();
        FakeOutput output = new FakeOutput();
        CountDownLatch decoding = new CountDownLatch(1);
        CountDownLatch releaseDecode = new CountDownLatch(1);
        EdgeAudioDecoder decoder = compressed -> {
            decoding.countDown();
            try {
                releaseDecode.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("decode interrupted", e);
            }
            return pcm((char) compressed[0]);
        };
        EdgeTTSImpl mouth = mouth(client, output, decoder, 0f, 100);
        mouth.start();
        CompletableFuture<Void> completion = new CompletableFuture<>();

        mouth.onVoiceProcessEvent(event("decode", "decode", completion));
        assertTrue(decoding.await(2, TimeUnit.SECONDS));
        mouth.shutUp(new TTSInterruptEvent("decode"));
        releaseDecode.countDown();

        await(completion);
        awaitCondition(() -> client.requests.size() == 1);
        assertTrue(client.cancelled.isEmpty(), "the completed network phase must not retain a cancellation marker");
        assertTrue(output.played.isEmpty());
    }

    @Test
    void middleSynthesisFailureStopsAlreadyPlayingAudioAndDropsRemainingSentences() throws Exception {
        FakeClient client = new FakeClient();
        FakeOutput output = new FakeOutput();
        output.blockPlayback();
        client.failText = "Two.";
        // The failure must land while sentence one is on the speaker: the synthesis worker runs ahead of
        // playback, and a failure raised before then would legitimately drop sentence one from the queue
        // instead of interrupting it, which is a different behaviour from the one under test.
        client.failOnlyAfter(output.playStarted);
        EdgeTTSImpl mouth = mouth(client, output, 0f, 100);
        mouth.start();
        CompletableFuture<Void> completion = new CompletableFuture<>();

        mouth.onVoiceProcessEvent(event("middle-failure", "one. two. three.", completion));

        assertTrue(output.playStarted.await(2, TimeUnit.SECONDS));
        assertInstanceOf(IOException.class, awaitFailure(completion));
        awaitCondition(output.interrupted::get);
        assertEquals(List.of("One.", "two."),
                client.requests.stream().map(EdgeSynthesisRequest::text).toList());
        assertEquals(1, output.played.size());
    }

    @Test
    void targetedInterruptionCancelsOnlyTheMatchingRequest() throws Exception {
        FakeClient client = new FakeClient();
        FakeOutput output = new FakeOutput();
        EdgeTTSImpl mouth = mouth(client, output, 0f, 100);
        client.blockRequest("first");
        mouth.start();
        CompletableFuture<Void> first = new CompletableFuture<>();
        CompletableFuture<Void> second = new CompletableFuture<>();

        mouth.onVoiceProcessEvent(event("first", "old", first));
        assertTrue(client.blockedSynthesis.await(2, TimeUnit.SECONDS));
        mouth.onVoiceProcessEvent(event("second", "new", second));
        mouth.shutUp(new TTSInterruptEvent("first"));

        await(first);
        await(second);
        assertEquals(List.of("first"), client.cancelled);
        assertEquals(1, output.played.size());
        assertEquals('N' * 100, sample(output.played.getFirst(), 160));
    }

    @Test
    void globalInterruptionFencesOldWorkAndAllowsNewSpeech() throws Exception {
        FakeClient client = new FakeClient();
        FakeOutput output = new FakeOutput();
        EdgeTTSImpl mouth = mouth(client, output, 0f, 100);
        client.blockRequest("active-old");
        mouth.start();
        CompletableFuture<Void> activeOld = new CompletableFuture<>();
        CompletableFuture<Void> queuedOld = new CompletableFuture<>();

        mouth.onVoiceProcessEvent(event("active-old", "active old", activeOld));
        assertTrue(client.blockedSynthesis.await(2, TimeUnit.SECONDS));
        mouth.onVoiceProcessEvent(event("queued-old", "queued old", queuedOld));
        mouth.interruptAndClear();
        CompletableFuture<Void> fresh = new CompletableFuture<>();
        mouth.onVoiceProcessEvent(event("fresh", "fresh", fresh));

        await(activeOld);
        await(queuedOld);
        await(fresh);
        assertEquals(List.of("active-old"), client.cancelled);
        assertEquals(1, output.played.size());
        assertEquals('F' * 100, sample(output.played.getFirst(), 160));
    }

    @Test
    void stopSettlesActiveAndQueuedHandlesAndTerminatesBothWorkers() throws Exception {
        FakeClient client = new FakeClient();
        FakeOutput output = new FakeOutput();
        EdgeTTSImpl mouth = mouth(client, output, 0f, 100);
        client.blockRequest("active");
        mouth.start();
        CompletableFuture<Void> active = new CompletableFuture<>();
        CompletableFuture<Void> queued = new CompletableFuture<>();
        AtomicInteger activeCompletions = completionCount(active);
        AtomicInteger queuedCompletions = completionCount(queued);

        mouth.onVoiceProcessEvent(event("active", "active", active));
        assertTrue(client.blockedSynthesis.await(2, TimeUnit.SECONDS));
        mouth.onVoiceProcessEvent(event("queued", "queued", queued));
        mouth.stop();

        await(active);
        await(queued);
        assertTrue(client.cancelAllCalled.get());
        assertTrue(output.interrupted.get());
        assertTrue(output.closed.get());
        assertTrue(mouth.workersStopped());
        assertEquals(1, activeCompletions.get());
        assertEquals(1, queuedCompletions.get());
    }

    @Test
    void serviceCanRestartAfterACompleteStop() throws Exception {
        FakeClient client = new FakeClient();
        FakeOutput output = new FakeOutput();
        EdgeTTSImpl mouth = mouth(client, output, 0f, 100);
        mouth.start();
        mouth.stop();

        mouth.start();
        CompletableFuture<Void> completion = new CompletableFuture<>();
        mouth.onVoiceProcessEvent(event("restart", "restart", completion));

        await(completion);
        assertFalse(mouth.workersStopped());
    }

    @Test
    void radioRequestsRemainUnclaimedWhereKokoroCanVoiceTheLanguage() {
        FakeClient client = new FakeClient();
        FakeOutput output = new FakeOutput();
        EdgeTTSImpl mouth = mouth(client, output, 0f, 100);
        mouth.start();
        VocalisationRequestEvent radio = new VocalisationRequestEvent(
                "radio", null, AiVoxResponseEvent.class, true, true, "station");

        mouth.onVoiceProcessEvent(radio);

        assertFalse(radio.handle().isHandled());
        assertTrue(client.requests.isEmpty());
    }

    /**
     * Kokoro has no Cyrillic front end, so Edge is the radio engine for Russian and Ukrainian: it must claim
     * the transmission, speak it in a locale voice it drew itself rather than the ship's voice, and keep that
     * one voice for every sentence of the message.
     */
    @Test
    void radioInACyrillicLanguageIsClaimedAndSpokenInOneDrawnLocaleVoice() throws Exception {
        FakeClient client = new FakeClient();
        FakeOutput output = new FakeOutput();
        EdgeTTSImpl mouth = mouth(client, output, new EdgeVoiceProvider(), Language.RU, 0f, 100);
        mouth.start();
        CompletableFuture<Void> completion = new CompletableFuture<>();
        VocalisationRequestEvent radio = VocalisationRequestEvent.tracked(
                "radio-ru", "one. two.", AiVoxResponseEvent.class, true, completion);

        mouth.onVoiceProcessEvent(radio);

        await(completion);
        List<String> voices = client.requests.stream()
                .map(request -> request.voice().shortName()).distinct().toList();
        assertEquals(1, voices.size(), "a station must not change speaker mid-transmission");
        assertTrue(voices.getFirst().startsWith("ru-RU-"), voices.getFirst());
        assertNotEquals(EdgeVoices.MARY.defaultShortName(), voices.getFirst(),
                "the transmission is a stranger, not the commander's own ship voice");
        assertEquals(2, output.played.size());
    }

    @Test
    void aRadioRoleEngineIgnoresNormalNarrationLeavingItToTheMainMouth() {
        FakeClient client = new FakeClient();
        FakeOutput output = new FakeOutput();
        EdgeTTSImpl mouth = mouth(client, output, new EdgeVoiceProvider(), Language.RU, 0f, 100);
        mouth.setRole(EdgeTTSImpl.Role.RADIO);
        mouth.start();
        CompletableFuture<Void> completion = new CompletableFuture<>();
        VocalisationRequestEvent narration = VocalisationRequestEvent.tracked(
                "narration", "course plotted", AiVoxResponseEvent.class, true, completion);

        mouth.onVoiceProcessEvent(narration);

        assertFalse(narration.handle().isHandled());
        assertTrue(client.requests.isEmpty());
    }

    private EdgeTTSImpl mouth(FakeClient client, FakeOutput output, float speed, int volume) {
        return mouth(client, output, compressed -> pcm((char) compressed[0]), speed, volume);
    }

    private EdgeTTSImpl mouth(
            FakeClient client,
            FakeOutput output,
            EdgeAudioDecoder decoder,
            float speed,
            int volume
    ) {
        return mouth(client, output, decoder, new EdgeVoiceProvider(), Language.EN, speed, volume);
    }

    private EdgeTTSImpl mouth(
            FakeClient client,
            FakeOutput output,
            EdgeVoiceProvider voiceProvider,
            Language language,
            float speed,
            int volume
    ) {
        return mouth(client, output, compressed -> pcm((char) compressed[0]), voiceProvider, language,
                speed, volume);
    }

    private EdgeTTSImpl mouth(
            FakeClient client,
            FakeOutput output,
            EdgeAudioDecoder decoder,
            EdgeVoiceProvider voiceProvider,
            Language language,
            float speed,
            int volume
    ) {
        EdgeTTSImpl mouth = new EdgeTTSImpl(
                client,
                decoder,
                voiceProvider,
                output,
                new Settings(EdgeVoices.MARY.defaultShortName(), language, speed, volume));
        mouths.add(mouth);
        return mouth;
    }

    private static VocalisationRequestEvent event(
            String requestId, String text, CompletableFuture<Void> completion
    ) {
        return VocalisationRequestEvent.tracked(
                requestId, text, AiVoxResponseEvent.class, true, completion);
    }

    private static byte[] pcm(char marker) {
        byte[] pcm = new byte[400];
        short value = (short) (marker * 100);
        for (int i = 0; i < pcm.length; i += 2) {
            pcm[i] = (byte) value;
            pcm[i + 1] = (byte) (value >>> 8);
        }
        return pcm;
    }

    private static short sample(byte[] pcm, int index) {
        return (short) (((pcm[index * 2 + 1] & 0xff) << 8) | (pcm[index * 2] & 0xff));
    }

    private static AtomicInteger completionCount(CompletableFuture<Void> future) {
        AtomicInteger count = new AtomicInteger();
        future.whenComplete((ignored, failure) -> count.incrementAndGet());
        return count;
    }

    private static void await(CompletableFuture<Void> future) throws Exception {
        future.get(2, TimeUnit.SECONDS);
    }

    private static Throwable awaitFailure(CompletableFuture<Void> future) throws Exception {
        try {
            await(future);
            return fail("future should have completed exceptionally");
        } catch (ExecutionException failure) {
            return failure.getCause();
        }
    }

    private static void awaitCondition(BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
        assertTrue(condition.getAsBoolean());
    }

    private record Settings(
            String selectedVoiceName,
            Language language,
            float speechSpeed,
            int voiceVolume
    ) implements EdgeTtsSettings {
    }

    private static final class FakeClient implements EdgeSynthesisClient {
        private final List<EdgeSynthesisRequest> requests = new CopyOnWriteArrayList<>();
        private final List<String> cancelled = new CopyOnWriteArrayList<>();
        private final CountDownLatch twoSyntheses = new CountDownLatch(2);
        private final CountDownLatch blockedSynthesis = new CountDownLatch(1);
        private final CountDownLatch releaseSynthesis = new CountDownLatch(1);
        private final AtomicBoolean cancelAllCalled = new AtomicBoolean();
        private final AtomicInteger voiceListAttempts = new AtomicInteger();
        private volatile String blockedRequest;
        private volatile String failText;
        private volatile CountDownLatch failGate;
        private volatile boolean failVoiceList;

        @Override
        public List<EdgeVoice> listVoices() throws IOException {
            voiceListAttempts.incrementAndGet();
            if (failVoiceList) {
                throw new IOException("simulated voice-list failure");
            }
            return List.of(new EdgeVoice(
                    null, EdgeVoices.MARY.defaultShortName(), "Female", "en-US",
                    EdgeProtocolConstants.OUTPUT_FORMAT));
        }

        @Override
        public byte[] synthesize(EdgeSynthesisRequest request) throws IOException, InterruptedException {
            requests.add(request);
            twoSyntheses.countDown();
            if (request.requestId().equals(blockedRequest)) {
                blockedSynthesis.countDown();
                releaseSynthesis.await();
            }
            if (failText != null && request.text().equalsIgnoreCase(failText)) {
                CountDownLatch gate = failGate;
                if (gate != null && !gate.await(2, TimeUnit.SECONDS)) {
                    throw new IOException("simulated failure gate never opened");
                }
                throw new IOException("simulated network failure");
            }
            return new byte[]{(byte) request.text().charAt(0)};
        }

        @Override
        public void cancel(String requestId) {
            cancelled.add(requestId);
            releaseSynthesis.countDown();
        }

        @Override
        public void cancelAll() {
            cancelAllCalled.set(true);
            releaseSynthesis.countDown();
        }

        void blockRequest(String requestId) {
            blockedRequest = requestId;
        }

        /**
         * Holds the simulated failure back until {@code gate} opens, so a failure races nothing.
         */
        void failOnlyAfter(CountDownLatch gate) {
            failGate = gate;
        }
    }

    private static final class FakeOutput implements EdgeAudioOutput {
        private final List<byte[]> played = new CopyOnWriteArrayList<>();
        private final CountDownLatch playStarted = new CountDownLatch(1);
        private final CountDownLatch playbackRelease = new CountDownLatch(1);
        private final AtomicBoolean blockPlayback = new AtomicBoolean();
        private final AtomicBoolean interrupted = new AtomicBoolean();
        private final AtomicBoolean closed = new AtomicBoolean();
        private final AtomicBoolean failNext = new AtomicBoolean();

        @Override
        public void open() {
        }

        @Override
        public boolean play(byte[] pcm, BooleanSupplier interruptionRequested) throws Exception {
            played.add(pcm.clone());
            playStarted.countDown();
            if (blockPlayback.get()) {
                while (!playbackRelease.await(20, TimeUnit.MILLISECONDS)) {
                    if (interruptionRequested.getAsBoolean()) {
                        return false;
                    }
                }
            }
            if (failNext.compareAndSet(true, false)) {
                throw new IOException("simulated audio device failure");
            }
            return !interruptionRequested.getAsBoolean();
        }

        @Override
        public void interrupt() {
            interrupted.set(true);
            playbackRelease.countDown();
        }

        @Override
        public void close() {
            closed.set(true);
        }

        void blockPlayback() {
            blockPlayback.set(true);
        }

        void releasePlayback() {
            playbackRelease.countDown();
        }
    }

    private static final class SuccessRecorder {
        private final AtomicInteger count = new AtomicInteger();

        @Subscribe
        public void onSuccess(VocalisationSuccessfulEvent<?> event) {
            count.incrementAndGet();
        }
    }
}
