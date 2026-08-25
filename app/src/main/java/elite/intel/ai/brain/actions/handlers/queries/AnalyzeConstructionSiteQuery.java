package elite.intel.ai.brain.actions.handlers.queries;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.queries.struct.AiDataStruct;
import elite.intel.db.FuzzySearch;
import elite.intel.db.dao.ConstructionSiteDao.Site;
import elite.intel.db.managers.ConstructionSiteManager;
import elite.intel.gameapi.colonisation.ConstructionCargo;
import elite.intel.gameapi.colonisation.ManifestAge;
import elite.intel.session.PlayerSession;
import elite.intel.util.StringUtls;
import elite.intel.util.yaml.ToYamlConvertable;
import elite.intel.util.yaml.YamlFactory;

import java.util.List;

/**
 * How the colonisation build is going: how far along, what it still wants, and what the next haul is.
 * <p>
 * Answers from the stored manifest rather than requiring the commander to be standing on the depot's pad -
 * the question is most useful in the middle of a shopping run, which is precisely when they are not. The
 * answer therefore says WHEN the figures were last true: other commanders haul to the same depot, so a
 * manifest from an hour ago is a claim about the past and reporting it as the present is how a commander
 * ends up buying six hundred tonnes of something that was finished while they were away.
 */
@RegisterQuery
public class AnalyzeConstructionSiteQuery extends BaseQueryAnalyzer implements IntelQuery {
    public static final String ID = "query_construction_site_progress";

    private final ConstructionSiteManager constructionSiteManager = ConstructionSiteManager.getInstance();
    private final PlayerSession playerSession = PlayerSession.getInstance();

    @Override
    public String llmDescription() {
        return "Report progress on the colonisation construction site the commander is hauling to: how "
                + "complete the build is, which commodities it still needs and how many tonnes of each, and "
                + "what is already in the hold for it. Use for questions about the construction site, the "
                + "colony build, or what the site still needs.";
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public JsonObject handle(String action, JsonObject params, String originalUserInput) throws Exception {
        Site site = constructionSiteManager.currentSite();
        if (site == null) {
            return process(StringUtls.localizedResponse("query.construction.noSite"));
        }

        List<ConstructionCargo.Outstanding> manifest = ConstructionCargo.outstanding(
                constructionSiteManager.requirements(site.getMarketId()),
                ConstructionCargo.heldBySymbol(playerSession.getShipCargo()));

        List<LineDto> lines = manifest.stream()
                .limit(MAX_LINES_REPORTED)
                .map(line -> new LineDto(
                        // Localized here, in Java: the payload is what the model reads out, and the symbol
                        // behind it is an identifier that must never reach the voice.
                        displayName(line),
                        line.outstanding(),
                        line.held(),
                        line.shortfall(),
                        line.payment()))
                .toList();

        String instructions = """
                Report progress on the colonisation construction site.
                
                Data fields:
                - siteName: the construction site's name. Say it as written. This is the site the commander
                  was last docked at, which is the one "the construction site" always means.
                - sitesTracked: how many builds the commander is hauling to. Mention that there are others
                  ONLY when this is greater than 1, and only in passing.
                - starSystem: the system it is in; may be null if never recorded.
                - percentComplete: build completion, already rounded, 0-100.
                - complete / failed: build state flags.
                - lastSeenUtc: when this manifest was read off the site's own panel, ISO-8601 UTC.
                - staleHours: whole hours since then, already computed.
                - outstandingCommodities: how many commodities still want tonnes.
                - totalTonnesOutstanding: tonnes still wanted across all of them.
                - lines: the largest shortfalls first, at most a handful. For each -
                  commodity (say this name), outstandingTonnes (what the site still wants),
                  heldTonnes (already in our hold for it), shortfallTonnes (still to buy),
                  paymentPerTonne (credits the depot pays).
                
                Lead with the percentage and the site name. Name the next two or three commodities with
                their shortfall in tonnes. If staleHours is 1 or more, say the figures are from the last
                visit and may have moved - other commanders deliver to the same site. If complete is
                true, say the build is finished. If failed is true, say the build failed.
                Do not read out any figure that is not in the data.
                """;

        long totalOutstanding = manifest.stream().mapToLong(ConstructionCargo.Outstanding::outstanding).sum();
        return process(new AiDataStruct(instructions, new DataDto(
                site.getStationName(),
                constructionSiteManager.siteCount(),
                site.getStarSystem(),
                (int) Math.round(site.getProgress() * 100),
                site.isComplete(),
                site.isFailed(),
                site.getVisitedAt(),
                ManifestAge.hoursSince(site.getVisitedAt()),
                manifest.size(),
                totalOutstanding,
                lines)), originalUserInput);
    }

    /**
     * Enough for the model to name the next few hauls without handing it seventeen lines to summarise. The
     * manifest is sorted largest-first, so the ones that matter are at the top.
     */
    private static final int MAX_LINES_REPORTED = 6;

    /**
     * The commodity in the commander's language, falling back to the game's own name for a good the
     * commodities table has no symbol for, and to the bare symbol only when there is nothing else at all.
     */
    private static String displayName(ConstructionCargo.Outstanding line) {
        String english = FuzzySearch.commodityNameForSymbol(line.symbol());
        if (english != null) return FuzzySearch.localizedCommodityName(english);
        if (line.gameName() != null && !line.gameName().isBlank()) return line.gameName();
        return line.symbol();
    }

    record LineDto(String commodity, int outstandingTonnes, int heldTonnes, int shortfallTonnes,
                   long paymentPerTonne) implements ToYamlConvertable {
        @Override
        public String toYaml() {
            return YamlFactory.toYaml(this);
        }
    }

    record DataDto(String siteName, int sitesTracked, String starSystem, int percentComplete, boolean complete,
                   boolean failed,
                   String lastSeenUtc, long staleHours, int outstandingCommodities, long totalTonnesOutstanding,
                   List<LineDto> lines) implements ToYamlConvertable {
        @Override
        public String toYaml() {
            return YamlFactory.toYaml(this);
        }
    }
}
