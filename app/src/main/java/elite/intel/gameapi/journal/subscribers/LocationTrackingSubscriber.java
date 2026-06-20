package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.mouth.subscribers.events.NavigationVocalisationEvent;
import elite.intel.eventbus.GameEventBus;
import elite.intel.gameapi.gamestate.status_events.InGlideEvent;
import elite.intel.gameapi.gamestate.status_events.PlayerMovedEvent;
import elite.intel.gameapi.journal.events.DisembarkEvent;
import elite.intel.gameapi.journal.events.dto.TargetLocation;
import elite.intel.session.PlayerSession;
import elite.intel.session.Status;
import elite.intel.util.NavigationUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static elite.intel.util.NavigationUtils.calculateGlideAngle;
import static elite.intel.util.StringUtls.localizedEvent;

/**
 * The LocationTrackingSubscriber class is responsible for managing and processing
 * location tracking events based on a player's movement within the game environment.
 * It determines the player's positional and navigational states, handles transitions
 * between surface and orbital navigation, and manages announcements related to
 * tracking and movement.
 * <p>
 * This class includes methods to analyze player heading, altitude, and proximity to
 * various destinations, ensuring smooth user experience and timely notifications.
 * It supports glide path calculations, state transitions, and threshold-based
 * announcement systems.
 */
public class LocationTrackingSubscriber {

    public static final int MAX_NORAMAL_SAPCE_SPEED = 700;
    public static final int APPROXIMATE_DRP_ALTITUDE = 30_000;
    private static final Logger log = LogManager.getLogger(LocationTrackingSubscriber.class);
    private static final double HYSTERESIS = 7;
    private static final double ARRIVAL_RADIUS = 50;
    // 100 km is the atmospheric commit altitude - do not descend below this until ready to burn in.
    // We target being ~600 km (surface distance) from the destination when we hit that altitude.
    // Ideal descent initiation: 1000–2000 km from target at 200–300 km altitude.
    private static final double GLIDE_ENTRY_ALTITUDE_M = 50_000;
    private static final double GLIDE_ENTRY_TARGET_DISTANCE_M = 600_000;
    private final ExecutorService navExecutor = Executors.newSingleThreadExecutor(
            Thread.ofVirtual().name("nav-worker").factory());
    private final PlayerSession playerSession = PlayerSession.getInstance();
    private boolean hasAnnouncedOrbital = false;
    private boolean hasAnnouncedSurface = false;
    // TTS latency + journal tick delay: project forward to compensate for speech synthesis delay.
    // Cap the lookahead distance so high-speed supercruise entry doesn't exaggerate angles.
    private static final double LOOKAHEAD_SECONDS = 5.0;
    private static final double MAX_LOOKAHEAD_DISTANCE_M = 50_000; // 50 km cap regardless of speed
    private TargetLocation lastTracking = null;
    private double lastDistance = -1;
    private double lastAltitude = -1;
    private long lastAnnounceTime = -1;
    private boolean lookForLandingSpotAnnounced = false;
    private boolean isGliding = false;
    private boolean beginDescentCued = false; // true once "begin descent" has been announced at low priority
    private boolean levelOffCued = false;     // true once "level off" has been announced - stays silent until pilot actually levels off

    /**
     * Handles the event triggered when a player moves within the game environment.
     * This method processes the player's movement and navigational state
     * based on their proximity, heading, altitude, and speed relative to a target location.
     * It manages surface and orbital navigation, throttles announcements to avoid excessive updates,
     * and ensures proper state transitions for tracking logic.
     *
     * @param event An instance of {@code PlayerMovedEvent} containing the player's current
     *              latitude, longitude, altitude, and planetary radius data.
     */
    @Subscribe
    public void onPlayerMoved(PlayerMovedEvent event) {
        navExecutor.submit(() -> {
            TargetLocation targetLocation = playerSession.getTracking();
            if (targetLocation == null || !targetLocation.isEnabled()) {
                resetTrackingState();
                return;
            }

            if (!targetLocation.equals(lastTracking) && lastTracking != null) {
                resetTrackingState();
                lastTracking = targetLocation;
            }

            NavigationUtils.Direction navigator = NavigationUtils.getDirections(
                    targetLocation.getLatitude(),
                    targetLocation.getLongitude(),
                    event
            );

            if (navigator.distanceToTarget() == 0 && navigator.altitude() == 0) {
                // we are not on the planet and not in orbit
                log.info("Navigation ON, but not on planet and not in orbit. Skipping navigation.");
                return;
            } else {
                log.info(navigator.toString());
            }

            if (Status.getInstance().isGlideMode()) {
                glideNavigation(navigator, event);
            } else if (isInOrbit(event)) {
                orbitalNavigation(navigator, event);
            } else {
                surfaceNavigation(navigator, event);
            }

            lastDistance = navigator.distanceToTarget();
            lastAltitude = event.getAltitude();
        });
    }

