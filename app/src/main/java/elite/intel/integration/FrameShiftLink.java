package elite.intel.integration;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

/**
 * The FrameShift Command side of the {@link IntegrationSocket}: who is registered, which pages it offers, and the
 * page-opening calls waiting for its answer. Protocol in {@code docs/FRAMESHIFT_INTEGRATION.md}.
 * <p>
 * FrameShift connects, sends {@code integration.register} with its page list and is answered
 * {@code integration.registered}. {@link #openPage} then sends it a {@code tool_call} and blocks the caller - the
 * action lane, one virtual thread per command - until the matching {@code tool_result} arrives, the connection
 * drops, or {@link #DEFAULT_TIMEOUT} passes. Nothing is queued: with no FrameShift registered the call fails at
 * once. A second registration replaces the first.
 * <p>
 * The page list reaches the LLM prompt, so it is held to plain ids and short plain labels and capped in size:
 * whatever registers is local, but it is still another program's text.
 */
public final class FrameShiftLink {

    private static final Logger log = LogManager.getLogger(FrameShiftLink.class);

    static final String CLIENT = "frameshift-command";
    static final String TOOL = "frameshift_open_page";
    static final int PROTOCOL_VERSION = 1;
    static final int MAX_PAGES = 32;
    static final int MAX_LABEL_LENGTH = 40;
    static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(5);

    private static final Pattern PAGE_ID = Pattern.compile("[A-Za-z0-9_-]{1,32}");
    private static final Pattern LABEL_UNSAFE = Pattern.compile("[^\\p{L}\\p{N} &'./()-]");
    private static final Pattern SPACES = Pattern.compile("\\s+");

    private static final FrameShiftLink INSTANCE = new FrameShiftLink(DEFAULT_TIMEOUT);

    /** One connected program, as the socket sees it: something to send text to. Identity is the connection. */
    public interface Peer {
        void send(String text);
    }

    public record Page(String id, String label) {
    }

    /** How a page-opening call ended. */
    public sealed interface OpenResult {
        record Opened(Page page) implements OpenResult {
        }

        /** FrameShift answered, and could not open it. */
        record Refused(Page page, String code, String message) implements OpenResult {
        }

        record Offline() implements OpenResult {
        }

        record TimedOut() implements OpenResult {
        }

        /** The page is not one FrameShift registered (the list changed, or the model invented one). */
        record UnknownPage(String pageId) implements OpenResult {
        }
    }

    private record Registration(Peer peer, List<Page> pages) {
        Optional<Page> find(String pageId) {
            return pages.stream().filter(p -> p.id().equals(pageId)).findFirst();
        }
    }

    private record Pending(Peer peer, Page page, CompletableFuture<OpenResult> result) {
    }

    private final Duration timeout;
    private final Map<String, Pending> pending = new ConcurrentHashMap<>();
    private volatile Registration registration;

    FrameShiftLink(Duration timeout) {
        this.timeout = timeout;
    }

    public static FrameShiftLink getInstance() {
        return INSTANCE;
    }

    public boolean isConnected() {
        return registration != null;
    }

    /** The pages the registered FrameShift offers, in its order; empty when none is registered. */
    public List<Page> pages() {
        Registration reg = registration;
        return reg == null ? List.of() : reg.pages();
    }

    /**
     * Asks FrameShift to open a page and waits for its answer. Blocks for at most the timeout.
     */
    public OpenResult openPage(String pageId) {
        Registration reg = registration;
        if (reg == null) return new OpenResult.Offline();
        Optional<Page> page = reg.find(pageId);
        if (page.isEmpty()) return new OpenResult.UnknownPage(pageId);

        String id = UUID.randomUUID().toString();
        CompletableFuture<OpenResult> result = new CompletableFuture<>();
        pending.put(id, new Pending(reg.peer(), page.get(), result));
        try {
            reg.peer().send(toolCall(id, page.get().id()).toString());
            return result.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            return new OpenResult.TimedOut();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new OpenResult.TimedOut();
        } catch (ExecutionException | RuntimeException e) {
            log.warn("FrameShift call failed: {}", e.getMessage());
            return new OpenResult.Offline();
        } finally {
            pending.remove(id);
        }
    }

    /** A text message from a connected program. Anything not addressed to this link is ignored. */
    public void onMessage(Peer peer, String text) {
        JsonObject message;
        try {
            JsonElement parsed = JsonParser.parseString(text);
            if (!parsed.isJsonObject()) return;
            message = parsed.getAsJsonObject();
        } catch (RuntimeException e) {
            log.debug("Ignoring non-JSON integration message");
            return;
        }
        String type = string(message, "type");
        if ("integration.register".equals(type)) {
            register(peer, message);
        } else if ("tool_result".equals(type)) {
            toolResult(peer, message);
        }
    }

