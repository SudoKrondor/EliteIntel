package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.brain.vega.CompanionRuntime;
import elite.intel.db.managers.DeferredNotificationManager;
import elite.intel.gameapi.journal.events.CarrierJumpRequestEvent;
import elite.intel.session.PlayerSession;

import java.time.Duration;
import java.time.Instant;

import static elite.intel.util.StringUtls.localizedEvent;
import static elite.intel.util.StringUtls.localizedEventPlural;

@SuppressWarnings("unused")
public class CarrierJumpRequestSubscriber {

    @Subscribe
    public void onCarrierJumpRequestEvent(CarrierJumpRequestEvent event) {
        Thread.ofVirtual().start(() -> {
            // The system is the destination the commander plots and thinks in, and it is the field the game always
            // fills. Body is the arrival point inside that system and is sometimes absent entirely - notably when
            // the jump was scheduled by typing just a system name, which is what following a carrier route does.
            // Announcing off Body alone left the payload with no destination while the instruction still demanded
            // one, and the model answered by inventing a familiar system name.
            String destination = firstNonBlank(event.getSystemName(), event.getBody());
            String rawDepartureTime = event.getDepartureTime();

            Instant departureInstant = Instant.parse(rawDepartureTime);
            long totalMinutes = Duration.between(Instant.now(), departureInstant).toMinutes();
            long hours = totalMinutes / 60;
            long minutes = totalMinutes % 60;

            String hoursStr = localizedEventPlural((int) hours, "event.time.hours");
            String minutesStr = localizedEventPlural((int) minutes, "event.time.minutes");
            String timeUntil;
            if (hours > 0 && minutes > 0) {
                timeUntil = localizedEvent("event.time.hoursAndMinutes", hoursStr, minutesStr);
            } else if (hours > 0) {
                timeUntil = hoursStr;
            } else {
                timeUntil = minutesStr;
            }

            String data;
            String instructions;
            if (destination != null) {
                data = localizedEvent("event.carrier.scheduledDepartTo", destination, timeUntil);
                instructions = "Report the carrier departure. State the destination and the time until departure.";
            } else {
                data = localizedEvent("event.carrier.scheduledDepart", timeUntil);
                // Never ask for a destination this payload does not carry: an instruction to state a missing fact
                // is an instruction to invent one.
                instructions = "Report the carrier departure. State the time until departure. "
                        + "The destination is unknown - do not name one.";
            }

            PlayerSession playerSession = PlayerSession.getInstance();
            playerSession.setCarrierDepartureTime(rawDepartureTime);

            long millis = Instant.parse(event.getDepartureTime()).toEpochMilli() - (1000 * 60 * 3);
            DeferredNotificationManager.getInstance().scheduleNotification(localizedEvent("event.carrier.departingThreeMinutes"), millis);
            CompanionRuntime.narrator().narrate(data, instructions);
        });
    }

    /**
     * The first value that is neither null nor blank, or null when there is none.
     */
    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
