package elite.intel.community;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

/**
 * The four calls this app makes to Firebase, over plain REST: an anonymous sign-up, a token refresh, and
 * writing or deleting this install's own two documents.
 * <p>
 * WHY no Firebase SDK: there is no client SDK for desktop Java, and the Admin SDK is privileged and must
 * never ship. These are the documented REST endpoints the client SDKs call themselves.
 * <p>
 * The API key is not a secret. It ships in every copy of the app, is restricted to these three APIs, and
 * grants nothing on its own: the security rules let a signed-in install write only {@code installs/{uid}}
 * and {@code reports/{uid}}, once a day each, in a fixed shape, and read nothing.
 */
final class FirebaseClient {

    static final String API_KEY = "AIzaSyAhgP5LnyLGTY-s9JHkuKFuxDwHL6rV1Mg";
    static final String PROJECT_ID = "elite-intel-firebase";

    private static final Duration TIMEOUT = Duration.ofSeconds(20);

    /**
     * A signed-in install: its uid, a token good for an hour, and the refresh token that gets the next one.
     */
    record Session(String uid, String idToken, String refreshToken, Instant expiresAt) {

        boolean isFresh(Instant now) {
            // WHY: a minute's margin so a token does not expire between the check and the request.
            return now.isBefore(expiresAt.minusSeconds(60));
        }
    }

    /**
     * A call Firebase answered with something other than success. {@code status} is the HTTP status:
     * 400 for a refresh token it no longer accepts, 403 for a write the rules refused, 429 for a quota.
     */
    static final class CallFailed extends IOException {
        final int status;

        CallFailed(String call, int status) {
            super(call + " failed: HTTP " + status);
            this.status = status;
        }
    }

    private final URI identityBase;
    private final URI tokenBase;
    private final URI firestoreBase;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();

    FirebaseClient() {
        this(URI.create("https://identitytoolkit.googleapis.com/"),
                URI.create("https://securetoken.googleapis.com/"),
                URI.create("https://firestore.googleapis.com/"));
    }

    /**
     * For tests, which point all three at a local stub. Never at the real endpoints.
     */
    FirebaseClient(URI identityBase, URI tokenBase, URI firestoreBase) {
        this.identityBase = identityBase;
        this.tokenBase = tokenBase;
        this.firestoreBase = firestoreBase;
    }

    Session signUp() throws IOException, InterruptedException {
        JsonObject body = new JsonObject();
        body.addProperty("returnSecureToken", true);
        JsonObject r = post(identityBase.resolve("v1/accounts:signUp?key=" + API_KEY), body.toString(),
                "application/json", null, "sign-up");
        return new Session(r.get("localId").getAsString(), r.get("idToken").getAsString(),
                r.get("refreshToken").getAsString(), expiry(r.get("expiresIn").getAsString()));
    }

    Session refresh(String refreshToken) throws IOException, InterruptedException {
        String form = "grant_type=refresh_token&refresh_token=" + URLEncoder.encode(refreshToken, StandardCharsets.UTF_8);
        JsonObject r = post(tokenBase.resolve("v1/token?key=" + API_KEY), form,
                "application/x-www-form-urlencoded", null, "token refresh");
        return new Session(r.get("user_id").getAsString(), r.get("id_token").getAsString(),
                r.get("refresh_token").getAsString(), expiry(r.get("expires_in").getAsString()));
    }

    /**
     * Replaces {@code installs/{uid}}. The rules refuse a second write within 20 hours.
     */
    void writeInstall(Session session, String appVersion, String os) throws IOException, InterruptedException {
        JsonObject fields = new JsonObject();
        fields.add("appVersion", string(appVersion));
        fields.add("os", string(os));
        commit(session, update("installs/" + session.uid(), fields), "stats ping");
    }

    /**
     * Replaces {@code reports/{uid}}. The rules refuse a second write within 20 hours and a payload over
     * 64,000 characters.
     */
    void writeReport(Session session, int format, String pairs) throws IOException, InterruptedException {
        JsonObject fields = new JsonObject();
        JsonObject f = new JsonObject();
        f.addProperty("integerValue", String.valueOf(format));
        fields.add("format", f);
        fields.add("pairs", string(pairs));
        commit(session, update("reports/" + session.uid(), fields), "report upload");
    }

    void deleteReport(Session session) throws IOException, InterruptedException {
        JsonObject write = new JsonObject();
        write.addProperty("delete", documentName("reports/" + session.uid()));
        commit(session, write, "report withdrawal");
    }

    // ---------------------------------------------------------------- helpers

    /**
     * A full replace of the document, with {@code updatedAt} set from the server's clock. The rules demand it
     * equal the request time, so a client cannot backdate a write to get round the daily limit.
     */
    private JsonObject update(String path, JsonObject fields) {
        JsonObject doc = new JsonObject();
        doc.addProperty("name", documentName(path));
        doc.add("fields", fields);
        JsonObject transform = new JsonObject();
        transform.addProperty("fieldPath", "updatedAt");
        transform.addProperty("setToServerValue", "REQUEST_TIME");
        JsonArray transforms = new JsonArray();
        transforms.add(transform);
        JsonObject write = new JsonObject();
        write.add("update", doc);
        write.add("updateTransforms", transforms);
        return write;
    }

    private void commit(Session session, JsonObject write, String call) throws IOException, InterruptedException {
        JsonArray writes = new JsonArray();
        writes.add(write);
        JsonObject body = new JsonObject();
        body.add("writes", writes);
        post(firestoreBase.resolve("v1/projects/" + PROJECT_ID + "/databases/(default)/documents:commit"),
                body.toString(), "application/json", session.idToken(), call);
    }

    private JsonObject post(URI uri, String body, String contentType, String bearer, String call)
            throws IOException, InterruptedException {
        HttpRequest.Builder request = HttpRequest.newBuilder(uri)
                .timeout(TIMEOUT)
                .header("Content-Type", contentType)
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (bearer != null) request.header("Authorization", "Bearer " + bearer);
        HttpResponse<String> response = http.send(request.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) throw new CallFailed(call, response.statusCode());
        return JsonParser.parseString(response.body()).getAsJsonObject();
    }

    private static String documentName(String path) {
        return "projects/" + PROJECT_ID + "/databases/(default)/documents/" + path;
    }

    private static JsonObject string(String value) {
        JsonObject o = new JsonObject();
        o.addProperty("stringValue", value);
        return o;
    }

    private static Instant expiry(String seconds) {
        return Instant.now().plusSeconds(Long.parseLong(seconds));
    }
}
