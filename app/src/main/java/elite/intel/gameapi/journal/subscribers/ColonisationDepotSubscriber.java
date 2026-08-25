package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.db.dao.ConstructionSiteDao.Requirement;
import elite.intel.db.dao.ConstructionSiteDao.Site;
import elite.intel.db.managers.ConstructionSiteManager;
import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.JournalSymbol;
import elite.intel.gameapi.journal.events.ColonisationConstructionDepotEvent;
import elite.intel.gameapi.journal.events.ColonisationConstructionDepotEvent.ResourceRequired;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.session.DockedMarket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;
import java.util.function.LongFunction;
import java.util.function.ObjLongConsumer;

/**
 * Keeps the colonisation shopping list level with the game's own manifest.
 * <p>
 * <b>Why the fingerprint.</b> The game republishes the whole manifest every 15-30 seconds for as long as
 * the ship sits on the pad - fifty identical copies over an ordinary visit, and a visit can be much
 * longer than that. Writing each one would mean a delete-and-reinsert of seventeen rows every twenty
 * seconds for no change at all. So the manifest is hashed and written only when the hash moves, which it
 * does exactly when something happened: we delivered, or somebody else did, or the architect changed the
 * build.
 * <p>
 * <b>What the fingerprint must NOT gate.</b> Which build the commander is working on, and when its panel
 * was last read, are facts about the PAD rather than about the manifest - so they are recorded on every
 * event, unchanged manifest or not. Hanging them off the write is how the overlay went on showing Johri
 * Horizons after the commander flew back to Divis Gateway: the manifest they returned to had not moved
 * while they were away, so nothing was written, so nothing made it current again.
 * <p>
 * <b>Why nothing is deducted here.</b> A {@code ColonisationContribution} is followed within a second by
 * a fresh manifest that already accounts for it, so subtracting our delivery ourselves would either
 * double-count it or race the event that corrects it. The journal is the authority; this only caches its
 * last word, timestamped, so the commander can be told when it was true.
 */
public class ColonisationDepotSubscriber {

    private static final Logger log = LogManager.getLogger(ColonisationDepotSubscriber.class);

    private final BiConsumer<Site, List<Requirement>> writer;
    private final ObjLongConsumer<String> onPad;
    private final LongFunction<LocationDto> locator;

    /**
     * Last manifest written per site, so an unchanged republish costs one string comparison. In memory
     * only: after a restart the first manifest of the session is written once and then settles again.
     */
    private final ConcurrentMap<Long, String> lastWritten = new ConcurrentHashMap<>();

    public ColonisationDepotSubscriber() {
        this(ConstructionSiteManager.getInstance()::save,
                (visitedAt, marketId) -> ConstructionSiteManager.getInstance().arrivedAt(marketId, visitedAt),
                marketId -> LocationManager.getInstance().findByMarketId(marketId));
    }

    /**
     * Seam for tests.
     *
     * @param writer  stores a site and its whole manifest
     * @param onPad   records that the ship is standing on this depot right now, manifest untouched
     * @param locator names the place behind a MarketID, or returns null when nothing has named it yet
     */
    ColonisationDepotSubscriber(BiConsumer<Site, List<Requirement>> writer, ObjLongConsumer<String> onPad,
                                LongFunction<LocationDto> locator) {
        this.writer = writer;
        this.onPad = onPad;
        this.locator = locator;
    }

    @Subscribe
    public void onConstructionDepot(ColonisationConstructionDepotEvent event) {
        if (event == null || event.getMarketID() == 0) return;

        // This event only ever arrives while the ship is on this depot's pad, and it repeats every 15-30
        // seconds. That makes it the repair for a marker lost to a restart made while docked: without it the
        // shopping command would stay unavailable for the rest of the visit, because the Docked that would
        // have set it is in an already-read journal.
        DockedMarket.getInstance().arrived(event.getMarketID());

        List<Requirement> manifest = manifest(event);
        String fingerprint = fingerprint(event, manifest);
        if (fingerprint.equals(lastWritten.get(event.getMarketID()))) {
            // Nothing to rewrite, but we are still standing here - and that is the fact the overlay and the
            // shopping command read to decide WHICH build they are about.
            onPad.accept(event.getTimestamp(), event.getMarketID());
            return;
        }

        Site site = site(event);
        writer.accept(site, manifest);
        lastWritten.put(event.getMarketID(), fingerprint);
        log.debug("Construction site {} ({}) at {}%, {} of {} lines outstanding",
                site.getStationName(), event.getMarketID(), Math.round(event.getConstructionProgress() * 100),
                manifest.stream().filter(line -> line.outstanding() > 0).count(), manifest.size());
    }

    /**
     * The depot event carries only a MarketID, so the name of the place comes from the location the
     * {@code Docked} that preceded it recorded against that same id.
     * <p>
     * A site with no name yet is still worth storing: the manifest is what the shopping commands need,
     * and the existing name is kept rather than overwritten with a null on a later visit.
     */
    private Site site(ColonisationConstructionDepotEvent event) {
        Site site = new Site();
        site.setMarketId(event.getMarketID());
        site.setProgress(event.getConstructionProgress());
        site.setComplete(event.isConstructionComplete());
        site.setFailed(event.isConstructionFailed());
        site.setVisitedAt(event.getTimestamp());

        LocationDto location = locator.apply(event.getMarketID());
        if (location != null) {
            site.setStationName(location.getStationName());
            site.setStarSystem(location.getStarName());
            site.setSystemAddress(location.getSystemAddress() == 0 ? null : location.getSystemAddress());
        }
        return site;
    }

    private static List<Requirement> manifest(ColonisationConstructionDepotEvent event) {
        List<Requirement> manifest = new ArrayList<>();
        for (ResourceRequired resource : event.getResourcesRequired()) {
            String symbol = JournalSymbol.normalize(resource.getName());
            if (symbol == null) continue;
            Requirement requirement = new Requirement();
            requirement.setMarketId(event.getMarketID());
            requirement.setSymbol(symbol);
            requirement.setGameName(resource.getNameLocalised());
            requirement.setRequiredAmount(resource.getRequiredAmount());
            requirement.setProvidedAmount(resource.getProvidedAmount());
            requirement.setPayment(resource.getPayment());
            manifest.add(requirement);
        }
        return manifest;
    }

    /**
     * What "the manifest changed" means: the build's own state plus every line's counts. The payment is
     * in it too - it is what the depot pays per tonne and the commander is shown it, so a revision is a
     * change worth storing even when no tonnage moved.
     */
    static String fingerprint(ColonisationConstructionDepotEvent event, List<Requirement> manifest) {
        StringBuilder sb = new StringBuilder()
                .append(event.getConstructionProgress()).append('|')
                .append(event.isConstructionComplete()).append('|')
                .append(event.isConstructionFailed());
        for (Requirement line : manifest) {
            sb.append('|').append(line.getSymbol()).append(':')
                    .append(line.getRequiredAmount()).append('/')
                    .append(line.getProvidedAmount()).append('@')
                    .append(line.getPayment());
        }
        return sb.toString();
    }
}
