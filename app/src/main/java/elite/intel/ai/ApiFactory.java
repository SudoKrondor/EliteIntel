package elite.intel.ai;

import elite.intel.ai.brain.AiAnalysisInterface;
import elite.intel.ai.brain.AiPromptFactory;
import elite.intel.ai.brain.commons.PromptFactory;
import elite.intel.ai.brain.inference.anthropic.AnthropicAnalysisEndpoint;
import elite.intel.ai.brain.inference.anthropic.AnthropicPromptFactory;
import elite.intel.ai.brain.inference.deepseek.DeepSeekAnalysisEndpoint;
import elite.intel.ai.brain.inference.gemini.GeminiAnalysisEndpoint;
import elite.intel.ai.brain.inference.lmstudio.LMStudioAnalysisEndpoint;
import elite.intel.ai.brain.inference.mistral.MistralAnalysisEndpoint;
import elite.intel.ai.brain.inference.ollama.OllamaAnalysisEndpoint;
import elite.intel.ai.brain.inference.openai.OpenAiAnalysisEndPoint;
import elite.intel.ai.brain.inference.xai.GrokAnalysisEndpoint;
import elite.intel.ai.ears.EarsInterface;
import elite.intel.ai.ears.parakeet.ParakeetSTTImpl;
import elite.intel.ai.mouth.MouthInterface;
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
            return switch (systemSession.getLocalLlmProvider()) {
                case LMSTUDIO -> LMStudioAnalysisEndpoint.getInstance();
                default -> OllamaAnalysisEndpoint.getInstance();
            };
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
        if (systemSession.useLocalCommandLlm()) {
            return PromptFactory.getInstance();
        }
        ProviderEnum provider = LlmProviderResolver.detectCloudProvider();
        return switch (provider) {
            case ANTHROPIC -> AnthropicPromptFactory.getInstance();
            default -> PromptFactory.getInstance();
        };
    }

    ///
    @SuppressWarnings({"SwitchStatementWithTooFewBranches", "EnhancedSwitchMigration"})
    public MouthInterface getMouthImpl() {
        if (systemSession.useLocalTTS()) {
            KokoroTTS kokoro = KokoroTTS.getInstance();
            kokoro.setRole(KokoroTTS.Role.MAIN);
            return kokoro;
        }

        String apiKey = SystemSession.getInstance().getTtsApiKey();
        ProviderEnum provider = KeyDetector.detectProvider(apiKey, "TTS");
        switch (provider) {
            case GOOGLE_TTS:
                return GoogleTTSImpl.getInstance();
            // TODO: Add ElevenLabs, AWS Polly, etc.
            default:
                KokoroTTS kokoro = KokoroTTS.getInstance();
                kokoro.setRole(KokoroTTS.Role.MAIN);
                return kokoro;
        }
    }

    /// -- no choices here
    public EarsInterface getEarsImpl() {
        return new ParakeetSTTImpl();
    }
}