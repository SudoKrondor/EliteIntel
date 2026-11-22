package elite.intel.gameapi.journal.subscribers;

import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.journal.ScanBodyClassifier;
import elite.intel.gameapi.journal.events.FSDJumpEvent;
import elite.intel.gameapi.journal.events.ScanEvent;
import elite.intel.gameapi.journal.events.dto.LocationDto;

import static elite.intel.gameapi.journal.events.dto.LocationDto.LocationType.BELT_CLUSTER;

/**
 * Files what an old journal says about the places a commander went, for the import of journal backups.
 * <p>
 * The live subscribers and the startup replay read the journal as the present: an arrival moves the commander, a
 * scan describes the body as it is now. An archive is years out of date and is read after the live journal has
 * already been, so this follows two rules they do not need:
 * <ul>
 *   <li><b>Nothing moves.</b> The commander is wherever the live journal says, not where they were in 2019.</li>
 *   <li><b>Old never overwrites new.</b> A record the app already holds keeps everything on it - a system's
 *       population, factions and powerplay state change, and the live journal's are the true ones. What an old
 *       scan can still add is that the commander discovered or mapped a body first, and that only ever turns on:
 *       a later scan of the same body always reports it already discovered, by them.</li>
 * </ul>
 * Each method is the archive's half of a live writer ({@link FsdJumpArrival}, {@link ScanRecord}) and goes through
 * the same locked write, so a live event arriving meanwhile cannot be lost under it.
 */
public final class ArchivedJourneyRecord {

    private ArchivedJourneyRecord() {
    }

    /**
     * Files the star a jump arrived at, when the app has no record of that system at all.
     */
    public static void arrival(FSDJumpEvent event) {
        if (event.getBody() == null || event.getStarPos() == null || event.getStarPos().length < 3) return;
        LocationManager locations = LocationManager.getInstance();
        if (locations.findSystemRecord(event.getSystemAddress()).getBodyId() != -1) return;
        locations.updateNamedBody(event.getSystemAddress(), event.getBodyId(), event.getBody(), star -> {
            if (isNew(star)) FsdJumpArrival.describe(star, event);
        });
    }

    /**
     * Files a scanned body the app has never seen, or raises the discovery flags of one it has.
     * <p>
     * A scan in a system with no record is skipped: the body's coordinates come from the system's record, and a
     * body filed at the galaxy's origin would mislead every distance search that found it.
     */
    public static void scan(ScanEvent event) {
        if (event.getBodyName() == null || event.getBodyID() == null) return;
        if (BELT_CLUSTER.equals(ScanBodyClassifier.classify(event))) return;

        LocationManager locations = LocationManager.getInstance();
        LocationDto system = locations.findSystemRecord(event.getSystemAddress());
        if (system.getBodyId() == -1) return;

        boolean believable = !ScanEventSubscriber.carriesNoDiscoveryInformation(event, system.getPopulation());
        locations.updateNamedBody(event.getSystemAddress(), event.getBodyID(), event.getBodyName(), body -> {
            if (isNew(body)) {
                ScanRecord.describe(body, event, system);
                return;
            }
            if (!believable) return;
            if (!event.isWasDiscovered()) body.setOurDiscovery(true);
            if (!event.isWasMapped()) body.setWeMappedIt(true);
        });
    }

    /**
     * A record the store had nothing for: every stored record names its system, since one without it is not saved.
     */
    private static boolean isNew(LocationDto location) {
        return location.getStarName() == null;
    }
}
