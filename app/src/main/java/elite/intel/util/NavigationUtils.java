package elite.intel.util;

import elite.intel.gameapi.gamestate.status_events.PlayerMovedEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static elite.intel.util.StringUtls.localizedEvent;

/**
 * The NavigationUtils class provides a set of utility methods for calculating navigation-related data.
 * These include determining directions, distances, speeds, angles, and bearings between geographical
 * locations or points in a three-dimensional space. The class is designed to handle computations
 * with respect to planetary and galactic contexts, using various mathematical and geospatial formulas.
 *
 * The class leverages both public methods for external use and private methods for internal calculations.
 * It is primarily focused on providing precise and formatted navigation information based on
 * input parameters such as coordinates, altitude, and context-specific attributes.
 *
 * Fields:
 * - log: Used for logging purposes within the class.
 * - lastMoveTime: Tracks the timestamp of the last recorded movement.
 * - lastLatitude: Stores the latitude of the user's last location.
 * - lastLongitude: Stores the longitude of the user's last location.
 * - lastValidHeading: Retains the last valid heading value for reference.
 *
 * Methods:
 * - getDirections: Computes navigation directions to a target location.
 * - calculateRhumbBearing: Calculates the rhumb bearing to a target location.
 * - calculateSpeed: Determines speed based on position changes over time.
 * - calculateHeading: Computes heading and distance using geospatial formulas.
 * - calculateGalacticDistance: Calculates the distance between two points in 3D space.
 * - calculateSurfaceDistance: Calculates surface distance on a sphere or spheroid.
 * - calculateGlideAngle: Computes the glide angle required to reach a target.
 * - formatDistance: Formats a given distance for display purposes.
 *
 * This class operates within the constraints of assumed coordinate systems and unit consistency.
 */
public class NavigationUtils {

    private static final Logger log = LogManager.getLogger(NavigationUtils.class);

    private static long lastMoveTime = 0;
    private static double lastLatitude = -1;
    private static double lastLongitude = -1;
    private static int lastValidHeading = 0; // Persist last valid heading

    /**
     * Computes and returns navigation directions to a target location based on the player's current position,
     * movement attributes, and the planetary context. It calculates the heading, distance, speed, and altitude,
     * and formats a detailed direction description.
     *
     * @param targetLatitude the latitude of the target location in decimal degrees
     * @param targetLongitude the longitude of the target location in decimal degrees
     * @param event an instance of PlayerMovedEvent, which contains the player's current position, altitude, and other movement data
     * @return a Direction object containing the bearing to the target, distance to the target, user heading,
     *         speed, altitude, and a descriptive direction message
     */
    public static Direction getDirections(double targetLatitude, double targetLongitude, PlayerMovedEvent event) {

        Result resultForDirections = calculateHeading(targetLatitude, targetLongitude, event.getLatitude(), event.getLongitude(), event.getPlanetRadius(), event.getAltitude());
        int bearingToTarget = calculateRhumbBearing(event.getLatitude(), event.getLongitude(), targetLatitude, targetLongitude);
        Result resultForCurrentPosition = calculateHeading(event.getLatitude(), event.getLongitude(), lastLatitude, lastLongitude, event.getPlanetRadius(), event.getAltitude());
        double speed = calculateSpeed(event.getLatitude(), event.getLongitude(), event.getPlanetRadius(), event.getAltitude());
        int userHeading = resultForCurrentPosition.roundedBearing() != 0 ? resultForCurrentPosition.roundedBearing() : lastValidHeading;

        // Update last valid heading
        if (resultForCurrentPosition.roundedBearing() != 0) {
            lastValidHeading = resultForCurrentPosition.roundedBearing();
        }

        log.info("Nav Debug: Rhumb bearing=" + bearingToTarget +
                ", Great-circle bearing=" + resultForDirections.roundedBearing() +
                ", Distance=" + formatDistance(resultForDirections.distanceInMeters()) +
                ", Altitude=" + formatDistance(event.getAltitude()) +
                ", UserHeading=" + userHeading +
                ", LastLat=" + lastLatitude + ", LastLon=" + lastLongitude +
                ", CurrLat=" + event.getLatitude() + ", CurrLon=" + event.getLongitude());

        return new Direction(
                bearingToTarget,
                resultForDirections.distanceInMeters(),
                userHeading,
                Math.round(speed),
                event.getAltitude(),
                localizedEvent("event.nav.headingDistance", bearingToTarget, formatDistance(resultForDirections.distanceInMeters()))
        );
    }

