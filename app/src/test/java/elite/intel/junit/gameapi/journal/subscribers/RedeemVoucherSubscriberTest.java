package elite.intel.junit.gameapi.journal.subscribers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import elite.intel.db.managers.MissionManager;
import elite.intel.gameapi.journal.events.MissionAcceptedEvent;
import elite.intel.gameapi.journal.events.RedeemVoucherEvent;
import elite.intel.gameapi.journal.events.dto.BountyDto;
import elite.intel.gameapi.journal.subscribers.MissionAcceptedSubscriber;
import elite.intel.gameapi.journal.subscribers.RedeemVoucherSubscriber;
import elite.intel.session.PlayerSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RedeemVoucherSubscriberTest {

    private final RedeemVoucherSubscriber subscriber = new RedeemVoucherSubscriber();
    private final MissionAcceptedSubscriber missionSubscriber = new MissionAcceptedSubscriber();
    private final PlayerSession session = PlayerSession.getInstance();

    @BeforeEach
    void clearState() {
        session.clearBounties();
        MissionManager.getInstance().clear();
        session.setCurrentPrimaryStarName("Sol");
    }

    @Test
    void withNoPirateMissionsActiveBountiesAreCleared() {
        seedBounty("Space Pirates", 10_000L);

        subscriber.onRedeemVoucherEvent(redeemEvent(10_000L));

        assertTrue(session.getBounties().isEmpty());
    }

    @Test
    void withActivePirateMissionsBountiesAreMarkedCashedInNotRemoved() {
        missionSubscriber.onMissionAcceptedEvent(pirateMissionAccepted(300L, "Space Pirates", "Deciat"));
        seedBounty("Space Pirates", 10_000L);

        subscriber.onRedeemVoucherEvent(redeemEvent(10_000L));

        Set<BountyDto> remaining = session.getBounties();
        assertFalse(remaining.isEmpty(), "bounties should remain when pirate missions are active");
        assertTrue(remaining.stream().allMatch(BountyDto::isCashedIn), "all bounties should be marked cashed-in");
    }

    private void seedBounty(String victimFaction, long reward) {
        BountyDto dto = new BountyDto();
        dto.setPilotName("$Pirate_Test;");
        dto.setTarget("sidewinder");
        dto.setVictimFaction(victimFaction);
        dto.setTotalReward(reward);
        dto.setEarnedAt(Instant.now().toString());
        session.addBounty(dto);
    }

    private static MissionAcceptedEvent pirateMissionAccepted(long missionId, String targetFaction, String destinationSystem) {
        JsonObject j = new JsonObject();
        j.addProperty("timestamp", Instant.now().toString());
        j.addProperty("event", "MissionAccepted");
        j.addProperty("MissionID", missionId);
        j.addProperty("Name", "Mission_Massacre");
        j.addProperty("LocalisedName", "Kill pirates");
        j.addProperty("Faction", "EmpireFaction");
        j.addProperty("Reward", 200_000L);
        j.addProperty("DestinationSystem", destinationSystem);
        j.addProperty("TargetFaction", targetFaction);
        j.addProperty("Expiry", Instant.now().plusSeconds(3600).toString());
        return new MissionAcceptedEvent(j);
    }

    private static RedeemVoucherEvent redeemEvent(long amount) {
        JsonObject j = new JsonObject();
        j.addProperty("timestamp", Instant.now().toString());
        j.addProperty("event", "RedeemVoucher");
        j.addProperty("Type", "bounty");
        j.addProperty("Amount", amount);
        JsonArray factions = new JsonArray();
        JsonObject f = new JsonObject();
        f.addProperty("Faction", "Federation");
        f.addProperty("Amount", amount);
        factions.add(f);
        j.add("Factions", factions);
        return new RedeemVoucherEvent(j);
    }
}
