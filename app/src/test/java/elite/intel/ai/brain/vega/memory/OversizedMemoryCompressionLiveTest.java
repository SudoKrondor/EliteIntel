package elite.intel.ai.brain.vega.memory;

import elite.intel.ai.brain.vega.VegaRuntimeGeneration;
import elite.intel.ai.brain.vega.llm.LlmGateway;
import elite.intel.ai.brain.vega.llm.VegaLlmGatewayFactory;
import elite.intel.ai.brain.vega.model.memory.MemoryRecord;
import elite.intel.db.util.Database;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;
import elite.intel.util.Cypher;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Opt-in verification of record-level gist compression against the configured live VEGA LLM.
 */
@Tag("local-integration")
class OversizedMemoryCompressionLiveTest {

    @Test
    void liveModelStoresBoundedTradeRouteGistWithoutReasoningLeak() {
        Cypher.initializeKey();
        Database.init().close();
        SystemSession session = SystemSession.getInstance();
        Language previousLanguage = session.getLanguage();
        session.setLanguage(Language.EN);
        String answer = "A trade route is currently engaged. The first leg runs from the Hranit system, "
                + "Young Enterprise station, to the Synuefe E M P U B Four Nine Two system, Leonikeno "
                + "Progress station. The second leg runs from the Synuefe E M P U B Four Nine Two system, Leonikeno "
                + "Progress station, to the Kubeo system, Medupe City station.";

        try (LlmGateway live = VegaLlmGatewayFactory.create()) {
            SessionMemoryGateway memory = new SessionMemoryGateway(text -> 0);
            try (OversizedMemoryCompressor compressor = new OversizedMemoryCompressor(
                    memory, live, new VegaRuntimeGeneration(), Runnable::run)) {
                memory.setOversizedMemoryListener(compressor);
                memory.write(MemoryRecord.query(Instant.now(), "What is the trade route right now?", answer));

                String gist = memory.readRecentHistory().getFirst().vegaText();
                System.out.println("LIVE MEMORY GIST (" + gist.length() + " chars): " + gist);
                assertTrue(gist.length() <= VegaMemoryPolicy.entryMaxChars(), gist);
                assertFalse(gist.endsWith("..."), "live compression fell back to hard bounding: " + gist);
                assertFalse(gist.contains("The user"), "model reasoning leaked into memory: " + gist);
                assertTrue(gist.contains("Hranit"), "the route origin was lost: " + gist);
                assertTrue(gist.contains("Synuefe"), "the intermediate system was lost: " + gist);
                assertTrue(gist.contains("492") || gist.contains("Four Nine Two"),
                        "the system identifier was lost or changed: " + gist);
                assertTrue(gist.contains("Kubeo"), "the route destination was lost: " + gist);
            }
        } finally {
            session.setLanguage(previousLanguage);
        }
    }
}
