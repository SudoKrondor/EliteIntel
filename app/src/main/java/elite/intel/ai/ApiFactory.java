package elite.intel.ai;

import elite.intel.ai.brain.*;
import elite.intel.ai.brain.commons.PromptFactory;
import elite.intel.ai.brain.commons.ResponseRouter;
import elite.intel.ai.brain.inference.anthropic.AnthropicAnalysisEndpoint;
import elite.intel.ai.brain.inference.anthropic.AnthropicCommandEndPoint;
import elite.intel.ai.brain.inference.anthropic.AnthropicPromptFactory;
import elite.intel.ai.brain.inference.anthropic.AnthropicUserEndPoint;
import elite.intel.ai.brain.inference.deepseek.DeepSeekAnalysisEndpoint;
import elite.intel.ai.brain.inference.deepseek.DeepSeekChatEndPoint;
import elite.intel.ai.brain.inference.deepseek.DeepSeekCommandEndPoint;
import elite.intel.ai.brain.inference.gemini.GeminiAnalysisEndpoint;
import elite.intel.ai.brain.inference.gemini.GeminiChatEndPoint;
import elite.intel.ai.brain.inference.gemini.GeminiCommandEndPoint;
import elite.intel.ai.brain.inference.lmstudio.LMStudioAnalysisEndpoint;
import elite.intel.ai.brain.inference.lmstudio.LMStudioCommandEndPoint;
import elite.intel.ai.brain.inference.lmstudio.LMStudioUserInputProcessor;
import elite.intel.ai.brain.inference.mistral.MistralAnalysisEndpoint;
import elite.intel.ai.brain.inference.mistral.MistralChatEndPoint;
import elite.intel.ai.brain.inference.mistral.MistralCommandEndPoint;
import elite.intel.ai.brain.inference.ollama.OllamaAnalysisEndpoint;
import elite.intel.ai.brain.inference.ollama.OllamaCommandEndPoint;
import elite.intel.ai.brain.inference.ollama.OllamaUserInputProcessor;
import elite.intel.ai.brain.inference.openai.OpenAiAnalysisEndPoint;
import elite.intel.ai.brain.inference.openai.OpenAiChatEndPoint;
import elite.intel.ai.brain.inference.openai.OpenAiCommandEndPoint;
import elite.intel.ai.brain.inference.xai.GrokAnalysisEndpoint;
import elite.intel.ai.brain.inference.xai.GrokChatEndPoint;
import elite.intel.ai.brain.inference.xai.GrokCommandEndPoint;
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

    public AIChatInterface getChatEndpoint() {

        if (systemSession.useLocalQueryLlm()) {
            return switch (systemSession.getLocalLlmProvider()) {
                case LMSTUDIO -> LMStudioCommandEndPoint.getInstance();
                default -> OllamaCommandEndPoint.getInstance();
            };
        }

        ProviderEnum provider = LlmProviderResolver.detectCloudProvider();
        return switch (provider) {
            case GROK -> GrokChatEndPoint.getInstance();
            case DEEPSEEK -> DeepSeekChatEndPoint.getInstance();
            case MISTRAL -> MistralChatEndPoint.getInstance();
            case OPENAI -> OpenAiChatEndPoint.getInstance();
            case ANTHROPIC -> AnthropicUserEndPoint.getInstance();
            case GEMINI -> GeminiChatEndPoint.getInstance();
            default -> LMStudioCommandEndPoint.getInstance();
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

    public AiCommandInterface getCommandEndpoint() {
        if (systemSession.useLocalCommandLlm()) {
            return switch (systemSession.getLocalLlmProvider()) {
                case LMSTUDIO -> LMStudioUserInputProcessor.getInstance();
                default -> OllamaUserInputProcessor.getInstance();
            };
        }

        ProviderEnum provider = LlmProviderResolver.detectCloudProvider();
        return switch (provider) {
            case GROK -> GrokCommandEndPoint.getInstance();
            case DEEPSEEK -> DeepSeekCommandEndPoint.getInstance();
            case MISTRAL -> MistralCommandEndPoint.getInstance();
            case OPENAI -> OpenAiCommandEndPoint.getInstance();
            case ANTHROPIC -> AnthropicCommandEndPoint.getInstance();
            case GEMINI -> GeminiCommandEndPoint.getInstance();
            default -> LMStudioUserInputProcessor.getInstance();
        };
    }

    public AIRouterInterface getAiRouter() {
        return ResponseRouter.getInstance();
    }

    ///
    @SuppressWarnings({"SwitchStatementWithTooFewBranches", "EnhancedSwitchMigration"})
    public MouthInterface getMouthImpl() {
        if (systemSession.useLocalTTS()) {
            return KokoroTTS.getInstance();
        }

        String apiKey = SystemSession.getInstance().getTtsApiKey();
        ProviderEnum provider = KeyDetector.detectProvider(apiKey, "TTS");
        switch (provider) {
            case GOOGLE_TTS:
                return GoogleTTSImpl.getInstance();
            // TODO: Add ElevenLabs, AWS Polly, etc.
            default:
                return KokoroTTS.getInstance();
        }
    }

    /// -- no choices here
    public EarsInterface getEarsImpl() {
        return new ParakeetSTTImpl();
    }
}