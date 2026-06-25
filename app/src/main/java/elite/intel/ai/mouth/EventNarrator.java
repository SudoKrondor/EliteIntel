package elite.intel.ai.mouth;

import elite.intel.ai.mouth.subscribers.events.AiVoxResponseEvent;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.eventbus.GameEventBus;
import elite.intel.session.SystemSession;

/**
 * Single owner of game-event narration voice. Journal/status subscribers report what happened through
 * this seam instead of publishing {@code AiVoxResponseEvent}/{@code MissionCriticalAnnouncementEvent}
 * directly, so exactly one voice narrates events per mode:
 * <ul>
 *   <li>legacy command mode: speaks, as before;</li>
 *   <li>companion mode: stays silent, because the consciousness owns event commentary (the same game event
 *       reaches it via the {@code EventFilter -> EVENT thought} path).</li>
 * </ul>
 * A subscriber's state-processing (writing {@code PlayerSession} / DB managers) is unaffected - only its
 * spoken output flows through here. System/diagnostic speech (journal read errors, etc.) and user-toggled
 * ambient announcements (radar/discovery/mining/route/radio) are NOT routed through here and keep speaking.
 */
public final class EventNarrator {

    private EventNarrator() {
    }

    /** Narrate a game event with the interruptible voice. Silent in companion mode. */
    public static void say(String text) {
        if (suppressed()) {
            return;
        }
        GameEventBus.publish(new AiVoxResponseEvent(text));
    }

    /** Narrate a mission-critical game event with the non-interruptible voice. Silent in companion mode. */
    public static void critical(String text) {
        if (suppressed()) {
            return;
        }
        GameEventBus.publish(new MissionCriticalAnnouncementEvent(text));
    }

    /** Companion mode owns event commentary, so legacy subscriber narration is suppressed there. */
    private static boolean suppressed() {
        return SystemSession.getInstance().companionModeOn();
    }
}