    private boolean isOnSurface(PlayerMovedEvent event) {
        return event.getAltitude() < 2;
    }

    private boolean isInOrbit(PlayerMovedEvent event) {
        return event.getAltitude() > APPROXIMATE_DRP_ALTITUDE;
    }

    /**
     * Handles the navigation of a player in orbital flight. This method ensures
     * the player's movement and trajectory adhere to specific navigational parameters
     * based on their altitude, distance to target, glide angle, and heading. It provides
     * relevant announcements and corrections to maintain a safe and efficient orbital path.
     *
     * @param navigator An instance of {@code NavigationUtils.Direction} representing
     *                  the navigation details such as bearing and distance to the target.
     * @param event     An instance of {@code PlayerMovedEvent} containing the player's
     *                  current position data including altitude and coordinates.
     */
    private void orbitalNavigation(NavigationUtils.Direction navigator, PlayerMovedEvent event) {
        double distanceToTarget = navigator.distanceToTarget();
        double altitude = event.getAltitude();
        double planetRadius = event.getPlanetRadius();
        double speed = navigator.getSpeed();

        if (!hasAnnouncedOrbital) {
            vocalize(localizedEvent("event.nav.orbital") + " " + NavigationUtils.formatDistance(distanceToTarget) + bearingLabel(navigator.bearingToTarget()), 0, 0, true);
            hasAnnouncedOrbital = true;
        }

        double commitDist = commitDistance(planetRadius);

        // Project position forward to compensate for TTS latency (~5 s at orbital speed).
        // Cap lookahead distance at 50 km so supercruise/fast-descent speeds don't exaggerate angles.
        // Never project below commit distance.
        double projectedDistance = distanceToTarget;
        if (speed > MAX_NORAMAL_SAPCE_SPEED) {
            double lookaheadDist = Math.min(speed * LOOKAHEAD_SECONDS, MAX_LOOKAHEAD_DISTANCE_M);
            double candidate = distanceToTarget - lookaheadDist;
            if (candidate > commitDist) {
                projectedDistance = candidate;
            }
        }

        // Actual descent angle: computed from consecutive journal ticks.
        // Raw atan(Δalt/Δarc) overstates the angle at high altitude because surface arc grows
        // slower than 3D horizontal distance. Multiply by R/(R+h) to recover true nose pitch.
        double actualDescentAngle = 0;
        boolean hasActualAngle = false;
        if (lastAltitude > 0 && lastDistance > 0 && altitude < lastAltitude && distanceToTarget < lastDistance) {
            double deltaAlt = lastAltitude - altitude;           // m descended (positive)
            double deltaDist = lastDistance - distanceToTarget;   // m approached (positive)
            if (deltaDist > 0) {
                double scaleFactor = planetRadius / (planetRadius + altitude); // R/(R+h)
                actualDescentAngle = Math.toDegrees(Math.atan((deltaAlt / deltaDist) * scaleFactor));
                hasActualAngle = true;
            }
        }

        // Keep commit-point angle in log for reference
        double commitAngle = computeRequiredDescentAngle(altitude, distanceToTarget, planetRadius);

        int bearingToTarget = navigator.bearingToTarget();
        int userHeading = navigator.userHeading();

        String orbLog = String.format(
                "[ORB] alt=%.0fkm | dist=%.0fkm | proj=%.0fkm | commit=%.0fkm | speed=%.0fm/s | " +
                        "descent=%s | commit_angle=%.1f° | bearing=%d° | heading=%d°",
                altitude / 1000, distanceToTarget / 1000, projectedDistance / 1000,
                commitDist / 1000, speed,
                hasActualAngle ? String.format("%.1f°", actualDescentAngle) : "n/a",
                commitAngle, bearingToTarget, userHeading);
        log.info(orbLog);

        // Suppress descent guidance at supercruise speeds - still decelerating into orbit.
        if (speed > 50_000) {
            vocalize(NavigationUtils.formatDistance(projectedDistance) + bearingLabel(bearingToTarget), 0, 0, false);
            return;
        }

        // Inside commit distance: on final approach.
        if (projectedDistance <= commitDist) {
            vocalize(localizedEvent("event.nav.final") + " " + NavigationUtils.formatDistance(distanceToTarget) + bearingLabel(bearingToTarget), 0, 0, false);
            return;
        }

        // Level flight: -5 to +5° inclusive (|descent| < 6°) - orbital mechanics zone.
        boolean levelFlight = !hasActualAngle || actualDescentAngle < 6.0;

        // Altitude floor: pilot is below 100 km but still far from target.
        // Only fire "Level off" when truly descending, and only once - stay silent after.
        if (altitude < 100_000 && distanceToTarget > commitDist * 3) {
            if (levelFlight) {
                levelOffCued = false; // pilot levelled off - reset so we can warn again if they descend
                vocalize(NavigationUtils.formatDistance(projectedDistance) + bearingLabel(bearingToTarget), 0, 0, false);
            } else if (!levelOffCued) {
                vocalize(localizedEvent("event.nav.levelOff") + " " + NavigationUtils.formatDistance(distanceToTarget) + bearingLabel(bearingToTarget), 0, 0, true);
                levelOffCued = true;
            }
            // If levelOffCued && !levelFlight: already told them once - stay silent
            return;
        } else {
            levelOffCued = false;
        }

        if (levelFlight) {
            if (distanceToTarget < 1_000_000) {
                // Within 1000 km: give the geometric angle aimed directly at the target.
                double scaleFactor = planetRadius / (planetRadius + altitude);
                int geoAngle = (int) Math.round(Math.toDegrees(Math.atan((altitude / distanceToTarget) * scaleFactor)));
                boolean urgent = commitAngle > 20.0 || speed > 15_000;
                vocalize(localizedEvent("event.nav.pitchMinus", geoAngle) + " " + NavigationUtils.formatDistance(projectedDistance) + bearingLabel(bearingToTarget), 0, 0, urgent);
            } else if (commitAngle >= 5.0 && commitAngle <= 45.0) {
                // Comfortable descent window - cue the pilot with the required angle.
                boolean urgent = commitAngle > 20.0 || speed > 15_000;
                vocalize(localizedEvent("event.nav.pitchMinus", (int) Math.round(commitAngle)) + " " + NavigationUtils.formatDistance(projectedDistance) + bearingLabel(bearingToTarget), 0, 0, !beginDescentCued || urgent);
                beginDescentCued = true;
            } else if (commitAngle > 45.0 && altitude < 400_000) {
                // Too steep from here - orbit for a better pass.
                vocalize(localizedEvent("event.nav.circleForApproach") + " " + NavigationUtils.formatDistance(projectedDistance) + bearingLabel(bearingToTarget), 0, 0, false);
                beginDescentCued = false;
            } else {
                // Too far or still entering orbit.
                beginDescentCued = false;
                vocalize(NavigationUtils.formatDistance(projectedDistance) + bearingLabel(bearingToTarget), 0, 0, false);
            }
        } else {
            // Actively descending - confirm actual angle so pilot can compare with HUD.
            beginDescentCued = false;
            vocalize(localizedEvent("event.nav.pitchMinus", (int) Math.round(actualDescentAngle)) + " " + NavigationUtils.formatDistance(projectedDistance) + bearingLabel(bearingToTarget), 0, 0, false);
        }
    }

