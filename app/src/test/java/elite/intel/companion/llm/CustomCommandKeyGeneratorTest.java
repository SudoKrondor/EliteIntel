package elite.intel.companion.llm;

import elite.intel.companion.model.llm.LlmRequest;
import elite.intel.companion.model.llm.LlmResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CustomCommandKeyGeneratorTest {

    /**
     * Fake gateway: only the plain-text compression turn is used by the generator.
     */
    private static LlmGateway gatewayReturning(Function<LlmRequest, String> text) {
        return new LlmGateway() {
            @Override
            public CompletableFuture<LlmResult> submit(LlmRequest request) {
                throw new UnsupportedOperationException("not used");
            }

            @Override
            public CompletableFuture<String> compressMidTermMemory(LlmRequest request) {
                return CompletableFuture.completedFuture(text.apply(request));
            }
        };
    }

    @Test
    void mapsModelOutputToSanitizedSnakeCaseKey() throws Exception {
        LlmGateway gateway = gatewayReturning(request -> "select_suit_specific_tool");
        String key = CustomCommandKeyGenerator.generate(gateway,
                "выбрать специальный инструмент скафандра, выбрать инструмент", List.of());
        assertEquals("select_suit_specific_tool", key);
    }

    @Test
    void sanitizesNoisyModelOutput() throws Exception {
        // The model wrapped the key in quotes / a sentence despite instructions; sanitize folds it clean.
        LlmGateway gateway = gatewayReturning(request -> "\"Navigate To Mission\".");
        String key = CustomCommandKeyGenerator.generate(gateway, "лететь к миссии", List.of());
        assertEquals("navigate_to_mission", key);
    }

    @Test
    void appendsSuffixWhenKeyCollidesWithAnExistingCommand() throws Exception {
        LlmGateway gateway = gatewayReturning(request -> "go_to_mission");
        String key = CustomCommandKeyGenerator.generate(gateway, "go to mission", List.of("go_to_mission"));
        assertEquals("go_to_mission_2", key);
    }

    @Test
    void sendsThePhrasesAsTheUserMessage() throws Exception {
        String[] seen = new String[1];
        LlmGateway gateway = gatewayReturning(request -> {
            seen[0] = request.messages().get(request.messages().size() - 1).content();
            return "test_key";
        });
        CustomCommandKeyGenerator.generate(gateway, "жечь топливо, форсаж", List.of());
        assertEquals("жечь топливо, форсаж", seen[0]);
    }

    @Test
    void blankModelOutputRaisesKeyGenerationException() {
        LlmGateway gateway = gatewayReturning(request -> "   ");
        assertThrows(CustomCommandKeyGenerator.KeyGenerationException.class,
                () -> CustomCommandKeyGenerator.generate(gateway, "go to mission", List.of()));
    }

    @Test
    void nullModelOutputRaisesKeyGenerationException() {
        // A provider/transport error surfaces as null from the plain-text turn; must not yield a fallback key.
        LlmGateway gateway = gatewayReturning(request -> null);
        assertThrows(CustomCommandKeyGenerator.KeyGenerationException.class,
                () -> CustomCommandKeyGenerator.generate(gateway, "go to mission", List.of()));
    }

    @Test
    void blankPhrasesRaiseWithoutCallingTheModel() {
        LlmGateway gateway = gatewayReturning(request -> {
            throw new AssertionError("model must not be called for blank phrases");
        });
        assertThrows(CustomCommandKeyGenerator.KeyGenerationException.class,
                () -> CustomCommandKeyGenerator.generate(gateway, "   ", List.of()));
    }
}
