package elite.intel.ai.mouth.kokoro;

import com.k2fsa.sherpa.onnx.*;
import elite.intel.ai.mouth.TtsProvider;
import elite.intel.ai.mouth.sherpa.SherpaOnnxTTS;
import elite.intel.i18n.Language;
import elite.intel.util.AppPaths;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;

/**
 * Offline TTS using Kokoro (multi-lang v1.0) via sherpa-onnx JNI, on the shared {@link SherpaOnnxTTS}
 * pipeline.
 * <p>
 * The shipped default engine and the one with the wider cast (53 voices). Its espeak-ng phonemizer has no
 * Cyrillic front end, so a Russian or Ukrainian commander never reaches it - Supertonic stands in (see
 * {@link TtsProvider#forLanguage}).
 */
public class KokoroTTS extends SherpaOnnxTTS {

    private static volatile KokoroTTS instance;

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

    @Override
    protected TtsProvider provider() {
        return TtsProvider.KOKORO;
    }

    /**
     * The phonemizer language is baked into the model config, so a language switch needs a new engine.
     */
    @Override
    protected boolean rebuildsOnLanguageSwitch() {
        return true;
    }

    @Override
    protected GeneratedAudio generate(OfflineTts tts, String text, int sid, float speed, Language language) {
        return tts.generate(text, sid, speed);
    }

    /**
     * Comma clauses are split too: Kokoro runs them together otherwise.
     */
    @Override
    protected String sentenceBoundary() {
        return "(?<=[.,!?])\\s+(?=\\S)";
    }

    @Override
    protected String defaultVoiceName() {
        return KokoroVoices.DEFAULT_VOICE.name();
    }

    @Override
    protected boolean isInTheCast(String voiceName) {
        return Arrays.stream(KokoroVoices.values()).anyMatch(voice -> voice.name().equals(voiceName));
    }

    @Override
    protected int sidOf(String voiceName) {
        KokoroVoices voice = voiceName != null ? KokoroVoices.voiceOrDefault(voiceName) : systemSession.getKokoroVoice();
        return voice.getSid();
    }

    @Override
    protected String radioVoiceNameFor(String speakerKey, Set<String> reserved) {
        return KokoroVoices.radioVoiceFor(speakerKey, systemSession.getKokoroVoice().name(), reserved).name();
    }

    // -- Engine construction ---------------------------------------------------

    /**
     * The espeak-ng phonemizer language Kokoro reads the text with. This is what decides pronunciation, and
     * it is separate from the voice: a language with no native Kokoro voice (German) is still phonemized
     * correctly here and merely spoken with the accent of whatever voice is selected. Getting this wrong is
     * worse than an accent — German text read with "en-us" rules is mangled, not accented.
     * <p>
     * Cyrillic (RU/UK) has no entry on purpose: it cannot be phonemized at all, so those sessions never
     * reach this engine - Supertonic stands in for it (see {@code TtsProvider#forLanguage}).
     */
    private static String kokoroLangCode(Language language) {
        return switch (language) {
            case FR -> "fr";
            case ES -> "es";
            case IT -> "it";
            case DE -> "de";
            // Kokoro ships Brazilian Portuguese only, so European Portuguese speaks with a Brazilian accent.
            case PT, PTBZ -> "pt-br";
            default -> "en-us";
        };
    }

    @Override
    protected OfflineTts buildOfflineTts(Language language) {
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
                .setLang(kokoroLangCode(language))
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
}
