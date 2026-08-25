package elite.intel.gameapi.carrier;

import elite.intel.db.managers.FleetCarrierManager;
import elite.intel.db.managers.SquadronCarrierManager;
import elite.intel.gameapi.journal.events.dto.CarrierDataDto;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * Finding which of the commander's carriers an event is about, and writing back to that one.
 * <p>
 * A commander can own one fleet carrier and one squadron carrier, no more - but the two live in different
 * tables, and most carrier events identify their subject by only one of two handles: an id (a fuel deposit,
 * a trade order) or a station name (a market, the pad under the ship, which for a carrier IS its callsign).
 * Every caller that has to act on "our carrier" needs the same lookup, and a squadron carrier written back
 * to the fleet carrier's row is a silent swap of one ship for the other.
 */
public final class OurCarriers {

    private OurCarriers() {
    }

    /**
     * One of the commander's carriers, with the way back to its own table.
     */
    public record Ours(CarrierDataDto data, Consumer<CarrierDataDto> save) {
        /**
         * Applies a change and files it. Read-change-write in one call, so no caller can do the first two
         * and forget the third.
         */
        public void update(Consumer<CarrierDataDto> change) {
            change.accept(data);
            save.accept(data);
        }
    }

    /**
     * The carrier bearing this station name, if either does.
     * <p>
     * By callsign, because a carrier's station name IS its callsign and the squadron carrier's
     * {@code StationType} is not in Frontier's published schema - see {@link OwnCarrierHold}.
     */
    public static Optional<Ours> byCallSign(String stationName) {
        if (stationName == null || stationName.isBlank()) return Optional.empty();
        String name = stationName.trim();
        return match(carrier -> name.equalsIgnoreCase(carrier.getCallSign()));
    }

    /**
     * The carrier with this id, if either has it. A carrier's id is also its MarketID.
     * <p>
     * Zero never matches: it is what an id reads as before the commander has opened that carrier's
     * management panel, so treating it as a value would make every unidentified carrier the same carrier.
     */
    public static Optional<Ours> byId(long carrierId) {
        if (carrierId == 0) return Optional.empty();
        return match(carrier -> carrier.getCarrierId() == carrierId);
    }

    private static Optional<Ours> match(java.util.function.Predicate<CarrierDataDto> test) {
        CarrierDataDto fleet = FleetCarrierManager.getInstance().get();
        if (fleet != null && test.test(fleet)) {
            return Optional.of(new Ours(fleet, FleetCarrierManager.getInstance()::save));
        }
        CarrierDataDto squadron = SquadronCarrierManager.getInstance().get();
        if (squadron != null && test.test(squadron)) {
            return Optional.of(new Ours(squadron, SquadronCarrierManager.getInstance()::save));
        }
        return Optional.empty();
    }
}
