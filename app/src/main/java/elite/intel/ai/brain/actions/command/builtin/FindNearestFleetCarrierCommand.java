package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.db.managers.LocationManager;
import elite.intel.eventbus.GameEventBus;
import elite.intel.gameapi.inputs.RoutePlotter;
import elite.intel.gameapi.journal.events.dto.CarrierDataDto;
import elite.intel.search.spansh.findcarrier.CarrierAccess;
import elite.intel.search.spansh.findcarrier.FleetCarrierSearch;
import elite.intel.search.spansh.findcarrier.FleetCarrierSearchResultsDto;
import elite.intel.session.PlayerSession;
import elite.intel.session.Status;
import elite.intel.util.StringUtls;
import elite.intel.util.TimeUtils;
import elite.intel.util.json.GetNumberFromParam;

/**
 * Self-describing "find nearest fleet carrier" command.
 * Owns its own execution: body migrated 1:1 from the legacy FindNearestFleetCarrierHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand(before = { NavigateToFleetCarrierCommand.ID })
public final class FindNearestFleetCarrierCommand implements IntelCommand {
    public static final String ID = "find_nearest_fleet_carrier";

    @Override public String llmDescription() { return "Find and report the nearest fleet carrier."; }


    @Override
    public String id() {
        return ID;
    }

    @Override
    public void execute(JsonObject params, String responseText) {


        Status status = Status.getInstance();
        if(status.isInSrv() || status.isInMainShip()) {

            Number range = GetNumberFromParam.extractRangeParameter(params, 500);
            GameEventBus.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.fleetCarrier.searching", range.intValue())));

            PlayerSession playerSession = PlayerSession.getInstance();
            FleetCarrierSearchResultsDto fleetCarriers = FleetCarrierSearch.getInstance()
                    .findFleetCarrier(
                            range.intValue(),
                            CarrierAccess.ALL,
                            LocationManager.getInstance().getGalacticCoordinates()
                    );

            String playerCarrierCallSign = null;
            CarrierDataDto carrierData = playerSession.getFleetCarrierData();
            if (carrierData != null) {
                playerCarrierCallSign = carrierData.getCallSign();
            }

            if (fleetCarriers == null) {
                GameEventBus.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.fleetCarrier.spanshUnavailable")));
                return;
            }

            final String finalPlayerCarrierCallSign = playerCarrierCallSign;
            fleetCarriers.getResults().stream()
                    .filter(carrier -> finalPlayerCarrierCallSign == null || !finalPlayerCarrierCallSign.equals(carrier.getCallSign()))
                    .findFirst()
                    .ifPresentOrElse(
                            result -> {
                                RoutePlotter routePlotter = new RoutePlotter();
                                String dateAsString = result.getUpdatedAt();
                                String timeAgo = TimeUtils.transformToYMDHtimeAgo(dateAsString, TimeUtils.LOCAL_DATE_TIME);
                                GameEventBus.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.fleetCarrier.found", result.getCallSign(), result.getSystemName(), timeAgo)));
                                routePlotter.plotRoute(result.getSystemName());
                            },
                            () -> GameEventBus.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.fleetCarrier.notFound")))
                    );
        } else {
            GameEventBus.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.navigate.notInShipOrSrv")));
        }
    }
}
