package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.ai.mouth.subscribers.events.RadarContactAnnouncementEvent;
import elite.intel.db.managers.MissionManager;
import elite.intel.gameapi.EventBusManager;
import elite.intel.gameapi.journal.events.ShipTargetedEvent;
import elite.intel.session.PlayerSession;
import elite.intel.util.Md5Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Locale;
import java.util.Set;

import static elite.intel.util.StringUtls.localizedEvent;

// Instantiated and invoked through SubscriberRegistration and Guava EventBus reflection.
@SuppressWarnings("unused")
public class ShipTargetedEventSubscriber {

    private static final int[] HULL_ALERT_THRESHOLDS = {75, 50, 25};

    private final Logger log = LogManager.getLogger(ShipTargetedEventSubscriber.class);
    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final MissionManager missionManager = MissionManager.getInstance();
    private String trackedCombatTarget;
    private float previousShieldHealth = Float.NaN;
    private float previousHullHealth = Float.NaN;

    @Subscribe public void onShipTargetedEvent(ShipTargetedEvent event) {

        log.debug(event.toJson());

        if (!event.isTargetLocked()) {
            resetCombatTracking();
            EventBusManager.publish(new RadarContactAnnouncementEvent(localizedEvent("event.target.contactLost")));
            return;
        }

        String pilotRank = event.getPilotRank();
        String legalStatus = event.getLegalStatus() == null ? null : event.getLegalStatus().toLowerCase();
        int bounty = event.getBounty();
        String missionTargetOrNull = isMissionTargetOrNull(event);

        announceCombatStatus(event);

        if (announceScan(event, legalStatus, missionTargetOrNull)) {
            String info = localizedEvent(
                    "event.target.scanSummary",
                    localizeLegalStatus(legalStatus),
                    localizePilotRank(pilotRank),
                    Integer.toString(bounty),
                    firstAvailable(event.getPilotNameLocalised(), event.getPilotName()),
                    firstAvailable(event.getShipLocalised(), event.getShip())
            );

            String data = buildCanonicalShipString(event);
            String key = Md5Utils.generateMd5(data);
            if (playerSession.getShipScan(key) == null || playerSession.getShipScan(key).isEmpty()) {
                //new scan
                playerSession.putShipScan(key, data);
                EventBusManager.publish(new MissionCriticalAnnouncementEvent(info));
            }
        }
    }

    private void announceCombatStatus(ShipTargetedEvent event) {
        // Incomplete scans report zero health values, so only trust full-scan telemetry.
        if (event.getScanStage() < 3) return;

        String targetKey = buildCombatTargetKey(event);
        float shieldHealth = event.getShieldHealth();
        float hullHealth = event.getHullHealth();

        if (!targetKey.equals(trackedCombatTarget)) {
            trackedCombatTarget = targetKey;
            previousShieldHealth = shieldHealth;
            previousHullHealth = hullHealth;
            return;
        }

        if (previousShieldHealth > 0f && shieldHealth <= 0f) {
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(
                    localizedEvent("event.target.shieldsOffline")
            ));
        }

        for (int threshold : HULL_ALERT_THRESHOLDS) {
            if (previousHullHealth > threshold && hullHealth <= threshold) {
                EventBusManager.publish(new MissionCriticalAnnouncementEvent(
                        localizedEvent("event.target.hull", Math.round(hullHealth))
                ));
                break;
            }
        }

        previousShieldHealth = shieldHealth;
        previousHullHealth = hullHealth;
    }

    private String buildCombatTargetKey(ShipTargetedEvent event) {
        return firstAvailable(event.getPilotNameLocalised(), event.getPilotName())
                + "|" + firstAvailable(event.getShipLocalised(), event.getShip());
    }

    private void resetCombatTracking() {
        trackedCombatTarget = null;
        previousShieldHealth = Float.NaN;
        previousHullHealth = Float.NaN;
    }

    private String localizePilotRank(String rank) {
        if (rank == null || rank.isBlank()) return localizedEvent("event.target.rankUnknown");

        String key = switch (normalizeJournalValue(rank)) {
            case "harmless" -> "event.target.rank.harmless";
            case "mostlyharmless" -> "event.target.rank.mostlyHarmless";
            case "novice" -> "event.target.rank.novice";
            case "competent" -> "event.target.rank.competent";
            case "expert" -> "event.target.rank.expert";
            case "master" -> "event.target.rank.master";
            case "dangerous" -> "event.target.rank.dangerous";
            case "deadly" -> "event.target.rank.deadly";
            case "elite" -> "event.target.rank.elite";
            default -> null;
        };
        return key == null ? readableJournalValue(rank) : localizedEvent(key);
    }

    private String localizeLegalStatus(String status) {
        if (status == null || status.isBlank()) return localizedEvent("event.target.legalStatusUnknown");

        String key = switch (normalizeJournalValue(status)) {
            case "clean" -> "event.target.legal.clean";
            case "wanted" -> "event.target.legal.wanted";
            case "lawless" -> "event.target.legal.lawless";
            default -> null;
        };
        return key == null ? readableJournalValue(status) : localizedEvent(key);
    }

    private String normalizeJournalValue(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private String readableJournalValue(String value) {
        return value.replace('_', ' ').trim();
    }

    private String firstAvailable(String localizedValue, String rawValue) {
        if (localizedValue != null && !localizedValue.isBlank()) return localizedValue;
        if (rawValue != null && !rawValue.isBlank()) return readableJournalValue(rawValue);
        return "";
    }


    private String buildCanonicalShipString(ShipTargetedEvent event) {
        String pilot = event.getPilotNameLocalised();
        String shipType = event.getShipLocalised();
        String faction = event.getFaction();
        String legalStatus = event.getLegalStatus();
        return pilot + "|" + shipType + "|" + faction + "|" + legalStatus;
    }

    private String isMissionTargetOrNull(ShipTargetedEvent event) {
        String faction = event.getFaction();
        String legalStatus = event.getLegalStatus();
        if (faction == null || faction.isBlank()) return null;
        if (legalStatus == null || legalStatus.isBlank()) return null;

        Set<String> targetFactions = missionManager.getTargetFactions(
                missionManager.getPirateMissionTypes()
        );
        if (!targetFactions.isEmpty() && targetFactions.contains(faction)) {
            return " Mission Target ";
        }

        if (legalStatus.equalsIgnoreCase("wanted")) {
            return " Legal Target ";
        } else return null;
    }

    private boolean announceScan(ShipTargetedEvent event, String legalStatus, String missionTarget) {
        if (missionTarget == null || missionTarget.isBlank()) return false;
        if (legalStatus == null) return false;
        if (event == null) return false;
        if (legalStatus.isBlank()) return false;
        if ("clean".equalsIgnoreCase(legalStatus)) return false;
        if (event.getScanStage() == 0) return false;
        return "wanted".equalsIgnoreCase(legalStatus);
    }
}