    /**
     * Monitors the atmospheric burn glide phase.
     * Glide state is managed by the game engine; pilot manages their own pitch.
     * Callouts: distance + bearing, plus a "maintain descent" warning if climbing
     * (pitching above ~-5° cancels glide mode).
     */
    private void glideNavigation(NavigationUtils.Direction navigator, PlayerMovedEvent event) {
        double distanceToTarget = navigator.distanceToTarget();
        double altitude = event.getAltitude();
        int slopeAngle = calculateGlideAngle(altitude, distanceToTarget);
        boolean climbing = lastAltitude > 0 && altitude > lastAltitude;

        String glideLog = String.format(
                "[GLIDE] alt=%.0fkm | dist=%.0fkm | slope=%.1f° | bearing=%d° | %s",
                altitude / 1000, distanceToTarget / 1000, (double) slopeAngle, navigator.bearingToTarget(),
                climbing ? "CLIMBING" : "ok");
        log.info(glideLog);

        if (climbing) {
            // Pitching up risks cancelling glide - warn immediately.
            vocalize(localizedEvent("event.nav.maintainDescent") + " " + NavigationUtils.formatDistance(distanceToTarget) + bearingLabel(navigator.bearingToTarget()), 0, 0, true);
        } else {
            vocalize(NavigationUtils.formatDistance(distanceToTarget) + bearingLabel(navigator.bearingToTarget()), 0, 0, false);
        }
        lastDistance = distanceToTarget;
    }