    /** A connection closed: forget it if it was FrameShift, and fail its calls now rather than at the timeout. */
    public void onClose(Peer peer) {
        Registration reg = registration;
        if (reg != null && reg.peer() == peer) {
            registration = null;
            log.info("FrameShift Command disconnected");
        }
        pending.values().stream()
                .filter(p -> p.peer() == peer)
                .forEach(p -> p.result().complete(new OpenResult.Offline()));
    }

    private void register(Peer peer, JsonObject message) {
        if (!CLIENT.equals(string(message, "client"))) {
            peer.send(registered(false, "unknown_client", "Only " + CLIENT + " can register.").toString());
            return;
        }
        if (intValue(message, "version") != PROTOCOL_VERSION) {
            peer.send(registered(false, "unsupported_version", "Protocol version " + PROTOCOL_VERSION + " is supported.").toString());
            return;
        }
        List<Page> pages = pagesOf(message);
        if (pages.isEmpty()) {
            peer.send(registered(false, "no_valid_pages", "No page had a valid id.").toString());
            return;
        }
        registration = new Registration(peer, List.copyOf(pages));
        log.info("FrameShift Command registered {} pages", pages.size());
        peer.send(registered(true, null, null).toString());
    }

    private void toolResult(Peer peer, JsonObject message) {
        String id = string(message, "id");
        Pending call = id == null ? null : pending.get(id);
        // Only the connection the call went to may answer it.
        if (call == null || call.peer() != peer) return;
        if (message.has("ok") && message.get("ok").isJsonPrimitive() && message.get("ok").getAsBoolean()) {
            call.result().complete(new OpenResult.Opened(call.page()));
            return;
        }
        JsonObject error = message.has("error") && message.get("error").isJsonObject()
                ? message.getAsJsonObject("error") : new JsonObject();
        String code = string(error, "code");
        String text = string(error, "message");
        log.warn("FrameShift could not open {}: {} {}", call.page().id(), code, text);
        call.result().complete(new OpenResult.Refused(call.page(), code, text));
    }

    private static List<Page> pagesOf(JsonObject message) {
        List<Page> pages = new ArrayList<>();
        if (!message.has("pages") || !message.get("pages").isJsonArray()) return pages;
        JsonArray array = message.getAsJsonArray("pages");
        for (JsonElement element : array) {
            if (pages.size() == MAX_PAGES) break;
            if (!element.isJsonObject()) continue;
            String id = string(element.getAsJsonObject(), "id");
            if (id == null || !PAGE_ID.matcher(id).matches()) continue;
            if (pages.stream().anyMatch(p -> p.id().equals(id))) continue;
            pages.add(new Page(id, cleanLabel(string(element.getAsJsonObject(), "label"), id)));
        }
        return pages;
    }

    /** A label as plain words: unsafe characters dropped, spaces collapsed, length capped; the id when nothing is left. */
    static String cleanLabel(String label, String fallback) {
        if (label == null) return fallback;
        String clean = SPACES.matcher(LABEL_UNSAFE.matcher(label).replaceAll(" ")).replaceAll(" ").strip();
        if (clean.length() > MAX_LABEL_LENGTH) clean = clean.substring(0, MAX_LABEL_LENGTH).strip();
        return clean.isEmpty() ? fallback : clean;
    }

    private static JsonObject toolCall(String id, String pageId) {
        JsonObject call = new JsonObject();
        call.addProperty("version", PROTOCOL_VERSION);
        call.addProperty("type", "tool_call");
        call.addProperty("id", id);
        call.addProperty("target", CLIENT);
        call.addProperty("tool", TOOL);
        JsonObject arguments = new JsonObject();
        arguments.addProperty("page", pageId);
        call.add("arguments", arguments);
        return call;
    }

    private static JsonObject registered(boolean ok, String code, String text) {
        JsonObject reply = new JsonObject();
        reply.addProperty("version", PROTOCOL_VERSION);
        reply.addProperty("type", "integration.registered");
        reply.addProperty("client", CLIENT);
        reply.addProperty("ok", ok);
        if (!ok) {
            JsonObject error = new JsonObject();
            error.addProperty("code", code);
            error.addProperty("message", text);
            reply.add("error", error);
        }
        return reply;
    }

    private static String string(JsonObject object, String field) {
        JsonElement value = object.get(field);
        return value != null && value.isJsonPrimitive() ? value.getAsString() : null;
    }

    private static int intValue(JsonObject object, String field) {
        JsonElement value = object.get(field);
        try {
            return value != null && value.isJsonPrimitive() ? value.getAsInt() : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
