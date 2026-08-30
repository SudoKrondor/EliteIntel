package elite.intel.ai.brain.actions.handlers.queries;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.queries.struct.AiDataStruct;
import elite.intel.ai.brain.vega.SpokenAmounts;
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
public class AnalyzeExplorationProfitsQuery extends BaseQueryAnalyzer implements IntelQuery {
    public static final String ID = "query_exploration_profits";

    @Override
    public String llmDescription() {
        return "Report exobiology (organic-scan) earnings: credits already earned this session versus the total potential if every known species in the current system were fully scanned.";
    }


    @Override public String id() { return ID; }


    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();
    private final CodexEntryManager codexEntryManager = CodexEntryManager.getInstance();

    @Override public JsonObject handle(String action, JsonObject params, String originalUserInput) throws Exception {
        //GameEventBus.publish(new AiVoxResponseEvent("Analyzing exploration data. Stand by."));

        String instructions = """
                Report exobiology exploration profits for this session.
                
                Data fields:
                - potentialProfit: total credits available if all known genus in the current system are fully scanned
                - acquiredProfit: total credits already earned from completed bio samples and codex entries this session
                - unconfirmedFirstDiscoveryBonus: extra credits possible on bodies someone else charted, IF nobody has
                  sampled their organics before us. Unknowable, so it is NOT part of potentialProfit.
                
                State potentialProfit and acquiredProfit in credits, combined into a single response.
                Mention unconfirmedFirstDiscoveryBonus only when it is above zero, and only as a possible
                extra that depends on nobody having sampled those bodies first. Never add it to a total.
                """ + SpokenAmounts.RULE;
        return process(
                new AiDataStruct(
                        instructions,
                        buildData()),
                originalUserInput
        );
    }

    private DataDto buildData() {
        Projection projection = calculatePotentialProfit();
        return new DataDto(projection.certain(), calculateActualProfit(), projection.unconfirmedBonus());
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

    /**
     * What the unsampled organics in this system are worth, split by how sure we are of it.
     *
     * <p>The first-discovery bonus is Vista Genomics paying for the first log of an organism, and the
     * journal never says whether anyone has logged it here. Charting the body ourselves settles it -
     * nobody can have sampled a body nobody had found - so that bonus is counted. On a body someone
     * else charted the bonus is a coin toss, and it used to be added to the projection anyway, which
     * quoted the commander a figure they had no reason to expect. It is now reported separately, as
     * the maybe it is.
     */
    private Projection calculatePotentialProfit() {
        Collection<LocationDto> stellarObjects = locationManager.findAllBySystemAddress(playerSession.getLocationData().getSystemAddress());
        long certain = 0;
        long unconfirmedBonus = 0;
        for (LocationDto dto : stellarObjects) {
            if (dto.isBioScansCompleted()) continue; // sampled out; nothing here is still on offer
            List<GenusDto> genus = dto.getGenus();
            for (GenusDto g : genus) {
                certain = certain + g.getRewardInCredits();
                if (dto.isOurDiscovery()) {
                    certain = certain + g.getBonusCreditsForFirstDiscovery();
                } else {
                    unconfirmedBonus = unconfirmedBonus + g.getBonusCreditsForFirstDiscovery();
                }
            }
        }
        return new Projection(certain, unconfirmedBonus);
    }

    /**
     * The projection split into what the commander can count on and what merely might arrive.
     */
    private record Projection(long certain, long unconfirmedBonus) {
    }

    record DataDto(
            long potentialProfit,
            long acquiredProfit,
            long unconfirmedFirstDiscoveryBonus
    ) implements ToYamlConvertable {
        @Override public String toYaml() {
            // Spoken siblings appended the same way as the finance announcements. See SpokenAmounts.RULE.
            return YamlFactory.toYaml(this)
                    + SpokenAmounts.yamlLine("potentialProfit", potentialProfit)
                    + SpokenAmounts.yamlLine("acquiredProfit", acquiredProfit)
                    + SpokenAmounts.yamlLine("unconfirmedFirstDiscoveryBonus", unconfirmedFirstDiscoveryBonus);
        }
    }
}
