package elite.intel.ai.brain.actions.handlers.query;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.query.struct.AiDataStruct;
import elite.intel.ai.brain.actions.query.IntelQuery;
import elite.intel.ai.brain.actions.query.RegisterQuery;
import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.journal.events.dto.BioSampleDto;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.session.PlayerSession;
import elite.intel.util.yaml.ToYamlConvertable;
import elite.intel.util.yaml.YamlFactory;

import java.util.List;

@RegisterQuery
public class AnalyzeLastScanQueryCommand extends BaseQueryAnalyzer implements IntelQuery {
    public static final String ID = "query_last_scan";


    @Override public String id() { return ID; }


    private final LocationManager locationManager = LocationManager.getInstance();

    @Override public JsonObject handle(String action, JsonObject params, String originalUserInput) throws Exception {
        //GameEventBus.publish(new AiVoxResponseEvent("Analyzing scanner. Stand by."));
        PlayerSession playerSession = PlayerSession.getInstance();
        LocationDto lastScan = playerSession.getLastScan();

        LocationDto currentLocation = locationManager.findByLocationData(playerSession.getLocationData());
        List<BioSampleDto> partialScans = currentLocation.getPartialBioSamples();


        String instructions = """
                Answer the user's question about the last scanned body or current partial bio scans.
                
                Data fields:
                - lastScan: full data for the last scanned body. Key fields:
                  - planetShortName / starName: name of the body
                  - bodyType / starClass: type of body
                  - gravity, surfaceTemperature, atmosphere: surface conditions
                  - isLandable: whether the surface can be landed on
                  - isTerraformable: terraforming candidate
                  - bioSignals, geoSignals: number of biological and geological signals detected
                  - ourDiscovery, weMappedIt: whether we were first to discover or map
                - partialScans: list of bio samples currently in progress (genus, species, scanXof3, payout)
                
                Rules:
                - If asked about the last scanned body: use lastScan fields relevant to the question.
                - If asked about current bio scans in progress: use partialScans.
                - Answer only what was asked.
                """;
        return process(new AiDataStruct(instructions, new DataDto(lastScan, partialScans)), originalUserInput);
    }

    record DataDto(LocationDto lastScan, List<BioSampleDto> partialScans) implements ToYamlConvertable {
        @Override public String toYaml() {
            return YamlFactory.toYaml(this);
        }
    }
}
