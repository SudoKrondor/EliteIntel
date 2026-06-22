package elite.intel.ai.mouth.kokoro;

import com.google.common.eventbus.Subscribe;
import com.k2fsa.sherpa.onnx.*;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import elite.intel.ai.ears.AudioDeviceEnumerator;
import elite.intel.ai.mouth.AudioDeClicker;
import elite.intel.ai.mouth.MouthInterface;
import elite.intel.ai.mouth.RadioFilter;
import elite.intel.ai.mouth.subscribers.events.AiVoxResponseEvent;
import elite.intel.ai.mouth.subscribers.events.TTSInterruptEvent;
import elite.intel.ai.mouth.subscribers.events.VocalisationRequestEvent;
import elite.intel.eventbus.GameEventBus;
import elite.intel.eventbus.UiBus;
import elite.intel.i18n.Language;
import elite.intel.session.PlayerSession;
import elite.intel.session.Status;
import elite.intel.session.SystemSession;
import elite.intel.ui.event.AiResponseLogEvent;
import elite.intel.ui.event.AppLogEvent;
import elite.intel.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Offline TTS using Kokoro via sherpa-onnx JNI.
 * <p>
 * Two-queue pipeline: sentence splitting → synthesis queue → playback queue.
 * Synthesis of sentence N+1 overlaps with playback of sentence N.
 */
public class KokoroTTS implements MouthInterface {

    private static final Logger log = LogManager.getLogger(KokoroTTS.class);

    private static final int SAMPLE_RATE = 24000;
    private static final int DEFAULT_SID = KokoroVoices.GEORGE.getSid();

    private static volatile KokoroTTS instance;

    private final AtomicBoolean interruptRequested = new AtomicBoolean(false);
    private final AtomicBoolean canBeInterrupted = new AtomicBoolean(true);
    private final AtomicReference<SourceDataLine> currentLine = new AtomicReference<>();

    private record SynthesisTask(String text, String voiceName, boolean isRadio,
                                 @Nullable CompletableFuture<Void> completionFuture) {
    }

    /**
     * Synthesized PCM paired with an optional completion future from the originating request.
     */
    private record PlaybackTask(byte[] pcm, @Nullable CompletableFuture<Void> completionFuture) {
    }

    // Stage 1: raw sentence strings waiting for synthesis
    private final BlockingQueue<SynthesisTask> synthesisQueue = new LinkedBlockingQueue<>();
    // Stage 2: synthesized PCM waiting for playback
    private final BlockingQueue<PlaybackTask> playbackQueue = new LinkedBlockingQueue<>();

    private final SystemSession systemSession = SystemSession.getInstance();
    private SourceDataLine persistentLine;
    private volatile boolean running = false;
    private Thread synthesisThread;
    private Thread playbackThread;
    private OfflineTts tts;
    private Language lastBuiltLanguage;

    private KokoroTTS() {
    }

    public static KokoroTTS getInstance() {
        if (instance == null) {
            synchronized (KokoroTTS.class) {
                if (instance == null) instance = new KokoroTTS();
            }
        }
        return instance;
    }

    // -- Lifecycle -------------------------------------------------------------

    @Override
    public synchronized void start() {
        if (running) return;
        log.info("KokoroTTS.start() called from thread: {}", Thread.currentThread().getName());
        try {
            SherpaOnnxNatives.load();
        } catch (Exception e) {
            log.error("KokoroTTS: native lib load failed - TTS unavailable", e);
            return;
        }

        Language currentLanguage = SystemSession.getInstance().getLanguage();
        if (tts == null || lastBuiltLanguage != currentLanguage) {
            if (tts != null) {
                try {
                    tts.release();
                } catch (Exception e) {
                    log.warn("KokoroTTS: tts.release on language switch failed", e);
                }
                tts = null;
            }
            try {
                tts = buildOfflineTts();
                lastBuiltLanguage = currentLanguage;
            } catch (Exception e) {
                log.error("KokoroTTS: engine init failed", e);
                return;
            }
        }

        running = true;
        synthesisQueue.clear();
        playbackQueue.clear();
        interruptRequested.set(false); // ← reset after stop() left it true

        synthesisThread = new Thread(this::processSynthesisQueue, "KokoroTTS-Synthesis");
        synthesisThread.setDaemon(true);
        synthesisThread.start();

        playbackThread = new Thread(this::processPlaybackQueue, "KokoroTTS-Playback");
        playbackThread.setDaemon(true);
        playbackThread.start();

        GameEventBus.register(this);
        log.info("KokoroTTS started - voice: {} sid={}", KokoroVoices.GEORGE.getDisplayName(), DEFAULT_SID);
        GameEventBus.publish(new AiVoxResponseEvent(StringUtls.greeting(PlayerSession.getInstance().getConfiguredPlayerName())));
    }

