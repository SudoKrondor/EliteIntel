package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.db.managers.DeferredNotificationManager;
import elite.intel.eventbus.GameEventBus;
import elite.intel.gameapi.SensorDataEvent;
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
            String destinationStellarBody = event.getBody();
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

            StringBuilder sb = new StringBuilder();
            if (destinationStellarBody != null && !destinationStellarBody.isBlank()) {
                sb.append(localizedEvent("event.carrier.scheduledDepartTo", destinationStellarBody, timeUntil));
            } else {
                sb.append(localizedEvent("event.carrier.scheduledDepart", timeUntil));
            }

            PlayerSession playerSession = PlayerSession.getInstance();
            playerSession.setCarrierDepartureTime(rawDepartureTime);

            long millis = Instant.parse(event.getDepartureTime()).toEpochMilli() - (1000 * 60 * 3);
            DeferredNotificationManager.getInstance().scheduleNotification(localizedEvent("event.carrier.departingThreeMinutes"), millis);
            String instructions = "Report the carrier departure. State the destination and the time until departure.";
            GameEventBus.publish(new SensorDataEvent(sb.toString(), instructions));
        });
    }
}
