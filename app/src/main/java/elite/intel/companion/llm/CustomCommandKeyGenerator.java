package elite.intel.companion.llm;

import elite.intel.ai.brain.actions.customcommand.CustomCommandKeyDeriver;
import elite.intel.companion.model.llm.LlmMessage;
import elite.intel.companion.model.llm.LlmMessageRole;
import elite.intel.companion.model.llm.LlmRequest;
import elite.intel.companion.model.llm.PromptCacheProfile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Generates a custom command's English {@code actionKey} from its trigger phrases via the LLM.
 * <p>
 * The action key must be an ASCII snake_case identifier: in companion mode it becomes the LLM tool name
 * ({@code GameToolCandidates} → {@link elite.intel.companion.model.llm.LlmToolDefinition}), and provider
 * tool-name schemas (OpenAI, Anthropic, Gemini, ...) accept only {@code [a-zA-Z0-9_-]}. A Java-derived key
 * that preserved the commander's own script (e.g. Cyrillic {@code лететь_к_миссии}) is rejected by those
 * APIs, so a non-English commander's custom command would never call. Deriving the key in-language cannot
 * solve this; only the model can translate the intent to an English identifier. That is what this does.
 * <p>
 * It lives in {@code companion.llm} (not with the custom-command code) on purpose: it rides the surviving
 * companion LLM plumbing - {@link CompanionLlmGatewayFactory} for provider selection and the gateway's
 * plain-text {@link LlmGateway#compressMidTermMemory} turn (no tools) for a text-in/text-out call across
 * every wired provider. Placing it here keeps a one-way dependency ({@code companion.llm} → custom-command
 * for {@link CustomCommandKeyDeriver}); the reverse edge would be a package cycle.
 * <p>
 * When no provider is wired (Ollama, or an unconfigured/unsupported provider) the factory throws and this
 * surfaces the failure as a {@link KeyGenerationException} - the caller is expected to warn the commander,
 * not silently fall back to an in-language key that would not route.
 */
public final class CustomCommandKeyGenerator {

    private static final Logger log = LogManager.getLogger(CustomCommandKeyGenerator.class);

    /**
     * Hard cap on the round-trip; the editor is modal, so a hung request must not wedge the UI thread's worker.
     */
    private static final long TIMEOUT_MS = TimeUnit.SECONDS.toMillis(30);

    private static final String SYSTEM_PROMPT = """
            You turn a voice command's trigger phrases into a routing identifier for a game assistant.
            The phrases may be in any language. Output ONE English snake_case identifier that names the
            command's intent: lowercase ASCII letters, digits and underscores only, two to five words,
            no leading or trailing underscore. Translate non-English phrases to English first, then name
            the intent - do not transliterate the foreign words.
            Reply with ONLY the identifier: no quotes, no punctuation, no explanation, no code fences.
            Example input: выбрать специальный инструмент скафандра, выбрать инструмент
            Example output: select_suit_specific_tool""";

    private CustomCommandKeyGenerator() {
    }

    /**
     * Generates a unique English action key for {@code phrases}.
     *
     * @param phrases   the command's comma-separated trigger phrases (the colloquial meanings), any language
     * @param takenKeys action keys already used by other commands, excluded from the result for uniqueness
     * @return a routing-safe, unique English snake_case key
     * @throws KeyGenerationException when no supported provider is configured, the model does not respond in
     *                                time, or it returns nothing usable
     */
    public static String generate(String phrases, Collection<String> takenKeys) throws KeyGenerationException {
        if (phrases == null || phrases.isBlank()) {
            throw new KeyGenerationException("No trigger phrases to generate a key from.");
        }

        LlmGateway gateway;
        try {
            gateway = CompanionLlmGatewayFactory.create();
        } catch (UnsupportedOperationException e) {
            // The factory throws this (with its supported-provider message) when the configured provider has
            // no wired adapter; pass that guidance straight through to the commander. Any other runtime
            // failure is unexpected and propagates so it is logged by the caller rather than misreported here.
            throw new KeyGenerationException(e.getMessage());
        }
        // The gateway owns a single-thread executor; close it once the (single, blocking) call has returned
        // so repeated generations do not leak a thread each.
        try (gateway) {
            return generate(gateway, phrases, takenKeys);
        }
    }

    /**
     * Test seam: run the generation against an injected gateway, skipping provider detection.
     */
    static String generate(LlmGateway gateway, String phrases, Collection<String> takenKeys) throws KeyGenerationException {
        if (phrases == null || phrases.isBlank()) {
            throw new KeyGenerationException("No trigger phrases to generate a key from.");
        }

        LlmRequest request = new LlmRequest(
                UUID.randomUUID().toString(),
                List.of(
                        LlmMessage.of(LlmMessageRole.SYSTEM, SYSTEM_PROMPT),
                        LlmMessage.of(LlmMessageRole.USER, phrases)),
                List.of(),
                PromptCacheProfile.KEY_GENERATION);

        String raw;
        try {
            raw = gateway.compressMidTermMemory(request).get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            throw new KeyGenerationException("The language model did not respond in time. Please try again.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KeyGenerationException("Key generation was interrupted.");
        } catch (ExecutionException e) {
            if (e.getCause() instanceof TimeoutException) {
                throw new KeyGenerationException("The language model did not respond in time. Please try again.");
            }
            log.warn("Action-key generation failed", e);
            throw new KeyGenerationException("Key generation failed. Check your provider and connection, then try again.");
        } catch (Exception e) {
            log.warn("Action-key generation failed", e);
            throw new KeyGenerationException("Key generation failed. Check your provider and connection, then try again.");
        }

        String base = CustomCommandKeyDeriver.toBaseKey(raw);
        if (base.isBlank()) {
            // Null/blank from the plain-text turn usually means an empty or error response from the provider
            // (e.g. bad API key), not a legitimately empty answer.
            log.warn("Action-key generation produced no usable key from model output: {}", raw);
            throw new KeyGenerationException("The language model did not return a usable key. Please try again.");
        }
        return CustomCommandKeyDeriver.uniquify(base, takenKeys);
    }

    /**
     * Signals that an action key could not be generated; its message is safe to show the commander.
     */
    public static final class KeyGenerationException extends Exception {
        public KeyGenerationException(String message) {
            super(message);
        }
    }
}