    @Override
    public synchronized void stop() {
        running = false;
        try {
            GameEventBus.unregister(this);
        } catch (IllegalArgumentException ignored) {
            log.warn("Kokoro is not registered on event bus, ignore");
        }
        synthesisQueue.clear();
        playbackQueue.clear();
        interruptRequested.set(true);

        if (persistentLine != null && persistentLine.isOpen()) {
            try {
                persistentLine.stop();
                persistentLine.flush();
            } catch (Exception ignored) {
            }
        }

        if (synthesisThread != null) {
            synthesisThread.interrupt();
            try {
                synthesisThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                synthesisThread = null;
            }
        }
        if (playbackThread != null) {
            playbackThread.interrupt();
            try {
                playbackThread.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                playbackThread = null;
            }
        }

        closePersistentLine();

        // NOTE:
        // tts is intentionally NOT released here. tts.release() crashes in the
        // KokoroMultiLangLexicon destructor (SIGSEGV) due to shared native state with ONNX Runtime.
        // KokoroTTS is a singleton there is exactly one OfflineTts per process lifetime.
        // Native memory is reclaimed when the process exits.
        // Language changes force a rebuild in start(), which releases the old instance at a safe point.
    }

    // -- MouthInterface --------------------------------------------------------

    @Override
    public void interruptAndClear() {
        if (!canBeInterrupted.get()) return;

        // Drain queues and complete any pending customCommand completion futures to avoid 30s timeout
        List<SynthesisTask> drainedSynthesis = new ArrayList<>();
        synthesisQueue.drainTo(drainedSynthesis);
        drainedSynthesis.stream().map(SynthesisTask::completionFuture).filter(Objects::nonNull).forEach(f -> f.complete(null));

        List<PlaybackTask> drainedPlayback = new ArrayList<>();
        playbackQueue.drainTo(drainedPlayback);
        drainedPlayback.stream().map(PlaybackTask::completionFuture).filter(Objects::nonNull).forEach(f -> f.complete(null));

        interruptRequested.set(true);

        SourceDataLine line = currentLine.get();
        if (line != null && line.isOpen()) {
            line.stop();
            line.flush();
            line.start();
        }
        interruptRequested.set(false);

        log.info("KokoroTTS interrupted and queues cleared");

        if (synthesisThread == null || !synthesisThread.isAlive() ||
                playbackThread == null || !playbackThread.isAlive()) {
            log.warn("KokoroTTS worker died - restarting");
            start();
        }
    }

    @Subscribe
    public void shutUp(TTSInterruptEvent event) {
        interruptAndClear();
    }

    @Override
    @Subscribe
    public void onVoiceProcessEvent(VocalisationRequestEvent event) {
        if (!running) return;
        canBeInterrupted.set(event.canBeInterrupted());

        String sanitizedText = StringUtls.sanitizeTts(event.getText());
        if (sanitizedText.isBlank()) return;

        GameEventBus.publish(new PlayBeepEvent(AudioPlayer.BEEP_2));
        UiBus.publish(new AiResponseLogEvent(sanitizedText));

        // Split on sentence boundaries and enqueue each piece for synthesis
        String[] allSentences = sanitizedText.split("(?<=[.,!?])\\s+(?=\\S)");
        // Collect non-blank sentences so the completion future goes to the actual last one
        List<String> sentences = new ArrayList<>();
        for (String s : allSentences) {
            if (!s.isBlank()) sentences.add(s);
        }
        CompletableFuture<Void> completionFuture = event.getCompletionFuture();
        for (int i = 0; i < sentences.size(); i++) {
            boolean isLast = (i == sentences.size() - 1);
            boolean isRadio = event.isRadio();
            if (!Status.getInstance().isInMainShip()) isRadio = true;
            synthesisQueue.offer(new SynthesisTask(sentences.get(i), event.getVoiceName(), isRadio, isLast ? completionFuture : null));
        }
    }

