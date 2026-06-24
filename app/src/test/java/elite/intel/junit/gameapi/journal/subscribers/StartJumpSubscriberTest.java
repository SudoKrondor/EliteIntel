package elite.intel.junit.gameapi.journal.subscribers;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.google.gson.JsonObject;
import elite.intel.gameapi.journal.events.StartJumpEvent;
import elite.intel.gameapi.journal.subscribers.StartJumpSubscriber;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.Instant;
import java.util.function.BooleanSupplier;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.fail;

class StartJumpSubscriberTest {

    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private final StartJumpSubscriber subscriber = new StartJumpSubscriber();

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
        wm.stubFor(get(urlPathEqualTo("/api-system-v1/traffic"))
                .willReturn(ok()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"traffic":{"total":100,"week":10,"day":1}}
                                """)));
        wm.stubFor(get(urlPathEqualTo("/api-system-v1/deaths"))
                .willReturn(ok()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"deaths":{"total":3,"week":1,"day":0}}
                                """)));
    }

    @Test
    void hyperspaceJumpQueriesEdsmForTrafficAndDeaths() throws InterruptedException {
        subscriber.onStartJumpEvent(startJumpEvent("Sol", "G", "Hyperspace"));

        // Wait for both EDSM calls to complete (deaths comes after traffic in the VT)
        awaitTrue(() ->
                !wm.findAll(getRequestedFor(urlPathEqualTo("/api-system-v1/traffic"))).isEmpty()
                        && !wm.findAll(getRequestedFor(urlPathEqualTo("/api-system-v1/deaths"))).isEmpty());

        wm.verify(getRequestedFor(urlPathEqualTo("/api-system-v1/traffic"))
                .withQueryParam("systemName", equalTo("Sol")));
        wm.verify(getRequestedFor(urlPathEqualTo("/api-system-v1/deaths"))
                .withQueryParam("systemName", equalTo("Sol")));
    }

    @Test
    void supercruiseJumpDoesNotQueryEdsm() throws InterruptedException {
        subscriber.onStartJumpEvent(startJumpEvent("Achenar", "B", "Supercruise"));

        Thread.sleep(200);

        wm.verify(0, getRequestedFor(urlPathEqualTo("/api-system-v1/traffic")));
        wm.verify(0, getRequestedFor(urlPathEqualTo("/api-system-v1/deaths")));
    }

    private static StartJumpEvent startJumpEvent(String system, String starClass, String jumpType) {
        JsonObject j = new JsonObject();
        j.addProperty("timestamp", Instant.now().plusSeconds(1).toString());
        j.addProperty("event", "StartJump");
        j.addProperty("JumpType", jumpType);
        j.addProperty("StarSystem", system);
        j.addProperty("SystemAddress", 10477373803L);
        j.addProperty("StarClass", starClass);
        return new StartJumpEvent(j);
    }

    private static void awaitTrue(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 2000;
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() > deadline) fail("Condition not met within 2 seconds");
            Thread.sleep(10);
        }
    }
}
