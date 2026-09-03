package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.journal.events.SupercruiseExitEvent;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.session.PlayerSession;

import java.util.Locale;

import static elite.intel.gameapi.journal.events.dto.LocationDto.LocationType.*;

@SuppressWarnings("unused")
public class SupercruiseExitedSubscriber {

    /**
     * The journal's BodyType for a barycentre - the empty point two stars orbit. It is not a place: it has no
     * surface, no scan and no record of its own, and BodyID 0 has been seen on one, which is also the primary
     * star's id.
     */
    private static final String BARYCENTRE = "null";

    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();

    @Subscribe
    public void onSupercruiseExited(SupercruiseExitEvent event) {
        Thread.ofVirtual().start(() -> {
            Long bodyId = event.getBodyId();
            if (bodyId == null) return;
            playerSession.setCurrentLocationId(bodyId, event.getSystemAddress());

            String name = event.getBody();
            if (name == null || name.isBlank()) return;

            LocationDto.LocationType droppedAt = classify(event);
            if (droppedAt == null) return;

            // Addressed by name: a station docked at this body is stored under the same BodyID, so an ID
            // lookup can return the station's record and rename it to the body on save.
            locationManager.updateNamedBody(event.getSystemAddress(), bodyId, name, here -> {
                here.setSystemAddress(event.getSystemAddress());
                here.setBodyId(bodyId);
                here.setStarName(event.getStarSystem());
                here.setBodyType(event.getBodyType());

                // The drop reports an orbital station under its own name, never the body's.
                if (STATION == droppedAt) {
                    here.setStationName(name);
                } else {
                    here.setPlanetName(name);
                }

                // A Scan classifies better than a drop does - it tells a moon from a planet, and the primary
                // star from its companions - so only the blank is filled in, never an existing answer.
                if (here.getLocationType() == null || UNCLASSIFIED == here.getLocationType()) {
                    here.setLocationType(droppedAt);
                }
            });
        });
    }

    /**
     * What we dropped at, from the journal's own vocabulary for a supercruise exit: Planet, Star, Station,
     * PlanetaryRing, StellarRing, AsteroidCluster and the literal "Null" of a barycentre are every value seen
     * across two months of journals.
     * <p>
     * Returns {@code null} for a barycentre - there is nothing there to record - and UNCLASSIFIED for a value
     * we do not know, so the drop is still recorded under its name and a later scan can say what it is.
     * {@link LocationDto#determineType} is not used here: it reads station and body-class strings, and answers
     * {@code null} for "Planet", which is why a planet's name was never recorded.
     */
    static LocationDto.LocationType classify(SupercruiseExitEvent event) {
        String bodyType = event.getBodyType() == null ? "" : event.getBodyType().toLowerCase(Locale.ROOT);
        if (bodyType.isBlank() || BARYCENTRE.equals(bodyType)) return null;
        String body = event.getBody() == null ? "" : event.getBody();
        return switch (bodyType) {
            // Only the star a system is named after is claimed as primary; a companion keeps its distance.
            case "star" -> body.equalsIgnoreCase(event.getStarSystem()) ? PRIMARY_STAR : STAR;
            case "planet" -> PLANET;
            case "station" -> STATION;
            case "planetaryring", "stellarring" -> PLANETARY_RING;
            case "asteroidcluster" -> BELT_CLUSTER;
            default -> UNCLASSIFIED;
        };
    }
}
