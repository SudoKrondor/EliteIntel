package elite.intel.companion.mind;

import elite.intel.companion.model.Urgency;
import elite.intel.gameapi.journal.events.BaseEvent;

/**
 * Assigns urgency at thought birth (§1.1.4): commander input by urgent-phrase matchers, events by an
 * urgent event-type list. Urgency drives preemption in the {@code ThoughtDispatcher}. The matchers are a
 * tunable concern (§7.1); the default treats everything as NORMAL until they are configured, so the
 * preemption machinery is fully wired but never fires yet.
 */
interface UrgencyPolicy {

    Urgency forCommander(String input);

    Urgency forEvent(BaseEvent event);

    /** Default policy: nothing is urgent yet (the urgent matchers are a later tuning step). */
    static UrgencyPolicy normalOnly() {
        return new UrgencyPolicy() {
            @Override
            public Urgency forCommander(String input) {
                return Urgency.NORMAL;
            }

            @Override
            public Urgency forEvent(BaseEvent event) {
                return Urgency.NORMAL;
            }
        };
    }
}
