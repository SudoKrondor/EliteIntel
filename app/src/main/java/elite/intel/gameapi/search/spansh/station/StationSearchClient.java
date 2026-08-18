package elite.intel.gameapi.search.spansh.station;

import elite.intel.gameapi.search.spansh.client.SpanshClient;
import elite.intel.gameapi.search.spansh.station.interstellarfactors.InterstellarFactorsResultDto;
import elite.intel.gameapi.search.spansh.station.interstellarfactors.InterstellarFactorsSearchCriteria;
import elite.intel.gameapi.search.spansh.station.marketstation.TradeStationSearchCriteria;
import elite.intel.gameapi.search.spansh.station.marketstation.TradeStationSearchResultDto;
import elite.intel.gameapi.search.spansh.station.traderandbroker.TraderAndBrokerSearchDto;
import elite.intel.gameapi.search.spansh.station.vista.VistaGenomicsLocationDto;
import elite.intel.util.json.GsonFactory;
import elite.intel.util.json.ToJsonConvertible;

public class StationSearchClient extends SpanshClient {

    private static StationSearchClient instance;

    private StationSearchClient() {
        super("https://spansh.co.uk/api/stations/search/save", "https://spansh.co.uk/api/stations/search/recall/");
    }

    public static synchronized StationSearchClient getInstance() {
        if (instance == null) {
            instance = new StationSearchClient();
        }
        return instance;
    }

    public TraderAndBrokerSearchDto searchTradersOrBrokers(ToJsonConvertible searchCriteria) {
        return GsonFactory.getGson().fromJson(performSearch(searchCriteria.toJson()), TraderAndBrokerSearchDto.class);
    }

    public VistaGenomicsLocationDto searchVistaGenomics(ToJsonConvertible searchCriteria) {
        return GsonFactory.getGson().fromJson(performSearch(searchCriteria.toJson()), VistaGenomicsLocationDto.class);
    }

    /**
     * Runs a stations search and hands back the page Spansh answered with, or null when it answered with
     * nothing. Serves every caller that asks the stations endpoint about markets - the station a trade
     * route starts from and the markets selling a given commodity are the same question with different
     * filters.
     */
    public TradeStationSearchResultDto searchStations(TradeStationSearchCriteria criteria) {
        return GsonFactory.getGson().fromJson(performSearch(criteria), TradeStationSearchResultDto.class);
    }

    public InterstellarFactorsResultDto searchInterstellarFactors(InterstellarFactorsSearchCriteria criteria) {
        return GsonFactory.getGson().fromJson(performSearch(criteria.toJson()), InterstellarFactorsResultDto.class);
    }
}
