package elite.intel.ai.brain.actions.handlers.queries;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.queries.struct.AiDataStruct;
import elite.intel.db.FuzzySearch;
import elite.intel.db.dao.ConstructionSiteDao.Site;
import elite.intel.db.managers.ConstructionSiteManager;
import elite.intel.gameapi.StationName;
import elite.intel.gameapi.colonisation.*;
import elite.intel.gameapi.colonisation.CarrierStockpile.Stash;
import elite.intel.gameapi.colonisation.ShoppingShelves.Shop;
import elite.intel.session.PlayerSession;
import elite.intel.util.StringUtls;
import elite.intel.util.yaml.ToYamlConvertable;
import elite.intel.util.yaml.YamlFactory;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * How the colonisation build is going: how far along, what it still wants, and what the next haul is.
 * <p>
 * Answers from the stored manifest rather than requiring the commander to be standing on the depot's pad -
 * the question is most useful in the middle of a shopping run, which is precisely when they are not. The
 * answer therefore says WHEN the figures were last true: other commanders haul to the same depot, so a
 * manifest from an hour ago is a claim about the past and reporting it as the present is how a commander
 * ends up buying six hundred tonnes of something that was finished while they were away.
 * <p>
 * <b>Measured against everything bought, not just the ship's hold.</b> The tonnes are read through
 * {@link ConstructionShopping}, the same arithmetic the HUD card uses, so cargo staged on a carrier counts.
 * It was measuring the hold alone, and a commander with 5,281 tonnes of titanium on their carrier was told
 * over the top of a card reading {@code 5,281/6,161} that the site was "missing 6,161 tonnes of titanium".
 * The screen and the voice answering the same question differently in the same second is the one outcome
 * this whole area is built to avoid.
 * <p>
 * <b>Every figure is finished before the model sees it.</b> "How much do we still need to buy" is a
 * subtraction, and a small local model is a pattern matcher rather than a calculator, so
 * {@code stillToBuyTonnes} is computed here and the instructions forbid reading out anything absent from the
 * data. {@code soldHere} does the same job for "here": whether this market can supply a good is a fact the
 * shelves already know, and is not something to leave the model inferring from a name.
 */
@RegisterQuery
public class AnalyzeConstructionSiteQuery extends BaseQueryAnalyzer implements IntelQuery {
    public static final String ID = "query_construction_site_progress";

    private final ConstructionSiteManager constructionSiteManager = ConstructionSiteManager.getInstance();
    private final PlayerSession playerSession = PlayerSession.getInstance();

    @Override
    public String llmDescription() {
        return "Report progress on the colonisation construction site the commander is hauling to: how "
                + "complete the build is, which commodities it still needs and how many tonnes of each, how "
                + "much is already bought for it in the ship or on the carrier, and how many tonnes of a "
                + "commodity are left to buy at this market. Use for questions about the construction site, "
                + "the colony build or what the site still needs, and for EVERY question asking how much or "
                + "how many tonnes of something the build wants - including a commodity the commander names, "
                + "as in 'how much steel do we still need to buy'. It reports figures only: it never searches "
                + "markets and never plots a route.";
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

        Set<String> stillWanted = manifest.stream()
                .map(ConstructionCargo.Outstanding::symbol)
                .collect(Collectors.toUnmodifiableSet());
        Optional<Shop> shop = ShoppingShelves.getInstance().current();
        List<LineDto> lines = report(manifest,
                CarrierStockpile.forBuild(site, stillWanted).orElse(null),
                shop.map(Shop::stock).orElse(Set.of()));

        String instructions = """
                Answer the commander's question about the colonisation construction site, then stop.
                The HUD already shows the site and its progress, so never volunteer a status report.
                
                Data fields:
                - siteName: the construction site's name. Say it as written. This is the site the commander
                  was last docked at, which is the one "the construction site" always means.
                - starSystem: the system it is in; may be absent.
                - percentComplete: build completion, already rounded, 0-100.
                - buildState: present ONLY when the build is finished or has failed. Say it when present.
                - staleHours: whole hours since this manifest was read off the site's own panel.
                - otherSitesTracked: other builds the commander is also hauling to. Present only when there
                  are some, and only worth a word if they ask about other builds.
                - outstandingCommodities: how many commodities still want tonnes.
                - totalTonnesOutstanding: tonnes still wanted across all of them.
                - marketName: the market the commander can buy from right now; absent when there is none, and
                  then nothing can be bought "here" and there is no "here" to speak of.
                - lines: goods already part bought first, then the largest requirement. For each -
                  commodity (say this name), outstandingTonnes (what the site still wants in total),
                  ownedTonnes (already bought for it, ship and carrier together), stillToBuyTonnes (what is
                  left to buy - ALREADY SUBTRACTED, never work it out yourself), soldHere (whether marketName
                  stocks it), paymentPerTonne (credits the depot pays).
                
                Shape the answer to the question asked:
                - HOW MUCH of something, or what to buy next: open with the tonnage. The number is the first
                  thing out of your mouth, and one sentence is the whole answer.
                - Asked about one commodity: answer about that one only.
                - Asked what to buy HERE: use only lines whose soldHere is true, and say plainly that a good
                  with soldHere false is not sold at marketName rather than giving its tonnage as if it
                  could be bought.
                - Only when asked how the build is GOING, or for its status: percentComplete and siteName,
                  then two or three commodities with their stillToBuyTonnes.
                
                When staleHours is 1 or more, hang "as of {staleHours} hours ago" off the end of the figure
                you give - a trailing clause, never an opening one. Other commanders deliver to this site.
                A commodity whose stillToBuyTonnes is 0 is already bought in full and needs delivering,
                not buying.
                A field that is absent has nothing to say: it is never a negative to report. Do not read out
                any figure that is not in the data.
                """;

        long totalOutstanding = manifest.stream().mapToLong(ConstructionCargo.Outstanding::outstanding).sum();
        return process(new AiDataStruct(instructions, new DataDto(
                site.getStationName(),
                site.getStarSystem(),
                (int) Math.round(site.getProgress() * 100),
                buildState(site),
                ManifestAge.hoursSince(site.getVisitedAt()),
                otherSitesTracked(),
                manifest.size(),
                totalOutstanding,
                marketName(shop),
                lines)), originalUserInput);
    }

