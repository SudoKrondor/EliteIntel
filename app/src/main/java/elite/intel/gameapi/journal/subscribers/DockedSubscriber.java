package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.db.dao.LocationDao;
import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.EventBusManager;
import elite.intel.gameapi.SensorDataEvent;
import elite.intel.gameapi.journal.events.DockedEvent;
import elite.intel.gameapi.journal.events.dto.CarrierDataDto;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.session.PlayerSession;

import java.util.List;
import java.util.Locale;

import static elite.intel.gameapi.journal.events.dto.LocationDto.LocationType.FLEET_CARRIER;
import static elite.intel.gameapi.journal.events.dto.LocationDto.LocationType.STATION;
import static elite.intel.util.StringUtls.localizedEvent;

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

                StringBuilder sb = new StringBuilder();
                List<String> stationServices = event.getStationServices();
                if (stationServices != null && !stationServices.isEmpty()) {
                    sb.append(localizedEvent("event.docked.services")).append(": ");
                    for (String service : stationServices) {
                        sb.append(service);
                        sb.append(", ");
                    }
                    sb.append(".");
                }

                DockedEvent.LandingPads landingPads = event.getLandingPads();
                if (landingPads != null) {
                    sb.append(" ").append(localizedEvent("event.docked.landingPads")).append(":");
                    sb.append(" ").append(localizedEvent("event.docked.padLarge")).append(": ").append(landingPads.getLarge()).append(", ");
                    sb.append(" ").append(localizedEvent("event.docked.padMedium")).append(": ").append(landingPads.getMedium()).append(", ");
                    sb.append(" ").append(localizedEvent("event.docked.padSmall")).append(": ").append(landingPads.getSmall()).append(".");
                }

                String availableData = LocalServicesData.setLocalServicesData(event.getMarketID());
                if (!availableData.isEmpty()) {
                    sb.append(" ").append(availableData);
                    EventBusManager.publish(new MissionCriticalAnnouncementEvent(localizedEvent("event.docked.marketData")));
                }

                if (!sb.isEmpty()) {
                    EventBusManager.publish(new SensorDataEvent(sb.toString(), "Announce the docking information including available services and landing pad configuration."));
                }
                locationManager.save(location);
            }); // end virtual thread
        });
    }
}
