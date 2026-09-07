package elite.intel.junit.gameapi.journal.subscribers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import elite.intel.gameapi.journal.events.BountyEvent;
import elite.intel.gameapi.journal.events.dto.BountyDto;
import elite.intel.gameapi.journal.subscribers.BountyEventSubscriber;
import elite.intel.session.PlayerSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class BountyEventSubscriberTest {

    private final BountyEventSubscriber subscriber = new BountyEventSubscriber();
    private final PlayerSession session = PlayerSession.getInstance();

    @BeforeEach
    void clearBounties() throws InterruptedException {
        session.clearBounties();
        // addBountyReward runs after addBounty in the same virtual thread; a prior test's
        // awaitTrue on getBounties() can return before addBountyReward completes.
        // 100ms is enough for those in-flight DB writes to drain before we reset.
        Thread.sleep(100);
        session.setTotalBountyClaimed(0);
    }

    @Test
    void bountyIsStoredInSession() throws InterruptedException {
        subscriber.onBountyEvent(event("$Pirate_Alpha;", "sidewinder", "Space Pirates", 15_000L, "Federation", 15_000L));

        awaitTrue(() -> !session.getBounties().isEmpty());
        Set<BountyDto> bounties = session.getBounties();
        assertEquals(1, bounties.size());
        BountyDto stored = bounties.iterator().next();
        assertEquals("Space Pirates", stored.getVictimFaction());
        assertEquals(15_000L, stored.getTotalReward());
    }

    @Test
    void bountyRewardAccumulatesIntoPlayerTotal() throws InterruptedException {
        subscriber.onBountyEvent(event("$Pirate_Alpha;", "sidewinder", "Space Pirates", 15_000L, "Federation", 15_000L));
        awaitTrue(() -> session.getTotalBountyClaimed() >= 15_000L);

        subscriber.onBountyEvent(event("$Pirate_Beta;", "cobra", "Other Pirates", 10_000L, "Empire", 10_000L));
        awaitTrue(() -> session.getTotalBountyClaimed() >= 25_000L);

        assertEquals(25_000L, session.getTotalBountyClaimed());
    }

    @Test
    void multipleBountiesFromDifferentKillsAreStoredSeparately() throws InterruptedException {
        subscriber.onBountyEvent(event("$Pirate_Alpha;", "sidewinder", "Gang A", 5_000L, "Federation", 5_000L));
        subscriber.onBountyEvent(event("$Pirate_Beta;", "eagle", "Gang B", 8_000L, "Empire", 8_000L));

        awaitTrue(() -> session.getBounties().size() >= 2);
        assertEquals(2, session.getBounties().size());
    }

    private static BountyEvent event(String pilotName, String target, String victimFaction,
                                     long totalReward, String rewardFaction, long rewardAmount) {
        JsonObject j = new JsonObject();
        j.addProperty("timestamp", Instant.now().toString());
        j.addProperty("event", "Bounty");
        j.addProperty("PilotName", pilotName);
        j.addProperty("Target", target);
        j.addProperty("VictimFaction", victimFaction);
        j.addProperty("TotalReward", totalReward);

        JsonArray rewards = new JsonArray();
        JsonObject reward = new JsonObject();
        reward.addProperty("Faction", rewardFaction);
        reward.addProperty("Reward", rewardAmount);
        rewards.add(reward);
        j.add("Rewards", rewards);

        return new BountyEvent(j);
    }

    private static void awaitTrue(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 2000;
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() > deadline) fail("Condition not met within 2 seconds");
            Thread.sleep(10);
        }
    }
}
