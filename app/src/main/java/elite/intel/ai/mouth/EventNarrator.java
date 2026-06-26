package elite.intel.ai.mouth;

import elite.intel.ai.mouth.subscribers.events.AiVoxResponseEvent;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.eventbus.GameEventBus;

/**
 * Single owner of deterministic game-event narration voice. Journal/status subscribers report what
 * happened through this seam instead of publishing {@code AiVoxResponseEvent}/{@code
 * MissionCriticalAnnouncementEvent} directly, so all functional callouts share one entry point.
 *
 * <p>These are functional and safety-critical callouts (fuel/oxygen/hull warnings, ship proximity,
 * target scan, pirate alert, cargo-scan detected, kill confirmation, crime alert, etc.) that the
 * commander needs in every mode - so they narrate <strong>regardless of companion mode</strong>. The
 * companion consciousness does not re-speak them: every event whose voice is owned here is classified
 * {@link elite.intel.gameapi.journal.events.BaseEvent.Importance#NORMAL}, which keeps it in memory but
 * off the consciousness's spoken channel, so there is no double narration.
 *
 * <p>A subscriber's state-processing (writing {@code PlayerSession} / DB managers) is unaffected - only
 * its spoken output flows through here. System/diagnostic speech (journal read errors, etc.) and
 * user-toggled ambient announcements (radar/discovery/mining/route/radio) are NOT routed through here
 * and keep their own paths.
 */
public final class EventNarrator {

    private EventNarrator() {
    }

    /** Narrate a game event with the interruptible voice, in every mode. */
    public static void say(String text) {
        GameEventBus.publish(new AiVoxResponseEvent(text));
    }

    /** Narrate a mission-critical game event with the non-interruptible voice, in every mode. */
    public static void critical(String text) {
        GameEventBus.publish(new MissionCriticalAnnouncementEvent(text));
    }
}
