package elite.intel.companion.prompt;

import elite.intel.companion.model.Urgency;
import elite.intel.companion.model.Verbosity;

/**
 * Decides whether an EVENT thought may comment, gating the {@code speak} tool by urgency and the
 * commander's verbosity (§2.11/§4.2). Preferred enforcement: when commentary is not allowed, {@code speak}
 * is simply not offered to the EVENT thought, which then has only {@code nothing_to_do}.
 * <p>
 * An urgent event may always speak; otherwise {@code QUIET} stays silent and {@code NORMAL}/{@code CHATTY}
 * may comment (the {@code GameEventFilter} already decided the event is noteworthy enough to reach a thought).
 */
public final class EventSpeechPolicy {

    private EventSpeechPolicy() {
    }

    /** Whether an EVENT thought of this urgency may speak under the current verbosity. */
    public static boolean mayComment(Urgency urgency, Verbosity verbosity) {
        return urgency == Urgency.URGENT || verbosity != Verbosity.QUIET;
    }
}
