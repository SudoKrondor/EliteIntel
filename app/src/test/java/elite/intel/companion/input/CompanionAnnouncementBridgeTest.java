package elite.intel.companion.input;

import elite.intel.ai.mouth.subscribers.events.AiVoxResponseEvent;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.companion.CompanionRuntime;
import elite.intel.companion.mind.CompanionState;
import elite.intel.companion.mind.VerbatimNarrationSink;
import elite.intel.companion.model.ConversationTopic;
import elite.intel.companion.model.Urgency;
import elite.intel.db.util.Database;
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
 * synchronous emitter's completion future so it waits for playback. A capturing {@link VerbatimNarrationSink}
 * keeps this off the real lanes.
 */
class CompanionAnnouncementBridgeTest {

    /** The bridge eagerly resolves PlayerSession (DB-backed); the in-memory test DB satisfies it. */
    @BeforeAll
    static void initDb() {
        Cypher.initializeKey();
        Database.init().close(); // init() returns an open pooled handle; close it so the pool isn't starved
    }

    private record Submission(String text, ConversationTopic topic, Urgency urgency, CompletableFuture<Void> signal) {}

    private final List<Submission> submissions = new ArrayList<>();
    private final VerbatimNarrationSink sink = new VerbatimNarrationSink() {
        @Override public void submitVerbatimNarration(String text, ConversationTopic topic) {
            submissions.add(new Submission(text, topic, null, null));
        }
        @Override public void submitVerbatimNarration(String text, ConversationTopic topic, Urgency urgency,
                                                      CompletableFuture<Void> signal) {
            submissions.add(new Submission(text, topic, urgency, signal));
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
    }
}