    private String bearingLabel(double bearing) {
        if (bearing <= 0) return "";
        return localizedEvent("event.nav.bearing", (int) bearing);
    }

    /**
     * Commit distance scales with planet radius so small moons don't hit TOGA immediately.
     * Factor 0.08 → 1500 km body ≈ 120 km commit → ~39 km exit at −25° burn.
     * Capped at GLIDE_ENTRY_TARGET_DISTANCE_M for very large planets.
     */
    private double commitDistance(double planetRadius) {
        return Math.min(planetRadius * 0.08, GLIDE_ENTRY_TARGET_DISTANCE_M);
    }

    /**
     * Normal approach: angle needed to reach the atmospheric commit point (100 km altitude)
     * at commitDistance(planetRadius) from the target.
     * Returns 90 if already inside commit distance, 0 if already below commit altitude.
     */
    private double computeRequiredDescentAngle(double altitude, double distanceToTarget, double planetRadius) {
        double availableHorizontal = distanceToTarget - commitDistance(planetRadius);
        if (availableHorizontal <= 0) return 90.0;
        double verticalDrop = altitude - GLIDE_ENTRY_ALTITUDE_M;
        if (verticalDrop <= 0) return 0.0;
        return Math.toDegrees(Math.atan(verticalDrop / availableHorizontal));
    }

    private boolean isGlideAngleOk(PlayerMovedEvent event, NavigationUtils.Direction navigator) {
        if (event.getAltitude() > 100_000) {
            return calculateGlideAngle(event.getAltitude(), navigator.distanceToTarget()) < 60;
        } else if (event.getAltitude() < 50_000) {
            return calculateGlideAngle(event.getAltitude(), navigator.distanceToTarget()) < 45;
        } else {
            return calculateGlideAngle(event.getAltitude(), navigator.distanceToTarget()) < 36;
        }
    }

