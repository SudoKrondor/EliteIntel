package elite.intel.gameapi.journal.subscribers;

import com.google.gson.JsonObject;
import elite.intel.db.managers.CombatBondManager;
import elite.intel.db.managers.MissionManager;
import elite.intel.db.util.Database;
import elite.intel.gameapi.journal.events.RedeemVoucherEvent;
import elite.intel.gameapi.journal.events.dto.MissionDto;
import elite.intel.util.Cypher;
import elite.intel.util.json.GsonFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Cashing in combat bonds. They are a war massacre's only record of its kills, so a commander who cashes in
 * halfway through a contract must not see its bar drop back to nothing - and with no contract to count them,
 * paid still means gone.
 */
class RedeemVoucherSubscriberTest {

    private final RedeemVoucherSubscriber subscriber = new RedeemVoucherSubscriber();
    private final CombatBondManager bonds = CombatBondManager.getInstance();
    private final MissionManager missions = MissionManager.getInstance();

    @BeforeAll
    static void boot() throws Exception {
        Cypher.initializeKey();
        Database.init().close();
    }

    @BeforeEach
    @AfterEach
    void clean() {
        bonds.clear();
        missions.clear();
    }

    @Test
    void bondsCashedInDuringAWarMassacreStayAsItsKills() {
        missions.save(mission("MISSION_MASSACRE_CONFLICT"));
        earnTwoBonds();

        subscriber.onRedeemVoucherEvent(voucher("CombatBond"));

        assertEquals(0, bonds.pending().kills(), "paid, so no longer held");
        assertEquals(2, bonds.kills().size(), "still the contract's kills");
    }

    /**
     * A war contract accepted before MISSION_MASSACRE_CONFLICT existed is on file as a pirate massacre.
     */
    @Test
    void aWarContractFiledAsPirateKeepsItsBondsToo() {
        missions.save(mission("MISSION_PIRATE_MASSACRE"));
        earnTwoBonds();

        subscriber.onRedeemVoucherEvent(voucher("CombatBond"));

        assertEquals(2, bonds.kills().size());
    }

    @Test
    void bondsCashedInWithNoMassacreOpenAreGone() {
        earnTwoBonds();

        subscriber.onRedeemVoucherEvent(voucher("CombatBond"));

        assertEquals(0, bonds.pending().kills());
        assertEquals(0, bonds.kills().size());
    }

    /**
     * A bond earned after the cash-in is held again, and counted with the ones before it.
     */
    @Test
    void aBondEarnedAfterTheCashInIsHeldAgain() {
        missions.save(mission("MISSION_MASSACRE_CONFLICT"));
        earnTwoBonds();
        subscriber.onRedeemVoucherEvent(voucher("CombatBond"));

        bonds.record(1L, "Gallant Investment Brokers", "Fire Lotus Coalition", 47_238, "2026-11-13T19:54:40Z");

        assertEquals(1, bonds.pending().kills());
        assertEquals(47_238, bonds.pending().credits());
        assertEquals(3, bonds.kills().size());
    }

    private void earnTwoBonds() {
        bonds.record(1L, "Gallant Investment Brokers", "Fire Lotus Coalition", 26_007, "2026-11-13T19:50:27Z");
        bonds.record(1L, "Gallant Investment Brokers", "Fire Lotus Coalition", 23_018, "2026-11-13T19:52:10Z");
        assertEquals(2, bonds.pending().kills());
    }

    private static MissionDto mission(String type) {
        String json = """
                {"missionId":1071218957,"missionType":"%s","faction":"Gallant Investment Brokers",
                 "acceptedAt":"2026-11-13T19:47:49Z","killCount":24,"missionTargetFaction":"Fire Lotus Coalition"}
                """.formatted(type);
        return GsonFactory.getGson().fromJson(json, MissionDto.class);
    }

    private static RedeemVoucherEvent voucher(String type) {
        String json = """
                {"timestamp":"2026-11-13T20:10:00Z","event":"RedeemVoucher","Type":"%s","Amount":96263}
                """.formatted(type);
        return new RedeemVoucherEvent(GsonFactory.getGson().fromJson(json, JsonObject.class));
    }
}
