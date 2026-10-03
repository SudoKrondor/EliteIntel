package elite.intel.ai.brain.vega.llm;

import elite.intel.ai.brain.vega.model.llm.LlmRequest;
import elite.intel.ai.brain.vega.model.llm.LlmResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * The gateway VEGA runs on while the commander is set up for a cloud model but has not picked a provider.
 * There is nobody to send a request to, so every request reports {@link LlmResult.Status#SERVICE_UNAVAILABLE}
 * at once, exactly as an unreachable provider would, and the commander hears the service-unreachable phrase.
 * <p>
 * It exists so a missing provider does not stop the services from starting: with them up,
 * {@link elite.intel.setup.SetupCheck} can say out loud what is missing, and the reflex commands, which never
 * ask a model, keep working in the meantime.
 */
final class UnselectedProviderGateway implements LlmGateway {

    private static final LlmResult UNAVAILABLE = new LlmResult(LlmResult.Status.SERVICE_UNAVAILABLE, List.of());

    @Override
    public CompletableFuture<LlmResult> submit(LlmRequest request) {
        return CompletableFuture.completedFuture(UNAVAILABLE);
    }

    @Override
    public CompletableFuture<String> completePlainText(LlmRequest request) {
        return CompletableFuture.completedFuture(null);
    }
}