    // -- Stage 1: Synthesis thread ---------------------------------------------

    private void processSynthesisQueue() {
        while (running) {
            try {
                SynthesisTask task = synthesisQueue.take();
                if (interruptRequested.get()) {
                    // Complete future immediately so customCommand doesn't wait until timeout
                    if (task.completionFuture() != null) task.completionFuture().complete(null);
                    continue;
                }

                KokoroVoices voice = task.voiceName() != null
                        ? KokoroVoices.valueOf(task.voiceName())
                        : systemSession.getKokoroVoice();
                int sid = voice.getSid();

                resetNumericLocale();
                GeneratedAudio audio = tts.generate(
                        //Remove dots, TTS say "dot" all the time.
                        task.text().replace(".", " "),
                        sid,
                        1f + systemSession.getSpeechSpeed()
                );

                if (audio == null || audio.getSamples() == null || audio.getSamples().length == 0) {
                    log.warn("KokoroTTS: empty audio for: {}", task.text());
                    if (task.completionFuture() != null) task.completionFuture().complete(null);
                    continue;
                }

                byte[] pcm = floatToPcm16(audio.getSamples());

                AudioDeClicker.sanitize(pcm, 5);
                AudioDeClicker.applyVolume(pcm, systemSession.getVoiceVolume() / 100f);
                if (task.isRadio()) {
                    RadioFilter.apply(pcm);
                }
                playbackQueue.put(new PlaybackTask(pcm, task.completionFuture()));

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.warn("KokoroTTS synthesis error: {}", e.getMessage(), e);
            }
        }
    }

    // -- Stage 2: Playback thread ----------------------------------------------

