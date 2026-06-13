package elite.intel.ai.brain.actions.handlers.commands;

import com.google.gson.JsonObject;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.EventBusManager;
import elite.intel.gameapi.journal.events.dto.CarrierDataDto;
import elite.intel.search.spansh.findcarrier.CarrierAccess;
import elite.intel.search.spansh.findcarrier.FleetCarrierSearch;
import elite.intel.search.spansh.findcarrier.FleetCarrierSearchResultsDto;
import elite.intel.session.PlayerSession;
import elite.intel.session.Status;
import elite.intel.util.StringUtls;
import elite.intel.util.TimeUtils;
import elite.intel.util.json.GetNumberFromParam;

public class FindNearestFleetCarrierHandler implements CommandHandler {



    @Override public void handle(String action, JsonObject params, String responseText) {


        Status status = Status.getInstance();
        if(status.isInSrv() || status.isInMainShip()) {

            Number range = GetNumberFromParam.extractRangeParameter(params, 500);
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.fleetCarrier.searching", range.intValue())));

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
                EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.fleetCarrier.spanshUnavailable")));
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
                                EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.fleetCarrier.found", result.getCallSign(), result.getSystemName(), timeAgo)));
                                routePlotter.plotRoute(result.getSystemName());
                            },
                            () -> EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.fleetCarrier.notFound")))
                    );
        } else {
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.navigate.notInShipOrSrv")));
        }
    }
}