    /**
     * Calculates the rhumb bearing from the user's current location to a target location.
     * The rhumb bearing is the constant compass direction between two points on the earth's surface.
     * If a mathematical error occurs during the calculation, the method calculates the regular heading as a fallback.
     *
     * @param userLatitude the latitude of the user's location in decimal degrees
     * @param userLongitude the longitude of the user's location in decimal degrees
     * @param targetLatitude the latitude of the target location in decimal degrees
     * @param targetLongitude the longitude of the target location in decimal degrees
     * @return the rhumb bearing in degrees, rounded to the nearest integer
     */
    private static int calculateRhumbBearing(double userLatitude, double userLongitude, double targetLatitude, double targetLongitude) {
        double lat1 = Math.toRadians(userLatitude);
        double lon1 = Math.toRadians(userLongitude);
        double lat2 = Math.toRadians(targetLatitude);
        double lon2 = Math.toRadians(targetLongitude);
        double deltaLon = lon2 - lon1;
        double deltaPhi;
        try {
            deltaPhi = Math.log(Math.tan(Math.PI / 4 + lat2 / 2) / Math.tan(Math.PI / 4 + lat1 / 2));
        } catch (ArithmeticException e) {
            return calculateHeading(userLatitude, userLongitude, targetLatitude, targetLongitude, 0, 0).roundedBearing();
        }
        double bearing = Math.atan2(deltaLon, deltaPhi);
        double bearingDegrees = Math.toDegrees(bearing);
        bearingDegrees = (bearingDegrees + 360) % 360;
        return (int) Math.round(bearingDegrees);
    }

    /**
     * Calculates the speed of movement based on the change in position and time elapsed
     * since the last recorded movement. It uses the user's current latitude, longitude,
     * and altitude along with the radius of the planet to compute the distance traveled
     * and calculates the speed accordingly.
     *
     * @param userLatitude the current latitude of the user in decimal degrees
     * @param userLongitude the current longitude of the user in decimal degrees
     * @param planetRadius the radius of the planet in units (e.g., meters, kilometers, etc.)
     * @param altitude the current altitude above the planet's surface in the same units as the radius
     * @return the calculated speed in the same units as the radius per second
     */
    private static double calculateSpeed(double userLatitude, double userLongitude, double planetRadius, double altitude) {
        long now = System.currentTimeMillis();
        double speed = 0;
        if (lastMoveTime > 0 && lastLatitude != -1 && lastLongitude != -1) {
            double deltaTimeSec = (now - lastMoveTime) / 1000.0;
            double deltaDist = calculateSurfaceDistance(
                    lastLatitude,
                    lastLongitude,
                    userLatitude,
                    userLongitude,
                    planetRadius,
                    altitude
            );
            if (deltaTimeSec > 0) {
                speed = deltaDist / deltaTimeSec;
            }
        }
        lastMoveTime = now;
        lastLatitude = userLatitude;
        lastLongitude = userLongitude;
        return speed;
    }

    /**
     * Calculates the heading and surface distance to a target location from a user's current location.
     * The method computes the heading using the initial bearing formula and surface distance
     * using the Haversine formula, taking into account the planet's radius and altitude.
     *
     * @param targetLatitude the latitude of the target location in decimal degrees
     * @param targetLongitude the longitude of the target location in decimal degrees
     * @param userLatitude the latitude of the user's location in decimal degrees
     * @param userLongitude the longitude of the user's location in decimal degrees
     * @param planetRadius the radius of the planet or sphere in units (e.g., meters, kilometers, etc.)
     * @param altitude the altitude above the planet's surface in the same units as the radius
     * @return a Result object containing the rounded heading in degrees and the surface distance to the target in the same units as the radius
     */
    private static Result calculateHeading(double targetLatitude, double targetLongitude, double userLatitude, double userLongitude, double planetRadius, double altitude) {
        double lat1 = Math.toRadians(userLatitude);
        double lon1 = Math.toRadians(userLongitude);
        double lat2 = Math.toRadians(targetLatitude);
        double lon2 = Math.toRadians(targetLongitude);
        double deltaLon = lon2 - lon1;
        double y = Math.sin(deltaLon) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(deltaLon);
        double bearing = Math.atan2(y, x);
        double bearingDegrees = Math.toDegrees(bearing);
        bearingDegrees = (bearingDegrees + 360) % 360;
        int roundedBearing = (int) Math.round(bearingDegrees);
        // Distance to a surface target is always the arc on the surface (radius only).
        // Using (radius + altitude) would inflate distances at orbital height and break all angle math.
        return new Result(roundedBearing, calculateSurfaceDistance(targetLatitude, targetLongitude, userLatitude, userLongitude, planetRadius, 0));
    }