    private void processPlaybackQueue() {
        if (!openPersistentLine()) return;

        while (running) {
            try {
                PlaybackTask task = playbackQueue.poll(200, TimeUnit.MILLISECONDS);
                if (task == null) continue;
                if (interruptRequested.get()) {
                    // Complete future immediately so customCommand doesn't wait until timeout
                    if (task.completionFuture() != null) task.completionFuture().complete(null);
                    continue;
                }

                playPcm(task.pcm());
                if (task.completionFuture() != null) {
                    task.completionFuture().complete(null);
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.warn("KokoroTTS playback error: {}", e.getMessage(), e);
            } finally {
                UiBus.publish(new AppLogEvent(""));
            }
        }
        closePersistentLine();
    }

    private void playPcm(byte[] audioData) {
        if (persistentLine == null || !persistentLine.isOpen()) {
            if (!openPersistentLine()) return;
        }

        currentLine.set(persistentLine);

        AudioFormat fmt = persistentLine.getFormat();
        int frameSize = fmt.getFrameSize();

        // Small silence gap between sentences
        byte[] silence = new byte[(int) (SAMPLE_RATE * 0.03f) * frameSize];
        persistentLine.write(silence, 0, silence.length);

        final int CHUNK = 8192;
        for (int offset = 0; offset < audioData.length; offset += CHUNK) {
            if (interruptRequested.get()) break;
            int remaining = audioData.length - offset;
            int thisChunk = (Math.min(CHUNK, remaining) / frameSize) * frameSize;
            if (thisChunk == 0) break;
            persistentLine.write(audioData, offset, thisChunk);
        }

        if (!interruptRequested.get()) persistentLine.drain();
        else persistentLine.flush();
    }

    private boolean openPersistentLine() {
        try {
            AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            Mixer.Info outputMixer = AudioDeviceEnumerator.resolveOutputDevice(systemSession.getAudioOutputDevice());
            persistentLine = AudioDeviceEnumerator.openOutputLine(info, outputMixer);
            persistentLine.open(format, (int) (format.getFrameSize() * format.getSampleRate() / 10));
            persistentLine.start();
            log.info("KokoroTTS audio line: {}Hz 16-bit mono", SAMPLE_RATE);
            return true;
        } catch (Exception e) {
            log.error("KokoroTTS: failed to open audio line", e);
            return false;
        }
    }

    private void closePersistentLine() {
        if (persistentLine != null && persistentLine.isOpen()) {
            try {
                if (interruptRequested.get()) {
                    persistentLine.flush(); // forced stop - discard buffered audio immediately
                } else {
                    persistentLine.drain(); // normal end - play out remaining audio
                }
                persistentLine.stop();
                persistentLine.close();
            } catch (Exception e) {
                log.warn("KokoroTTS: error closing audio line", e);
            } finally {
                persistentLine = null;
            }
        }
    }

    // -- Engine construction ---------------------------------------------------

    /**
     * NOTE: Kokoro will speak other languages but with accent.
     * Hindi hi
     * Italian it
     * Japanese ja
     * Portuguese (Brazilian) pt-br
     * Chinese (Mandarin) zh
     */
    private static String kokoroLangCode(Language language) {
        return switch (language) {
            case FR -> "fr";
            case ES -> "es";
            default -> "en-us";
        };
    }

    private OfflineTts buildOfflineTts() {
        Path modelDir = AppPaths.getTtsModelDir().resolve("kokoro-multi-lang-v1_0");
        if (!Files.exists(modelDir)) {
            throw new IllegalStateException(
                    "Kokoro model missing at: " + modelDir +
                            " - run the installer to download TTS models.");
        }

        OfflineTtsKokoroModelConfig kokoro = OfflineTtsKokoroModelConfig.builder()
                .setModel(AppPaths.toNativePath(modelDir.resolve("model.onnx")))
                .setVoices(AppPaths.toNativePath(modelDir.resolve("voices.bin")))
                .setTokens(AppPaths.toNativePath(modelDir.resolve("tokens.txt")))
                .setDataDir(AppPaths.toNativePath(modelDir.resolve("espeak-ng-data")))
                .setLang(kokoroLangCode(SystemSession.getInstance().getLanguage()))
                .build();

        OfflineTtsModelConfig modelConfig = OfflineTtsModelConfig.builder()
                .setKokoro(kokoro)
                .setNumThreads(2)
                .setDebug(false)
                .setProvider("cpu")
                .build();

        OfflineTtsConfig config = OfflineTtsConfig.builder()
                .setModel(modelConfig)
                .setMaxNumSentences(1)
                .build();

        return new OfflineTts(config);
    }

    // -- Locale fix ------------------------------------------------------------

    /**
     * ONNX Runtime (initialized by OfflineRecognizer / Parakeet STT) calls setlocale()
     * which can change LC_NUMERIC to the system locale (e.g. de_DE uses "," as decimal).
     * espeak-ng inside Generate() calls stof() which is locale-sensitive and crashes
     * with std::invalid_argument if LC_NUMERIC is not "C".
     * Reset before every generate() call so Parakeet's init can't corrupt TTS synthesis.
     */
    private interface CLib extends Library {
        String setlocale(int category, String locale);
    }

    private static void resetNumericLocale() {
        try {
            // LC_NUMERIC: Linux=1, macOS=4, Windows=2
            int LC_NUMERIC = Platform.isLinux() ? 1 : Platform.isMac() ? 4 : 2;
            String libName = Platform.isWindows() ? "msvcrt" : "c";
            Native.load(libName, CLib.class).setlocale(LC_NUMERIC, "C");
        } catch (Throwable e) {
            log.warn("KokoroTTS: could not reset LC_NUMERIC locale: {}", e.getMessage());
        }
    }

    // -- Helpers ---------------------------------------------------------------

    private static byte[] floatToPcm16(float[] samples) {
        byte[] out = new byte[samples.length * 2];
        for (int i = 0; i < samples.length; i++) {
            short s = (short) (Math.max(-1f, Math.min(1f, samples[i])) * 32767);
            out[2 * i] = (byte) (s & 0xFF);
            out[2 * i + 1] = (byte) ((s >> 8) & 0xFF);
        }
        return out;
    }


}
