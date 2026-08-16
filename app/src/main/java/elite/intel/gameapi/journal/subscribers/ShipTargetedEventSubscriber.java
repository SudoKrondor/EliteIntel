package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.brain.vega.CompanionRuntime;
import elite.intel.ai.mouth.EventNarrator;
import elite.intel.db.managers.MissionManager;
import elite.intel.gameapi.journal.events.ShipTargetedEvent;
import elite.intel.session.PlayerSession;
import elite.intel.util.Md5Utils;
import elite.intel.util.Ranks;
import elite.intel.util.TTSFriendlyNumberConverter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Set;

import static elite.intel.util.StringUtls.localizedEvent;

public class ShipTargetedEventSubscriber {

    /**
     * The combat rank names ShipTargeted reports in {@code PilotRank}, ordered so the list index is
     * the rank number {@link Ranks#getCombatRankMap()} is keyed by. The journal gives the target's
     * rank as a name while the rank map is numeric, so this is the bridge between the two.
     */
    private static final List<String> PILOT_COMBAT_RANKS = List.of(
            "Harmless", "Mostly Harmless", "Novice", "Competent", "Expert", "Master",
            "Dangerous", "Deadly", "Elite", "Elite I", "Elite II", "Elite III", "Elite IV", "Elite V"
    );

    private final Logger log = LogManager.getLogger(ShipTargetedEventSubscriber.class);
    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final MissionManager missionManager = MissionManager.getInstance();

    @Subscribe public void onShipTargetedEvent(ShipTargetedEvent event) {

        log.debug(event.toJson());

        if (!event.isTargetLocked() && playerSession.isRadarContactAnnouncementOn()) {
            CompanionRuntime.narrator().announce(localizedEvent("event.target.contactLost"), true);
        }

        String pilotRankLocalized = localizedCombatRank(event.getPilotRank());

        String legalStatus = event.getLegalStatus() == null ? null : event.getLegalStatus().toLowerCase();
        int bounty = event.getBounty();
        String missionTargetOrNull = isMissionTargetOrNull(event);

        float shieldHealth = event.getShieldHealth();
        float hullHealth = event.getHullHealth();
        StringBuilder info = new StringBuilder();
        if (announceScan(event, legalStatus, missionTargetOrNull)) {

            String contactType = missionTargetOrNull.trim().equals("Mission Target")
                    ? localizedEvent("event.target.missionTarget")
                    : localizedEvent("event.target.legalTarget");
            info.append(localizedEvent("event.target.contact", contactType));

            info.append(pilotRankLocalized == null ? localizedEvent("event.target.rankUnknown") : pilotRankLocalized);
            info.append(", ");

            info.append(legalStatus == null ? localizedEvent("event.target.legalStatusUnknown") : Ranks.getLocalizedLegalStatus(event.getLegalStatus()));
            info.append(", ");

            info.append(bounty == 0 ? localizedEvent("event.target.noBounty") : localizedEvent("event.target.bounty", TTSFriendlyNumberConverter.formatBountyForSpeech(bounty)));
            info.append(", ");

            if (shieldHealth != 100 || hullHealth != 100) {
                if (shieldHealth == 0) {
                    info.append(localizedEvent("event.target.shieldsOffline"));
                } else if (shieldHealth < 50) {
                    info.append(", ");
                    info.append(localizedEvent("event.target.shields", String.format("%.0f", shieldHealth)));
                }

                info.append(", ");
                if (hullHealth < 50) {
                    info.append(", ");
                    info.append(localizedEvent("event.target.hull", String.format("%.0f", hullHealth)));
                }
            }
            String data = buildCanonicalShipString(event);
            String key = Md5Utils.generateMd5(data);
            if (playerSession.getShipScan(key) == null || playerSession.getShipScan(key).isEmpty()) {
                //new scan
                playerSession.putShipScan(key, data);
                EventNarrator.critical(info.toString());
            }
        }
    }


    /**
     * Translates the target's combat rank name into the active language, or returns {@code null}
     * when the journal reported no rank or one we do not know, so the caller can fall back to
     * "rank unknown" rather than announce a wrong rank.
     */
    static String localizedCombatRank(String pilotRank) {
        if (pilotRank == null || pilotRank.isBlank()) return null;
        int rankNumber = PILOT_COMBAT_RANKS.indexOf(pilotRank.trim());
        if (rankNumber < 0) return null;
        return Ranks.getCombatRankMap().get(rankNumber);
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
        // Bounty is only populated at the final scan stage (3); announcing earlier reports a real
        // bounty as "no bounty" and the dedup then suppresses the corrected stage-3 announcement.
        if (event.getScanStage() < 3) return false;
        return "wanted".equalsIgnoreCase(legalStatus);
    }
}
