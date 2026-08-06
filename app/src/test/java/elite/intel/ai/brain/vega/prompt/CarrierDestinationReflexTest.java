package elite.intel.ai.brain.vega.prompt;

import elite.intel.ai.brain.actions.handlers.commands.CommandRegistry;
import elite.intel.ai.brain.actions.handlers.queries.QueryRegistry;
import elite.intel.ai.brain.vega.model.GameStateSnapshot;
import elite.intel.db.util.Database;
import elite.intel.i18n.Language;
import elite.intel.session.PlayerSituation;
import elite.intel.session.Status;
import elite.intel.session.SystemSession;
import elite.intel.util.Cypher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Guards "enter next fleet carrier destination", which came back as an argument instead of typing the leg.
 *
 * <p>The aliases only owned "enter carrier destination", so the spoken "next" form matched nothing verbatim
 * and fell through to the local model. The reducer kept eight carrier-ish candidates, and the model - reading
 * a description that talks about a destination system - decided a system name was missing and asked for it
 * with {@code request_input}, for a command that declares no parameters at all (it reads the next leg from
 * the stored carrier route). The retry then answered the order with sarcasm.
 *
 * <p>Both reported utterances now belong to the command verbatim, so the order runs with no model in the loop.
 * The bare noun stays with {@code query_carrier_voyage}: asking where the carrier is going is a question.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CarrierDestinationReflexTest {

    private ReflexResolver resolver;

    @BeforeAll
    void boot() throws Exception {
        Cypher.initializeKey();
        Database.init().close();
        CommandRegistry.getInstance().load();
        QueryRegistry.getInstance().load();
        SystemSession.getInstance().setLanguage(Language.EN);
        resolver = new ReflexResolver();
    }

    @Test
    void bothReportedUtterancesTypeTheNextLeg() {
        assertEquals("enter_fleet_carrier_destination",
                resolve("enter next fleet carrier destination").orElseThrow().actionId());
        assertEquals("enter_fleet_carrier_destination",
                resolve("enter next fleet destination").orElseThrow().actionId());
    }

    @Test
    void theBareNounStillAsksWhereTheCarrierIsGoing() {
        assertEquals("query_carrier_voyage", resolve("carrier destination").orElseThrow().actionId());
    }

    /**
     * The commander was on foot in a concourse; this command is deliberately visible everywhere.
     */
    private Optional<ReflexResolver.Reflex> resolve(String utterance) {
        return resolver.resolve(utterance, GameStateSnapshot.capture(
                Status.detached(PlayerSituation.ON_FOOT_SOCIAL)));
    }
}
