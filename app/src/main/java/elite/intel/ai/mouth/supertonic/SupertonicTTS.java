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
 * The alternative local engine, beside Kokoro: the one the Cyrillic locales get, because Kokoro cannot read
 * them, and a choice for everyone else (see {@link TtsProvider}). Unlike Kokoro, Supertonic is a single
 * multilingual model: the language is not baked into the engine at build time, it is passed on every
 * {@code generate} call (see {@link #supertonicLangCode(Language)}). The engine is therefore built exactly
 * once and never rebuilt on a language switch. It generates at 44.1 kHz; the pipeline resamples.
 */
public class SupertonicTTS extends SherpaOnnxTTS {

    private static final int NUM_STEPS = 8;

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

    @Override
    protected GeneratedAudio generate(OfflineTts tts, String text, int sid, float speed, Language language) {
        GenerationConfig genConfig = new GenerationConfig();
        genConfig.setSid(sid);
        genConfig.setSpeed(speed);
        genConfig.setNumSteps(NUM_STEPS);
        genConfig.setExtra(Map.of("lang", supertonicLangCode(language)));
        Consumer<float[]> noStreaming = samples -> {
        };
        return tts.generateWithConfigAndCallback(text, genConfig, noStreaming);
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
     * ships one multilingual model covering all nine languages this app supports, Cyrillic included, so unlike
     * Kokoro there is no language this falls back to English for - which is why it is the stand-in for Kokoro
     * in the Cyrillic locales (see {@link TtsProvider#forLanguage}).
     */
    private static String supertonicLangCode(Language language) {
        return switch (language) {
            case RU -> "ru";
            case UK -> "uk";
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
