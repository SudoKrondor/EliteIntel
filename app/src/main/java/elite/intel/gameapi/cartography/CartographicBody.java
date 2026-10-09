package elite.intel.gameapi.cartography;

/**
 * One body's cartographic data as Universal Cartographics will price it: what the scan said about the body,
 * and what the commander has done to it since.
 *
 * @param primaryStar   the star the ship arrived at, which carries the system's honk bonus
 * @param starType      the journal's {@code StarType}, null on a planet
 * @param stellarMass   solar masses, stars only
 * @param planetClass   the journal's {@code PlanetClass}, null on a star
 * @param massEM        Earth masses, planets only
 * @param wasDiscovered the scan's {@code WasDiscovered}: false pays the first discoverer's multiplier
 * @param wasMapped     the scan's {@code WasMapped}: false makes the commander the first mapper
 * @param mapped        the commander has surface-mapped it since
 * @param efficient     the mapping met the probe target
 */
public record CartographicBody(String bodyName,
                               boolean primaryStar,
                               String starType,
                               double stellarMass,
                               String planetClass,
                               boolean terraformable,
                               double massEM,
                               boolean wasDiscovered,
                               boolean wasMapped,
                               boolean mapped,
                               boolean efficient) {

    public boolean isStar() {
        return starType != null && !starType.isBlank();
    }

    /**
     * The same body, surface-mapped.
     */
    public CartographicBody withMapping(boolean efficient) {
        return new CartographicBody(bodyName, primaryStar, starType, stellarMass, planetClass, terraformable,
                massEM, wasDiscovered, wasMapped, true, efficient);
    }
}
