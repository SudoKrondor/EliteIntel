package elite.intel.ai.mouth.supertonic;

import com.k2fsa.sherpa.onnx.*;
import elite.intel.ai.mouth.TtsProvider;
import elite.intel.ai.mouth.sherpa.SherpaOnnxTTS;
import elite.intel.i18n.Language;
import elite.intel.util.AppPaths;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Offline TTS using Supertonic 3 via sherpa-onnx JNI, on the shared {@link SherpaOnnxTTS} pipeline.
 * <p>
 * The alternative local engine, beside Kokoro (see {@link TtsProvider}), and the radio engine for a Russian
 * game client, whose Cyrillic chatter Kokoro cannot read. Unlike Kokoro, Supertonic is a single
 * multilingual model: the language is not baked into the engine at build time, it is passed on every
 * {@code generate} call (see {@link #supertonicLangCode(Language)}). The engine is therefore built exactly
 * once and never rebuilt on a language switch. It generates at 44.1 kHz; the pipeline resamples.
 */
public class SupertonicTTS extends SherpaOnnxTTS {

    private static final int NUM_STEPS = 8;
    private static final float SPEED_MULTIPLIER = 0.85f;

    private static volatile SupertonicTTS instance;

    private SupertonicTTS() {
    }

    public static SupertonicTTS getInstance() {
        if (instance == null) {
            synchronized (SupertonicTTS.class) {
                if (instance == null) instance = new SupertonicTTS();
            }
        }
        return instance;
    }

    @Override
    protected TtsProvider provider() {
        return TtsProvider.SUPERTONIC;
    }

    /**
     * One multilingual model, language per call: built once for the life of the process.
     */
    @Override
    protected boolean rebuildsOnLanguageSwitch() {
        return false;
    }

    /**
     * WHY the model is never asked to go faster than this: above its own pace Supertonic keeps the clip's
     * length but renders the last word as silence ("Preparing for FTL" came out "Preparing for F" about two
     * times in three at 1.15, and still about one in ten at 1.0). At its default pace it spoke 45 of 45 whole;
     * the rest of the pace is added afterwards by {@link TimeStretch}, which keeps the pitch.
     */
    private static final float MAX_MODEL_SPEED = SPEED_MULTIPLIER;

    @Override
    protected GeneratedAudio generate(OfflineTts tts, String text, int sid, float speed, Language language,
                                      boolean russian) {
        float pace = speed * SPEED_MULTIPLIER;
        float modelPace = Math.min(pace, MAX_MODEL_SPEED);
        GenerationConfig genConfig = new GenerationConfig();
        genConfig.setSid(sid);
        genConfig.setSpeed(modelPace);
        genConfig.setNumSteps(NUM_STEPS);
        genConfig.setExtra(Map.of("lang", russian ? "ru" : supertonicLangCode(language)));
        Consumer<float[]> noStreaming = samples -> {
        };
        GeneratedAudio audio = tts.generateWithConfigAndCallback(text, genConfig, noStreaming);
        if (pace <= modelPace || audio == null || audio.getSamples() == null) {
            return audio;
        }
        float[] faster = TimeStretch.speedUp(audio.getSamples(), audio.getSampleRate(), pace / modelPace);
        return new GeneratedAudio(faster, audio.getSampleRate());
    }

    /**
     * Split only on sentence-ending punctuation. Keeping comma clauses together lets Supertonic control the
     * comma pause through its own prosody instead of adding a queue boundary and gap.
     */
    @Override
    protected String sentenceBoundary() {
        return "(?<=[.!?])\\s+(?=\\S)";
    }

    @Override
    protected String defaultVoiceName() {
        return SupertonicVoices.DEFAULT_VOICE.name();
    }

    @Override
    protected boolean isInTheCast(String voiceName) {
        return Arrays.stream(SupertonicVoices.values()).anyMatch(voice -> voice.name().equals(voiceName));
    }

    @Override
    protected int sidOf(String voiceName) {
        SupertonicVoices voice = voiceName != null
                ? SupertonicVoices.voiceOrDefault(voiceName)
                : systemSession.getSupertonicVoice();
        return voice.getSid();
    }

    @Override
    protected String radioVoiceNameFor(String speakerKey, Set<String> reserved) {
        return SupertonicVoices.radioVoiceFor(speakerKey, systemSession.getSupertonicVoice().name(), reserved).name();
    }

    // -- Engine construction ---------------------------------------------------

    /**
     * The language code Supertonic reads the text with, passed per-call via {@link GenerationConfig#setExtra}
     * rather than baked into the model at build time (see {@link #buildOfflineTts(Language)}). Supertonic
     * ships one multilingual model covering every language this app supports, and Russian besides - which is
     * why it voices a Russian game client's radio chatter (see {@link elite.intel.ai.mouth.RadioVoicing}).
     */
    private static String supertonicLangCode(Language language) {
        return switch (language) {
            case FR -> "fr";
            case ES -> "es";
            case IT -> "it";
            case DE -> "de";
            // Supertonic ships one Portuguese code; European Portuguese speaks with whatever accent that
            // single model carries, same trade-off Kokoro made with its Brazilian-only Portuguese voice.
            case PT, PTBZ -> "pt";
            default -> "en";
        };
    }

    @Override
    protected OfflineTts buildOfflineTts(Language language) {
        Path modelDir = AppPaths.getTtsModelDir().resolve("sherpa-onnx-supertonic-3-tts-int8-2026-05-11");
        if (!Files.exists(modelDir)) {
            throw new IllegalStateException(
                    "Supertonic model missing at: " + modelDir +
                            " - run the installer to download TTS models.");
        }

        OfflineTtsSupertonicModelConfig supertonic = OfflineTtsSupertonicModelConfig.builder()
                .setDurationPredictor(AppPaths.toNativePath(modelDir.resolve("duration_predictor.int8.onnx")))
                .setTextEncoder(AppPaths.toNativePath(modelDir.resolve("text_encoder.int8.onnx")))
                .setVectorEstimator(AppPaths.toNativePath(modelDir.resolve("vector_estimator.int8.onnx")))
                .setVocoder(AppPaths.toNativePath(modelDir.resolve("vocoder.int8.onnx")))
                .setTtsJson(AppPaths.toNativePath(modelDir.resolve("tts.json")))
                .setUnicodeIndexer(AppPaths.toNativePath(modelDir.resolve("unicode_indexer.bin")))
                .setVoiceStyle(AppPaths.toNativePath(modelDir.resolve("voice.bin")))
                .build();

        OfflineTtsModelConfig modelConfig = OfflineTtsModelConfig.builder()
                .setSupertonic(supertonic)
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
}
