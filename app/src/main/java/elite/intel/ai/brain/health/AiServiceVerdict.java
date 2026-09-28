package elite.intel.ai.brain.health;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * What {@link AiServiceCheck} found, one case per thing the commander would do differently. Each case carries only
 * what its own sentence needs; {@link AiServiceReport} turns it into words.
 */
public sealed interface AiServiceVerdict {

    /**
     * True only when the model answered.
     */
    default boolean connected() {
        return this instanceof Connected;
    }

    /**
     * True for the setup gaps {@code SetupCheck} already speaks about at startup, where a second announcement
     * of the same thing would only repeat it.
     */
    default boolean isSetupGap() {
        return this instanceof NotConfigured || this instanceof NoProvider;
    }

    /**
     * No model is set up at all: no cloud key and no local model, or local with no model named.
     */
    record NotConfigured() implements AiServiceVerdict {
    }

    /**
     * Set up for a cloud model, but no provider is chosen.
     */
    record NoProvider() implements AiServiceVerdict {
    }

    /**
     * LM Studio gave no answer at all.
     */
    record LocalHostNotRunning() implements AiServiceVerdict {
    }

    /**
     * LM Studio is running, but does not list the model the commander chose.
     */
    record LocalModelMissing() implements AiServiceVerdict {
    }

    /**
     * A cloud provider could not be reached at all.
     */
    record Unreachable(String service) implements AiServiceVerdict {
        public Unreachable {
            Objects.requireNonNull(service, "service");
        }
    }

    /**
     * The provider answered, but not with a reply.
     *
     * @param refusingFor how long every request has been refused, when that started before this check
     * @param keyRejected the refusal was a 401 or 403 - the one a commander might fix, though a provider can send
     *                    it for other reasons too, so it is only ever offered as something to check
     */
    record Refused(String service, Optional<Duration> refusingFor, boolean keyRejected) implements AiServiceVerdict {
        public Refused {
            Objects.requireNonNull(service, "service");
            Objects.requireNonNull(refusingFor, "refusingFor");
        }
    }

    /**
     * No answer of any kind came back within {@code waited}.
     */
    record NoAnswer(String service, Duration waited) implements AiServiceVerdict {
        public NoAnswer {
            Objects.requireNonNull(service, "service");
            Objects.requireNonNull(waited, "waited");
        }
    }

    /**
     * The model answered.
     *
     * @param typicalReply     the median time recent answers took, when there are any
     * @param unsupportedModel a local model other than the one this version is built around
     */
    record Connected(String service, Optional<Duration> typicalReply, boolean unsupportedModel)
            implements AiServiceVerdict {
        public Connected {
            Objects.requireNonNull(service, "service");
            Objects.requireNonNull(typicalReply, "typicalReply");
        }
    }
}
