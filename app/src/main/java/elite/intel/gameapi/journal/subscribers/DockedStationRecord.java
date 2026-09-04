package elite.intel.gameapi.journal.subscribers;

import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.StationName;
import elite.intel.gameapi.journal.events.ApproachSettlementEvent;
import elite.intel.gameapi.journal.events.DockedEvent;
import elite.intel.gameapi.journal.events.LocationEvent;
import elite.intel.gameapi.journal.events.dto.LocationDto;

import java.util.List;

/**
 * The port the ship is standing on, as the journal reports it, written to a record of its own.
 * <p>
 * <b>Why its own record.</b> A fleet carrier, an orbital construction depot and a planetary port are not the
 * body we dropped at, and the location table is addressed by that body. Writing the station's fields onto the
 * body's record re-labelled the body: that is how a moon came to be stored - and narrated - as a
 * FLEET_CARRIER. Across two months of journals, 833 of 1616 dockings were at a station the drop did not name,
 * so this is the common case rather than the exception.
 * <p>
 * <b>Why the MarketID identifies it.</b> A station has no BodyID of its own unless the drop happened to name
 * it (the 783 orbital dockings, where a record already exists under that name and keeps the BodyID it was
 * given). For the rest the MarketID is the handle: it is far outside the range of a real BodyID, so no body
 * lookup can ever return a station, and it is distinct per station, so the stations of one system do not
 * collapse onto each other where records are gathered by BodyID.
 * <p>
 * Both events that report a docked station - {@code Docked}, and {@code Location} for a commander who quit on
 * the pad and came back - go through here, so they cannot disagree about what they wrote.
 */
public record DockedStationRecord(
        long systemAddress,
        String starSystem,
        long marketId,
        String name,
        String type,
        List<String> services,
        String economy,
        String government,
        String faction,
        String allegiance,
        double distanceLs) {

    /**
     * The journal's station type for a fleet carrier; the one type that is never part of a system.
     */
    private static final String FLEET_CARRIER_TYPE = "FleetCarrier";

    public static DockedStationRecord of(DockedEvent event) {
        return new DockedStationRecord(
                event.getSystemAddress(),
                event.getStarSystem(),
                event.getMarketID(),
                event.getStationName(),
                event.getStationType(),
                event.getStationServices(),
                event.getStationEconomyLocalised(),
                event.getStationGovernmentLocalised(),
                event.getStationFaction() == null ? null : event.getStationFaction().getName(),
                null, // Docked carries no StationAllegiance; Location does, on about a third of them
                event.getDistFromStarLS());
    }

    public static DockedStationRecord of(LocationEvent event) {
        return new DockedStationRecord(
                event.getSystemAddress(),
                event.getStarSystem(),
                event.getMarketID(),
                event.getStationName(),
                event.getStationType(),
                event.getStationServices(),
                event.getStationEconomyLocalised(),
                event.getStationGovernmentLocalised(),
                event.getStationFaction() == null ? null : event.getStationFaction().getName(),
                event.getStationAllegiance(),
                event.getDistFromStarLS());
    }

    /**
     * A settlement seen from the air. It reports everything a docking does except the station type - which
     * arrives when the ship lands on its pad - and the star system, which the caller supplies because the
     * event names only the body.
     */
    public static DockedStationRecord of(ApproachSettlementEvent event, String starSystem) {
        return new DockedStationRecord(
                event.getSystemAddress(),
                starSystem,
                event.getMarketID(),
                event.getName(),
                null,
                event.getStationServices(),
                event.getStationEconomyLocalised(),
                event.getStationGovernmentLocalised(),
                event.getStationFaction() == null ? null : event.getStationFaction().getName(),
                event.getStationAllegiance(),
                0);
    }

    /**
     * The name the station's row is filed under. It has to be the one {@code LocationManager.save} will
     * compute, and that getter peels the "$EXT_PANEL_ColonisationShip;" decoration off a colonisation ship.
     */
    public String recordKey() {
        return StationName.display(name);
    }

    public boolean isFleetCarrier() {
        return FLEET_CARRIER_TYPE.equalsIgnoreCase(type);
    }

    /**
     * Files the station under its own name. Only what this event actually carried is written: two thirds of
     * these events name no allegiance, and half the fields of a station are optional, so a blank is left as a
     * blank rather than erasing what an earlier visit established.
     */
    public void store() {
        if (marketId == 0 || !hasText(name) || !hasText(starSystem)) return;

        LocationManager.getInstance().updateNamedBody(systemAddress, marketId, recordKey(), station -> {
            station.setSystemAddress(systemAddress);
            station.setStarName(starSystem);
            station.setStationName(name);
            station.setMarketID(marketId);
            station.setLocationType(isFleetCarrier()
                    ? LocationDto.LocationType.FLEET_CARRIER
                    : LocationDto.LocationType.STATION);

            if (hasText(type)) station.setStationType(type);
            if (services != null && !services.isEmpty()) station.setStationServices(services);
            if (hasText(economy)) station.setStationEconomy(economy);
            if (hasText(government)) station.setStationGovernment(government);
            if (hasText(faction)) station.setStationFaction(faction);
            if (hasText(allegiance)) station.setStationAllegiance(allegiance);
            if (distanceLs > 0) station.setDistance(distanceLs);
        });
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
