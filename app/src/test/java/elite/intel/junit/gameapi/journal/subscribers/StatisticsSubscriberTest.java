package elite.intel.junit.gameapi.journal.subscribers;

import com.google.gson.JsonObject;
import elite.intel.gameapi.journal.events.StatisticsEvent;
import elite.intel.gameapi.journal.subscribers.StatisticsSubscriber;
import elite.intel.session.PlayerSession;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class StatisticsSubscriberTest {

    private final StatisticsSubscriber subscriber = new StatisticsSubscriber();
    private final PlayerSession session = PlayerSession.getInstance();

    @Test
    void combatBountyTotalIsStoredFromCombatSection() throws InterruptedException {
        subscriber.onStatisticsEvent(eventWithCombat(77_000L));

        awaitTrue(() -> session.getTotalBountyClaimed() == 77_000L);
        assertEquals(77_000L, session.getTotalBountyClaimed());
    }

    @Test
    void explorationSystemsVisitedIsStoredFromExplorationSection() throws InterruptedException {
        subscriber.onStatisticsEvent(eventWithExploration(2222, 30_000.7, 500_000L));

        awaitTrue(() -> session.getTotalSystemsVisited() == 2222L);
        assertEquals(2222L, session.getTotalSystemsVisited());
        assertEquals(30_000.7, session.getTotalDistanceTraveled(), 0.1);
    }

    @Test
    void tradingHighestTransactionIsStoredFromTradingSection() throws InterruptedException {
        subscriber.onStatisticsEvent(eventWithTrading(8_888_888L));

        awaitTrue(() -> session.getHighestTransaction() == 8_888_888L);
        assertEquals(8_888_888L, session.getHighestTransaction());
    }

    @Test
    void exobiologyProfitsAreStoredFromExobiologySection() throws InterruptedException {
        subscriber.onStatisticsEvent(eventWithExobiology(1_500_000L));

        awaitTrue(() -> session.getTotalExobiologyProfits() == 1_500_000L);
        assertEquals(1_500_000L, session.getTotalExobiologyProfits());
    }

    private static StatisticsEvent eventWithCombat(long bountiesClaimed) {
        JsonObject j = base();
        JsonObject combat = new JsonObject();
        combat.addProperty("Bounties_Claimed", bountiesClaimed);
        combat.addProperty("Bounty_Hunting_Profit", bountiesClaimed);
        j.add("Combat", combat);
        return new StatisticsEvent(j);
    }

    private static StatisticsEvent eventWithExploration(int systemsVisited, double greatestDistance, long hyperspaceDistance) {
        JsonObject j = base();
        JsonObject exploration = new JsonObject();
        exploration.addProperty("Systems_Visited", systemsVisited);
        exploration.addProperty("Total_Hyperspace_Distance", hyperspaceDistance);
        exploration.addProperty("Total_Hyperspace_Jumps", 0);
        exploration.addProperty("Greatest_Distance_From_Start", greatestDistance);
        exploration.addProperty("Exploration_Profits", 0);
        exploration.addProperty("Planets_Scanned_To_Level_2", 0);
        exploration.addProperty("Planets_Scanned_To_Level_3", 0);
        j.add("Exploration", exploration);
        return new StatisticsEvent(j);
    }

    private static StatisticsEvent eventWithTrading(long highestTransaction) {
        JsonObject j = base();
        JsonObject trading = new JsonObject();
        trading.addProperty("Highest_Single_Transaction", highestTransaction);
        trading.addProperty("Market_Profits", 0);
        trading.addProperty("Goods_Sold", 0);
        j.add("Trading", trading);
        return new StatisticsEvent(j);
    }

    private static StatisticsEvent eventWithExobiology(long profits) {
        JsonObject j = base();
        JsonObject exobiology = new JsonObject();
        exobiology.addProperty("Organic_Data_Profits", profits);
        exobiology.addProperty("First_Logged", 5);
        j.add("Exobiology", exobiology);
        return new StatisticsEvent(j);
    }

    private static JsonObject base() {
        JsonObject j = new JsonObject();
        j.addProperty("timestamp", Instant.now().toString());
        j.addProperty("event", "Statistics");
        return j;
    }

    private static void awaitTrue(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 2000;
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() > deadline) fail("Condition not met within 2 seconds");
            Thread.sleep(10);
        }
    }
}
