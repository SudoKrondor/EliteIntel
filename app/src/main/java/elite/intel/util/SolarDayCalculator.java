package elite.intel.util;

import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.journal.events.dto.LocationDto;

/**
 * Single owner of a body's solar (apparent) day length in seconds, derived from its rotation and orbital periods and
 * its tidal-lock state. Encapsulates the non-obvious astronomy so every call site shares one answer:
 * <ul>
 *     <li>a tidally locked body sees its sidereal rotation as its day;</li>
 *     <li>a moon tidally locked to its planet takes the planet's orbital period around the star, not its own;</li>
 *     <li>retrograde rotation shortens the apparent day, prograde lengthens it.</li>
 * </ul>
 * Returns {@code 0} when the inputs are missing or the result is not physically meaningful, so call sites present it
 * as "unknown". Both {@code query_current_location} and the companion body-fact source read through here.
 */
public final class SolarDayCalculator {

    /** Seconds in one standard day, used to normalise rotation/orbital periods before combining them. */
    private static final double DAY = 86400.0;

    private SolarDayCalculator() {
    }

    /**
     * Solar day length in seconds for the given body, or {@code 0} when unknown. Resolves a locked moon's parent
     * planet through {@link LocationManager} so the moon reports the planet's year as its day.
     */
    public static double solarDaySeconds(LocationDto location) {
        // Moons tidally locked to a planet: their solar day equals the parent planet's orbital period around the star,
        // not the moon's own rotation/orbital period around the planet.
        if (location.isTidalLocked()
                && LocationDto.LocationType.MOON.equals(location.getLocationType())
                && location.getParentBodyId() > 0) {
            LocationDto parentPlanet = LocationManager.getInstance().getLocation(location.getStarName(), location.getParentBodyId());
            double parentOrbitalPeriod = parentPlanet == null ? 0 : parentPlanet.getOrbitalPeriod();
            if (parentOrbitalPeriod > 0) {
                return Math.abs(parentOrbitalPeriod);
            }
        }
        return solarDaySeconds(location.getRotationPeriod(), location.getOrbitalPeriod(), location.isTidalLocked());
    }

    /**
     * Pure computation of the solar day in seconds from the sidereal rotation period, the orbital period (both in
     * seconds) and the tidal-lock flag. Returns {@code 0} when the result would be unknown or absurd. Package-visible
     * for testing.
     */
    static double solarDaySeconds(double rotationPeriodSeconds, double orbitalPeriodSeconds, boolean tidalLocked) {
        if (tidalLocked) {
            // For tidal lock the solar day equals the sidereal day (rotation period).
            return Math.abs(rotationPeriodSeconds);
        }
        if (orbitalPeriodSeconds <= 0) {
            // No orbit data: fall back to the sidereal day.
            return Math.abs(rotationPeriodSeconds);
        }

        double siderealAbs = Math.abs(rotationPeriodSeconds);
        if (siderealAbs < 60) {
            return 0;
        }

        double siderealDays = siderealAbs / DAY;
        double orbitalDays = orbitalPeriodSeconds / DAY;

        double relativeSpeed;
        if (rotationPeriodSeconds < 0) {
            // Retrograde: the apparent day is shorter than the sidereal day.
            relativeSpeed = 1.0 / siderealDays + 1.0 / orbitalDays;
        } else {
            // Prograde.
            relativeSpeed = Math.abs(1.0 / orbitalDays - 1.0 / siderealDays);
        }

        if (relativeSpeed < 1e-9) {
            // Synchronous / near-lock: the apparent day is the sidereal day.
            return siderealAbs;
        }

        double solarSeconds = DAY / relativeSpeed;
        // Safety cap: guard against absurd values from a bad orbital period.
        if (solarSeconds > 1e10 || solarSeconds < 60) {
            return siderealAbs;
        }
        return solarSeconds;
    }
}
