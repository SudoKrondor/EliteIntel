package elite.intel.gameapi.carrier;

import elite.intel.gameapi.journal.events.CarrierStatsEvent;
import elite.intel.gameapi.journal.events.dto.CarrierDataDto;

/**
 * The full reading of a carrier the game writes when the commander opens its management panel.
 * <p>
 * <b>Why this is not a method on either manager.</b> A commander can own a fleet carrier AND a squadron
 * carrier, and the game reports both through the same {@code CarrierStats} event, distinguished only by
 * {@code CarrierType}. The reading itself is identical for the two - the same balances, the same tank, the
 * same holds - so it belongs neither to the fleet carrier's table nor to the squadron carrier's, and lives
 * here where both can apply it to their own record.
 * <p>
 * Before this existed, every {@code CarrierStats} was written to the fleet carrier. A squadron carrier's
 * panel therefore overwrote the fleet carrier's callsign, fuel and balances with another ship's, and the
 * squadron carrier itself never acquired a callsign at all - which is what {@link OwnCarrierHold} needs to
 * recognise it as the commander's own.
 */
public final class CarrierStatsReading {

    /**
     * The value {@code CarrierType} takes for the squadron carrier. The fleet carrier's is
     * {@code FleetCarrier}, but nothing is decided on that string: see {@link #isSquadron}.
     */
    private static final String SQUADRON = "SquadronCarrier";

    private CarrierStatsReading() {
    }

    /**
     * Whether this reading is the squadron carrier's.
     * <p>
     * Only an explicit match counts, and everything else - including a missing {@code CarrierType} - is the
     * fleet carrier. Squadron carriers are the newcomer; a journal that says nothing about which kind of
     * carrier this is predates them, and every such reading for the last several years has been a fleet
     * carrier's. Guessing the other way round would move a commander's whole carrier record on the strength
     * of an absent field.
     */
    public static boolean isSquadron(CarrierStatsEvent event) {
        return event != null && SQUADRON.equalsIgnoreCase(event.getCarrierType());
    }

    /**
     * Writes the reading onto a carrier's record. The caller has already chosen which record - saving it is
     * theirs too, since the two carriers live in different tables.
     */
    public static void applyTo(CarrierDataDto carrier, CarrierStatsEvent event) {
        if (carrier == null || event == null) return;

        // The id every other carrier event keys on - a fuel deposit, a trade order, a jump - and the same
        // number as the carrier's MarketID. Nothing else reports it, so this reading is where we learn it.
        carrier.setCarrierId(event.getCarrierID());
        carrier.setCallSign(event.getCallsign());
        carrier.setCarrierName(event.getName());
        carrier.setCarrierType(event.getCarrierType());
        carrier.setDockingAccess(event.getDockingAccess());
        carrier.setAllowNotorious(event.isAllowNotorious());
        carrier.setPendingDecommission(event.isPendingDecommission());
        carrier.setMeasuredFuelLevel(event.getFuelLevel());

        CarrierStatsEvent.SpaceUsage spaceUsage = event.getSpaceUsage();
        if (spaceUsage != null) {
            carrier.setCargoSpaceUsed(spaceUsage.getCargo());
            carrier.setCargoSpaceReserved(spaceUsage.getCargoSpaceReserved());
            carrier.setShipRacks(spaceUsage.getShipPacks());
            carrier.setModulePacks(spaceUsage.getModulePacks());
            carrier.setFreeSpaceInCargo(spaceUsage.getFreeSpace());
            carrier.setCargoCapacity(spaceUsage.getTotalCapacity());
        }

        CarrierStatsEvent.Finance finance = event.getFinance();
        if (finance != null) {
            carrier.setTotalBalance(finance.getCarrierBalance());
            carrier.setReserveBalance(finance.getReserveBalance());
            carrier.setMarketBalance(finance.getAvailableBalance());
            carrier.setPioneerSupplyTax(finance.getTaxRatePioneerSupplies());
            carrier.setShipYardSupplyTax(finance.getTaxRateShipyard());
            carrier.setRearmSupplyTax(finance.getTaxRateRearm());
            carrier.setRepairSupplyTax(finance.getTaxRateRepair());
            carrier.setRefuelSupplyTax(finance.getTaxRateRefuel());
        }
    }
}
