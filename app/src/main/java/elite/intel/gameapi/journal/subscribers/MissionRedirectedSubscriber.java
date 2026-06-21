package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.db.managers.MissionManager;
import elite.intel.eventbus.GameEventBus;
import elite.intel.gameapi.SensorDataEvent;
import elite.intel.gameapi.journal.events.MissionRedirectedEvent;
import elite.intel.gameapi.journal.events.dto.MissionDto;
import elite.intel.util.yaml.ToYamlConvertable;
import elite.intel.util.yaml.YamlFactory;

@SuppressWarnings("unused")
public class MissionRedirectedSubscriber {

    private final MissionManager missionManager = MissionManager.getInstance();

    @Subscribe
    public void onMissionRedirectedSubscriber(MissionRedirectedEvent event) {
        Thread.ofVirtual().start(() -> {
            MissionDto mission = missionManager.getMission(event.getMissionID());
            String newDestinationStation = event.getNewDestinationStation();
            String newDestinationSystem = event.getNewDestinationSystem();

            if (!newDestinationStation.isEmpty()) {
                mission.setDestinationStation(newDestinationStation);
            }
            if (!newDestinationSystem.isEmpty()) {
                mission.setDestinationSystem(newDestinationSystem);
            }
            missionManager.save(mission);

            String instructions = """
                        Notify user of mission update.
                         - IF new destination system present, announce new destination star system.
                         - IF new station is present announce new destination station.
                         Example: Mission for <faction> is redirected to <New System> - <New Station>
                    """;
            GameEventBus.publish(
                    new SensorDataEvent(
                            new MissionRedirectData(mission.getFaction(), newDestinationSystem, newDestinationStation).toYaml(),
                            instructions
                    )
            );
        });
    }

    record MissionRedirectData(String faction, String newDestination, String newStation) implements ToYamlConvertable {
        @Override public String toYaml() {
            return YamlFactory.toYaml(this);
        }
    }

}
