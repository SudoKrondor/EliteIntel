package elite.intel.ai.mouth.piper;

import com.k2fsa.sherpa.onnx.*;
import elite.intel.ai.mouth.TtsProvider;
import elite.intel.ai.mouth.sherpa.SherpaOnnxTTS;
import elite.intel.i18n.Language;
import elite.intel.util.AppPaths;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

/** Offline French Piper VITS voices through the same sherpa-onnx playback pipeline as Kokoro and Supertonic. */
public final class PiperTTS extends SherpaOnnxTTS {

    private static final PiperTTS INSTANCE = new PiperTTS();

    private volatile OfflineTts upmcTts;
    private volatile OfflineTts siwisTts;
    // stop() holds the lifecycle monitor while waiting for the synthesis worker. Model loading must
    // use a different lock so that worker can finish if a provider switch races its first Siwis request.
    private final Object modelLock = new Object();

    private PiperTTS() {
    }

    public static PiperTTS getInstance() {
        return INSTANCE;
    }

    @Override
    protected TtsProvider provider() {
        return TtsProvider.PIPER;
    }

    @Override
    protected boolean rebuildsOnLanguageSwitch() {
        return false;
    }

    @Override
    protected GeneratedAudio generate(OfflineTts tts, String text, int sid, float speed, Language language,
                                      boolean russian) {
        return tts.generate(text, sid, speed);
    }

    @Override
    protected String sentenceBoundary() {
        return "(?<=[.!?])\\s+(?=\\S)";
    }

    @Override
    protected String defaultVoiceName() {
        return PiperVoices.DEFAULT_VOICE.name();
    }

    @Override
    protected boolean isInTheCast(String voiceName) {
        return PiperVoices.roster().anyMatch(voice -> voice.name().equals(voiceName));
    }

    @Override
    protected int sidOf(String voiceName) {
        return resolveVoice(voiceName).getSid();
    }

    @Override
    protected String radioVoiceNameFor(String speakerKey, Set<String> reserved) {
        return PiperVoices.radioVoiceFor(speakerKey, defaultVoiceName(), reserved).name();
    }

    @Override
    protected OfflineTts buildOfflineTts(Language language) {
        PiperVoices initialVoice = systemSession.getPiperVoice();
        OfflineTts initial = buildOfflineTts(initialVoice);
        if (initialVoice.usesSiwisModel()) siwisTts = initial;
        else upmcTts = initial;
        return initial;
    }

    @Override
    protected OfflineTts ttsForVoice(OfflineTts defaultTts, String voiceName, Language language) {
        PiperVoices voice = resolveVoice(voiceName);
        OfflineTts selected = voice.usesSiwisModel() ? siwisTts : upmcTts;
        if (selected != null) return selected;
        synchronized (modelLock) {
            selected = voice.usesSiwisModel() ? siwisTts : upmcTts;
            if (selected == null) {
                selected = buildOfflineTts(voice);
                if (voice.usesSiwisModel()) siwisTts = selected;
                else upmcTts = selected;
            }
        }
        return selected;
    }

    private PiperVoices resolveVoice(String voiceName) {
        return voiceName == null ? systemSession.getPiperVoice() : PiperVoices.voiceOrDefault(voiceName);
    }

    private OfflineTts buildOfflineTts(PiperVoices voice) {
        String modelName = voice.usesSiwisModel()
                ? "vits-piper-fr_FR-siwis-medium"
                : "vits-piper-fr_FR-upmc-medium";
        String onnxName = voice.usesSiwisModel()
                ? "fr_FR-siwis-medium.onnx"
                : "fr_FR-upmc-medium.onnx";
        Path modelDir = AppPaths.getTtsModelDir().resolve(modelName);
        Path model = modelDir.resolve(onnxName);
        Path tokens = modelDir.resolve("tokens.txt");
        Path espeakData = modelDir.resolve("espeak-ng-data");
        if (!Files.isRegularFile(model) || !Files.isRegularFile(tokens) || !Files.isDirectory(espeakData)) {
            throw new IllegalStateException("Piper model files are missing from " + modelDir
                    + " - run scripts/prepare_piper_models.ps1 to download them.");
        }

        OfflineTtsVitsModelConfig vits = OfflineTtsVitsModelConfig.builder()
                .setModel(AppPaths.toNativePath(model))
                .setTokens(AppPaths.toNativePath(tokens))
                .setDataDir(AppPaths.toNativePath(espeakData))
                .build();
        OfflineTtsModelConfig modelConfig = OfflineTtsModelConfig.builder()
                .setVits(vits)
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
