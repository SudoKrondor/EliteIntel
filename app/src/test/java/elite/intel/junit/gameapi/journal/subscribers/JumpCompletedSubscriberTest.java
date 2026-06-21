package elite.intel.junit.gameapi.journal.subscribers;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.journal.events.FSDJumpEvent;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.gameapi.journal.subscribers.JumpCompletedSubscriber;
import elite.intel.session.PlayerSession;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.Instant;
import java.util.function.BooleanSupplier;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.*;

class JumpCompletedSubscriberTest {

    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private final JumpCompletedSubscriber subscriber = new JumpCompletedSubscriber();
    private final LocationManager locationManager = LocationManager.getInstance();
    private final PlayerSession session = PlayerSession.getInstance();

    private static final long ALIOTH_ADDRESS = 1733119412506L;
    private static final long SOL_ADDRESS = 10477373803L;

    @BeforeAll
    static void configureEdsmBaseUrl() {
        System.setProperty("edsm.base.url", "http://localhost:" + wm.getPort());
        System.setProperty("edsm.min.interval.ms", "0");
    }

    @AfterAll
    static void clearEdsmBaseUrl() {
        System.clearProperty("edsm.base.url");
        System.clearProperty("edsm.min.interval.ms");
    }

    @BeforeEach
    void stubEdsmEndpoints() throws InterruptedException {
        Thread.sleep(100);
        wm.stubFor(get(urlPathEqualTo("/api-system-v1/bodies"))
                .willReturn(ok()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{}")));
        wm.stubFor(get(urlPathEqualTo("/api-system-v1/traffic"))
                .willReturn(ok()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"traffic":{"total":800,"week":40,"day":5}}
                                """)));
        wm.stubFor(get(urlPathEqualTo("/api-system-v1/deaths"))
                .willReturn(ok()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"deaths":{"total":2,"week":1,"day":0}}
                                """)));
    }

    @Test
    void fsdJumpSavesLocationWithStarName() throws InterruptedException {
        subscriber.onFSDJumpEvent(fsdJumpEvent("Alioth", ALIOTH_ADDRESS, "Alioth A", 0L));

        awaitTrue(() -> {
            LocationDto loc = locationManager.findBySystemAddress(ALIOTH_ADDRESS);
            return "Alioth".equals(loc.getStarName());
        });

        LocationDto saved = locationManager.findBySystemAddress(ALIOTH_ADDRESS);
        assertEquals("Alioth", saved.getStarName());
    }

    @Test
    void fsdJumpToFinalDestinationStoresTrafficAndDeathsData() throws InterruptedException {
        session.setFinalDestination("Sol");

        subscriber.onFSDJumpEvent(fsdJumpEvent("Sol", SOL_ADDRESS, "Sol", 0L));

        awaitTrue(() -> {
            LocationDto loc = locationManager.findBySystemAddress(SOL_ADDRESS);
            return "Sol".equals(loc.getStarName()) && loc.getTrafficDto() != null;
        });

        LocationDto saved = locationManager.findBySystemAddress(SOL_ADDRESS);
        assertNotNull(saved.getTrafficDto());
        assertEquals(800, saved.getTrafficDto().getData().getTraffic().getTotal());
        assertEquals(2, saved.getDeathsDto().getData().getDeaths().getTotal());
    }

    private static FSDJumpEvent fsdJumpEvent(String system, long systemAddress, String body, long bodyId) {
        JsonObject j = new JsonObject();
        j.addProperty("timestamp", Instant.now().plusSeconds(1).toString());
        j.addProperty("event", "FSDJump");
        j.addProperty("StarSystem", system);
        j.addProperty("SystemAddress", systemAddress);
        j.addProperty("Body", body);
        j.addProperty("BodyID", bodyId);
        j.addProperty("BodyType", "Star");
        j.addProperty("SystemAllegiance", "Independent");
        j.addProperty("SystemSecurity", "$SYSTEM_SECURITY_high;");
        j.addProperty("SystemSecurity_Localised", "High Security");
        j.addProperty("SystemGovernment", "$government_Democracy;");
        j.addProperty("SystemGovernment_Localised", "Democracy");
        j.addProperty("Population", 5_000_000L);
        j.addProperty("JumpDist", 12.5);
        j.addProperty("FuelUsed", 1.2);
        j.addProperty("FuelLevel", 28.0);
        JsonArray starPos = new JsonArray();
        starPos.add(84.5);
        starPos.add(65.0);
        starPos.add(-105.625);
        j.add("StarPos", starPos);
        return new FSDJumpEvent(j);
    }

    private static void awaitTrue(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 2000;
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() > deadline) fail("Condition not met within 2 seconds");
            Thread.sleep(10);
        }
    }
}
