package elite.intel.gameapi.search.spansh.rawmaterials;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import elite.intel.gameapi.search.spansh.stellarobjects.StellarObjectSearchResultDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Both searches are asked of Spansh live, so what matters is the question each puts. A brain tree search
 * names the genus and no range, because the caller names the nearest site even when it lies beyond the
 * range. A surface search names landable bodies within the range. Both sort nearest first: Spansh returns
 * its matches in no useful order unless told to.
 */
class MaterialBodyClientTest {

    private static final String REF = "material-body-ref-001";
    private static final String SAVE = "/api/bodies/search/save";
    private static final String RECALL = "/api/bodies/search/recall/" + REF;

    private static final String RESULTS_JSON = """
            {
              "count": 2, "from": 0, "size": 5,
              "results": [
                { "name": "Far Body 1", "system_name": "Far System", "distance": 900.0,
                  "system_x": 900.0, "system_y": 0.0, "system_z": 0.0,
                  "materials": [ { "name": "Tellurium", "share": 1.2 } ] },
                { "name": "Near Body 2", "system_name": "Near System", "distance": 12.5,
                  "system_x": 12.5, "system_y": 0.0, "system_z": 0.0,
                  "materials": [ { "name": "Tellurium", "share": 0.8 } ] }
              ]
            }
            """;

    @RegisterExtension
    WireMockExtension wm = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort().gzipDisabled(true))
            .build();

    private MaterialBodyClient client;

    @BeforeEach
    void configure() {
        System.setProperty("spansh.base.url", wm.baseUrl());
        client = new MaterialBodyClient();
    }

    @AfterEach
    void cleanup() {
        System.clearProperty("spansh.base.url");
    }

    @Test
    void aBrainTreeSearchAsksForBrainTreesHoldingTheMaterialAtAnyDistanceNearestFirst() {
        stub(RESULTS_JSON);

        client.nearestBrainTrees("Tellurium", 10.5, -20.25, 30.0, 5);

        wm.verify(postRequestedFor(urlEqualTo(SAVE))
                .withRequestBody(matchingJsonPath("$.filters.genuses.value[0]", equalTo("Brain Trees")))
                .withRequestBody(matchingJsonPath("$.filters.materials[0].name", equalTo("Tellurium")))
                .withRequestBody(matchingJsonPath("$.filters.distance.max", equalTo(String.valueOf(Integer.MAX_VALUE))))
                .withRequestBody(notContaining("is_landable"))
                .withRequestBody(matchingJsonPath("$.sort[0].distance.direction", equalTo("asc")))
                .withRequestBody(matchingJsonPath("$.reference_coords.x", equalTo("10.5")))
                .withRequestBody(matchingJsonPath("$.reference_coords.y", equalTo("-20.25")))
                .withRequestBody(matchingJsonPath("$.reference_coords.z", equalTo("30.0")))
                .withRequestBody(matchingJsonPath("$.size", equalTo("5"))));
    }

    @Test
    void aSurfaceSearchAsksForLandableBodiesHoldingTheMaterialWithinTheRange() {
        stub(RESULTS_JSON);

        client.nearestLandableBodies("Polonium", 0, 0, 0, 120, 50);

        wm.verify(postRequestedFor(urlEqualTo(SAVE))
                .withRequestBody(matchingJsonPath("$.filters.is_landable.value", equalTo("true")))
                .withRequestBody(matchingJsonPath("$.filters.materials[0].name", equalTo("Polonium")))
                .withRequestBody(matchingJsonPath("$.filters.distance.max", equalTo("120")))
                .withRequestBody(notContaining("genuses"))
                .withRequestBody(matchingJsonPath("$.sort[0].distance.direction", equalTo("asc")))
                .withRequestBody(matchingJsonPath("$.size", equalTo("50"))));
    }

    @Test
    void returnsTheBodiesNearestFirstWithTheirMaterials() {
        stub(RESULTS_JSON);

        List<StellarObjectSearchResultDto.Result> sites = client.nearestBrainTrees("Tellurium", 0, 0, 0, 5);

        assertEquals(List.of("Near System", "Far System"),
                sites.stream().map(StellarObjectSearchResultDto.Result::getSystemName).toList());
        assertEquals("Near Body 2", sites.getFirst().getBodyName());
        assertEquals(0.8, sites.getFirst().getMaterials().getFirst().getShare());
    }

    @Test
    void aSpanshThatDoesNotAnswerFindsNothing() {
        wm.stubFor(post(urlEqualTo(SAVE)).willReturn(aResponse().withStatus(500)));

        assertTrue(client.nearestBrainTrees("Tellurium", 0, 0, 0, 5).isEmpty());
        assertTrue(client.nearestLandableBodies("Tellurium", 0, 0, 0, 500, 50).isEmpty());
    }

    private void stub(String resultsJson) {
        wm.stubFor(post(urlEqualTo(SAVE)).willReturn(okJson("{\"search_reference\":\"" + REF + "\"}")));
        wm.stubFor(get(urlEqualTo(RECALL)).willReturn(okJson(resultsJson)));
    }
}
