package elite.intel.ai.brain.actions.handlers.query;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.query.struct.AiDataStruct;
import elite.intel.ai.brain.actions.query.IntelQuery;
import elite.intel.ai.brain.actions.query.RegisterQuery;
import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.journal.events.dto.BioSampleDto;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.session.PlayerSession;
import elite.intel.session.Status;
import elite.intel.util.NavigationUtils;
import elite.intel.util.StringUtls;
import elite.intel.util.yaml.ToYamlConvertable;
import elite.intel.util.yaml.YamlFactory;

@RegisterQuery
public class AnalyzeDistanceFromLastBioSampleQueryCommand extends BaseQueryAnalyzer implements IntelQuery {
    public static final String ID = "query_distance_to_bio_sample";


    @Override public String id() { return ID; }


    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();

    @Override public JsonObject handle(String action, JsonObject params, String originalUserInput) throws Exception {
        //GameEventBus.publish(new AiVoxResponseEvent("Analyzing exobiology collection data. Stand by."));

        Status status = Status.getInstance();
        LocationDto currentLocation = locationManager.findByLocationData(playerSession.getLocationData());

        String instructions = """
                Report the distance to the last partial bio sample.
                
                Data fields:
                - distanceInMeters: pre-computed distance to the last partial scan location
                - genusName: genus of the bio sample
                
                State the distance in meters and name the genus.
                """;

        if (status.getStatus() == null) {
            return process(StringUtls.localizedLlm("query.bio.noPlanetData"));
        }

        double latitude = status.getStatus().getLatitude();
        double longitude = status.getStatus().getLongitude();
        double planetRadius = status.getStatus().getPlanetRadius();

        if (latitude == 0 || longitude == 0 || planetRadius == 0) {
            return process(StringUtls.localizedLlm("query.noPosition"));
        }

        if (currentLocation.getPartialBioSamples().isEmpty()) {
            return process(StringUtls.localizedLlm("query.bio.noPartialScans"));
        }

        BioSampleDto bioSample = currentLocation.getPartialBioSamples().getLast();
        double distance = NavigationUtils.calculateSurfaceDistance(
                bioSample.getScanLatitude(),
                bioSample.getScanLongitude(),
                status.getStatus().getLatitude(),
                status.getStatus().getLongitude(),
                planetRadius,
                status.getStatus().getAltitude()
        );


        return process(new AiDataStruct(instructions, new DataDto((int) distance, bioSample.getGenus())), originalUserInput);
    }

    record DataDto(int distanceInMeters, String genusName) implements ToYamlConvertable {
        @Override public String toYaml() {
            return YamlFactory.toYaml(this);
        }
    }
}
