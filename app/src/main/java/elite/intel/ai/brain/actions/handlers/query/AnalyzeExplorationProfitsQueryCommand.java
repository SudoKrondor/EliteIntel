package elite.intel.ai.brain.actions.handlers.query;
import elite.intel.ai.brain.actions.query.IntelQuery;
import elite.intel.ai.brain.actions.query.QueryIds;
import elite.intel.ai.brain.actions.query.RegisterQuery;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.query.struct.AiDataStruct;
import elite.intel.db.dao.CodexEntryDao;
import elite.intel.db.managers.CodexEntryManager;
import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.journal.events.dto.BioSampleDto;
import elite.intel.gameapi.journal.events.dto.GenusDto;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.session.PlayerSession;
import elite.intel.util.yaml.ToYamlConvertable;
import elite.intel.util.yaml.YamlFactory;

import java.util.Collection;
import java.util.List;

@RegisterQuery
public class AnalyzeExplorationProfitsQueryCommand extends BaseQueryAnalyzer implements IntelQuery {

    @Override public String id() { return QueryIds.EXPLORATION_PROFITS; }


    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();
    private final CodexEntryManager codexEntryManager = CodexEntryManager.getInstance();

    @Override public JsonObject handle(String action, JsonObject params, String originalUserInput) throws Exception {
        //EventBusManager.publish(new AiVoxResponseEvent("Analyzing exploration data. Stand by."));

        String instructions = """
                Report exobiology exploration profits for this session.
                
                Data fields:
                - potentialProfit: total credits available if all known genus in the current system are fully scanned
                - acquiredProfit: total credits already earned from completed bio samples and codex entries this session
                
                State both values in credits. Combine both values into a single response.
                """;
        return process(
                new AiDataStruct(
                        instructions,
                        new DataDto(calculatePotentialProfit(), calculateActualProfit())),
                originalUserInput
        );
    }

    private long calculateActualProfit() {
        List<BioSampleDto> allCompletedBioSamples = playerSession.getBioCompletedSamples();
        long result = 0;
        for (BioSampleDto dto : allCompletedBioSamples) {
            result = result + dto.getPayout();
        }
        List<CodexEntryDao.CodexEntry> allCodexEntries = codexEntryManager.findAll();
        for (CodexEntryDao.CodexEntry entry : allCodexEntries) {
            result = result + entry.getVoucherAmount();
        }
        return result;
    }

    private long calculatePotentialProfit() {
        Collection<LocationDto> stellarObjects = locationManager.findAllBySystemAddress(playerSession.getLocationData().getSystemAddress());
        long result = 0;
        for (LocationDto dto : stellarObjects) {
            List<GenusDto> genus = dto.getGenus();
            for (GenusDto g : genus) {
                result = result + g.getRewardInCredits() + g.getBonusCreditsForFirstDiscovery();
            }
        }
        return result;
    }

    record DataDto(long potentialProfit, long acquiredProfit) implements ToYamlConvertable {
        @Override public String toYaml() {
            return YamlFactory.toYaml(this);
        }
    }
}
