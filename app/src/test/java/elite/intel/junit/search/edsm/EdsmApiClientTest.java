package elite.intel.junit.search.edsm;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import elite.intel.search.edsm.EdsmApiClient;
import elite.intel.search.edsm.dto.MarketDto;
import elite.intel.search.edsm.dto.StarSystemDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

@WireMockTest
class EdsmApiClientTest {

    @BeforeEach
    void configure(WireMockRuntimeInfo wm) {
        System.setProperty("edsm.base.url", wm.getHttpBaseUrl());
        System.setProperty("edsm.min.interval.ms", "0");
    }

    @AfterEach
    void cleanup() {
        System.clearProperty("edsm.base.url");
        System.clearProperty("edsm.min.interval.ms");
    }

    // --- searchStarSystem ---

    @Test
    void searchStarSystem_parsesNameFromArrayResponse() {
        // EDSM /api-v1/systems always returns an array, even for a single match.
        stubFor(get(urlPathEqualTo("/api-v1/systems"))
                .willReturn(okJson("[{\"name\":\"Sol\",\"information\":{}}]")));

        StarSystemDto dto = EdsmApiClient.searchStarSystem("Sol", 1);

        assertNotNull(dto.data);
        assertEquals("Sol", dto.data.getName());
    }

    @Test
    void searchStarSystem_picksFirstMatchFromMultiElementArray() {
        stubFor(get(urlPathEqualTo("/api-v1/systems"))
                .willReturn(okJson("[{\"name\":\"Synuefe NL-N c23-4\",\"information\":"
                        + "{\"allegiance\":null,\"government\":null,\"security\":\"Anarchy\","
                        + "\"economy\":\"None\",\"secondEconomy\":\"None\",\"reserve\":null}},"
                        + "{\"name\":\"Synuefe NL-N c23-5\"}]")));

        StarSystemDto dto = EdsmApiClient.searchStarSystem("Synuefe NL-N c23-4", 1);

        assertNotNull(dto.data);
        assertEquals("Synuefe NL-N c23-4", dto.data.getName());
        assertEquals("Anarchy", dto.data.getInformation().getSecurity());
    }

    @Test
    void searchStarSystem_returnsBlankDataOnEmptyArray() {
        // EDSM returns [] when no system matches.
        stubFor(get(urlPathEqualTo("/api-v1/systems"))
                .willReturn(okJson("[]")));

        StarSystemDto dto = EdsmApiClient.searchStarSystem("No Such System", 1);

        assertNotNull(dto.data);
        assertNull(dto.data.getName());
    }

    @Test
    void searchStarSystem_sendsSystemNameAndShowInformationParams() {
        stubFor(get(urlPathEqualTo("/api-v1/systems"))
                .willReturn(okJson("[{\"name\":\"Sol\"}]")));

        EdsmApiClient.searchStarSystem("Sol", 1);

        verify(getRequestedFor(urlPathEqualTo("/api-v1/systems"))
                .withQueryParam("systemName", equalTo("Sol"))
                .withQueryParam("showInformation", equalTo("1")));
    }

    @Test
    void searchStarSystem_urlEncodesSpacesInSystemName() {
        stubFor(get(urlPathEqualTo("/api-v1/systems"))
                .willReturn(okJson("[{\"name\":\"Alpha Centauri\"}]")));

        EdsmApiClient.searchStarSystem("Alpha Centauri", 0);

        verify(getRequestedFor(urlPathEqualTo("/api-v1/systems"))
                .withQueryParam("systemName", equalTo("Alpha Centauri")));
    }

    @Test
    void searchStarSystem_returnsEmptyDtoWithoutHttpCallOnNullName() {
        StarSystemDto dto = EdsmApiClient.searchStarSystem(null, 1);

        assertNull(dto.data);
        verify(0, anyRequestedFor(anyUrl()));
    }

    @Test
    void searchStarSystem_returnsEmptyDtoOnHttpError() {
        stubFor(get(urlPathEqualTo("/api-v1/systems"))
                .willReturn(aResponse().withStatus(503)));

        StarSystemDto dto = EdsmApiClient.searchStarSystem("Sol", 0);

        // Non-200 returns empty string → StarSystemData is created blank, not null
        assertNotNull(dto.data);
        assertNull(dto.data.getName());
    }

    @Test
    void searchStarSystem_returnsEmptyDtoOnMalformedJson() {
        stubFor(get(urlPathEqualTo("/api-v1/systems"))
                .willReturn(okJson("not-valid-json")));

        StarSystemDto dto = EdsmApiClient.searchStarSystem("Sol", 0);

        assertNull(dto.data);
    }

    @Test
    void searchStarSystem_retriesOn429AndSucceeds() {
        stubFor(get(urlPathEqualTo("/api-v1/systems"))
                .inScenario("rate-limit")
                .whenScenarioStateIs("Started")
                .willReturn(aResponse()
                        .withStatus(429)
                        .withHeader("x-rate-limit-reset", "0"))
                .willSetStateTo("retried"));

        stubFor(get(urlPathEqualTo("/api-v1/systems"))
                .inScenario("rate-limit")
                .whenScenarioStateIs("retried")
                .willReturn(okJson("[{\"name\":\"Sol\"}]")));

        StarSystemDto dto = EdsmApiClient.searchStarSystem("Sol", 0);

        assertNotNull(dto.data);
        assertEquals("Sol", dto.data.getName());
        verify(2, getRequestedFor(urlPathEqualTo("/api-v1/systems")));
    }

    // --- searchMarket ---

    @Test
    void searchMarket_byMarketId_usesMarketIdParam() {
        stubFor(get(urlPathEqualTo("/api-system-v1/stations/market"))
                .willReturn(okJson("{\"marketId\":128928253,\"name\":\"Jameson Memorial\"}")));

        MarketDto dto = EdsmApiClient.searchMarket(128928253L, null, null, 0);

        assertEquals(128928253L, dto.data.getMarketId());
        assertEquals("Jameson Memorial", dto.data.getName());
        verify(getRequestedFor(urlPathEqualTo("/api-system-v1/stations/market"))
                .withQueryParam("marketId", equalTo("128928253")));
    }

    @Test
    void searchMarket_bySystemAndStation_usesNamesAsParams() {
        stubFor(get(urlPathEqualTo("/api-system-v1/stations/market"))
                .willReturn(okJson("{\"marketId\":0}")));

        EdsmApiClient.searchMarket(0L, "Sol", "Titan City", 0);

        verify(getRequestedFor(urlPathEqualTo("/api-system-v1/stations/market"))
                .withQueryParam("systemName", equalTo("Sol"))
                .withQueryParam("stationName", equalTo("Titan City")));
    }

    @Test
    void searchMarket_returnsEmptyDtoWithoutHttpCallWhenBothParamsMissing() {
        MarketDto dto = EdsmApiClient.searchMarket(0L, null, null, 0);

        assertNull(dto.data);
        verify(0, anyRequestedFor(anyUrl()));
    }
}