    /**
     * Handles the surface navigation of a player based on their movement and proximity to
     * a target location. It evaluates various factors including speed, heading deviation,
     * glide angle, and distance to provide navigational feedback and updates. Additionally,
     * it determines whether the player is approaching or moving away from the target, and
     * announces relevant guidance or warnings.
     *
     * @param navigator An instance of {@code NavigationUtils.Direction} representing navigation
     *                  parameters including distance and bearing to the target.
     * @param event     An instance of {@code PlayerMovedEvent} containing details about the player's
     *                  current position, altitude, and other movement data.
     */
    private void surfaceNavigation(NavigationUtils.Direction navigator, PlayerMovedEvent event) {
        //Low altitude flights or Surface Recon Vehicle.
        if (!hasAnnouncedSurface) {
            vocalize(null, navigator.distanceToTarget(), navigator.bearingToTarget(), true);
            hasAnnouncedSurface = true;
            return;
        }

        boolean headingDeviation = isHeadingDeviation(navigator);
        boolean glideAngleOk = isGlideAngleOk(event, navigator);

        int glideAngle = calculateGlideAngle(event.getAltitude(), navigator.distanceToTarget());
        boolean movingAway = navigator.distanceToTarget() > lastDistance;

        if (isOnSurface(event)) {
            //CRAWLING on the surface (SRV or on foot)
            if (navigator.distanceToTarget() <= ARRIVAL_RADIUS && navigator.altitude() == 0) {
                vocalize(localizedEvent("event.nav.arrived"), 0, navigator.bearingToTarget(), true);
                TargetLocation t = playerSession.getTracking();
                t.setEnabled(false);
                playerSession.setTracking(t);
                resetTrackingState();
            } else if (headingDeviation) {
                vocalize(movingAway ? localizedEvent("event.nav.diverging") : localizedEvent("event.nav.converging"), navigator.distanceToTarget(), navigator.bearingToTarget(), false);
            } else {
                announceBearingAndDistances(navigator, movingAway ? localizedEvent("event.nav.diverging") : localizedEvent("event.nav.converging"));
            }
        } else {
            //FLYING in normal space above surface
            if (navigator.distanceToTarget() < 1_000 && !lookForLandingSpotAnnounced && event.getAltitude() > 10) {
                lookForLandingSpotAnnounced = true;
                vocalize(localizedEvent("event.nav.landingSpot"), 0, 0, true);
                if (movingAway) {
                    vocalize(localizedEvent("event.nav.diverging"), 0, 0, false);
                }
            } else {
                if (navigator.distanceToTarget() > 1500) {
                    lookForLandingSpotAnnounced = false;
                }

                if (headingDeviation) {
                    vocalize(movingAway ? localizedEvent("event.nav.diverging") : localizedEvent("event.nav.converging"), navigator.distanceToTarget(), navigator.bearingToTarget(), movingAway);
                } else if (event.getAltitude() > 3_000 && glideAngleOk) {
                    announceBearingAndDistances(navigator, movingAway ? localizedEvent("event.nav.diverging") : localizedEvent("event.nav.convergingGlide", glideAngle));
                } else {
                    announceBearingAndDistances(navigator, movingAway ? localizedEvent("event.nav.diverging") : localizedEvent("event.nav.converging"));
                }
            }
        }
    }

    private boolean isHeadingDeviation(NavigationUtils.Direction navigator) {
        return (Math.abs(navigator.bearingToTarget()) - navigator.userHeading()) > HYSTERESIS;
    }

    private boolean isAboveAnnouncementThreshold(boolean highPriority) {
        if (highPriority) {
            return System.currentTimeMillis() - lastAnnounceTime > 6_000;
        } else {
            return System.currentTimeMillis() - lastAnnounceTime > 12_000;
        }
    }

    private void announceBearingAndDistances(NavigationUtils.Direction navigator, String prefix) {
        vocalize(prefix, navigator.distanceToTarget(), navigator.bearingToTarget(), false);
    }

    private void resetTrackingState() {
        hasAnnouncedOrbital = false;
        hasAnnouncedSurface = false;
        lookForLandingSpotAnnounced = false;
        beginDescentCued = false;
        levelOffCued = false;
        playerSession.setTracking(new TargetLocation());
        lastTracking = null;
        lastDistance = -1;
        lastAltitude = -1;
        lastAnnounceTime = -1;
        isGliding = false;
    }

    private void vocalize(String text, double distance, double bearing, boolean highPriority) {
        if (this.isGliding) {
            return;
        }

        if (isAboveAnnouncementThreshold(highPriority)) {
            StringBuilder sb = new StringBuilder();
            if (text != null) sb.append(text);
            if (distance > 0) sb.append(NavigationUtils.formatDistance(distance));
            if (bearing > 0) sb.append(localizedEvent("event.nav.bearing", (int) bearing));
            log.info(sb.toString());
            GameEventBus.publish(new NavigationVocalisationEvent(sb.toString()));
            lastAnnounceTime = System.currentTimeMillis();
        } else {
            log.info("Not enough time passed to announce. and not high priority.");
        }
    }

    @Subscribe
    public void onDisembarkEvent(DisembarkEvent event) {
        resetTrackingState();
    }

    @Subscribe
    public void onInGlideEvent(InGlideEvent event) {
        this.isGliding = event.isGliding();
    }
}
