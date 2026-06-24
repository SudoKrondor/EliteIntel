package elite.intel.junit.search.spansh.carrierroute;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import elite.intel.search.spansh.carrierroute.CarrierJump;
import elite.intel.search.spansh.carrierroute.CarrierRouteCriteria;
import elite.intel.search.spansh.carrierroute.SpanshCarrierRouteClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.*;

class SpanshCarrierRouteClientTest {

    private static final String JOB_ID = "carrier-job-001";

    // Index 0 = current position (skipped by the client), indices 1+ are actual jumps
    private static final String TWO_JUMP_RESULT = """
            {
              "result": {
                "jumps": [
                  {
                    "name": "Sol", "fuel_used": 0, "distance": 0.0,
                    "has_icy_ring": false, "is_system_pristine": false,
                    "x": 0.0, "y": 0.0, "z": 0.0
                  },
                  {
                    "name": "Alpha Centauri", "fuel_used": 100, "distance": 4.38,
                    "has_icy_ring": true, "is_system_pristine": false,
                    "x": 3.17, "y": -0.45, "z": 3.31
                  },
                  {
                    "name": "Proxima Centauri", "fuel_used": 150, "distance": 1.295,
                    "has_icy_ring": false, "is_system_pristine": true,
                    "x": 3.17, "y": -0.45, "z": 3.31
                  }
                ]
              }
            }
            """;

    // Spansh client sends Accept-Encoding: gzip; disable Jetty compression so
    // the plain-Java HttpClient (which doesn't auto-decompress) sees real JSON.
    @RegisterExtension
    WireMockExtension wm = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort().gzipDisabled(true))
            .build();

    private SpanshCarrierRouteClient client;

    @BeforeEach
    void configure() {
        System.setProperty("spansh.base.url", wm.baseUrl());
        client = new SpanshCarrierRouteClient();
    }

    @AfterEach
    void cleanup() {
        System.clearProperty("spansh.base.url");
    }

    // --- request construction ---

    @Test
    void calculateRoute_sendsAllRequiredFormParams() {
        stubJobAndResult(TWO_JUMP_RESULT);

        client.calculateRoute(new CarrierRouteCriteria("Sol", "Sagittarius A*", 25000, 8000, 1000));

        wm.verify(postRequestedFor(urlEqualTo("/api/fleetcarrier/route"))
                .withRequestBody(containing("source=Sol"))
                .withRequestBody(containing("destinations=Sagittarius+A*"))
                .withRequestBody(containing("capacity=25000"))
                .withRequestBody(containing("capacity_used=8000"))
                .withRequestBody(containing("calculate_starting_fuel=0"))
                .withRequestBody(containing("starting_fuel=1000")));
    }

    // --- fuel calculation ---

    @Test
    void calculateRoute_tracksFuelCorrectlyAcrossJumps() {
        stubJobAndResult(TWO_JUMP_RESULT);

        Map<Integer, CarrierJump> route = client.calculateRoute(
                new CarrierRouteCriteria("Sol", "Proxima Centauri", 25000, 0, 1000));

        assertEquals(2, route.size());
        assertEquals(900, route.get(1).getRemainingFuel());  // 1000 - 100
        assertEquals(750, route.get(2).getRemainingFuel());  // 900 - 150
    }

    @Test
    void calculateRoute_skipsIndexZeroWhichIsCurrentPosition() {
        stubJobAndResult(TWO_JUMP_RESULT);

        Map<Integer, CarrierJump> route = client.calculateRoute(
                new CarrierRouteCriteria("Sol", "Proxima Centauri", 25000, 0, 1000));

        assertFalse(route.containsKey(0));
        assertEquals("Alpha Centauri", route.get(1).getSystemName());
        assertEquals("Proxima Centauri", route.get(2).getSystemName());
    }

    @Test
    void calculateRoute_roundsDistanceToOneDecimalPlace() {
        stubJobAndResult(TWO_JUMP_RESULT);

        Map<Integer, CarrierJump> route = client.calculateRoute(
                new CarrierRouteCriteria("Sol", "Proxima Centauri", 25000, 0, 1000));

        // 1.295 → Math.round(1.295 * 10.0) / 10.0 = 1.3
        assertEquals(1.3, route.get(2).getDistance());
    }

    @Test
    void calculateRoute_mapsHasIcyRingAndPristineFlags() {
        stubJobAndResult(TWO_JUMP_RESULT);

        Map<Integer, CarrierJump> route = client.calculateRoute(
                new CarrierRouteCriteria("Sol", "Proxima Centauri", 25000, 0, 1000));

        assertTrue(route.get(1).getHasIcyRing());
        assertFalse(route.get(1).isPristine());
        assertFalse(route.get(2).getHasIcyRing());
        assertTrue(route.get(2).isPristine());
    }

    // --- error handling ---

    @Test
    void calculateRoute_returnsEmptyMapWhenResultHasNoJumpsField() {
        stubJobAndResult("{\"result\":{}}");

        Map<Integer, CarrierJump> route = client.calculateRoute(
                new CarrierRouteCriteria("Sol", "Somewhere", 25000, 0, 1000));

        assertTrue(route.isEmpty());
    }

    @Test
    void calculateRoute_returnsEmptyMapWhenJumpsArrayHasFewerThanTwoEntries() {
        String oneJump = """
                {"result":{"jumps":[
                  {"name":"Sol","fuel_used":0,"distance":0.0,
                   "has_icy_ring":false,"is_system_pristine":false,"x":0,"y":0,"z":0}
                ]}}
                """;
        stubJobAndResult(oneJump);

        Map<Integer, CarrierJump> route = client.calculateRoute(
                new CarrierRouteCriteria("Sol", "Somewhere", 25000, 0, 1000));

        assertTrue(route.isEmpty());
    }

    @Test
    void calculateRoute_returnsEmptyMapWhenPostFails() {
        wm.stubFor(post(urlEqualTo("/api/fleetcarrier/route"))
                .willReturn(aResponse().withStatus(500)));

        Map<Integer, CarrierJump> route = client.calculateRoute(
                new CarrierRouteCriteria("Sol", "Somewhere", 25000, 0, 1000));

        assertTrue(route.isEmpty());
    }

    // --- helper ---

    private void stubJobAndResult(String resultJson) {
        wm.stubFor(post(urlEqualTo("/api/fleetcarrier/route"))
                .willReturn(aResponse().withStatus(202).withBody("{\"job\":\"" + JOB_ID + "\"}")));
        wm.stubFor(get(urlEqualTo("/api/results/" + JOB_ID))
                .willReturn(okJson(resultJson)));
    }
}