    /**
     * Calculates the galactic distance between two points in a three-dimensional galactic coordinate system.
     * The calculation is based on the Euclidean distance formula.
     *
     * @param x1 the x-coordinate of the first point
     * @param y1 the y-coordinate of the first point
     * @param z1 the z-coordinate of the first point
     * @param x2 the x-coordinate of the second point
     * @param y2 the y-coordinate of the second point
     * @param z2 the z-coordinate of the second point
     * @return the calculated galactic distance between the two points
     */
    public static double calculateGalacticDistance(double x1, double y1, double z1, double x2, double y2, double z2) {
        double deltaX = x2 - x1;
        double deltaY = y2 - y1;
        double deltaZ = z2 - z1;
        return Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
    }

    /**
     * Calculates the surface distance between two points on a sphere or spheroid, given their latitudes, longitudes,
     * and an optional altitude above the specified sphere's radius. The calculation uses the Haversine formula
     * to account for the curvature of the sphere.
     *
     * @param lat1 the latitude of the first point in decimal degrees
     * @param lon1 the longitude of the first point in decimal degrees
     * @param lat2 the latitude of the second point in decimal degrees
     * @param lon2 the longitude of the second point in decimal degrees
     * @param radius the radius of the sphere in units (e.g., meters, kilometers, etc.)
     * @param altitude the altitude above the surface of the sphere in the same units as the radius
     * @return the calculated surface distance between the two points, accounting for the altitude, in the same units as the radius
     */
    public static double calculateSurfaceDistance(double lat1, double lon1, double lat2, double lon2, double radius, double altitude) {
        double lat1Rad = Math.toRadians(lat1);
        double lon1Rad = Math.toRadians(lon1);
        double lat2Rad = Math.toRadians(lat2);
        double lon2Rad = Math.toRadians(lon2);
        double deltaLat = lat2Rad - lat1Rad;
        double deltaLon = lon2Rad - lon1Rad;
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) + Math.cos(lat1Rad) * Math.cos(lat2Rad) * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return (radius + altitude) * c;
    }

    /**
     * Calculates the glide angle required to reach a target based on the given altitude and distance to the target.
     * The glide angle is computed as the arctangent of the ratio between altitude and distance, and the result is
     * converted to degrees.
     *
     * @param altitude the current altitude above the target in units, must be a positive value
     * @param distanceToTarget the horizontal distance to the target in the same units, must be a positive value
     * @return the glide angle in degrees, rounded to the nearest integer; returns 0 if the input values are invalid
     */
    public static int calculateGlideAngle(double altitude, double distanceToTarget) {
        // Check for invalid inputs (non-positive altitude or distance)
        if (altitude <= 0 || distanceToTarget <= 0) {
            return 0;
        }

        // Calculate the angle using arctangent (atan) and convert radians to degrees
        double angleInRadians = Math.atan(altitude / distanceToTarget);
        double angleInDegrees = Math.toDegrees(angleInRadians);

        // Round to nearest integer and return
        return (int) Math.round(angleInDegrees);
    }



    public static String formatDistance(double meters) {
        double km = meters / 1000;
        if (meters >= 10000) {
            return localizedEvent("event.nav.km", (int) km);
        } else if (meters >= 1000) {
            return localizedEvent("event.nav.km", String.format("%.1f", km));
        } else {
            return localizedEvent("event.nav.meters", (int) meters);
        }
    }

    private record Result(int roundedBearing, double distanceInMeters) {
    }

    public record Direction(int bearingToTarget, double distanceToTarget, int userHeading, double getSpeed, double altitude, String vocalization) {
        public String toString() {
            return "Bearing: " + bearingToTarget +
                    " Heading: " + userHeading +
                    " Distance: " + formatDistance(distanceToTarget) +
                    " Speed: " + getSpeed +
                    " Altitude: " + formatDistance(altitude);
        }
    }
}