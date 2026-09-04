package elite.intel.gameapi.journal.subscribers;

import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.search.edsm.EdsmApiClient;
import elite.intel.gameapi.search.edsm.dto.MarketDto;
import elite.intel.gameapi.search.edsm.dto.OutfittingDto;
import elite.intel.gameapi.search.edsm.dto.ShipyardDto;

import static elite.intel.util.StringUtls.localizedEvent;

/**
 * What EDSM knows about one station's market, outfitting and shipyard, filed on that station's record.
 * <p>
 * It used to be written onto the current location - the body the ship dropped at - which is where a
 * settlement's market ended up sitting on a moon. The MarketID names the station these three lookups were
 * made for, so that is what the data is stored against; ask for it with
 * {@link LocationManager#findCurrentStation()}.
 */
public class LocalServicesData {

    /**
     * Fetches the three EDSM service listings for a station and records them against it, returning the
     * short list of what was found for the narration payload.
     */
    public static String forStation(long systemAddress, String starSystem, String recordKey, long marketId) {
        final StringBuilder sb = new StringBuilder();
        final MarketDto marketDto = EdsmApiClient.searchMarket(marketId, null, null, 0);
        final OutfittingDto outfittingDto = EdsmApiClient.searchOutfitting(marketId, null, null);
        final ShipyardDto shipyardDto = EdsmApiClient.searchShipyard(marketId, null, null);

        boolean hasMarket = marketDto.getData() != null && marketDto.getData().getCommodities() != null;
        boolean hasOutfitting = outfittingDto.getData() != null && outfittingDto.getData().getOutfitting() != null;
        boolean hasShipyard = shipyardDto.getData() != null && shipyardDto.getData().getShips() != null;

        if (hasMarket) {
            sb.append(" ").append(localizedEvent("event.docked.marketLabel")).append(" ");
            sb.append(marketDto.getData().getName());
            sb.append(", ");
        }
        if (hasOutfitting) sb.append(" ").append(localizedEvent("event.docked.outfitting")).append(", ");
        if (hasShipyard) sb.append(" ").append(localizedEvent("event.docked.shipyard")).append(", ");

        if (hasMarket || hasOutfitting || hasShipyard) {
            LocationManager.getInstance().updateNamedBody(systemAddress, marketId, recordKey, station -> {
                station.setStarName(starSystem);
                if (hasMarket) station.setMarket(marketDto);
                if (hasOutfitting) station.setOutfitting(outfittingDto);
                if (hasShipyard) station.setShipyard(shipyardDto);
            });
        }
        return sb.toString().trim();
    }
}
