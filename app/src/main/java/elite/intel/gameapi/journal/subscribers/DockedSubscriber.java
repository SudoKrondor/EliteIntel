package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.db.dao.LocationDao;
import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.journal.events.DockedEvent;
import elite.intel.gameapi.journal.events.dto.CarrierDataDto;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.session.PlayerSession;

import java.util.Locale;

import static elite.intel.gameapi.journal.events.dto.LocationDto.LocationType.FLEET_CARRIER;
import static elite.intel.gameapi.journal.events.dto.LocationDto.LocationType.STATION;

public class DockedSubscriber {

    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();

    @Subscribe
    public void onDockedEvent(DockedEvent event) {

        Thread.ofVirtual().start(() -> {

            /// this is a workaround
            /*
             * Docked Event does not have a system address or body id. However, it has market id and market data.
             * This means we have to grab the location where we dropped from supercruise and set these numbers here.
             * */
            LocationDto location = locationManager.findByLocationData(playerSession.getLocationData());
            //LocationDto location = locationManager.findByMarketId(event.getMarketID());

            location.setMarketID(event.getMarketID());
            location.setStationEconomy(event.getStationEconomyLocalised());
            location.setStationServices(event.getStationServices());
            location.setStationType(event.getStationType());
            location.setStationGovernment(event.getStationGovernmentLocalised());
            location.setStarName(event.getStarSystem());
            location.setStationName(event.getStationName());
            location.setPlanetName(null);
            location.setPlanetShortName(null);


            Thread.ofVirtual().start(() -> {
                LocationDto.LocationType locationType = LocationDto.determineType(event.getStationType().toLowerCase(Locale.ROOT), false);
                if (FLEET_CARRIER == locationType) {
                    location.setLocationType(FLEET_CARRIER);
                    LocationDao.Coordinates coordinates = LocationManager.getInstance().getGalacticCoordinates();
                    if (coordinates != null) {
                        CarrierDataDto carrierData = playerSession.getFleetCarrierData();
                        carrierData.setX(coordinates.x());
                        carrierData.setY(coordinates.y());
                        carrierData.setZ(coordinates.z());
                        carrierData.setStarName(event.getStarSystem());
                        playerSession.setFleetCarrierData(carrierData);
                    }
                } else {
                    location.setLocationType(STATION);
                }

                if (event.getStationFaction() != null) location.setStationFaction(event.getStationFaction().getName());
                locationManager.save(location);
            }); // end virtual thread
        });
    }
}
