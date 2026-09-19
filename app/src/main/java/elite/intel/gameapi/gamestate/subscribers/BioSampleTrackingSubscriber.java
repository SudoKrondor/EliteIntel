package elite.intel.gameapi.gamestate.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.brain.vega.VegaRuntime;
import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.gamestate.status_events.PlayerMovedEvent;
import elite.intel.gameapi.journal.BioSampleDistanceCalculator;
import elite.intel.gameapi.journal.events.dto.BioSampleDto;
import elite.intel.session.LocationData;
import elite.intel.session.PlayerSession;
import elite.intel.session.Status;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Tells the commander when they have walked (or driven) far enough from the last sample of a genus to
 * take the next one, and when they have strayed back inside that range.
 *
 * <p>WHY the row is written under the body's lock, and only on a change: this runs on every Status.json
 * tick - about once a second while moving on a surface - and it used to load the body, set the flags and
 * save the whole row back every time, outside any lock. The scan subscriber deletes a genus's partials
 * the instant its set completes; a tick that had loaded the row just before that deletion wrote its
 * stale copy back just after, and the finished genus's samples rose from the dead. The commander then
 * heard "minimum separation reached" all the way back to the ship after being told the survey was
 * complete. The same interleaving can just as silently drop a freshly recorded sample. See
 * {@link LocationManager#updateBody}.
 */
public class BioSampleTrackingSubscriber {

    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();
    private final Status status = Status.getInstance();

    /**
     * Whether the commander is clear of every tracked partial on the body, as last stored and as measured
     * now. Only a flip between the two is worth a word - or a write.
     */
    record Separation(boolean wasFarEnough, boolean isFarEnough) {
        boolean flipped() {
            return wasFarEnough != isFarEnough;
        }
    }

    @Subscribe
    public void onPlayerMovedEvent(PlayerMovedEvent event) {
        LocationData<Long, Long> where = playerSession.getLocationData();
        if (where.getSystemAddress() == null || where.getInGameId() == null) return;

        // Nearly every tick changes nothing: no partial on the body, or the stored flags already give
        // today's answer. Those are settled from a plain read, so the row is not rewritten once a second.
        Separation probe = refresh(locationManager.findByLocationData(where).getPartialBioSamples(), event);
        if (probe == null || !probe.flipped()) return;

        // Something did flip, so measure it again against the row as it is under the lock - a scan
        // handler may have changed the partials since the read above - and persist that answer.
        AtomicReference<Separation> settled = new AtomicReference<>();
        locationManager.updateBody(where.getSystemAddress(), where.getInGameId(),
                location -> settled.set(refresh(location.getPartialBioSamples(), event)));

        Separation separation = settled.get();
        if (separation == null || !separation.flipped() || status.isInSupercruise()) return;
        announce(separation.isFarEnough());
    }

    /**
     * Re-measures every partial's separation from the commander's position and records it on the sample.
     *
     * @return the aggregate before and after, or null when there is nothing being tracked
     */
    static Separation refresh(List<BioSampleDto> partials, PlayerMovedEvent event) {
        if (partials == null || partials.isEmpty()) return null;

        boolean wasFarEnough = partials.stream().allMatch(BioSampleDto::isPlayerFarEnough);
        boolean isFarEnough = true;
        for (BioSampleDto partial : partials) {
            boolean clear = BioSampleDistanceCalculator.isFarEnoughFromSample(
                    partial.getGenusSymbol(),
                    partial.getSpeciesSymbol(),
                    partial.getScanLatitude(),
                    partial.getScanLongitude(),
                    event.getLatitude(),
                    event.getLongitude(),
                    event.getPlanetRadius()
            );
            partial.setPlayerFarEnough(clear);
            isFarEnough &= clear;
        }
        return new Separation(wasFarEnough, isFarEnough);
    }

    private void announce(boolean farEnough) {
        String vehicle = status.isInSrv() ? "Surface Recon Vehicle" : (status.isOnFoot() ? "You Are" : shipName());
        if (farEnough) {
            VegaRuntime.narrator().narrate(vehicle + " is now far enough to take the new sample.", "State that minimum separation distance from the previous bio colony has been reached and a new sample can now be taken.");
        } else {
            VegaRuntime.narrator().narrate(vehicle + " is moved too close to previous colony to take new sample. insufficient bio diversity.", "Warn that we have moved within minimum separation distance of the previous bio colony and a new sample cannot be taken at this position.");
        }
    }

    /**
     * The ship's name, or a plain "The ship" before a Loadout has named it.
     */
    private String shipName() {
        var loadout = playerSession.getShipLoadout();
        String name = loadout == null ? null : loadout.getShipName();
        return name == null || name.isBlank() ? "The ship" : name;
    }
}
