package elite.intel.ai.brain.actions.handlers.query;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.query.struct.AiDataStruct;
import elite.intel.ai.brain.actions.query.IntelQuery;
import elite.intel.ai.brain.actions.query.RegisterQuery;
import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.journal.events.dto.BioSampleDto;
import elite.intel.gameapi.journal.events.dto.GenusDto;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.session.PlayerSession;
import elite.intel.util.ExoBio;
import elite.intel.util.StringUtls;
import elite.intel.util.yaml.ToYamlConvertable;
import elite.intel.util.yaml.YamlFactory;

import java.util.List;

import static elite.intel.util.ExoBio.calculateGenusNotYetScanned;
import static elite.intel.util.ExoBio.completedScansForPlanet;

@RegisterQuery
public class AnalyzeBioSamplesPlanetSurfaceQueryCommand extends BaseQueryAnalyzer implements IntelQuery {
    public static final String ID = "query_exobiology_samples";

    @Override public String llmDescription() { return "Report the biological samples scanned on the current planet surface."; }


    @Override public String id() { return ID; }


    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();

    @Override public JsonObject handle(String action, JsonObject params, String originalUserInput) throws Exception {
        //GameEventBus.publish(new AiVoxResponseEvent("Analyzing exobiology data. Stand by."));


        LocationDto currentLocation = locationManager.findByLocationData(playerSession.getLocationData());
        if (currentLocation.getBodyId() < 0) {
            return process(StringUtls.localizedLlm("query.bio.noLocation"));
        }
        List<BioSampleDto> partialScans = currentLocation.getPartialBioSamples();
        List<GenusDto> genusListForCurrentLocation = currentLocation.getGenus();
        List<ExoBio.DataDto> completedScansForThisLocation = completedScansForPlanet(playerSession.getBioCompletedSamples(), currentLocation.getPlanetName());
        List<GenusDto> genusListNotScannedForCurrentLocation = calculateGenusNotYetScanned(completedScansForThisLocation, genusListForCurrentLocation);

        String instructions = """
                Answer the user's question about exobiology scanning on this planet.
                
                Data fields:
                - remainingGenus: genus that still need scanning (pre-computed, authoritative)
                - currentlyScanning: genus with partial scans in progress (1 or 2 of 3 samples taken)
                - completedCount: number of genus fully completed (all 3 samples taken)
                
                Rules:
                - If asked what is left to scan: list names from all remainingGenus. If empty, say all known organics are completed.
                - If asked what we are currently scanning: list names from currentlyScanning.
                - Always return genus names, not just how many
                """;

        AiDataStruct struct = new AiDataStruct(instructions, new DataDto(
                genusListNotScannedForCurrentLocation,
                partialScans,
                completedScansForThisLocation.size()
        ));

        return process(struct, originalUserInput);
    }


    record DataDto(
            List<GenusDto> remainingGenus,
            List<BioSampleDto> currentlyScanning,
            int completedCount
    ) implements ToYamlConvertable {
        @Override public String toYaml() {
            return YamlFactory.toYaml(this);
        }
    }
}
