package elite.intel.junit.gameapi.journal.subscribers;

import com.google.gson.JsonObject;
import elite.intel.gameapi.journal.events.PowerplayEvent;
import elite.intel.gameapi.journal.events.dto.RankAndProgressDto;
import elite.intel.gameapi.journal.subscribers.PowerPlaySubscriber;
import elite.intel.session.PlayerSession;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class PowerPlaySubscriberTest {

    private final PowerPlaySubscriber subscriber = new PowerPlaySubscriber();
    private final PlayerSession session = PlayerSession.getInstance();

    @Test
    void pledgedPowerAndRankAreStoredInDto() throws InterruptedException {
        subscriber.onPowerPlayEvent(powerPlayEvent("Aisling Duval", 3, 500, 1_000_000L));

        awaitTrue(() -> {
            RankAndProgressDto rp = session.getRankAndProgressDto();
            return "Aisling Duval".equals(rp.getPledgedToPower());
        });

        RankAndProgressDto rp = session.getRankAndProgressDto();
        assertEquals("Aisling Duval", rp.getPledgedToPower());
        assertEquals(3, rp.getPowerRank());
        assertEquals(500, rp.getMerrits());
    }

    @Test
    void knownPowerSetsAllegianceFromPowerDetails() throws InterruptedException {
        // Edmund Mahon allegiance = "Independent"
        subscriber.onPowerPlayEvent(powerPlayEvent("Edmund Mahon", 2, 200, 300_000L));

        awaitTrue(() -> {
            RankAndProgressDto rp = session.getRankAndProgressDto();
            return "Edmund Mahon".equals(rp.getPledgedToPower());
        });

        assertEquals("Alliance", session.getRankAndProgressDto().getAllegiance());
    }

    private static PowerplayEvent powerPlayEvent(String power, int rank, int merits, long timePledged) {
        JsonObject j = new JsonObject();
        j.addProperty("timestamp", Instant.now().toString());
        j.addProperty("event", "Powerplay");
        j.addProperty("Power", power);
        j.addProperty("Rank", rank);
        j.addProperty("Merits", merits);
        j.addProperty("TimePledged", timePledged);
        return new PowerplayEvent(j);
    }

    private static void awaitTrue(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 2000;
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() > deadline) fail("Condition not met within 2 seconds");
            Thread.sleep(10);
        }
    }
}
