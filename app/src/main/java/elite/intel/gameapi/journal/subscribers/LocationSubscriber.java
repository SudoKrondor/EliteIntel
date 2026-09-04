package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.journal.events.LocationEvent;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.gameapi.search.edsm.EdsmApiClient;

import java.util.Locale;

public class LocationSubscriber {

    private final LocationManager locationManager = LocationManager.getInstance();

    @Subscribe
    public void onLocationEvent(LocationEvent event) {

        LocationDto dto = findLocation(event);
        dto.setX(event.getStarPos()[0]);
        dto.setY(event.getStarPos()[1]);
        dto.setZ(event.getStarPos()[2]);
        dto.setBodyId(event.getBodyID());
        dto.setStarName(event.getStarSystem());
        dto.setPlanetName(event.getBody());
        dto.setAllegiance(event.getSystemAllegiance());
        dto.setBodyType(event.getBodyType());
        dto.setControllingPower(event.getControllingPower());
        dto.setGovernment(event.getSystemGovernmentLocalised());
        dto.setPopulation(event.getPopulation());
        dto.setSecurity(event.getSystemSecurity());
        dto.setStarName(event.getStarSystem());
        dto.setDistance(event.getDistFromStarLS());
        dto.setEconomy(event.getSystemEconomyLocalised());
        dto.setSecondEconomy(event.getSystemSecondEconomyLocalised());

        // Only when the journal's body type maps to something: determineType answers null for "Planet", and
        // assigning that erased the classification a scan had established, on every startup.
        LocationDto.LocationType bodyType =
                LocationDto.determineType(event.getBodyType().toLowerCase(Locale.ROOT), event.getDistFromStarLS() > 0);
        if (bodyType != null) {
            dto.setLocationType(bodyType);
        }

        dto.setPopulation(event.getPopulation());
        dto.setPowerplayState(event.getPowerplayState());
        dto.setPowerplayStateControlProgress(event.getPowerplayStateControlProgress());
        dto.setPowerplayStateReinforcement(event.getPowerplayStateReinforcement());
        dto.setPowerplayStateUndermining(event.getPowerplayStateUndermining());
        dto.setSecurity(event.getSystemSecurityLocalised());

        if (event.getSystemFaction() != null) dto.setSystemFaction(event.getSystemFaction().getName());

        Thread.ofVirtual().start(() -> {
            dto.setTrafficDto(EdsmApiClient.searchTraffic(event.getStarSystem()));
            dto.setDeathsDto(EdsmApiClient.searchDeaths(event.getStarSystem()));

            if (dto.getStarName() != null && !dto.getStarName().isEmpty()) {
                //have to check for star name (primary star of the system). Sometimes the star name is empty.
                //do not save locations without star name.
                locationManager.save(dto);
            }

            // After the body, never onto it: this is the arrival of a commander who quit on a pad, and the
            // station standing here is not the body the event names. Written last so that where the two are
            // the same record - an orbital station, whose Body IS its own name - the station's fields land on
            // top of the body write rather than under it.
            if (event.isDocked()) {
                DockedStationRecord.of(event).store();
            }
        });
    }

    private LocationDto findLocation(LocationEvent event) {
        return locationManager.findBySystemAddress(event.getSystemAddress(), event.getBody());
    }
}
