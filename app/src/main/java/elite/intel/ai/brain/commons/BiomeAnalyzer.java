package elite.intel.ai.brain.commons;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.queries.BaseQueryAnalyzer;
import elite.intel.ai.brain.actions.handlers.queries.struct.AiDataStruct;
import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.data.BioForms;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.session.PlayerSession;
import elite.intel.util.yaml.ToYamlConvertable;
import elite.intel.util.yaml.YamlFactory;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class BiomeAnalyzer extends BaseQueryAnalyzer {

    private static BiomeAnalyzer instance;
    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();

    private BiomeAnalyzer() {
    }

    public static BiomeAnalyzer getInstance() {
        if (instance == null) {
            instance = new BiomeAnalyzer();
        }
        return instance;
    }

    public JsonObject analyzeBiome(@Nullable String originalUserInput, LocationData... locations) {
        if (locations == null) {
            JsonObject object = new JsonObject();
            object.addProperty("data", "No locations provided");
            return object;
        }


        String instructions = """
                Classify likely biology for each planet using the genusToBiome map.
                
                Data fields:
                - locations: list of planets to classify (planetName, planetClass, atmosphere, temperature in Kelvin, vulcanism, numBioSignals)
                - genusToBiome: map of genus name to the biome conditions it requires
                - starSystemCharacteristics: star types and planet types present in this system

                Rules:
                - If numBioSignals is zero: skip the planet entirely.
                - If numBioSignals is greater than zero: Bacterium is near-certain - always include it as confirmed.
                - For additional genus beyond Bacterium: strictly match planetClass, atmosphere, temperature, and vulcanism against genusToBiome. Only include a genus if conditions are a strong match. Do NOT use lenient or partial matching.
                - Additional genus are crowd-sourced statistical candidates only - they may or may not be present. Do not pad the list to reach numBioSignals; list only what genuinely fits the conditions.
                - Distinguish clearly between what is near-certain and what is speculative.
                - Output only planet names and genus. No explanations.
                
                Output format:
                Planet X: Confirmed: Bacterium. Possibly also: Genus2, Genus3.
                Planet Y: Confirmed: Bacterium. No other strong candidates.
                """;

        List<LocationData> list = List.of(locations);
        Map<Long, LocationDto> byPrimaryStar = locationManager.findByPrimaryStar(playerSession.getPrimaryStarName());
        return process(new AiDataStruct(instructions, new DataDto2(BioForms.getGenusToBiome(), list, starSystemCharacteristics(byPrimaryStar.values()))), originalUserInput);
    }

    private String starSystemCharacteristics(Collection<LocationDto> starSystem) {
        StringBuilder sb = new StringBuilder();
        sb.append("System:");
        for (LocationDto stellarObject : starSystem) {
            if (stellarObject.getStarClass() == null && stellarObject.getPlanetClass() != null) {
                sb.append(" has planet type:'").append(stellarObject.getPlanetClass()).append("' distance:").append(stellarObject.getDistance()).append(", ");
            } else if (stellarObject.getStarClass() != null) {
                sb.append("Star type").append(stellarObject.getBodyType()).append(" present, ");
            }
        }
        sb.append(" .");

        return sb.toString();
    }

    public record LocationData(String planetName, int numBioSignals, String planetClass, String distance, String vulcanism, String atmosphere, String temperature) implements ToYamlConvertable {
        @Override public String toYaml() {
            return YamlFactory.toYaml(this);
        }
    }

    record DataDto2(Map<String, String> genusToBiome, Collection<LocationData> locations, String starSystemCharacteristics) implements ToYamlConvertable {
        @Override public String toYaml() {
            return YamlFactory.toYaml(this);
        }
    }

}
