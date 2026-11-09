package elite.intel.gameapi.search.spansh.braintrees;

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
 * The brain tree search is asked of Spansh live, so what matters is the question it puts: only brain
 * trees, only bodies holding the material, nearest to the commander first. Spansh knows of more than ten
 * thousand such bodies and returns them in no useful order unless told to sort.
 */
class BrainTreeClientTest {

    private static final String REF = "brain-tree-ref-001";
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

    private BrainTreeClient client;

    @BeforeEach
    void configure() {
        System.setProperty("spansh.base.url", wm.baseUrl());
        client = new BrainTreeClient();
    }

    @AfterEach
    void cleanup() {
        System.clearProperty("spansh.base.url");
    }

    @Test
    void asksForBrainTreesHoldingTheMaterialSortedNearestToTheCommander() {
        stub(RESULTS_JSON);

        client.nearestWithMaterial("Tellurium", 10.5, -20.25, 30.0, 5);

        wm.verify(postRequestedFor(urlEqualTo(SAVE))
                .withRequestBody(matchingJsonPath("$.filters.genuses.value[0]", equalTo("Brain Trees")))
                .withRequestBody(matchingJsonPath("$.filters.materials[0].name", equalTo("Tellurium")))
                .withRequestBody(matchingJsonPath("$.sort[0].distance.direction", equalTo("asc")))
                .withRequestBody(matchingJsonPath("$.reference_coords.x", equalTo("10.5")))
                .withRequestBody(matchingJsonPath("$.reference_coords.y", equalTo("-20.25")))
                .withRequestBody(matchingJsonPath("$.reference_coords.z", equalTo("30.0")))
                .withRequestBody(matchingJsonPath("$.size", equalTo("5"))));
    }

    @Test
    void returnsTheSitesNearestFirst() {
        stub(RESULTS_JSON);

        List<StellarObjectSearchResultDto.Result> sites = client.nearestWithMaterial("Tellurium", 0, 0, 0, 5);

        assertEquals(List.of("Near System", "Far System"),
                sites.stream().map(StellarObjectSearchResultDto.Result::getSystemName).toList());
        assertEquals("Near Body 2", sites.getFirst().getBodyName());
    }

    @Test
    void aSpanshThatDoesNotAnswerFindsNothing() {
        wm.stubFor(post(urlEqualTo(SAVE)).willReturn(aResponse().withStatus(500)));

        assertTrue(client.nearestWithMaterial("Tellurium", 0, 0, 0, 5).isEmpty());
    }

    private void stub(String resultsJson) {
        wm.stubFor(post(urlEqualTo(SAVE)).willReturn(okJson("{\"search_reference\":\"" + REF + "\"}")));
        wm.stubFor(get(urlEqualTo(RECALL)).willReturn(okJson(resultsJson)));
    }
}
