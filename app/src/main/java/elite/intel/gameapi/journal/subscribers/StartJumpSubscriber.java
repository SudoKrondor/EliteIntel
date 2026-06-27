package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.db.managers.GlobalSettingsManager;
import elite.intel.eventbus.GameEventBus;
import elite.intel.gameapi.SensorDataEvent;
import elite.intel.gameapi.journal.events.StartJumpEvent;
import elite.intel.search.edsm.EdsmApiClient;
import elite.intel.search.edsm.dto.DeathsDto;
import elite.intel.search.edsm.dto.TrafficDto;
import elite.intel.session.PlayerSession;

import static elite.intel.util.StringUtls.localizedEvent;

@SuppressWarnings("unused")
public class StartJumpSubscriber {

    @Subscribe
    public void onStartJumpEvent(StartJumpEvent event) {
        if ("Hyperspace".equalsIgnoreCase(event.getJumpType())) {
            Thread.ofVirtual().start(() -> {
                GlobalSettingsManager settings = GlobalSettingsManager.getInstance();
                boolean announceRoute = settings.getAnnounceJumpRoute();
                boolean announceTraffic = settings.getAnnounceJumpTraffic();
                boolean announceDeaths = settings.getAnnounceJumpDeaths();

                StringBuilder sb = new StringBuilder();
                if (announceRoute) {
                    sb.append(localizedEvent("event.startJump.route", event.getStarSystem(), event.getStarClass()));
                }
                // Skip the EDSM lookups entirely when the matching announcement is disabled.
                if (announceTraffic) {
                    TrafficDto trafficDto = EdsmApiClient.searchTraffic(event.getStarSystem());
                    if (trafficDto.getData() != null && trafficDto.getData().getTraffic() != null && trafficDto.getData().getTraffic().getTotal() > 0) {
                        if (sb.length() > 0) sb.append(" ");
                        sb.append(localizedEvent("event.startJump.traffic", trafficDto.getData().getTraffic().toYaml()));
                    }
                }
                if (announceDeaths) {
                    DeathsDto deathsDto = EdsmApiClient.searchDeaths(event.getStarSystem());
                    if (deathsDto.getData() != null && deathsDto.getData().getDeaths() != null && deathsDto.getData().getDeaths().getTotal() > 0) {
                        if (sb.length() > 0) sb.append(" ");
                        sb.append(localizedEvent("event.startJump.deaths", deathsDto.getData().getDeaths().toYaml()));
                    }
                }

                PlayerSession playerSession = PlayerSession.getInstance();
                playerSession.clearGenusPaymentAnnounced();
                if (playerSession.isRouteAnnouncementOn() && sb.length() > 0) {
                    String instructions = """
                                    Notify User about the star system we are traveling to using this exact format.
                                    Example: In route to <name>, <class> class star.
                            
                            Data may include traffic and fatalities.
                            Traffic total,weekly and daily indicates number of ships traveled through this system. Deaths data indicates number of ships lost in this system.
                            Example: Traffic: total X, weekly Y, daily Z. Deaths: total A, weekly B, daily C.
                                - IF no traffic data is available, omit mentioning traffic info.
                                - IF no deaths data is available, omit mentioning fatalities.
                            """;
                    GameEventBus.publish(new SensorDataEvent(sb.toString(), instructions, SensorDataEvent.TOPIC_NAVIGATION));
                }
            }); // end virtual thread
        }
    }
}
