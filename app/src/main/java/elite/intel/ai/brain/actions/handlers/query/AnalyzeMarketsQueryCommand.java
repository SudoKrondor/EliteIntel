package elite.intel.ai.brain.actions.handlers.query;
import elite.intel.ai.brain.actions.query.IntelQuery;
import elite.intel.ai.brain.actions.query.QueryIds;
import elite.intel.ai.brain.actions.query.RegisterQuery;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.query.struct.AiDataStruct;
import elite.intel.search.edsm.EdsmApiClient;
import elite.intel.search.edsm.dto.StationsDto;
import elite.intel.search.edsm.dto.data.Station;
import elite.intel.session.PlayerSession;
import elite.intel.util.StringUtls;
import elite.intel.util.yaml.ToYamlConvertable;
import elite.intel.util.yaml.YamlFactory;

import java.util.LinkedList;
import java.util.List;

@RegisterQuery
public class AnalyzeMarketsQueryCommand extends BaseQueryAnalyzer implements IntelQuery {

    @Override public String id() { return QueryIds.ANALYZE_MARKETS; }


    private final PlayerSession playerSession = PlayerSession.getInstance();

    @Override public JsonObject handle(String action, JsonObject params, String originalUserInput) throws Exception {
        //EventBusManager.publish(new AiVoxResponseEvent("Analyzing stations data. Stand by."));

        String starName = playerSession.getPrimaryStarName();
        StationsDto stationsDto = EdsmApiClient.searchStations(starName, 0);
        List<Station> stations = stationsDto.getData().getStations();
        if (stations == null || stations.isEmpty()) {
            return process(StringUtls.localizedLlm("query.noData"));
        }

        List<StationData> stationData = new LinkedList<>();
        stations.forEach(s -> stationData.add(
                new StationData(
                        s.getType(),
                        s.getName(),
                        s.getBody() == null ? null : s.getBody().getName(),
                        s.getDistanceToArrivalInLightSeconds(),
                        s.getAllegiance(),
                        s.getGovernment(),
                        s.getEconomy(),
                        s.isHaveMarket(),
                        s.isHaveShipyard(),
                        s.isHaveOutfitting(),
                        s.getControllingFaction() == null ? null : s.getControllingFaction().getName()
                )
        ));

        String instructions = """
                Answer the user's question about stations in this star system.
                Answer only what was asked.
                
                Data fields (per station):
                - stationType: station type (Coriolis, Outpost, Planetary Port, Fleet Carrier, etc.)
                - stationName: station name
                - orbitingAround: body the station orbits (if applicable)
                - distanceToArrivalInLightSeconds: distance from the main star in light seconds
                - allegiance: faction allegiance
                - government: government type
                - economy: primary economy type
                - haveMarket: whether the station has a commodity market
                - haveShipyard: whether the station has a shipyard
                - haveOutfitting: whether the station has outfitting services
                - controllingFaction: faction controlling this station

                Rules:
                - If asked broadly whether stations exist: state the count by type. Do not list names.
                - If asked which stations have a market, shipyard, or outfitting: state the count and name up to three examples.
                - If asked about a specific station by name: give its details.
                - Fleet Carriers are player-owned ships, not permanent stations. Mention them as a count only.
                - Answer only what was asked.
                """;

        return process(
                new AiDataStruct(instructions,
                        new DataDto(starName, stationData)
                ), originalUserInput
        );
    }

    record DataDto(String starSystemName, List<StationData> stations) implements ToYamlConvertable {
        @Override public String toYaml() {
            return YamlFactory.toYaml(this);
        }
    }

    record StationData(
            String stationType,
            String stationName,
            String orbitingAround,
            double distanceToArrivalInLightSeconds,
            String allegiance,
            String government,
            String economy,
            boolean haveMarket,
            boolean haveShipyard,
            boolean haveOutfitting,
            String controllingFaction

    ) implements ToYamlConvertable {
        @Override public String toYaml() {
            return YamlFactory.toYaml(this);
        }
    }
}
