package elite.intel.ai;

import elite.intel.ai.brain.AiAnalysisInterface;
import elite.intel.ai.brain.AiPromptFactory;
import elite.intel.ai.brain.commons.PromptFactory;
import elite.intel.ai.brain.inference.anthropic.AnthropicAnalysisEndpoint;
import elite.intel.ai.brain.inference.deepseek.DeepSeekAnalysisEndpoint;
import elite.intel.ai.brain.inference.gemini.GeminiAnalysisEndpoint;
import elite.intel.ai.brain.inference.lmstudio.LMStudioAnalysisEndpoint;
import elite.intel.ai.brain.inference.mistral.MistralAnalysisEndpoint;
import elite.intel.ai.brain.inference.openai.OpenAiAnalysisEndPoint;
import elite.intel.ai.brain.inference.xai.GrokAnalysisEndpoint;
import elite.intel.ai.ears.EarsInterface;
import elite.intel.ai.ears.parakeet.ParakeetSTTImpl;
import elite.intel.ai.mouth.MouthInterface;
import elite.intel.ai.mouth.TtsProvider;
import elite.intel.ai.mouth.edge.EdgeTTSImpl;
import elite.intel.ai.mouth.google.GoogleTTSImpl;
import elite.intel.ai.mouth.kokoro.KokoroTTS;
import elite.intel.session.SystemSession;

/**
 * A singleton factory class responsible for providing various AI-related endpoint instances.
 * The instances provided include LLM, STT, TTS, and other AI services based on API keys
 * specified in the application configuration.
 */
public class ApiFactory {
    private static ApiFactory instance;
    private final SystemSession systemSession;

    private ApiFactory() {
        // Prevent instantiation.
        this.systemSession = SystemSession.getInstance();
    }

    public static synchronized ApiFactory getInstance() {
        if (instance == null) {
            instance = new ApiFactory();
        }
        return instance;
    }

    public AiAnalysisInterface getAnalysisEndpoint() {
        if (systemSession.useLocalQueryLlm()) {
            return LMStudioAnalysisEndpoint.getInstance(); // LM Studio is the only local host
        }
        ProviderEnum provider = LlmProviderResolver.detectCloudProvider();
        return switch (provider) {
            case GROK -> GrokAnalysisEndpoint.getInstance();
            case DEEPSEEK -> DeepSeekAnalysisEndpoint.getInstance();
            case MISTRAL -> MistralAnalysisEndpoint.getInstance();
            case OPENAI -> OpenAiAnalysisEndPoint.getInstance();
            case ANTHROPIC -> AnthropicAnalysisEndpoint.getInstance();
            case GEMINI -> GeminiAnalysisEndpoint.getInstance();
            default -> LMStudioAnalysisEndpoint.getInstance();
        };

    }

    public AiPromptFactory getAiPromptFactory() {
        // Single shared prompt factory: the analysis/sensor prompts are provider-agnostic. (Anthropic's
        // command-classification caching variant was removed with the legacy command pipeline.)
        return PromptFactory.getInstance();
    }

    ///
    public MouthInterface getMouthImpl() {
        return selectMouth(systemSession.getTtsProvider(), systemSession.getTtsApiKey());
    }

    /**
     * The engine the main mouth will actually be, which is the stored selection except when Google has no
     * usable key and Kokoro stands in for it (see {@link #selectMouth}). Callers that reason about the main
     * mouth - the radio engine decision, which must never hand the shared Kokoro singleton two roles - have to
     * see the substitution, not the setting.
     */
    public TtsProvider getActiveTtsProvider() {
        return resolveProvider(systemSession.getTtsProvider(), systemSession.getTtsApiKey());
    }

    /**
     * The engine is the stored selection, nothing else - a cloud key is what an engine needs, never what picks
     * it. The one exception is a safety net, not a selection rule: Google is the only engine that cannot speak
     * without a key, and it starts into silence when it has none, so the local engine stands in until a key is
     * configured. The switch is exhaustive on purpose - a new engine has to be given an implementation here.
     */
    // TODO: Add ElevenLabs, AWS Polly, etc.
    static MouthInterface selectMouth(TtsProvider provider, String ttsApiKey) {
        return switch (resolveProvider(provider, ttsApiKey)) {
            case GOOGLE -> GoogleTTSImpl.getInstance();
            case EDGE -> mainEdge();
            case KOKORO -> mainKokoro();
        };
    }

    /**
     * The stored selection, with the Google-without-a-key safety net applied.
     */
    static TtsProvider resolveProvider(TtsProvider provider, String ttsApiKey) {
        boolean googleWithoutKey = provider == TtsProvider.GOOGLE
                && KeyDetector.detectProvider(ttsApiKey, "TTS") != ProviderEnum.GOOGLE_TTS;
        return googleWithoutKey ? TtsProvider.KOKORO : provider;
    }

    private static EdgeTTSImpl mainEdge() {
        EdgeTTSImpl edge = EdgeTTSImpl.getInstance();
        edge.setRole(EdgeTTSImpl.Role.MAIN);
        return edge;
    }

    private static KokoroTTS mainKokoro() {
        KokoroTTS kokoro = KokoroTTS.getInstance();
        kokoro.setRole(KokoroTTS.Role.MAIN);
        return kokoro;
    }

    /// -- no choices here
    public EarsInterface getEarsImpl() {
        return new ParakeetSTTImpl();
    }
}
