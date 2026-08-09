package elite.intel.junit.gameapi.journal.subscribers;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import elite.intel.db.managers.FleetCarrierRouteManager;
import elite.intel.gameapi.journal.events.CarrierJumpEvent;
import elite.intel.gameapi.journal.events.CarrierLocationEvent;
import elite.intel.gameapi.journal.events.dto.CarrierDataDto;
import elite.intel.gameapi.journal.subscribers.CarrierJumpCompleteSubscriber;
import elite.intel.gameapi.journal.subscribers.CarrierLocationSubscriber;
import elite.intel.gameapi.search.spansh.carrierroute.CarrierJump;
import elite.intel.session.PlayerSession;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * The tritium a jump burned must be charged before the arrival is announced, whichever of the two arrival
 * events the app happens to process first.
 *
 * <p>WHY: a commander aboard for the jump gets both events, CarrierLocation and CarrierJump, and each
 * subscriber does its work on its own virtual thread. The announcement reads the fuel level, so if the
 * charge has not landed by then, the commander is told the level the carrier had <em>before</em> it jumped -
 * which for a carrier that departed with a full depot reads as "remaining fuel supply 1000 tons", the exact
 * capacity of the depot, after a jump that certainly cost tritium.
 *
 * <p>Worse than the wrong number: whichever event wins, the other must not undo its work. Both events set
 * the carrier's system, and the arrival test is "does the reported system differ from the one on file", so
 * one thread writing that field first makes the other believe the carrier never moved, and the jump is then
 * never charged at all.
 */
class CarrierArrivalFuelAccountingTest {

    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private static final String ORIGIN = "Praea Euq BS-M c7-0";
    private static final String FIRST_LEG = "Phylurn AZ-C b14-0";
    private static final int FULL_DEPOT = 1000;
    private static final int LEG_COST = 100;

    private final PlayerSession session = PlayerSession.getInstance();
    private final FleetCarrierRouteManager route = FleetCarrierRouteManager.getInstance();

    private final CarrierLocationSubscriber carrierLocationSubscriber = new CarrierLocationSubscriber();
    private final CarrierJumpCompleteSubscriber carrierJumpSubscriber = new CarrierJumpCompleteSubscriber();

    @BeforeAll
    static void pointEdsmAtTheStub() {
        System.setProperty("edsm.base.url", "http://localhost:" + wm.getPort());
        System.setProperty("edsm.min.interval.ms", "0");
    }

    @AfterAll
    static void releaseEdsm() {
        System.clearProperty("edsm.base.url");
        System.clearProperty("edsm.min.interval.ms");
    }

