package elite.intel.junit.gameapi.journal.subscribers;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.google.gson.JsonObject;
import elite.intel.gameapi.data.FsdTarget;
import elite.intel.gameapi.journal.events.FSDTargetEvent;
import elite.intel.gameapi.journal.subscribers.FSDTargetSubscriber;
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

class FSDTargetSubscriberTest {

    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private final FSDTargetSubscriber subscriber = new FSDTargetSubscriber();
    private final PlayerSession session = PlayerSession.getInstance();

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
    void stubEdsmEndpoints() {
        wm.stubFor(get(urlPathEqualTo("/api-v1/systems"))
                .willReturn(ok()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"name":"Sol","information":{"security":"High","allegiance":"Federation"}}
                                """)));

        wm.stubFor(get(urlPathEqualTo("/api-system-v1/traffic"))
                .willReturn(ok()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"traffic":{"total":1234,"week":56,"day":7}}
                                """)));

        wm.stubFor(get(urlPathEqualTo("/api-system-v1/deaths"))
                .willReturn(ok()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"deaths":{"total":5,"week":2,"day":1}}
                                """)));
    }

    @Test
    void fsdTargetIsPopulatedFromEdsmTrafficAndDeathsData() throws InterruptedException {
        subscriber.onFSDTargetEvent(fsdTargetEvent("Sol", 10477373803L, "G"));

        awaitTrue(() -> {
            FsdTarget t = session.getFsdTarget();
            return t != null && "Sol".equals(t.getName());
        });

        FsdTarget target = session.getFsdTarget();
        assertEquals("Sol", target.getName());
        assertEquals("G", target.getStarClass());
        assertEquals(1234, target.getTrafficDto().getData().getTraffic().getTotal());
        assertEquals(56, target.getTrafficDto().getData().getTraffic().getThisWeek());
        assertEquals(5, target.getDeathsDto().getData().getDeaths().getTotal());
    }

    @Test
    void fuelStarClassGIsMarkedRefuelable() throws InterruptedException {
        subscriber.onFSDTargetEvent(fsdTargetEvent("Sol", 10477373803L, "G"));

        awaitTrue(() -> session.getFsdTarget() != null && "Sol".equals(session.getFsdTarget().getName()));

        assertNotNull(session.getFsdTarget().getFuelStarStatus());
        assertFalse(session.getFsdTarget().getFuelStarStatus().isBlank());
    }

    @Test
    void edsmReturnsEmptyBodyGracefully() throws InterruptedException {
        wm.stubFor(get(urlPathEqualTo("/api-system-v1/traffic"))
                .willReturn(ok().withHeader("Content-Type", "application/json").withBody("{}")));
        wm.stubFor(get(urlPathEqualTo("/api-system-v1/deaths"))
                .willReturn(ok().withHeader("Content-Type", "application/json").withBody("{}")));

        subscriber.onFSDTargetEvent(fsdTargetEvent("Deciat", 83852530386689L, "K"));

        awaitTrue(() -> {
            FsdTarget t = session.getFsdTarget();
            return t != null && "Deciat".equals(t.getName());
        });

        FsdTarget target = session.getFsdTarget();
        assertEquals("Deciat", target.getName());
        // traffic/deaths getData() are non-null even when body is empty — DTOs return empty objects
        assertEquals(0, target.getTrafficDto().getData().getTraffic().getTotal());
        assertEquals(0, target.getDeathsDto().getData().getDeaths().getTotal());
    }

    private static FSDTargetEvent fsdTargetEvent(String name, long systemAddress, String starClass) {
        JsonObject j = new JsonObject();
        j.addProperty("timestamp", Instant.now().toString());
        j.addProperty("event", "FSDTarget");
        j.addProperty("Name", name);
        j.addProperty("SystemAddress", systemAddress);
        j.addProperty("StarClass", starClass);
        j.addProperty("RemainingJumpsInRoute", 3);
        return new FSDTargetEvent(j);
    }

    private static void awaitTrue(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 2000;
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() > deadline) fail("Condition not met within 2 seconds");
            Thread.sleep(10);
        }
    }
}
