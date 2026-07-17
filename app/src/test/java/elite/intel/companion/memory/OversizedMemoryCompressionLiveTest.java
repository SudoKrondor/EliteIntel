package elite.intel.companion.memory;

import elite.intel.companion.CompanionRuntimeGeneration;
import elite.intel.companion.llm.CompanionLlmGatewayFactory;
import elite.intel.companion.llm.LlmGateway;
import elite.intel.companion.model.memory.MemoryRecord;
import elite.intel.db.util.Database;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;
import elite.intel.util.Cypher;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Opt-in verification of record-level gist compression against the configured live companion LLM. */
@Tag("local-integration")
class OversizedMemoryCompressionLiveTest {

    @Test
    void liveModelStoresBoundedTradeRouteGistWithoutReasoningLeak() {
        Cypher.initializeKey();
        Database.init().close();
        SystemSession session = SystemSession.getInstance();
        Language previousLanguage = session.getLanguage();
        session.setLanguage(Language.RU);
        String answer = "В настоящее время задействован торговый маршрут. Первая нога идет из системы Хранит, "
                + "станция Янг Энтерпрайз, в систему Синуфе Эм Пи У Би Четыре Девять Два, станция Леоникено "
                + "Прогресс. Вторая нога идет из системы Синуфе Эм Пи У Би Четыре Девять Два, станция Леоникено "
                + "Прогресс, в систему Кубео, станция Медупе Сити.";

        try (LlmGateway live = CompanionLlmGatewayFactory.create()) {
            SessionMemoryGateway memory = new SessionMemoryGateway(text -> 0);
            try (OversizedMemoryCompressor compressor = new OversizedMemoryCompressor(
                    memory, live, new CompanionRuntimeGeneration(), Runnable::run)) {
                memory.setOversizedMemoryListener(compressor);
                memory.write(MemoryRecord.query(Instant.now(), "Какой сейчас торговый маршрут?", answer));

                String gist = memory.readRecentHistory().getFirst().companionText();
                System.out.println("LIVE MEMORY GIST (" + gist.length() + " chars): " + gist);
                assertTrue(gist.length() <= CompanionMemoryPolicy.entryMaxChars(), gist);
                assertFalse(gist.endsWith("..."), "live compression fell back to hard bounding: " + gist);
                assertFalse(gist.contains("The user"), "model reasoning leaked into memory: " + gist);
                assertTrue(gist.contains("Хранит"), "the route origin was lost: " + gist);
                assertTrue(gist.contains("Синуфе"), "the intermediate system was lost: " + gist);
                assertTrue(gist.contains("492") || gist.contains("Четыре Девять Два"),
                        "the system identifier was lost or changed: " + gist);
                assertTrue(gist.contains("Кубео"), "the route destination was lost: " + gist);
            }
        } finally {
            session.setLanguage(previousLanguage);
        }
    }
}
