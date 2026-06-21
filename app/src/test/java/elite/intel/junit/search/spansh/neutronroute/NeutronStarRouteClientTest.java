package elite.intel.junit.search.spansh.neutronroute;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import elite.intel.search.spansh.neutronroute.NeutronStarRoute;
import elite.intel.search.spansh.neutronroute.NeutronStarRouteCalculatorCriteria;
import elite.intel.search.spansh.neutronroute.NeutronStarRouteClient;
import elite.intel.search.spansh.neutronroute.NeutronStarSystemJump;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.*;

class NeutronStarRouteClientTest {

    private static final String JOB_ID = "neutron-job-001";

    private static final String ROUTE_RESULT_JSON = """
            {
              "job": "%s",
              "result": {
                "source_system": "Sol",
                "destination_system": "Sagittarius A*",
                "distance": 25978.0,
                "efficiency": "60",
                "range": "50.0",
                "total_jumps": 3,
                "system_jumps": [
                  {
                    "system": "Sol",
                    "id64": 10477373803,
                    "distance_jumped": 0.0, "distance_left": 25978.0,
                    "jumps": 0, "neutron_star": false,
                    "x": 0.0, "y": 0.0, "z": 0.0
                  },
                  {
                    "system": "Swoiwns ZB-J b40-1",
                    "id64": 98765,
                    "distance_jumped": 330.0, "distance_left": 25648.0,
                    "jumps": 1, "neutron_star": true,
                    "x": 1.0, "y": 2.0, "z": 3.0
                  }
                ]
              },
              "state": "queued",
              "status": "ok"
            }
            """.formatted(JOB_ID);

    // Spansh client sends Accept-Encoding: gzip; disable Jetty compression so
    // the plain-Java HttpClient (which doesn't auto-decompress) sees real JSON.
    @RegisterExtension
    WireMockExtension wm = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort().gzipDisabled(true))
            .build();

    private NeutronStarRouteClient client;

    @BeforeEach
    void configure() {
        System.setProperty("spansh.base.url", wm.baseUrl());
        client = new NeutronStarRouteClient();
    }

    @AfterEach
    void cleanup() {
        System.clearProperty("spansh.base.url");
    }

    // --- request construction ---

    @Test
    void calculateRoute_sendsEfficiencyRangeFromToAndSuperchargeParams() {
        stubJobAndResult(ROUTE_RESULT_JSON);

        client.calculateRoute(new NeutronStarRouteCalculatorCriteria("Sol", "Sagittarius A*", 60, 50.0, 4));

        wm.verify(postRequestedFor(urlEqualTo("/api/route"))
                .withRequestBody(containing("efficiency=60"))
                .withRequestBody(containing("range=50.0"))
                .withRequestBody(containing("from=Sol"))
                .withRequestBody(containing("to=Sagittarius+A*"))
                .withRequestBody(containing("supercharge_multiplier=4")));
    }

    @Test
    void calculateRoute_urlEncodesSpacesInSystemNames() {
        stubJobAndResult(ROUTE_RESULT_JSON);

        client.calculateRoute(new NeutronStarRouteCalculatorCriteria("Alpha Centauri", "Sag A*", 60, 50.0, 4));

        wm.verify(postRequestedFor(urlEqualTo("/api/route"))
                .withRequestBody(containing("from=Alpha+Centauri")));
    }

    // --- response parsing ---

    @Test
    void calculateRoute_parsesSourceAndDestinationSystemsFromResult() {
        stubJobAndResult(ROUTE_RESULT_JSON);

        NeutronStarRoute route = client.calculateRoute(
                new NeutronStarRouteCalculatorCriteria("Sol", "Sagittarius A*", 60, 50.0, 4));

        assertNotNull(route);
        assertNotNull(route.getResult());
        assertEquals("Sol", route.getResult().getSourceSystem());
        assertEquals("Sagittarius A*", route.getResult().getDestinationSystem());
        assertEquals(3, route.getResult().getTotalJumps());
    }

    @Test
    void calculateRoute_parsesSystemJumpsFromResult() {
        stubJobAndResult(ROUTE_RESULT_JSON);

        NeutronStarRoute route = client.calculateRoute(
                new NeutronStarRouteCalculatorCriteria("Sol", "Sagittarius A*", 60, 50.0, 4));

        assertNotNull(route.getResult().getSystemJumps());
        assertEquals(2, route.getResult().getSystemJumps().size());
        NeutronStarSystemJump neutronJump = route.getResult().getSystemJumps().get(1);
        assertTrue(neutronJump.isNeutronStar());
        assertEquals("Swoiwns ZB-J b40-1", neutronJump.getSystem());
    }

    // --- polling ---

    @Test
    @Timeout(15)
    void calculateRoute_pollsUntilResultIsReady() {
        wm.stubFor(post(urlEqualTo("/api/route"))
                .willReturn(aResponse().withStatus(202).withBody("{\"job\":\"" + JOB_ID + "\"}")));

        wm.stubFor(get(urlEqualTo("/api/results/" + JOB_ID))
                .inScenario("polling")
                .whenScenarioStateIs("Started")
                .willReturn(aResponse().withStatus(202).withBody("{\"status\":\"queued\"}"))
                .willSetStateTo("ready"));

        wm.stubFor(get(urlEqualTo("/api/results/" + JOB_ID))
                .inScenario("polling")
                .whenScenarioStateIs("ready")
                .willReturn(okJson(ROUTE_RESULT_JSON)));

        NeutronStarRoute route = client.calculateRoute(
                new NeutronStarRouteCalculatorCriteria("Sol", "Sagittarius A*", 60, 50.0, 4));

        assertNotNull(route);
        wm.verify(2, getRequestedFor(urlEqualTo("/api/results/" + JOB_ID)));
    }

    // --- error handling ---

    @Test
    void calculateRoute_returnsNullWhenPostFails() {
        wm.stubFor(post(urlEqualTo("/api/route"))
                .willReturn(aResponse().withStatus(500)));

        NeutronStarRoute route = client.calculateRoute(
                new NeutronStarRouteCalculatorCriteria("Sol", "Sagittarius A*", 60, 50.0, 4));

        assertNull(route);
    }

    // --- helper ---

    private void stubJobAndResult(String resultJson) {
        wm.stubFor(post(urlEqualTo("/api/route"))
                .willReturn(aResponse().withStatus(202).withBody("{\"job\":\"" + JOB_ID + "\"}")));
        wm.stubFor(get(urlEqualTo("/api/results/" + JOB_ID))
                .willReturn(okJson(resultJson)));
    }
}
