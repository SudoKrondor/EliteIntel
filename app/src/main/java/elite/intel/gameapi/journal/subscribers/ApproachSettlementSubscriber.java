package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.gameapi.EventBusManager;
import elite.intel.gameapi.SensorDataEvent;
import elite.intel.gameapi.journal.events.ApproachSettlementEvent;
import elite.intel.session.PlayerSession;

import java.util.List;

import static elite.intel.util.StringUtls.localizedEvent;

public class ApproachSettlementSubscriber {

    private final PlayerSession playerSession = PlayerSession.getInstance();

    @Subscribe
    public void onApproachSettlementEvent(ApproachSettlementEvent event) {
        Thread.ofVirtual().start(() -> {
            StringBuilder sb = new StringBuilder();

            sb.append(localizedEvent("event.approach.settlement.approaching", event.getName()));
            sb.append(" ");

            if ("$government_Engineer;".equalsIgnoreCase(event.getStationGovernment())) {
                sb.append(localizedEvent("event.approach.settlement.engineer", event.getStationFaction().getName()));
                sb.append(" ");
            }

            sb.append(localizedEvent("event.approach.settlement.allegiance", event.getStationAllegiance())).append(" ");
            sb.append(localizedEvent("event.approach.settlement.economy", event.getStationEconomy())).append(" ");
            sb.append(localizedEvent("event.approach.settlement.government", event.getStationGovernment())).append(" ");
            if (event.getStationFaction() != null) {
                sb.append(localizedEvent("event.approach.settlement.faction", event.getStationFaction().getName())).append(" ");
            }

            List<String> stationServices = event.getStationServices();
            if (stationServices != null && !stationServices.isEmpty()) {
                sb.append(localizedEvent("event.approach.settlement.services")).append(" ");
                for (String services : stationServices) {
                    sb.append(services);
                    sb.append(", ");
                }
            }

            String availableData = LocalServicesData.setLocalServicesData(event.getMarketID());
            if (!availableData.isEmpty()) sb.append(" ").append(localizedEvent("event.approach.settlement.moreData"));

            if (playerSession.isRouteAnnouncementOn()) {
                String instructions = """
                            Approaching settlement.
                            Provide very brief summary for the settlement data.
                            Do not list every service.
                        """;
                EventBusManager.publish(new SensorDataEvent(sb.toString(), instructions));
            }
        });
    }
}
