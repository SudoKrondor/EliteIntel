package elite.intel.ai.brain.health;

import elite.intel.ai.LlmProviderResolver;
import elite.intel.ai.ProviderEnum;
import elite.intel.ai.brain.LocalLlmModelCheck;
import elite.intel.ai.brain.inference.lmstudio.LMStudioClient;
import elite.intel.ai.brain.inference.lmstudio.LMStudioClient.ModelListing;
import elite.intel.ai.brain.vega.llm.LlmGateway;
import elite.intel.ai.brain.vega.llm.VegaLlmGatewayFactory;
import elite.intel.ai.brain.vega.model.llm.LlmMessage;
import elite.intel.ai.brain.vega.model.llm.LlmMessageRole;
import elite.intel.ai.brain.vega.model.llm.LlmRequest;
import elite.intel.ai.brain.vega.model.llm.PromptCacheProfile;
import elite.intel.session.SystemSession;
import elite.intel.setup.SetupCheck;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

/**
 * Tests the AI connection VEGA actually runs on and says what it found: the command model, through the same
 * gateway a commander turn uses, not a stand-in endpoint.
 * <p>
 * The verdict comes from what happened on the wire ({@link AiServiceHealth}), never from what a provider's status
 * code claims the cause was: providers send codes that describe something else entirely. For LM Studio the
 * server's own model list is asked first, since it is local and can be trusted to tell "not running" apart from
 * "running without the model".
 * <p>
 * Blocking: a check waits up to {@link #PROBE_TIMEOUT} for the model, so never call it on the EDT.
 */
public final class AiServiceCheck {

    private static final Logger log = LogManager.getLogger(AiServiceCheck.class);

    /**
     * How long the test request may take. Long enough for LM Studio to load a model on first use, short enough
     * that the commander is not left waiting on a service that is down.
     */
    static final Duration PROBE_TIMEOUT = Duration.ofSeconds(10);

    /**
     * The name LM Studio is spoken by.
     */
    static final String LOCAL_HOST_NAME = "LM Studio";

    /**
     * A run of refusals shorter than this is the check's own, not a standing outage worth putting a length on.
     */
    private static final Duration STANDING_REFUSAL = Duration.ofMinutes(1);

    private final Supplier<Configuration> configuration;
    private final Supplier<ModelListing> localModels;
    private final Supplier<LlmGateway> gateways;
    private final AiServiceHealth health;
    private final Clock clock;
    private final Duration probeTimeout;

    AiServiceCheck(Supplier<Configuration> configuration, Supplier<ModelListing> localModels,
                   Supplier<LlmGateway> gateways, AiServiceHealth health, Clock clock, Duration probeTimeout) {
        this.configuration = Objects.requireNonNull(configuration, "configuration");
        this.localModels = Objects.requireNonNull(localModels, "localModels");
        this.gateways = Objects.requireNonNull(gateways, "gateways");
        this.health = Objects.requireNonNull(health, "health");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.probeTimeout = Objects.requireNonNull(probeTimeout, "probeTimeout");
    }

    /**
     * A check against the live settings, LM Studio and provider.
     */
    public static AiServiceCheck live() {
        return new AiServiceCheck(Configuration::current, () -> LMStudioClient.getInstance().listModels(),
                VegaLlmGatewayFactory::create, AiServiceHealth.getInstance(), Clock.systemUTC(), PROBE_TIMEOUT);
    }

    /**
     * Runs the check. Sends one small request to the model when there is one to send it to.
     */
    public AiServiceVerdict run() {
        Configuration config = configuration.get();
        if (config.nothingConfigured()) {
            return new AiServiceVerdict.NotConfigured();
        }
        if (config.local()) {
            return checkLocal(config.localModel());
        }
        return config.cloudProvider()
                .map(provider -> probe(spokenName(provider), false))
                .orElseGet(AiServiceVerdict.NoProvider::new);
    }

