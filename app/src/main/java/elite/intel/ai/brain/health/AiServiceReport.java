package elite.intel.ai.brain.health;

import elite.intel.util.StringUtls;

import java.time.Duration;

/**
 * Says an {@link AiServiceVerdict} out loud, in the commander's language, from fixed templates.
 * <p>
 * No model writes these words: the check exists for the moment the model may be what is broken. Every sentence
 * reports what was observed and what the commander can check, never a cause a provider's status code claimed.
 */
public final class AiServiceReport {

    private static final String KEY = "query.selfDiagnostic.";

    private AiServiceReport() {
    }

    public static String spoken(AiServiceVerdict verdict) {
        return switch (verdict) {
            case AiServiceVerdict.NotConfigured ignored -> say("notConfigured");
            case AiServiceVerdict.NoProvider ignored -> say("noProvider");
            case AiServiceVerdict.LocalHostNotRunning ignored -> say("localNotRunning");
            case AiServiceVerdict.LocalModelMissing ignored -> say("localModelMissing");
            case AiServiceVerdict.Unreachable unreachable -> say("unreachable", unreachable.service());
            case AiServiceVerdict.Refused refused -> refused(refused);
            case AiServiceVerdict.NoAnswer noAnswer -> say("noAnswer", noAnswer.service(), seconds(noAnswer.waited()));
            case AiServiceVerdict.Connected connected -> connected(connected);
        };
    }

    private static String refused(AiServiceVerdict.Refused refused) {
        String sentence = refused.refusingFor()
                .map(running -> say("refusedFor", refused.service(), sinceDuration(running)))
                .orElseGet(() -> say("refused", refused.service()));
        return refused.keyRejected() ? sentence + " " + say("keyHint") : sentence;
    }

    private static String connected(AiServiceVerdict.Connected connected) {
        String sentence = connected.typicalReply()
                .map(reply -> say("connectedReplyTime", connected.service(), seconds(reply)))
                .orElseGet(() -> say("connected", connected.service()));
        return connected.unsupportedModel() ? sentence + " " + say("unsupportedModel") : sentence;
    }

    /**
     * Whole seconds, never fewer than one: "about 0 seconds" is not something to say.
     */
    private static String seconds(Duration duration) {
        long seconds = Math.max(1, Math.round(duration.toMillis() / 1000.0));
        return StringUtls.localizedEventPlural((int) seconds, "event.time.seconds");
    }

    /**
     * The largest whole unit only - "for 2 hours" - because a spoken outage length needs no precision.
     */
    private static String sinceDuration(Duration duration) {
        if (duration.toDays() > 0) {
            return StringUtls.localizedEventPlural((int) duration.toDays(), "event.time.days");
        }
        if (duration.toHours() > 0) {
            return StringUtls.localizedEventPlural((int) duration.toHours(), "event.time.hours");
        }
        return StringUtls.localizedEventPlural((int) Math.max(1, duration.toMinutes()), "event.time.minutes");
    }

    private static String say(String key, Object... args) {
        return StringUtls.localizedResponse(KEY + key, args);
    }
}
