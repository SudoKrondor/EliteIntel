package elite.intel.gameapi.journal.subscribers;

import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.search.edsm.EdsmApiClient;
import elite.intel.search.edsm.dto.MarketDto;
import elite.intel.search.edsm.dto.OutfittingDto;
import elite.intel.search.edsm.dto.ShipyardDto;
import elite.intel.session.PlayerSession;

import static elite.intel.util.StringUtls.localizedEvent;

public class LocalServicesData {

    public static String setLocalServicesData(long marketId) {
        final StringBuilder sb = new StringBuilder();
        final MarketDto marketDto = EdsmApiClient.searchMarket(marketId, null, null, 0);
        final OutfittingDto outfittingDto = EdsmApiClient.searchOutfitting(marketId, null, null);
        final ShipyardDto shipyardDto = EdsmApiClient.searchShipyard(marketId, null, null);
        final PlayerSession playerSession = PlayerSession.getInstance();
        final LocationManager locationManager = LocationManager.getInstance();

        LocationDto currentLocation = locationManager.findByLocationData(playerSession.getLocationData());
        if (marketDto.getData() != null && marketDto.getData().getCommodities() != null) {
            sb.append(" ").append(localizedEvent("event.docked.marketLabel")).append(" ");
            sb.append(marketDto.getData().getName());
            sb.append(", ");
            currentLocation.setMarket(marketDto);
        }

        if (outfittingDto.getData() != null && outfittingDto.getData().getOutfitting() != null) {
            sb.append(" ").append(localizedEvent("event.docked.outfitting")).append(", ");
            currentLocation.setOutfitting(outfittingDto);
        }

        if (shipyardDto.getData() != null && shipyardDto.getData().getShips() != null) {
            sb.append(" ").append(localizedEvent("event.docked.shipyard")).append(", ");
            currentLocation.setShipyard(shipyardDto);
        }
        locationManager.save(currentLocation);
        return sb.toString().trim();
    }
}
