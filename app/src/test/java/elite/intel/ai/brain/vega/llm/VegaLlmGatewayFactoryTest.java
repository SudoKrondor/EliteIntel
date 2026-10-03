package elite.intel.ai.brain.vega.llm;

import elite.intel.ai.brain.vega.model.llm.LlmResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The gateway a cloud setup runs on until the commander picks a provider: it must answer every request at
 * once as an unreachable service - never hang a turn, and never throw into the thought that asked.
 */
class VegaLlmGatewayFactoryTest {

    @Test
    void anUnselectedProviderAnswersEveryRequestAsAnUnreachableService() {
        try (LlmGateway gateway = new UnselectedProviderGateway()) {
            LlmResult result = gateway.submit(null).join();

            assertEquals(LlmResult.Status.SERVICE_UNAVAILABLE, result.status());
            assertEquals(0, result.toolInvocations().size());
            assertNull(gateway.completePlainText(null).join());
        }
    }
}
