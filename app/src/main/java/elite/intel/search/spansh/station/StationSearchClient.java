package elite.intel.search.spansh.station;

import elite.intel.search.spansh.client.SpanshClient;
import elite.intel.search.spansh.station.interstellarfactors.InterstellarFactorsResultDto;
import elite.intel.search.spansh.station.interstellarfactors.InterstellarFactorsSearchCriteria;
import elite.intel.search.spansh.station.marketstation.TradeStationSearchCriteria;
import elite.intel.search.spansh.station.marketstation.TradeStationSearchResultDto;
import elite.intel.search.spansh.station.traderandbroker.TraderAndBrokerSearchDto;
import elite.intel.search.spansh.station.vista.VistaGenomicsLocationDto;
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

    public TradeStationSearchResultDto searchTradeStation(TradeStationSearchCriteria initialStationCriteria) {
        return GsonFactory.getGson().fromJson(performSearch(initialStationCriteria), TradeStationSearchResultDto.class);
    }

    public InterstellarFactorsResultDto searchInterstellarFactors(InterstellarFactorsSearchCriteria criteria) {
        return GsonFactory.getGson().fromJson(performSearch(criteria.toJson()), InterstellarFactorsResultDto.class);
    }
}
