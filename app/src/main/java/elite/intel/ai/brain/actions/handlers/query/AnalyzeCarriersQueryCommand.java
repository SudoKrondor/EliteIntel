package elite.intel.ai.brain.actions.handlers.query;
import elite.intel.ai.brain.actions.query.IntelQuery;
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
public class AnalyzeCarriersQueryCommand extends BaseQueryAnalyzer implements IntelQuery {
    public static final String ID = "query_carriers";

    @Override public String llmDescription() { return "Report known fleet carriers and their details."; }


    @Override public String id() { return ID; }


    private final PlayerSession playerSession = PlayerSession.getInstance();

    @Override public JsonObject handle(String action, JsonObject params, String originalUserInput) throws Exception {
        String primaryStarName = playerSession.getPrimaryStarName();
        StationsDto stationsDto = EdsmApiClient.searchStations(primaryStarName, 0);
        ArrayList<StationData> stationsData = new ArrayList<>();
        StationsData data = stationsDto.getData();
        for (Station station : data.getStations()) {
            if ("Fleet Carrier".equalsIgnoreCase(station.getType())) {
                stationsData.add(new StationData(
                        station.getName(),
                        station.getCommodities() == null ? null : station.getCommodities().size(),
                        station.getAvailableServices() == null ? null : station.getAvailableServices().size()
                ));
            }
        }


        int totalCarriers = stationsData.size();
        long withCommodities = stationsData.stream().filter(s -> s.numberOfCommodities() != null && s.numberOfCommodities() > 0).count();
        long withServices = stationsData.stream().filter(s -> s.numberOfServices() != null && s.numberOfServices() > 0).count();

        String instructions = """
                Report fleet carriers present in this star system.
                
                Data fields:
                - carriers: list of fleet carriers (name, numberOfCommodities, numberOfServices)
                - totalCarriers: total number of fleet carriers present
                - carriersWithCommodities: number of carriers that have commodities listed
                - carriersWithServices: number of carriers that have services listed
                
                Rules:
                - Use the pre-computed counts directly. Do not recount from the list.
                - Answer only what the user asked.
                """;

        return process(new AiDataStruct(instructions, new DataDto(stationsData, totalCarriers, (int) withCommodities, (int) withServices)), originalUserInput);
    }

    record StationData(String name, Integer numberOfCommodities, Integer numberOfServices) implements ToYamlConvertable {
        @Override public String toYaml() {
            return YamlFactory.toYaml(this);
        }
    }


    record DataDto(List<StationData> carriers, int totalCarriers, int carriersWithCommodities,
                   int carriersWithServices) implements ToYamlConvertable {
        @Override public String toYaml() {
            return YamlFactory.toYaml(this);
        }
    }
}

