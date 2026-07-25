package elite.intel.ai.brain.actions.handlers.queries;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.queries.struct.AiDataStruct;
import elite.intel.gameapi.search.edsm.EdsmApiClient;
import elite.intel.gameapi.search.edsm.dto.StationsDto;
import elite.intel.gameapi.search.edsm.dto.data.Station;
import elite.intel.session.PlayerSession;
import elite.intel.util.StringUtls;
import elite.intel.util.yaml.ToYamlConvertable;
import elite.intel.util.yaml.YamlFactory;

import java.util.LinkedList;
import java.util.List;

@RegisterQuery
public class AnalyzeMarketsQuery extends BaseQueryAnalyzer implements IntelQuery {
    public static final String ID = "query_markets";

    /**
     * EDSM station type for a player fleet carrier.
     */
    private static final String FLEET_CARRIER_TYPE = "Fleet Carrier";

    @Override
    public String llmDescription() {
        return "List the stations, outposts and settlements in the current star system and which facilities each has (market, shipyard, outfitting), from external data. Does not report commodity prices.";
    }


    @Override public String id() { return ID; }


    private final PlayerSession playerSession = PlayerSession.getInstance();

    @Override public JsonObject handle(String action, JsonObject params, String originalUserInput) throws Exception {
        //GameEventBus.publish(new AiVoxResponseEvent("Analyzing stations data. Stand by."));

        String starName = playerSession.getPrimaryStarName();
        StationsDto stationsDto = EdsmApiClient.searchStations(starName, 0);
        List<Station> stations = stationsDto.getData().getStations();
        if (stations == null || stations.isEmpty()) {
            return process(StringUtls.localizedResponse("query.noData"));
        }

        List<StationData> stationData = new LinkedList<>();
        int fleetCarrierCount = 0;
        for (Station s : stations) {
            // A busy system can hold hundreds of fleet carriers; listing them blows the LLM's token budget for no
            // gain (they are transient player ships). Only their count survives; query_carriers reports details.
            if (FLEET_CARRIER_TYPE.equalsIgnoreCase(s.getType())) {
                fleetCarrierCount++;
                continue;
            }
            stationData.add(
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
            );
        }

        String instructions = """
                Answer the user's question about stations in this star system.
                Answer only what was asked.
                
                Top-level fields:
                - starSystemName: the star system these stations are in
                - fleetCarrierCount: how many fleet carriers are currently parked in this system
                - stations: the permanent stations (fleet carriers are not listed individually)

                Data fields (per station):
                - stationType: station type (Coriolis, Outpost, Planetary Port, etc.)
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
                - Fleet Carriers are player-owned ships, not permanent stations. Report fleetCarrierCount only; their
                  names and services are not available here.
                - Answer only what was asked.
                """;

        return process(
                new AiDataStruct(instructions,
                        new DataDto(starName, fleetCarrierCount, stationData)
                ), originalUserInput
        );
    }

    record DataDto(String starSystemName, int fleetCarrierCount,
                   List<StationData> stations) implements ToYamlConvertable {
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