    private AiServiceVerdict checkLocal(String model) {
        if (model.isBlank()) {
            // WHY: LM Studio answers an empty model name with whatever it has loaded, so probing now would report
            // a connection to a model the commander never chose.
            return new AiServiceVerdict.NotConfigured();
        }
        ModelListing listing = localModels.get();
        if (listing instanceof ModelListing.NotAnswering) {
            return new AiServiceVerdict.LocalHostNotRunning();
        }
        if (listing instanceof ModelListing.Listed listed && !listed.includes(model)) {
            return new AiServiceVerdict.LocalModelMissing();
        }
        AiServiceVerdict verdict = probe(LOCAL_HOST_NAME, true);
        if (verdict instanceof AiServiceVerdict.Connected connected && !LocalLlmModelCheck.isSupported(model)) {
            return new AiServiceVerdict.Connected(connected.service(), connected.typicalReply(), true);
        }
        return verdict;
    }

    /**
     * Sends the test request, then reads what the wire recorded for it.
     * <p>
     * WHY: the gateway reports only "a reply or none", and folds every failure into the same null. Whether the
     * provider was unreachable or answered with a refusal is known only to the transport, which records it in
     * {@link AiServiceHealth} before the reply future completes. A commander turn landing in the same seconds
     * could stand in for the probe's own exchange; either one describes the service as it is right now.
     */
    private AiServiceVerdict probe(String service, boolean local) {
        Instant started = clock.instant();
        sendTestRequest();
        AiServiceHealth.Snapshot wire = health.snapshot();
        if (!wire.exchangedSince(started)) {
            return new AiServiceVerdict.NoAnswer(service, probeTimeout);
        }
        return switch (wire.lastOutcome()) {
            case ANSWERED -> new AiServiceVerdict.Connected(service, wire.typicalReplyTime(), false);
            case UNREACHABLE -> local
                    ? new AiServiceVerdict.LocalHostNotRunning()
                    : new AiServiceVerdict.Unreachable(service);
            case REFUSED -> new AiServiceVerdict.Refused(service, refusingFor(wire.failingSince(), started),
                    isKeyRejection(wire.lastRefusalCode()));
        };
    }

    private void sendTestRequest() {
        try (LlmGateway gateway = gateways.get()) {
            CompletableFuture<String> reply = gateway.completePlainText(testRequest());
            try {
                reply.get(probeTimeout.toMillis(), TimeUnit.MILLISECONDS);
            } catch (TimeoutException slow) {
                reply.cancel(true);
            } catch (ExecutionException failed) {
                log.debug("AI connection test request failed: {}", failed.getMessage());
            } catch (InterruptedException e) {
                reply.cancel(true);
                Thread.currentThread().interrupt();
            }
        }
    }

    private static LlmRequest testRequest() {
        return new LlmRequest(UUID.randomUUID().toString(),
                List.of(LlmMessage.of(LlmMessageRole.USER, "Reply with the single word OK.")),
                List.of(), PromptCacheProfile.CONNECTION_PROBE);
    }

    /**
     * How long the refusals have run, when they began before this check rather than with it.
     */
    private static Optional<Duration> refusingFor(Instant failingSince, Instant checkStarted) {
        if (failingSince == null) {
            return Optional.empty();
        }
        Duration running = Duration.between(failingSince, checkStarted);
        return running.compareTo(STANDING_REFUSAL) < 0 ? Optional.empty() : Optional.of(running);
    }

    private static boolean isKeyRejection(Integer statusCode) {
        return statusCode != null && (statusCode == 401 || statusCode == 403);
    }

    /**
     * The provider as it is said aloud: its display name without the brackets around the model family, which a
     * speech engine may read out.
     */
    static String spokenName(ProviderEnum provider) {
        return provider.displayName().replace("(", "").replace(")", "");
    }

    /**
     * The settings a check depends on.
     *
     * @param nothingConfigured no cloud key and no local model at all
     * @param local             the command model runs in LM Studio
     * @param localModel        the LM Studio model name, blank when none
     * @param cloudProvider     the chosen cloud provider, empty when none
     */
    record Configuration(boolean nothingConfigured, boolean local, String localModel,
                         Optional<ProviderEnum> cloudProvider) {

        Configuration {
            localModel = localModel == null ? "" : localModel.strip();
            Objects.requireNonNull(cloudProvider, "cloudProvider");
        }

        static Configuration current() {
            SystemSession session = SystemSession.getInstance();
            return new Configuration(SetupCheck.isLlmUnconfigured(), session.useLocalCommandLlm(),
                    session.getLmStudioCommandModel(), LlmProviderResolver.cloudProvider());
        }
    }
}