    @BeforeEach
    void carrierSitsAtTheOriginWithAFullDepot() {
        wm.stubFor(get(urlPathEqualTo("/api-v1/systems"))
                .willReturn(ok()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"name\":\"" + FIRST_LEG + "\",\"coords\":{\"x\":1,\"y\":2,\"z\":3}}")));
        route.clear();
        session.setLastKnownCarrierLocation(ORIGIN);
        CarrierDataDto carrier = new CarrierDataDto();
        carrier.setMeasuredFuelLevel(FULL_DEPOT);
        session.setFleetCarrierData(carrier);
        Map<Integer, CarrierJump> plotted = new LinkedHashMap<>();
        plotted.put(1, leg(FIRST_LEG));
        plotted.put(2, leg("Phyluwyg OA-Y c1-5"));
        route.setFleetCarrierRoute(plotted);
    }

    /**
     * The order the journal writes them in.
     */
    @Test
    void arrivalIsChargedWhenCarrierLocationIsProcessedFirst() throws InterruptedException {
        carrierLocationSubscriber.onCarrierLocationEvent(carrierLocation(FIRST_LEG));
        awaitTrue(() -> FULL_DEPOT - LEG_COST == session.getFleetCarrierData().getFuelLevel());

        carrierJumpSubscriber.onCarrierJumpCompleteEvent(carrierJump(FIRST_LEG));
        Thread.sleep(300);

        assertEquals(FULL_DEPOT - LEG_COST, session.getFleetCarrierData().getFuelLevel(),
                "the second event for the same arrival must not restore the pre-jump level");
    }

    /**
     * The order the threads can deliver them in. Nothing sequences the two subscribers, so the aboard
     * event's virtual thread may reach the session first - and when it does, the jump must still be charged.
     */
    @Test
    void arrivalIsChargedWhenCarrierJumpIsProcessedFirst() throws InterruptedException {
        carrierJumpSubscriber.onCarrierJumpCompleteEvent(carrierJump(FIRST_LEG));
        awaitTrue(() -> FIRST_LEG.equals(session.getCurrentFleetCarrierSystem()));

        carrierLocationSubscriber.onCarrierLocationEvent(carrierLocation(FIRST_LEG));
        Thread.sleep(300);

        assertEquals(FULL_DEPOT - LEG_COST, session.getFleetCarrierData().getFuelLevel(),
                "the jump burned tritium whichever event told us about it first");
    }

    /**
     * Both events describe one arrival, so between them they cost one leg's tritium, never two.
     */
    @Test
    void oneArrivalIsChargedExactlyOnce() throws InterruptedException {
        carrierLocationSubscriber.onCarrierLocationEvent(carrierLocation(FIRST_LEG));
        awaitTrue(() -> FULL_DEPOT - LEG_COST == session.getFleetCarrierData().getFuelLevel());
        carrierJumpSubscriber.onCarrierJumpCompleteEvent(carrierJump(FIRST_LEG));
        carrierLocationSubscriber.onCarrierLocationEvent(carrierLocation(FIRST_LEG));
        Thread.sleep(300);

        assertEquals(FULL_DEPOT - LEG_COST, session.getFleetCarrierData().getFuelLevel());
        assertEquals(List.of("Phyluwyg OA-Y c1-5"), remainingSystems());
    }

    private List<String> remainingSystems() {
        return route.getFleetCarrierRoute().values().stream().map(CarrierJump::getSystemName).toList();
    }

    private static CarrierJump leg(String systemName) {
        CarrierJump jump = new CarrierJump();
        jump.setSystemName(systemName);
        jump.setFuelUsed(LEG_COST);
        jump.setX(1.0);
        jump.setY(2.0);
        jump.setZ(3.0);
        return jump;
    }

    private static CarrierLocationEvent carrierLocation(String starSystem) {
        JsonObject json = new JsonObject();
        json.addProperty("timestamp", Instant.now().toString());
        json.addProperty("event", "CarrierLocation");
        json.addProperty("CarrierType", "FleetCarrier");
        json.addProperty("CarrierID", 3712500736L);
        json.addProperty("StarSystem", starSystem);
        json.addProperty("SystemAddress", 728401193201L);
        json.addProperty("BodyID", 1);
        return new CarrierLocationEvent(json);
    }

    /**
     * The commander rode along, docked in his ship, which is when the announcement is heard.
     */
    private static CarrierJumpEvent carrierJump(String starSystem) {
        JsonObject json = new JsonObject();
        json.addProperty("timestamp", Instant.now().toString());
        json.addProperty("event", "CarrierJump");
        json.addProperty("Docked", true);
        json.addProperty("OnFoot", false);
        json.addProperty("StarSystem", starSystem);
        json.addProperty("SystemAddress", 728401193201L);
        json.addProperty("Body", starSystem + " A");
        json.addProperty("BodyID", 1);
        json.addProperty("BodyType", "Star");
        JsonArray starPos = new JsonArray();
        starPos.add(1.0);
        starPos.add(2.0);
        starPos.add(3.0);
        json.add("StarPos", starPos);
        return new CarrierJumpEvent(json);
    }

    private static void awaitTrue(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 3000;
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() > deadline) fail("Condition not met within 3 seconds");
            Thread.sleep(10);
        }
    }
}
