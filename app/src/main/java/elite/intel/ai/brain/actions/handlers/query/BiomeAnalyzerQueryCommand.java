package elite.intel.ai.brain.actions.handlers.query;
import elite.intel.ai.brain.actions.query.IntelQuery;
import elite.intel.ai.brain.actions.query.QueryIds;
import elite.intel.ai.brain.actions.query.RegisterQuery;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import elite.intel.ai.brain.commons.BiomeAnalyzer;
import elite.intel.ai.brain.commons.BiomeAnalyzer.LocationData;
import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.journal.events.dto.FssSignalDto;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.session.PlayerSession;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@RegisterQuery
public class BiomeAnalyzerQueryCommand extends BaseQueryAnalyzer implements IntelQuery {

    @Override public String id() { return QueryIds.PLANET_BIOME_ANALYSIS; }


    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final BiomeAnalyzer biomeAnalyzer = BiomeAnalyzer.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();

    @Override public JsonObject handle(String action, JsonObject params, String originalUserInput) throws Exception {
        //EventBusManager.publish(new AiVoxResponseEvent("Analyzing planetary and biome data. Stand by."));

        JsonElement key = params.get("key");
        String planetName = key == null ? null : key.getAsString().replace(" ", "");
        Collection<LocationDto> allStellarObjectsInStarSystem = locationManager.findAllBySystemAddress(
                playerSession.getLocationData().getSystemAddress()
        );

        boolean runSystemVide;
        LocationDto firstMatchingLocation = findFirstMatchingLocation(allStellarObjectsInStarSystem, planetName);
        runSystemVide = firstMatchingLocation == null;
        if (runSystemVide) {
            return biomeAnalyzer.analyzeBiome(originalUserInput, findPlanetsWithBioSignals(allStellarObjectsInStarSystem));
        } else {
            return biomeAnalyzer.analyzeBiome(
                    originalUserInput,
                    new LocationData(
                            firstMatchingLocation.getPlanetShortName(),
                            firstMatchingLocation.getBioSignals(),
                            firstMatchingLocation.getPlanetClass(),
                            String.valueOf(firstMatchingLocation.getDistance()),
                            firstMatchingLocation.getVolcanism(),
                            firstMatchingLocation.getAtmosphere(),
                            String.valueOf(firstMatchingLocation.getSurfaceTemperature()
                            ) // Keep Kelvin
                    )
            );
        }
    }

    public LocationDto findFirstMatchingLocation(Collection<LocationDto> locations, String planetName) {
        if (planetName == null || planetName.trim().isEmpty()) {
            return null;
        }

        for (LocationDto locationDto : locations) {
            String lowerPlanetName = locationDto.getPlanetShortName().toLowerCase().replace("planet", "").replace(" ", "");
            if (lowerPlanetName.contains(planetName.replace(" ", "")) && !planetName.isEmpty()) {
                return locationDto;
            }
        }

        return null;
    }

    public LocationData[] findPlanetsWithBioSignals(Collection<LocationDto> locations) {
        List<LocationData> result = new ArrayList<>();
        for (LocationDto v : locations) {
            Set<FssSignalDto> detectedSignals = v.getDetectedSignals();
            int bioSignalCounter = 0;
            for (FssSignalDto signal : detectedSignals) {
                if ("Biological".equalsIgnoreCase(signal.getSignalType())) {
                    bioSignalCounter++;
                }
            }
            if (bioSignalCounter > 0 || v.getBioSignals() > 0) {
                result.add(
                        new LocationData(
                                v.getPlanetShortName(),
                                bioSignalCounter,
                                v.getPlanetClass(),
                                String.valueOf(v.getDistance()),
                                v.getVolcanism(),
                                v.getAtmosphere(),
                                String.valueOf(v.getSurfaceTemperature())
                        )
                );
            }
        }
        return result.toArray(LocationData[]::new);
    }
}
