package elite.intel.community;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import elite.intel.community.FirebaseClient.CallFailed;
import elite.intel.community.FirebaseClient.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.net.URI;
import java.time.Instant;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The REST calls against a local stub, never Firebase itself.
 */
class FirebaseClientTest {

    @RegisterExtension
    WireMockExtension wm = WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

    private FirebaseClient client;

    @BeforeEach
    void setUp() {
        URI base = URI.create(wm.baseUrl() + "/");
        client = new FirebaseClient(base, base, base);
    }

    @Test
    void anAnonymousSignUpGivesAUidAndTokens() throws Exception {
        wm.stubFor(post(urlPathEqualTo("/v1/accounts:signUp")).willReturn(okJson("""
                {"localId":"uid-1","idToken":"id-1","refreshToken":"refresh-1","expiresIn":"3600"}""")));

        Session s = client.signUp();

        assertEquals("uid-1", s.uid());
        assertEquals("refresh-1", s.refreshToken());
        assertTrue(s.isFresh(Instant.now()));
        wm.verify(postRequestedFor(urlPathEqualTo("/v1/accounts:signUp"))
                .withQueryParam("key", equalTo(FirebaseClient.API_KEY))
                .withRequestBody(equalToJson("{\"returnSecureToken\":true}")));
    }

    @Test
    void aRefreshUsesTheTokenServicesOwnFieldNames() throws Exception {
        wm.stubFor(post(urlPathEqualTo("/v1/token")).willReturn(okJson("""
                {"user_id":"uid-1","id_token":"id-2","refresh_token":"refresh-2","expires_in":"3600"}""")));

        Session s = client.refresh("refresh-1");

        assertEquals("id-2", s.idToken());
        wm.verify(postRequestedFor(urlPathEqualTo("/v1/token"))
                .withRequestBody(equalTo("grant_type=refresh_token&refresh_token=refresh-1")));
    }

    @Test
    void theStatsPingReplacesItsOwnDocumentWithTheServersClock() throws Exception {
        wm.stubFor(post(urlPathMatching("/v1/projects/.*/documents:commit")).willReturn(okJson("{}")));

        client.writeInstall(new Session("uid-1", "id-1", "r", Instant.now().plusSeconds(3600)), "v-1.2.0001", "linux");

        wm.verify(postRequestedFor(urlPathEqualTo("/v1/projects/elite-intel-firebase/databases/(default)/documents:commit"))
                .withHeader("Authorization", equalTo("Bearer id-1"))
                .withRequestBody(matchingJsonPath("$.writes[0].update.name",
                        equalTo("projects/elite-intel-firebase/databases/(default)/documents/installs/uid-1")))
                .withRequestBody(matchingJsonPath("$.writes[0].update.fields.os.stringValue", equalTo("linux")))
                .withRequestBody(matchingJsonPath("$.writes[0].updateTransforms[0].setToServerValue", equalTo("REQUEST_TIME"))));
    }

    @Test
    void aRefusedWriteCarriesItsStatus() {
        wm.stubFor(post(urlPathMatching("/v1/projects/.*/documents:commit")).willReturn(aResponse().withStatus(403)));

        CallFailed e = assertThrows(CallFailed.class, () ->
                client.writeReport(new Session("uid-1", "id-1", "r", Instant.now().plusSeconds(3600)), 1, "[]"));

        assertEquals(403, e.status);
    }
}