    /**
     * The manifest as the model should read it: every tonnage finished, in the order a haul is worked.
     * <p>
     * Package-private and free of the session singletons on purpose - this is the part with arithmetic in it,
     * and it is the part worth pinning in a test.
     *
     * @param stash        the carrier working this build, or null when none is
     * @param onTheShelves what the market in front of the commander stocks, empty when there is no market
     */
    static List<LineDto> report(List<ConstructionCargo.Outstanding> manifest, Stash stash,
                                Set<String> onTheShelves) {
        return ConstructionShopping.toDeliver(manifest, stash).stream()
                .limit(MAX_LINES_REPORTED)
                .map(line -> new LineDto(
                        // Localized here, in Java: the payload is what the model reads out, and the symbol
                        // behind it is an identifier that must never reach the voice.
                        displayName(line.good()),
                        line.needed(),
                        line.owned(),
                        Math.max(0, line.needed() - line.owned()),
                        onTheShelves.contains(line.symbol()),
                        line.good().payment()))
                .toList();
    }

    /**
     * The one word worth saying about a build's state, or null while it is simply under way.
     */
    private static String buildState(Site site) {
        if (site.isFailed()) return "failed";
        if (site.isComplete()) return "complete";
        return null;
    }

    /**
     * Builds other than this one, or null when this is the only one - so "we are also hauling to eleven
     * other locations" cannot turn up in the answer to a question about a single commodity.
     */
    private Integer otherSitesTracked() {
        int others = constructionSiteManager.siteCount() - 1;
        return others > 0 ? others : null;
    }

    /**
     * The market the commander can buy from right now, or null when they are nowhere near one.
     */
    private static String marketName(Optional<Shop> shop) {
        String name = shop.map(current -> StationName.display(current.stationName())).orElse(null);
        return name == null || name.isBlank() ? null : name;
    }

    /**
     * Enough for the model to name the next few hauls without handing it seventeen lines to summarise.
     * <p>
     * Raised from six once the query started answering about ONE named commodity: a summary only ever needs
     * the top of the list, but "how much water purifier do we still need" is a question about the bottom of
     * it, and a line trimmed away cannot be answered from at all. A whole manifest is a dozen or so lines of
     * six short fields, which is affordable; the instructions, not the cap, are what keep the summary short.
     */
    private static final int MAX_LINES_REPORTED = 16;

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

    /**
     * @param outstandingTonnes what the site still wants in total, ignoring anything we hold
     * @param ownedTonnes       bought for it already - ship's hold and carrier together
     * @param stillToBuyTonnes  {@code outstandingTonnes - ownedTonnes}, floored at zero and done HERE: the
     *                          model is asked to read this number, never to arrive at it
     * @param soldHere          whether the market the commander is at stocks this good
     */
    record LineDto(String commodity, int outstandingTonnes, int ownedTonnes, int stillToBuyTonnes,
                   boolean soldHere, long paymentPerTonne) implements ToYamlConvertable {
        @Override
        public String toYaml() {
            return YamlFactory.toYaml(this);
        }
    }

    /**
     * WHY several fields are nullable rather than false or zero: the payload is serialized with nulls
     * omitted, so a null field is one the model never sees and therefore cannot narrate. A build that is
     * merely in progress used to arrive as {@code complete: false, failed: false}, and the answer to "how
     * much steel do we need" opened with "the build is not finished and has not failed" - a sentence about
     * two things that had not happened. State that is only worth saying when it is true is now only present
     * when it is true.
     */
    record DataDto(String siteName, String starSystem, int percentComplete, String buildState,
                   long staleHours, Integer otherSitesTracked,
                   int outstandingCommodities, long totalTonnesOutstanding,
                   String marketName, List<LineDto> lines) implements ToYamlConvertable {
        @Override
        public String toYaml() {
            return YamlFactory.toYaml(this);
        }
    }
}
