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
        dto.setStationAllegiance(event.getStationAllegiance());
        dto.setStationEconomy(event.getStationEconomyLocalised());
        dto.setStationGovernment(event.getStationGovernmentLocalised());
        dto.setStationServices(event.getStationServices());
        dto.setStarName(event.getStarSystem());
        dto.setStationType(event.getStationType());
        dto.setDistance(event.getDistFromStarLS());
        dto.setEconomy(event.getSystemEconomyLocalised());
        dto.setSecondEconomy(event.getSystemSecondEconomyLocalised());
        dto.setLocationType(LocationDto.determineType(event.getBodyType().toLowerCase(Locale.ROOT), event.getDistFromStarLS() > 0));


        dto.setStationType(event.getStationType());
        if ("FleetCarrier".equalsIgnoreCase(event.getStationType())) {
            dto.setLocationType(LocationDto.LocationType.FLEET_CARRIER);
        }

        dto.setPopulation(event.getPopulation());
        dto.setPowerplayState(event.getPowerplayState());
        dto.setPowerplayStateControlProgress(event.getPowerplayStateControlProgress());
        dto.setPowerplayStateReinforcement(event.getPowerplayStateReinforcement());
        dto.setPowerplayStateUndermining(event.getPowerplayStateUndermining());
        dto.setSecurity(event.getSystemSecurityLocalised());
        dto.setStationName(event.getStationName());

        if (event.getStationFaction() != null) dto.setStationFaction(event.getStationFaction().getName());

        Thread.ofVirtual().start(() -> {
            dto.setTrafficDto(EdsmApiClient.searchTraffic(event.getStarSystem()));
            dto.setDeathsDto(EdsmApiClient.searchDeaths(event.getStarSystem()));

            if (dto.getStarName() != null && !dto.getStarName().isEmpty()) {
                //have to check for star name (primary star of the system). Sometimes the star name is empty.
                //do not save locations without star name.
                locationManager.save(dto);
            }
        });
    }

    private LocationDto findLocation(LocationEvent event) {
        return locationManager.findBySystemAddress(event.getSystemAddress(), event.getBody());
    }
}
