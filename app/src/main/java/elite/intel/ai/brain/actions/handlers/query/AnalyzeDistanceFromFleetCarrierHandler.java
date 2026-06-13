package elite.intel.ai.brain.actions.handlers.query;

import com.google.gson.JsonObject;
import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.journal.events.dto.CarrierDataDto;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.gameapi.journal.events.dto.shiploadout.ShipLoadOutDto;
import elite.intel.session.PlayerSession;
import elite.intel.util.NavigationUtils;
import elite.intel.util.StringUtls;

public class AnalyzeDistanceFromFleetCarrierHandler extends BaseQueryAnalyzer implements QueryHandler {

    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();

    @Override public JsonObject handle(String action, JsonObject params, String originalUserInput) throws Exception {
        //EventBusManager.publish(new AiVoxResponseEvent("Analyzing travel telemetry. Stand by."));

        CarrierDataDto carrierData = playerSession.getFleetCarrierData();
        if (carrierData == null) return process(StringUtls.localizedLlm("query.noData"));
        double carrierLocationX = carrierData.getX();
        double carrierLocationY = carrierData.getY();
        double carrierDataZ = carrierData.getZ();

        if (carrierLocationX == 0 && carrierLocationY == 0 && carrierDataZ == 0) {
            return process(StringUtls.localizedLlm("query.carrier.noCoords"));
        }

        double x = 0, y = 0, z = 0;
        LocationDto playerLocation = locationManager.findPrimaryStar(playerSession.getPrimaryStarName());
        x = playerLocation.getX();
        y = playerLocation.getY();
        z = playerLocation.getZ();
        boolean arePlayerLocationCoordinatesAvailable = x == 0 && y == 0 && z == 0;
        if (arePlayerLocationCoordinatesAvailable) {
            return process(StringUtls.localizedLlm("query.noPlayerCoords"));
        }

        ShipLoadOutDto shipLoadout = playerSession.getShipLoadout();
        double jumpRange = shipLoadout == null ? -1 : shipLoadout.getMaxJumpRange();
        double distance = NavigationUtils.calculateGalacticDistance(x, y, z, carrierLocationX, carrierLocationY, carrierDataZ);

        int numberOfJumps = (int) (distance / jumpRange) + 1;
        return process(StringUtls.localizedLlm("query.carrier.distance", (int) distance, numberOfJumps));
    }
}