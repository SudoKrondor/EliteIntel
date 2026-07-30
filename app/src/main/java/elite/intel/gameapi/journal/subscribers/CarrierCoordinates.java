package elite.intel.gameapi.journal.subscribers;

import elite.intel.gameapi.journal.events.dto.CarrierDataDto;
import elite.intel.util.SystemAddressCoordinates;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Writes a carrier's position, and holds the one rule both arrival paths obey: the coordinates on
 * file always belong to the system named alongside them.
 *
 * <p>WHY shared by the live subscriber and the pre-scan: they resolve a position from different
 * sources (the pre-scan reaches for no network) but they must agree on what counts as a position and
 * on never leaving the previous system's coordinates behind, which is what made the distance query
 * answer confidently for a system the carrier had left.
 */
final class CarrierCoordinates {

    private static final Logger log = LogManager.getLogger(CarrierCoordinates.class);

    private CarrierCoordinates() {
        // static writer for a single invariant.
    }

    static void apply(CarrierDataDto carrierData, double x, double y, double z) {
        carrierData.setX(x);
        carrierData.setY(y);
        carrierData.setZ(z);
    }

    static void clear(CarrierDataDto carrierData) {
        apply(carrierData, 0, 0, 0);
    }

    /**
     * Positions the carrier from the boxel its SystemAddress names, the last resort that needs no
     * network and so is the only source that answers in uncharted space.
     *
     * @return false when there is no address to decode, leaving the carrier untouched.
     */
    static boolean applyBoxelCentre(CarrierDataDto carrierData, Long systemAddress) {
        return SystemAddressCoordinates.decode(systemAddress).map(centre -> {
            log.debug("No exact coordinates for {}; using its boxel centre, accurate to {} ly",
                    carrierData.getStarName(), centre.maxErrorLy());
            apply(carrierData, centre.x(), centre.y(), centre.z());
            return true;
        }).orElse(false);
    }

    /**
     * Whether the coordinates on file were resolved for this very system, and so must not be replaced
     * by a coarser estimate of it.
     */
    static boolean alreadyResolvedFor(CarrierDataDto carrierData, String starSystem) {
        boolean hasCoordinates = carrierData.getX() != 0 || carrierData.getY() != 0 || carrierData.getZ() != 0;
        return hasCoordinates && starSystem != null && starSystem.equalsIgnoreCase(carrierData.getStarName());
    }
}
