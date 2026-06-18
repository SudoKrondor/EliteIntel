package elite.intel.ai.brain.actions.handlers.query;
import elite.intel.ai.brain.actions.query.IntelQuery;
import elite.intel.ai.brain.actions.query.QueryIds;
import elite.intel.ai.brain.actions.query.RegisterQuery;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.query.struct.AiDataStruct;
import elite.intel.search.edsm.EdsmApiClient;
import elite.intel.search.edsm.dto.StationsDto;
import elite.intel.search.edsm.dto.data.Station;
import elite.intel.search.edsm.dto.data.StationsData;
import elite.intel.session.PlayerSession;
import elite.intel.util.yaml.ToYamlConvertable;
import elite.intel.util.yaml.YamlFactory;

import java.util.ArrayList;
import java.util.List;

@RegisterQuery
public class AnalyzeStationsQueryCommand extends BaseQueryAnalyzer implements IntelQuery {

    @Override public String id() { return QueryIds.QUERY_STATIONS; }


    private final PlayerSession playerSession = PlayerSession.getInstance();

    @Override public JsonObject handle(String action, JsonObject params, String originalUserInput) throws Exception {
        String primaryStarName = playerSession.getPrimaryStarName();
        StationsDto stationsDto = EdsmApiClient.searchStations(primaryStarName, 0);
        ArrayList<StationData> stationsData = new ArrayList<>();
        StationsData data = stationsDto.getData();
        if (data != null && data.getStations() != null && !data.getStations().isEmpty()) {
            for (Station station : data.getStations()) {
                /// skip all fleet carriers. (query carriers in another handler to save on tokens)
                if ("Fleet Carrier".equalsIgnoreCase(station.getType())) continue;
                stationsData.add(new StationData(
                        station.getName(),
                        station.getType(),
                        station.getAllegiance(),
                        station.getGovernment(),
                        station.getCommodities() == null ? null : station.getCommodities().size(),
                        station.getEconomy(),
                        station.getAvailableServices() == null ? null : station.getAvailableServices().size()
                ));
            }
        }


        String instructions = """
                Answer the user's question about stations in this star system. Fleet carriers are excluded.
                
                Data fields (per station):
                - name: station name
                - type: station type (Coriolis, Outpost, Planetary Port, etc.)
                - allegiance: faction allegiance
                - government: government type
                - economy: primary economy type
                - numberOfCommodities: count of commodities listed at this station
                - numberOfServices: count of services available at this station
                
                Rules:
                - Answer only what the user asked.
                - If asked about a specific station: match by name.
                - If no station data is available, say so.
                """;

        return process(new AiDataStruct(instructions, new DataDto(stationsData)), originalUserInput);
    }

    record StationData(String name, String type, String allegiance, String government, Integer numberOfCommodities, String economy, Integer numberOfServices) implements ToYamlConvertable {
        @Override public String toYaml() {
            return YamlFactory.toYaml(this);
        }
    }


    record DataDto(List<StationData> stations) implements ToYamlConvertable {
        @Override public String toYaml() {
            return YamlFactory.toYaml(this);
        }
    }
}
