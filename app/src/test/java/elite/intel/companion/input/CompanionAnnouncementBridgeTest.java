package elite.intel.companion.input;

import elite.intel.ai.mouth.subscribers.events.AiVoxResponseEvent;
import elite.intel.ai.mouth.subscribers.events.DiscoveryAnnouncementEvent;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.ai.mouth.subscribers.events.RadarContactAnnouncementEvent;
import elite.intel.companion.CompanionRuntime;
import elite.intel.companion.execution.ActiveToolCall;
import elite.intel.companion.mind.CompanionState;
import elite.intel.companion.mind.VerbatimNarrationSink;
import elite.intel.companion.model.ConversationTopic;
import elite.intel.companion.model.Urgency;
import elite.intel.db.util.Database;
import elite.intel.session.PlayerSession;
import elite.intel.util.Cypher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The bridge that hands a command/macro's own narration ({@code AiVoxResponseEvent} /
 * {@code MissionCriticalAnnouncementEvent}) to the companion: it tags both with the current global topic,
 * voices a mission-critical line urgently and a normal AI response at normal urgency, and forwards a
 * synchronous emitter's completion future so it waits for playback. It also checks the curated-announcement
 * routing: a radar contact preempts current speech (urgent) while a routine discovery callout queues at
 * normal urgency. A capturing {@link VerbatimNarrationSink} keeps this off the real lanes.
 */
class CompanionAnnouncementBridgeTest {

    /** The bridge eagerly resolves PlayerSession (DB-backed); the in-memory test DB satisfies it. */
    @BeforeAll
    static void initDb() {
        Cypher.initializeKey();
        Database.init().close(); // init() returns an open pooled handle; close it so the pool isn't starved
    }

    private record Submission(String text, ConversationTopic topic, Urgency urgency, CompletableFuture<Void> signal,
                              String toolCallId) {}

    private final List<Submission> submissions = new ArrayList<>();
    private final VerbatimNarrationSink sink = new VerbatimNarrationSink() {
        @Override public void submitVerbatimNarration(String text, ConversationTopic topic) {
            // Mirror the real dispatcher default: routine announcements queue at NORMAL urgency, so tests can
            // assert the resulting urgency instead of a null capture artifact.
            submissions.add(new Submission(text, topic, Urgency.NORMAL, null, null));
        }
        @Override public void submitVerbatimNarration(String text, ConversationTopic topic, Urgency urgency,
                                                      CompletableFuture<Void> signal) {
            submissions.add(new Submission(text, topic, urgency, signal, null));
        }
        @Override public void submitVerbatimNarration(String text, ConversationTopic topic, Urgency urgency,
                                                      CompletableFuture<Void> signal, String toolCallId) {
            submissions.add(new Submission(text, topic, urgency, signal, toolCallId));
        }
    };

    @BeforeEach
    void installGlobalTopic() {
        CompanionState state = new CompanionState();
        state.setGlobalTopic(ConversationTopic.TRADE);
        CompanionRuntime.install(null, null, null, null, null, state);
    }

    @AfterEach
    void clearRuntime() {
        CompanionRuntime.clear();
    }

    @Test
    void missionCriticalNarrationIsUrgentUnderTheGlobalTopic() {
        new CompanionAnnouncementBridge(sink).onMissionCritical(new MissionCriticalAnnouncementEvent("hull breach"));

        assertEquals(1, submissions.size());
        Submission s = submissions.get(0);
        assertEquals("hull breach", s.text());
        assertEquals(ConversationTopic.TRADE, s.topic(), "tagged with the current global topic");
        assertEquals(Urgency.URGENT, s.urgency(), "a mission-critical line preempts current speech");
        assertNull(s.signal(), "a fire-and-forget mission-critical line carries no completion signal");
    }

    @Test
    void aiVoxNarrationIsNormalAndForwardsTheCompletionFuture() {
        CompletableFuture<Void> done = new CompletableFuture<>();

        new CompanionAnnouncementBridge(sink).onAiVoxResponse(new AiVoxResponseEvent("route plotted", done));

        assertEquals(1, submissions.size());
        Submission s = submissions.get(0);
        assertEquals("route plotted", s.text());
        assertEquals(ConversationTopic.TRADE, s.topic());
        assertEquals(Urgency.NORMAL, s.urgency());
        assertSame(done, s.signal(),
                "the synchronous emitter's completion future is forwarded so it waits for playback");
        assertNull(s.toolCallId(), "no tool-call active on this thread -> recorded as free-standing speech");
    }

    @Test
    void radarContactPreemptsAtUrgentUrgency() {
        PlayerSession.getInstance().setRadarContactAnnouncementOn(true);

        new CompanionAnnouncementBridge(sink).onRadarContact(new RadarContactAnnouncementEvent("hostile contact"));

        assertEquals(1, submissions.size());
        Submission s = submissions.get(0);
        assertEquals("hostile contact", s.text());
        assertEquals(ConversationTopic.COMBAT, s.topic());
        assertEquals(Urgency.URGENT, s.urgency(), "a radar contact preempts a routine callout in flight");
    }

    @Test
    void discoveryNarrationQueuesAtNormalUrgency() {
        PlayerSession.getInstance().setDiscoveryAnnouncementOn(true);

        new CompanionAnnouncementBridge(sink).onDiscovery(new DiscoveryAnnouncementEvent("new water world"));

        assertEquals(1, submissions.size());
        Submission s = submissions.get(0);
        assertEquals("new water world", s.text());
        assertEquals(ConversationTopic.EXPLORATION, s.topic());
        assertEquals(Urgency.NORMAL, s.urgency(), "a routine discovery callout queues instead of cutting off speech");
    }

    @Test
    void aiVoxNarrationInheritsTheActiveToolCallId() {
        // The synchronous bus runs the subscriber on the publishing thread, so the tool-call being settled is
        // visible via ActiveToolCall - and is forwarded so the narration is recorded as that call's tool result.
        CompanionAnnouncementBridge bridge = new CompanionAnnouncementBridge(sink);
        ActiveToolCall.runWith("call-7",
                () -> bridge.onAiVoxResponse(new AiVoxResponseEvent("fuel at 40 percent")));

        assertEquals(1, submissions.size());
        assertEquals("call-7", submissions.get(0).toolCallId());
    }
}
