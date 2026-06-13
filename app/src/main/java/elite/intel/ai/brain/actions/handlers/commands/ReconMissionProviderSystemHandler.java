package elite.intel.ai.brain.actions.handlers.commands;

import com.google.gson.JsonObject;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.db.dao.PirateHuntingGroundsDao.HuntingGround;
import elite.intel.db.dao.PirateMissionProviderDao.MissionProvider;
import elite.intel.db.managers.HuntingGroundManager;
import elite.intel.db.managers.HuntingGroundManager.PirateMissionTuple;
import elite.intel.db.managers.LocationManager;
import elite.intel.db.managers.MissionManager;
import elite.intel.db.managers.ReminderManager;
import elite.intel.gameapi.EventBusManager;
import elite.intel.gameapi.UserInputEvent;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.gameapi.journal.events.dto.MissionDto;
import elite.intel.session.PlayerSession;
import elite.intel.util.StringUtls;
import elite.intel.util.yaml.ToYamlConvertable;
import elite.intel.util.yaml.YamlFactory;

import java.util.List;
import java.util.Map;

public class ReconMissionProviderSystemHandler implements CommandHandler {

    private final HuntingGroundManager huntingGroundManager = HuntingGroundManager.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();
    private final PlayerSession playerSession = PlayerSession.getInstance();


    @Override public void handle(String action, JsonObject params, String responseText) {

        LocationDto currentLocation = locationManager.findByLocationData(playerSession.getLocationData());
        List<PirateMissionTuple<HuntingGround, List<MissionProvider>>> huntingGrounds = huntingGroundManager.findInProviderForTargetStarSystem(currentLocation.getStarName(), null);

        /// if huntingGrounds is empty (we are not in a target system) try looking for matching target faction.
        if (huntingGrounds.isEmpty()) {
            MissionManager missionManager = MissionManager.getInstance();
            Map<Long, MissionDto> missions = missionManager.getMissions(missionManager.getPirateMissionTypes());
            if (!missions.isEmpty()) {
                String targetFaction = missions.values().stream().findFirst().get().getMissionTargetFaction();
                huntingGrounds = huntingGroundManager.findInProviderForTargetStarSystem(
                        huntingGroundManager.findStarSystemForFactionName(targetFaction),
                        null
                );
            }
        }


        MissionProvider provider = null;
        String targetStarSystemName = "";
        for (PirateMissionTuple<HuntingGround, List<MissionProvider>> pair : huntingGrounds) {
            List<MissionProvider> providers = pair.getMissionProvider();

            int size = providers.size();
            if (size == 1) {
                EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.pirate.oneMissionProvider", size, pair.getTarget().getStarSystem())));
            } else {
                EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.pirate.manyMissionProviders", size, pair.getTarget().getStarSystem())));
            }

            provider = providers.stream().filter(p -> p.getMissionProviderFaction() == null).findFirst().orElse(null);
            targetStarSystemName = pair.getTarget().getStarSystem();
            if (provider != null) break;
        }

        if (provider == null) {
            if (tryConfirmedMissionProvider()) {
                return;
            }
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.pirate.noProviderForTarget", targetStarSystemName)));
            return;
        }

        huntingGrounds.stream().filter(
                data -> data.getTarget().getTargetFaction() == null
        ).findFirst().map(PirateMissionTuple::getTarget);

        String starSystem = provider.getStarSystem();
        EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.pirate.plottingToProvider", starSystem, targetStarSystemName)));

        RoutePlotter plotter = new RoutePlotter();
        plotter.plotRoute(starSystem);
        ReminderManager.getInstance().setReminder(
                StringUtls.localizedLlm("handler.pirate.seekProviderReminder", targetStarSystemName),
                targetStarSystemName
        );
    }

    private boolean tryConfirmedMissionProvider() {
        LocationDto location = locationManager.findByLocationData(playerSession.getLocationData());
        List<MissionProvider> missionProviders = huntingGroundManager.findConfirmedMissionProviders();
        String destination = null;
        String targetSystem = null;
        for (MissionProvider provider : missionProviders) {
            if (!location.getStarName().equalsIgnoreCase(provider.getStarSystem())) {
                destination = provider.getStarSystem();
                targetSystem = provider.getTargetSystem();
                break;
            }
        }

        if (location.getStarName().equalsIgnoreCase(targetSystem)) {
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.pirate.checkPorts", targetSystem)));
        } else {
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.pirate.headTo", destination, targetSystem)));
        }

        if (destination == null) {
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.pirate.noKnowingProviders")));
            EventBusManager.publish(new UserInputEvent(" find hunting grounds"));
            return false;
        } else {
            RoutePlotter plotter = new RoutePlotter();
            plotter.plotRoute(destination);
            return true;
        }

    }

    record DataDto(String starSystem, String assignment) implements ToYamlConvertable {

        @Override public String toYaml() {
            return YamlFactory.toYaml(this);
        }
    }
}
